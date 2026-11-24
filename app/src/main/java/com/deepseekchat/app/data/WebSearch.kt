package com.deepseekchat.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 极简联网搜索：多引擎依次尝试（Bing 移动/桌面、百度、DuckDuckGo）。
 * 全部失败就返回空列表，由调用方退化成普通回答，绝不阻塞回复。
 */
object WebSearch {

    data class Hit(val title: String, val url: String, val snippet: String)

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110 Mobile Safari/537.36"
    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0 Safari/537.36"

    suspend fun search(query: String, limit: Int = 5): List<Hit> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = URLEncoder.encode(query, "UTF-8")
        withTimeoutOrNull(9_000) { runEngines(q, limit) }.orEmpty()
    }

    private suspend fun runEngines(q: String, limit: Int): List<Hit> {
        val bing = attempt { fetch("https://cn.bing.com/search?q=$q&setlang=zh-CN&ensearch=0", MOBILE_UA)?.let { parseBing(it, limit) } }
        if (bing.isNotEmpty()) return bing

        val bingDesktop = attempt { fetch("https://www.bing.com/search?q=$q&setlang=zh-CN", DESKTOP_UA)?.let { parseBing(it, limit) } }
        if (bingDesktop.isNotEmpty()) return bingDesktop

        val baidu = attempt { fetch("https://www.baidu.com/s?ie=utf-8&wd=$q", MOBILE_UA)?.let { parseBaidu(it, limit) } }
        if (baidu.isNotEmpty()) return baidu

        return emptyList()
    }

    private fun attempt(block: () -> List<Hit>?): List<Hit> =
        runCatching { block() }.getOrNull().orEmpty()

    private fun fetch(url: String, ua: String): String? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 7_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", ua)
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = try {
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            conn.disconnect()
        }
        if (code in 200..299) body else null
    }.getOrNull()

    private fun parseBing(html: String, limit: Int): List<Hit> {
        val hits = ArrayList<Hit>()
        Regex("<li class=\"b_algo.*?</li>", RegexOption.DOT_MATCHES_ALL).findAll(html).forEach { block ->
            val text = block.value
            val title = clean(Regex("<h2[^>]*>(.*?)</h2>", RegexOption.DOT_MATCHES_ALL)
                .find(text)?.groupValues?.get(1).orEmpty())
            val url = Regex("href=\"(http[^\"]+)\"").find(text)?.groupValues?.get(1).orEmpty()
            if (title.isBlank() || url.isBlank()) return@forEach
            val snippet = clean(Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
                .find(text)?.groupValues?.get(1).orEmpty())
            hits.add(Hit(title, url, snippet))
        }
        return hits.take(limit)
    }

    private fun parseBaidu(html: String, limit: Int): List<Hit> {
        val hits = ArrayList<Hit>()
        Regex("<div class=\"result[^\"]*c-container.*?</div>\\s*</div>", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).forEach { block ->
                val text = block.value
                val title = clean(Regex("<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL)
                    .find(text)?.groupValues?.get(1).orEmpty())
                if (title.isBlank()) return@forEach
                val url = Regex("href=\"(http[^\"]+)\"").find(text)?.groupValues?.get(1).orEmpty()
                val snippet = clean(text.replace(Regex("<[^>]+>"), " ")).take(200)
                hits.add(Hit(title, url.ifBlank { "https://www.baidu.com/s?wd=" + URLEncoder.encode(title, "UTF-8") }, snippet))
            }
        return hits.take(limit)
    }

    private fun clean(raw: String): String = raw
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#x27;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun asContext(query: String, hits: List<Hit>): String {
        if (hits.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("以下是「").append(query).append("」的联网搜索结果（可能过时或不完整），")
            .append("请以这些信息为依据回答，末尾用「来源：」列一两条链接；与问题无关就忽略。\n")
        hits.forEachIndexed { i, hit ->
            sb.append(i + 1).append(". ").append(hit.title)
            if (hit.snippet.isNotBlank()) sb.append("：").append(hit.snippet.take(220))
            sb.append("（").append(hit.url).append("）\n")
        }
        return sb.toString()
    }
}
