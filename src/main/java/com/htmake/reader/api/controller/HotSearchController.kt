package com.htmake.reader.api.controller

import com.htmake.reader.api.ReturnData
import com.htmake.reader.hotsearch.*
import io.vertx.ext.web.RoutingContext
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

class HotSearchController(override val coroutineContext: CoroutineContext) : BaseController(coroutineContext) {

    private val weiboProvider = WeiboHotSearchProvider()
    private val providers: List<HotSearchProvider> = listOf(
        weiboProvider,
        ZhihuHotSearchProvider(),
        BaiduHotSearchProvider()
    )
    private val providerMap = providers.associateBy { it.platformConfig().platformId }
    private val cache = HotSearchCache()
    private val weiboSessions = WeiboSessionStore()

    suspend fun getHotSearchOptions(context: RoutingContext): ReturnData {
        if (!checkAuth(context)) {
            return ReturnData().setErrorMsg("请先登录 Reader")
        }
        val platforms = providers.map {
            val config = it.platformConfig()
            mapOf("platformId" to config.platformId, "platformName" to config.platformName)
        }
        return ReturnData().setData(
            mapOf(
                "platforms" to platforms,
                "defaultPlatform" to providers.first().platformConfig().platformId,
                "weiboSession" to weiboSessionData(context)
            )
        )
    }

    suspend fun getHotSearches(context: RoutingContext): ReturnData {
        if (!checkAuth(context)) {
            return ReturnData().setErrorMsg("请先登录 Reader")
        }
        val platform = context.queryParam("platform").firstOrNull().orEmpty()
        val refreshValue = context.queryParam("refresh").firstOrNull()
        val validationError = validateRequest(providerMap, platform, refreshValue)
        if (validationError != null) {
            return ReturnData().setErrorMsg(validationError)
        }

        val provider = providerMap[platform]!!
        val sessionId = context.session().id()
        val cacheKey = if (platform == "weibo") "weibo/$sessionId" else platform
        val weiboCookie = if (platform == "weibo") weiboSessions.get(sessionId) else null
        if (platform == "weibo" && weiboCookie == null) {
            cache.remove(cacheKey)
            return ReturnData().setErrorMsg("请先配置微博登录 Cookie")
        }
        return try {
            val entry = cache.getOrFetch(cacheKey, forceRefresh = refreshValue == "true") {
                if (platform == "weibo") {
                    weiboProvider.fetchPersonalized(weiboCookie!!)
                } else {
                    provider.fetchHotSearches()
                }
            }
            ReturnData().setData(
                mapOf(
                    "platform" to platform,
                    "items" to entry.result.items,
                    "source" to entry.result.source,
                    "sourceName" to entry.result.sourceName,
                    "fetchedAt" to entry.fetchedAt,
                    "stale" to entry.stale
                )
            )
        } catch (e: HotSearchAuthenticationException) {
            weiboSessions.remove(sessionId)
            cache.remove(cacheKey)
            ReturnData().setErrorMsg(e.message ?: "微博登录已失效，请重新配置")
        } catch (e: Exception) {
            logger.error("Hot search fetch failed for $platform: ${e.message}")
            ReturnData().setErrorMsg("获取 ${provider.platformConfig().platformName} 热搜失败: ${e.message}")
        }
    }

    suspend fun getWeiboHotSearchSession(context: RoutingContext): ReturnData {
        if (!checkAuth(context)) {
            return ReturnData().setErrorMsg("请先登录 Reader")
        }
        return ReturnData().setData(weiboSessionData(context))
    }

    suspend fun setWeiboHotSearchSession(context: RoutingContext): ReturnData {
        if (!checkAuth(context)) {
            return ReturnData().setErrorMsg("请先登录 Reader")
        }
        val cookie = try {
            normalizeCookie(context.bodyAsJson.getString("cookie", ""))
        } catch (e: IllegalArgumentException) {
            return ReturnData().setErrorMsg(e.message ?: "无效的微博 Cookie")
        }

        return try {
            val result = weiboProvider.fetchPersonalized(cookie)
            val sessionId = context.session().id()
            val status = weiboSessions.put(sessionId, cookie)
            cache.getOrFetch("weibo/$sessionId", forceRefresh = true) { result }
            ReturnData().setData(
                mapOf(
                    "configured" to status.configured,
                    "expiresAt" to status.expiresAt
                )
            )
        } catch (e: HotSearchAuthenticationException) {
            ReturnData().setErrorMsg("微博 Cookie 无效或登录已失效")
        } catch (e: Exception) {
            logger.error("Weibo session validation failed: ${e.message}")
            ReturnData().setErrorMsg("验证微博登录失败: ${e.message}")
        }
    }

    suspend fun clearWeiboHotSearchSession(context: RoutingContext): ReturnData {
        if (!checkAuth(context)) {
            return ReturnData().setErrorMsg("请先登录 Reader")
        }
        val sessionId = context.session().id()
        weiboSessions.remove(sessionId)
        cache.remove("weibo/$sessionId")
        return ReturnData().setData(mapOf("configured" to false, "expiresAt" to 0L))
    }

    private fun weiboSessionData(context: RoutingContext): Map<String, Any> {
        val status = weiboSessions.status(context.session().id())
        return mapOf(
            "configured" to status.configured,
            "expiresAt" to status.expiresAt
        )
    }

    companion object {
        fun normalizeCookie(rawCookie: String?): String {
            val cookie = rawCookie?.trim().orEmpty()
            if (cookie.isEmpty()) {
                throw IllegalArgumentException("请输入微博 Cookie")
            }
            if (cookie.length > 16 * 1024) {
                throw IllegalArgumentException("微博 Cookie 过长")
            }
            if (cookie.contains('\r') || cookie.contains('\n')) {
                throw IllegalArgumentException("微博 Cookie 不能包含换行")
            }
            val hasSub = cookie.split(';').any { part -> part.trim().startsWith("SUB=") }
            if (!hasSub) {
                throw IllegalArgumentException("Cookie 中缺少 SUB 登录凭据")
            }
            return cookie
        }

        fun validateRequest(
            providerMap: Map<String, HotSearchProvider>,
            platform: String,
            refresh: String?
        ): String? {
            if (!providerMap.containsKey(platform)) {
                return "无效的热搜平台: $platform"
            }
            if (refresh != null && refresh != "true" && refresh != "false") {
                return "无效的刷新参数: $refresh"
            }
            return null
        }
    }
}
