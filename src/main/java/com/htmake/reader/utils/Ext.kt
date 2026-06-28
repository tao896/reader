package com.htmake.reader.utils

import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.WebClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.InputStream
import org.xml.sax.InputSource
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @Date: 2019-07-19 23:43
 * @Description:
 */

fun String.url(): String {
    if (this.startsWith("//")) {
        return ("http:" + this).toHttpUrl().toString()
    } else if (this.startsWith("http")) {
        return this.toHttpUrl().toString()
    }
    return this
}

fun WebClient.getEncodeAbs(absoluteURI: String): HttpRequest<Buffer> {
    return this.getAbs(absoluteURI.toHttpUrl().toString())
}

fun File.deleteRecursively() {
    if (this.exists()) {
        if (this.isFile() ) {
            this.delete();
        } else {
            this.listFiles().forEach{
                it.deleteRecursively()
            }
            this.delete()
        }
    }
}

fun File.unzip(descDir: String): Boolean {
    if (!this.exists()) {
        return false
    }
    val buffer = ByteArray(1024)
    try {
        val targetDir = File(descDir).canonicalFile
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        ZipFile(this.toString()).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val zipEntry: ZipEntry = entries.nextElement() as ZipEntry
                val descFile = File(targetDir, zipEntry.name).canonicalFile
                val targetDirPath = targetDir.path + File.separator
                if (descFile != targetDir && !descFile.path.startsWith(targetDirPath)) {
                    throw SecurityException("Zip entry outside target dir: ${zipEntry.name}")
                }

                if (zipEntry.isDirectory) {
                    if (!descFile.exists()) {
                        descFile.mkdirs()
                    }
                } else {
                    if (descFile == targetDir) {
                        throw SecurityException("Zip entry cannot overwrite target dir: ${zipEntry.name}")
                    }
                    val parentFile = descFile.parentFile
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs()
                    }
                    zf.getInputStream(zipEntry).use { inputStream ->
                        FileOutputStream(descFile).use { outputStream ->
                            var len: Int
                            while (inputStream.read(buffer).also { len = it } > 0) {
                                outputStream.write(buffer, 0, len)
                            }
                        }
                    }
                }
            }
        }
        return true
    } catch(e: Exception) {
        e.printStackTrace()
    }
    return false
}

fun File.zip(zipFilePath: String): Boolean {
    if (!this.exists()) {
        return false
    }
    if (this.isDirectory()) {
        val files = this.listFiles()
        val filesList: List<File> = files.toList()
        return zip(filesList, zipFilePath)
    } else {
        return zip(arrayListOf(this), zipFilePath)
    }
}

fun zip(files: List<File>, zipFilePath: String): Boolean {
    if (files.isEmpty()) {
        return false
    }

    val zipFile = createFile(zipFilePath)
    val buffer = ByteArray(1024)
    var zipOutputStream: ZipOutputStream? = null
    var inputStream: FileInputStream? = null
    try {
        zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
        for (file in files) {
            if (!file.exists()) continue
            zipOutputStream.putNextEntry(ZipEntry(file.name))
            inputStream = FileInputStream(file)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                zipOutputStream.write(buffer, 0, len)
            }
            zipOutputStream.closeEntry()
        }
        return true
    } catch(e: Exception) {
        e.printStackTrace()
    } finally {
        inputStream?.close()
        zipOutputStream?.close()
    }
    return false
}

fun createDir(filePath: String): File {
    val file = File(filePath)
    if (!file.exists()) {
        file.mkdirs()
    }
    return file
}

fun createFile(filePath: String): File {
    val file = File(filePath)
    val parentFile = file.parentFile!!
    if (!parentFile.exists()) {
        parentFile.mkdirs()
    }
    if (!file.exists()) {
        file.createNewFile()
    }
    return file
}

fun getFileExtetion(url: String, defaultExt: String=""): String {
    try {
        var seqs = url.split("?", ignoreCase = true, limit = 2)
        var file = seqs[0].split("/").last()
        val dotPos = file.lastIndexOf('.')
        return if (0 <= dotPos) {
            file.substring(dotPos + 1)
        } else {
            defaultExt
        }
    } catch (e: Exception) {
        return defaultExt
    }
}

fun xml2map(source: Any): MutableMap<String, Any> {
    //1.创建DocumentBuilderFactory对象
    val factory = DocumentBuilderFactory.newInstance()
    //2.创建DocumentBuilder对象
    var doc = mutableMapOf<String, Any>()
    try {
        val builder = factory.newDocumentBuilder()
        // val document = builder.parse(filePath)
        when {
            source is String -> {
                val document = builder.parse(source as String)
                return parseNode(document.getChildNodes())
            }
            source is InputStream -> {
                val document = builder.parse(source as InputStream)
                return parseNode(document.getChildNodes())
            }
            source is InputSource -> {
                val document = builder.parse(source as InputSource)
                return parseNode(document.getChildNodes())
            }
            else -> {
                return doc
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return doc
    }
}

fun parseNode(list: NodeList): MutableMap<String, Any> {
    var doc = mutableMapOf<String, Any>()
    for (i in 0 until list.getLength()) {
        val node = list.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            val childNodes = node.getChildNodes()
            // <Element><Text></Text><Element><Text></Text></Element></Element>
            // logger.info("index: {} node: {} type: {} childNodesLength: {}", i, node, node.getNodeType(), childNodes.getLength())
            if (childNodes.getLength() == 1 && node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
                doc.put(node.getNodeName(), node.getFirstChild().getNodeValue())
            } else if(childNodes.getLength() > 1) {
                doc.put(node.getNodeName(), parseNode(childNodes))
            }
        }
    }
    return doc
}



