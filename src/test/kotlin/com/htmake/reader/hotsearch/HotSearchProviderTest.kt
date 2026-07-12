package com.htmake.reader.hotsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HotSearchProviderTest {

    @Test
    fun `weibo official response is normalized`() {
        val json = """
            {
              "data": {
                "realtime": [
                  {"note":"测试微博热搜","num":123456,"label_name":"新","word_scheme":"#测试微博热搜#"}
                ]
              }
            }
        """.trimIndent()

        val item = WeiboHotSearchProvider().parseOfficial(json).single()
        assertEquals(1, item.rank)
        assertEquals("测试微博热搜", item.title)
        assertEquals("12万", item.hotValue)
        assertEquals("新", item.label)
        assertTrue(item.url.startsWith("https://s.weibo.com/weibo?q="))
    }

    @Test
    fun `weibo aggregated response keeps original link`() {
        val json = """
            {
              "code": 200,
              "data": [
                {"index":2,"title":"聚合微博热搜","hot":"81万","url":"https://s.weibo.com/weibo?q=test"}
              ]
            }
        """.trimIndent()

        val item = WeiboHotSearchProvider().parseAggregated(json).single()
        assertEquals(2, item.rank)
        assertEquals("聚合微博热搜", item.title)
        assertEquals("81万", item.hotValue)
        assertEquals("https://s.weibo.com/weibo?q=test", item.url)
    }

    @Test
    fun `weibo mine response keeps personalized rank description and label`() {
        val json = """
            {
              "ok":1,
              "data":{"realtime":[{
                "word":"我的微博热搜",
                "word_scheme":"#我的微博热搜#",
                "rank":3,
                "description":"下午霸榜",
                "icon_desc":"热"
              }]}
            }
        """.trimIndent()

        val item = WeiboHotSearchProvider().parsePersonalized(json).single()
        assertEquals(4, item.rank)
        assertEquals("我的微博热搜", item.title)
        assertEquals("下午霸榜", item.hotValue)
        assertEquals("热", item.label)
        assertTrue(item.url.endsWith("&t=547"))
    }

    @Test
    fun `weibo mine response ignores unranked operation cards`() {
        val json = """
            {
              "ok":1,
              "data":{"realtime":[
                {"word":"辟谣运营卡片","rank":null,"icon_desc":"辟谣"},
                {"word":"正常热搜","rank":0,"description":123456}
              ]}
            }
        """.trimIndent()

        val items = WeiboHotSearchProvider().parsePersonalized(json)
        assertEquals(1, items.size)
        assertEquals("正常热搜", items.single().title)
        assertEquals("123456", items.single().hotValue)
    }

    @Test
    fun `zhihu response converts api question to public page`() {
        val json = """
            {
              "data": [{
                "detail_text":"2992 万热度",
                "card_label":{"name":"沸"},
                "target":{
                  "id":12345,
                  "type":"question",
                  "title":"测试知乎热榜问题？",
                  "url":"https://api.zhihu.com/questions/12345",
                  "excerpt":"问题摘要"
                },
                "children":[{"thumbnail":"https://pic.example/thumbnail.webp"}]
              }]
            }
        """.trimIndent()

        val item = ZhihuHotSearchProvider().parseResponse(json).single()
        assertEquals("测试知乎热榜问题？", item.title)
        assertEquals("https://www.zhihu.com/question/12345", item.url)
        assertEquals("2992 万热度", item.hotValue)
        assertEquals("问题摘要", item.summary)
        assertEquals("https://pic.example/thumbnail.webp", item.imageUrl)
        assertEquals("沸", item.label)
    }

    @Test
    fun `baidu response extracts hot list card`() {
        val json = """
            {
              "success":true,
              "data":{"cards":[{
                "component":"hotList",
                "content":[{
                  "index":0,
                  "word":"测试百度热搜",
                  "url":"https://www.baidu.com/s?wd=test",
                  "hotScore":"7809174",
                  "desc":"百度热搜摘要",
                  "img":"https://img.example/baidu.jpg",
                  "newHotName":"新"
                }]
              }]}
            }
        """.trimIndent()

        val item = BaiduHotSearchProvider().parseResponse(json).single()
        assertEquals(1, item.rank)
        assertEquals("测试百度热搜", item.title)
        assertEquals("780.9 万热度", item.hotValue)
        assertEquals("百度热搜摘要", item.summary)
        assertEquals("新", item.label)
    }
}
