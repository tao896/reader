package com.htmake.reader.api.controller

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RssArticle
import io.legado.app.model.webBook.WebBook
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler;
import mu.KotlinLogging
import com.htmake.reader.config.AppConfig
import com.htmake.reader.config.BookConfig
import io.legado.app.constant.DeepinkBookSource
import com.htmake.reader.utils.error
import com.htmake.reader.utils.success
import com.htmake.reader.utils.getStorage
import com.htmake.reader.utils.saveStorage
import com.htmake.reader.utils.asJsonArray
import com.htmake.reader.utils.asJsonObject
import com.htmake.reader.utils.toDataClass
import com.htmake.reader.utils.toMap
import com.htmake.reader.utils.fillData
import com.htmake.reader.utils.getWorkDir
import com.htmake.reader.utils.getRandomString
import com.htmake.reader.utils.genEncryptedPassword
import com.htmake.reader.entity.User
import com.htmake.reader.utils.SpringContextUtils
import com.htmake.reader.utils.deleteRecursively
import com.htmake.reader.utils.unzip
import com.htmake.reader.utils.zip
import com.htmake.reader.utils.jsonEncode
import com.htmake.reader.utils.getRelativePath
import com.htmake.reader.verticle.RestVerticle
import com.htmake.reader.SpringEvent
import org.springframework.stereotype.Component
import io.vertx.core.json.JsonObject
import io.vertx.core.json.JsonArray
import io.vertx.core.http.HttpMethod
import com.htmake.reader.api.ReturnData
import io.legado.app.utils.MD5Utils
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.UUID;
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.HttpResponse
import io.vertx.core.buffer.Buffer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import java.io.File
import java.lang.Runtime
import kotlin.collections.mutableMapOf
import kotlin.system.measureTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat;
import io.legado.app.utils.EncoderUtils
import io.legado.app.model.rss.Rss
import org.springframework.scheduling.annotation.Scheduled
import io.legado.app.model.localBook.LocalBook
import java.nio.file.Paths
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private val logger = KotlinLogging.logger {}
private const val BOOK_SOURCE_SUBSCRIPTION_STORAGE_KEY = "bookSourceSubscriptions"

class BookSourceController(coroutineContext: CoroutineContext): BaseController(coroutineContext) {
    private var webClient: WebClient

    init {
        webClient = SpringContextUtils.getBean("webClient", WebClient::class.java)
    }

    suspend fun getUserBookSourceJson(userNameSpace: String): JsonArray? {
        var bookSourceList: JsonArray? = asJsonArray(getUserStorage(userNameSpace, "bookSource"))
        if (bookSourceList == null && !userNameSpace.equals("default")) {
            // 用户书源文件不存在，拷贝系统书源
            var systemBookSourceList: JsonArray? = asJsonArray(getUserStorage("default", "bookSource"))
            if (systemBookSourceList != null) {
                saveUserStorage(userNameSpace, "bookSource", systemBookSourceList.getList())
                bookSourceList = systemBookSourceList
            }
        }
        return bookSourceList
    }

    private fun normalizeSubscriptionUrl(url: String?): String {
        return url?.trim() ?: ""
    }

    private fun isValidSubscriptionUrl(url: String): Boolean {
        return try {
            val protocol = URL(url).protocol
            protocol.equals("http", true) || protocol.equals("https", true)
        } catch (e: MalformedURLException) {
            false
        }
    }

    private fun defaultSubscriptionName(url: String): String {
        return try {
            URL(url).host.ifEmpty { url }
        } catch (e: Exception) {
            url
        }
    }

    private fun getUserBookSourceSubscriptions(userNameSpace: String): JsonArray {
        return asJsonArray(getUserStorage(userNameSpace, BOOK_SOURCE_SUBSCRIPTION_STORAGE_KEY)) ?: JsonArray()
    }

    private fun saveUserBookSourceSubscriptions(userNameSpace: String, list: JsonArray) {
        saveUserStorage(userNameSpace, BOOK_SOURCE_SUBSCRIPTION_STORAGE_KEY, list)
    }

    private fun findSubscriptionIndex(list: JsonArray, url: String): Int {
        for (i in 0 until list.size()) {
            if (normalizeSubscriptionUrl(list.getJsonObject(i).getString("url", "")).equals(url)) {
                return i
            }
        }
        return -1
    }

    private fun normalizeSubscription(subscription: JsonObject): JsonObject {
        val now = System.currentTimeMillis()
        val url = normalizeSubscriptionUrl(subscription.getString("url", ""))
        val name = subscription.getString("name", "").trim().ifEmpty { defaultSubscriptionName(url) }
        return JsonObject()
            .put("name", name)
            .put("url", url)
            .put("createdAt", subscription.getLong("createdAt", now))
            .put("updatedAt", subscription.getLong("updatedAt", now))
            .put("lastSyncAt", subscription.getLong("lastSyncAt", 0L))
            .put("lastStatus", subscription.getString("lastStatus", ""))
            .put("lastError", subscription.getString("lastError", ""))
            .put("lastSourceCount", subscription.getInteger("lastSourceCount", 0))
    }

    private suspend fun fetchSubscriptionBody(url: String): Pair<String?, String?> {
        return suspendCancellableCoroutine { cont ->
            webClient.getAbs(url).timeout(10000).send { result ->
                if (result.failed()) {
                    cont.resume(Pair(null, result.cause()?.message ?: "订阅请求失败"))
                    return@send
                }
                val response: HttpResponse<Buffer> = result.result()
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    cont.resume(Pair(null, "HTTP ${response.statusCode()} ${response.statusMessage()}".trim()))
                    return@send
                }
                val body = response.bodyAsString()
                if (body.isNullOrBlank()) {
                    cont.resume(Pair(null, "订阅内容为空"))
                    return@send
                }
                cont.resume(Pair(body, null))
            }
        }
    }

    private suspend fun saveBookSourceObjects(userNameSpace: String, sources: List<BookSource>): Map<String, Any> {
        var bookSourceList: JsonArray = getUserBookSourceJson(userNameSpace) ?: JsonArray()
        var addCount = 0
        var updateCount = 0
        for (bookSource in sources) {
            var existIndex = -1
            for (i in 0 until bookSourceList.size()) {
                val existedBookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                if (existedBookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                    existIndex = i
                    break
                }
            }
            if (existIndex >= 0) {
                val sourceList = bookSourceList.getList()
                sourceList.set(existIndex, JsonObject.mapFrom(bookSource))
                bookSourceList = JsonArray(sourceList)
                updateCount += 1
            } else {
                bookSourceList.add(JsonObject.mapFrom(bookSource))
                addCount += 1
            }
        }
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        return mapOf("importCount" to sources.size, "addCount" to addCount, "updateCount" to updateCount)
    }

    suspend fun getBookSourceSubscriptions(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val userNameSpace = getUserNameSpace(context)
        val subscriptionList = getUserBookSourceSubscriptions(userNameSpace)
        val list = arrayListOf<Map<String, Any?>>()
        for (i in 0 until subscriptionList.size()) {
            list.add(normalizeSubscription(subscriptionList.getJsonObject(i)).map)
        }
        return returnData.setData(list)
    }

    suspend fun saveBookSourceSubscription(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val body = context.bodyAsJson ?: JsonObject()
        val url = normalizeSubscriptionUrl(body.getString("url", ""))
        if (url.isEmpty()) {
            return returnData.setErrorMsg("订阅链接不能为空")
        }
        if (!isValidSubscriptionUrl(url)) {
            return returnData.setErrorMsg("订阅链接只支持 http/https")
        }

        val userNameSpace = getUserNameSpace(context)
        val subscriptionList = getUserBookSourceSubscriptions(userNameSpace)
        val now = System.currentTimeMillis()
        val existIndex = findSubscriptionIndex(subscriptionList, url)
        val existed = if (existIndex >= 0) subscriptionList.getJsonObject(existIndex) else JsonObject()
        val subscription = normalizeSubscription(existed)
            .put("name", body.getString("name", "").trim().ifEmpty { defaultSubscriptionName(url) })
            .put("url", url)
            .put("createdAt", existed.getLong("createdAt", now))
            .put("updatedAt", now)

        if (existIndex >= 0) {
            val list = subscriptionList.getList()
            list.set(existIndex, subscription)
            saveUserBookSourceSubscriptions(userNameSpace, JsonArray(list))
        } else {
            subscriptionList.add(subscription)
            saveUserBookSourceSubscriptions(userNameSpace, subscriptionList)
        }
        return returnData.setData(subscription.map)
    }

    suspend fun deleteBookSourceSubscription(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val body = context.bodyAsJson ?: JsonObject()
        val url = normalizeSubscriptionUrl(body.getString("url", ""))
        if (url.isEmpty()) {
            return returnData.setErrorMsg("订阅链接不能为空")
        }

        val userNameSpace = getUserNameSpace(context)
        val subscriptionList = getUserBookSourceSubscriptions(userNameSpace)
        val existIndex = findSubscriptionIndex(subscriptionList, url)
        if (existIndex >= 0) {
            subscriptionList.remove(existIndex)
            saveUserBookSourceSubscriptions(userNameSpace, subscriptionList)
        }
        return returnData.setData("")
    }

    suspend fun updateBookSourceSubscription(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val body = context.bodyAsJson ?: JsonObject()
        val url = normalizeSubscriptionUrl(body.getString("url", ""))
        if (url.isEmpty()) {
            return returnData.setErrorMsg("订阅链接不能为空")
        }
        if (!isValidSubscriptionUrl(url)) {
            return returnData.setErrorMsg("订阅链接只支持 http/https")
        }

        val userNameSpace = getUserNameSpace(context)
        val subscriptionList = getUserBookSourceSubscriptions(userNameSpace)
        val existIndex = findSubscriptionIndex(subscriptionList, url)
        if (existIndex < 0) {
            return returnData.setErrorMsg("订阅不存在")
        }

        val now = System.currentTimeMillis()
        var subscription = normalizeSubscription(subscriptionList.getJsonObject(existIndex))
            .put("lastSyncAt", now)
        val (bodyString, fetchError) = fetchSubscriptionBody(url)
        if (fetchError != null || bodyString == null) {
            subscription = subscription
                .put("lastStatus", "error")
                .put("lastError", fetchError ?: "订阅请求失败")
                .put("updatedAt", now)
            val list = subscriptionList.getList()
            list.set(existIndex, subscription)
            saveUserBookSourceSubscriptions(userNameSpace, JsonArray(list))
            return returnData.setErrorMsg(subscription.getString("lastError"))
        }

        val sources = BookSource.fromJsonArray(bodyString).getOrElse {
            val errorMessage = it.localizedMessage ?: "订阅内容不是有效书源 JSON"
            subscription = subscription
                .put("lastStatus", "error")
                .put("lastError", errorMessage)
                .put("updatedAt", now)
            val list = subscriptionList.getList()
            list.set(existIndex, subscription)
            saveUserBookSourceSubscriptions(userNameSpace, JsonArray(list))
            return returnData.setErrorMsg(errorMessage)
        }.filter { it.bookSourceUrl.isNotBlank() }

        if (sources.isEmpty()) {
            val errorMessage = "订阅内容未包含有效书源"
            subscription = subscription
                .put("lastStatus", "error")
                .put("lastError", errorMessage)
                .put("updatedAt", now)
            val list = subscriptionList.getList()
            list.set(existIndex, subscription)
            saveUserBookSourceSubscriptions(userNameSpace, JsonArray(list))
            return returnData.setErrorMsg(errorMessage)
        }

        val importResult = saveBookSourceObjects(userNameSpace, sources)
        subscription = subscription
            .put("lastStatus", "success")
            .put("lastError", "")
            .put("lastSourceCount", sources.size)
            .put("updatedAt", now)
        val list = subscriptionList.getList()
        list.set(existIndex, subscription)
        saveUserBookSourceSubscriptions(userNameSpace, JsonArray(list))
        return returnData.setData(importResult + mapOf("subscription" to subscription.map))
    }

    suspend fun saveBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSource = BookSource.fromJson(context.bodyAsString).getOrNull()
        if (bookSource == null) {
            return returnData.setErrorMsg("参数错误")
        }
        // val bookSource = context.bodyAsJson.mapTo(BookSource::class.java)

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        // 遍历判断书本是否存在
        var existIndex: Int = -1
        for (i in 0 until bookSourceList.size()) {
            var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
            if (_bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                existIndex = i
                break;
            }
        }
        if (existIndex >= 0) {
            var sourceList = bookSourceList.getList()
            sourceList.set(existIndex, JsonObject.mapFrom(bookSource))
            bookSourceList = JsonArray(sourceList)
        } else {
            bookSourceList.add(JsonObject.mapFrom(bookSource))
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        return returnData.setData("")
    }

    suspend fun saveBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSourceJsonArray = context.bodyAsJsonArray
        if (bookSourceJsonArray == null) {
            return returnData.setErrorMsg("参数错误")
        }
        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        for (k in 0 until bookSourceJsonArray.size()) {
            val bookSource = BookSource.fromJson(bookSourceJsonArray.getJsonObject(k).toString()).getOrNull()
            if (bookSource == null) {
                continue
            }
            // var bookSource = bookSourceJsonArray.getJsonObject(k).mapTo(BookSource::class.java)
            // 遍历判断书本是否存在
            var existIndex: Int = -1
            for (i in 0 until bookSourceList!!.size()) {
                var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                if (_bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                    existIndex = i
                    break;
                }
            }
            if (existIndex >= 0) {
                var sourceList = bookSourceList.getList()
                sourceList.set(existIndex, JsonObject.mapFrom(bookSource))
                bookSourceList = JsonArray(sourceList)
            } else {
                bookSourceList.add(JsonObject.mapFrom(bookSource))
            }
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList!!)
        return returnData.setData("")
    }

    suspend fun getBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        checkAuth(context)
        var bookSourceUrl: String
        if (context.request().method() == HttpMethod.POST) {
            // post 请求
            bookSourceUrl = context.bodyAsJson.getString("bookSourceUrl")
        } else {
            // get 请求
            bookSourceUrl = context.queryParam("bookSourceUrl").firstOrNull() ?: ""
        }
        if (bookSourceUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("书源链接不能为空")
        }

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        // 遍历判断书本是否存在
        var existIndex: Int = -1
        for (i in 0 until bookSourceList.size()) {
            var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
            if (_bookSource.bookSourceUrl.equals(bookSourceUrl)) {
                existIndex = i
                break;
            }
        }
        if (existIndex < 0) {
            return returnData.setErrorMsg("书源信息不存在")
        }

        return returnData.setData(bookSourceList.getJsonObject(existIndex).map)
    }

    suspend fun getBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        checkAuth(context)
        var simple: Int = 0
        if (context.request().method() == HttpMethod.POST) {
            // post 请求
            simple = context.bodyAsJson.getInteger("simple", 0)
        } else {
            // get 请求
            simple = context.queryParam("simple").firstOrNull()?.toInt() ?: 0
        }
        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList != null) {
            if (simple > 0) {
                var list = arrayListOf<Map<String, Any?>>()
                for (i in 0 until bookSourceList.size()) {
                    var bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                    list.add(mapOf<String, Any?>(
                        "bookSourceGroup" to bookSource.bookSourceGroup,
                        "bookSourceName" to bookSource.bookSourceName,
                        "bookSourceUrl" to bookSource.bookSourceUrl,
                        "exploreUrl" to bookSource.exploreUrl
                    ))
                }
                return returnData.setData(list)
            }
            return returnData.setData(bookSourceList.getList())
        }
        return returnData.setData(arrayListOf<Int>())
    }

    suspend fun deleteBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSource = context.bodyAsJson.mapTo(BookSource::class.java)

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        // 遍历判断书本是否存在
        var existIndex: Int = -1
        for (i in 0 until bookSourceList.size()) {
            var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
            if (_bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                existIndex = i
                break;
            }
        }
        if (existIndex >= 0) {
            bookSourceList.remove(existIndex)
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        return returnData.setData("")
    }

    suspend fun deleteBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSourceJsonArray = context.bodyAsJsonArray

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        for (k in 0 until bookSourceJsonArray.size()) {
            var bookSource = bookSourceJsonArray.getJsonObject(k).mapTo(BookSource::class.java)
            // 遍历判断书本是否存在
            var existIndex: Int = -1
            for (i in 0 until bookSourceList.size()) {
                var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                if (_bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                    existIndex = i
                    break;
                }
            }
            if (existIndex >= 0) {
                bookSourceList.remove(existIndex)
            }
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        return returnData.setData("")
    }

    suspend fun deleteAllBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        var userNameSpace = getUserNameSpace(context)
        saveUserStorage(userNameSpace, "bookSource", JsonArray())
        return returnData.setData("")
    }

    suspend fun setAsDefaultBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        if (!checkManagerAuth(context)) {
            return returnData.setData("NEED_SECURE_KEY").setErrorMsg("请输入管理密码")
        }
        var username = context.bodyAsJson.getString("username")
        var bookSourceList: JsonArray? = asJsonArray(getUserStorage(username, "bookSource"))
        if (bookSourceList == null) {
            return returnData.setErrorMsg("用户书源不存在")
        }

        // 保存为默认书源
        saveUserStorage("default", "bookSource", bookSourceList.getList())
        return returnData.setData("设置默认书源成功")
    }

    suspend fun readSourceFile(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (context.fileUploads() == null || context.fileUploads().isEmpty()) {
            return returnData.setErrorMsg("请上传文件")
        }
        var sourceList = JsonArray()
        context.fileUploads().forEach {
            // logger.info("readSourceFile: {}", it.uploadedFileName())
            var file = File(it.uploadedFileName())
            if (file.exists()) {
                sourceList.add(file.readText())
                file.delete()
            }
        }
        return returnData.setData(sourceList.getList())
    }

    suspend fun readRemoteSourceFile(context: RoutingContext) {
        val returnData = ReturnData()
        var url: String
        if (context.request().method() == HttpMethod.POST) {
            // post 请求
            url = context.bodyAsJson.getString("url") ?: ""
        } else {
            // get 请求
            url = context.queryParam("url").firstOrNull() ?: ""
        }
        if (url.isNullOrEmpty()) {
            context.success(returnData.setErrorMsg("请输入远程书源链接"))
            return
        }

        launch(Dispatchers.IO) {
            webClient.getAbs(url).timeout(3000).send {
                var body = it.result()?.bodyAsString()
                if (body != null) {
                    context.success(returnData.setData(arrayListOf(body)))
                } else {
                    context.success(returnData.setErrorMsg("远程书源链接错误"))
                }
            }
        }
    }

    suspend fun deleteUserBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        if (!checkManagerAuth(context)) {
            return returnData.setData("NEED_SECURE_KEY").setErrorMsg("请输入管理密码")
        }
        val userJsonArray = context.bodyAsJsonArray
        for (i in 0 until userJsonArray.size()) {
            var username = userJsonArray.getString(i)
            var userBookSourceFile = File(getWorkDir("storage", "data", username, "bookSource.json"))
            // 删除用户书源文件，恢复默认书源
            if (userBookSourceFile.exists()) {
                userBookSourceFile.deleteRecursively()
            }
        }
        return returnData.setData("删除书源成功")
    }

    suspend fun deleteBookSourcesFile(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        var userNameSpace = getUserNameSpace(context)
        var userBookSourceFile = File(getWorkDir("storage", "data", userNameSpace, "bookSource.json"))
        // 删除用户书源文件，恢复默认书源
        if (userBookSourceFile.exists()) {
            userBookSourceFile.deleteRecursively()
        }
        return returnData.setData("")
    }
}
