package com.deepseekchat.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek 开放平台接口（api.deepseek.com）。
 * 只用 API Key 调接口，不碰网页登录。
 */
object DeepSeekApi {

    /** 调试日志出口：把请求地址、状态码、原始返回片段抛给界面显示 */
    var logger: ((String) -> Unit)? = null

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun normalizeBase(base: String): String {
        var trimmed = base.trim().ifBlank { "https://api.deepseek.com" }.trimEnd('/')
        // 用户常把 /v1 或 /chat/completions 粘进来，统一归一化到根地址
        trimmed = trimmed.removeSuffix("/chat/completions").removeSuffix("/v1")
        return trimmed.trimEnd('/')
    }

    private fun maskKey(key: String): String =
        if (key.length <= 10) "****" else key.take(6) + "…" + key.takeLast(4)

    /** 流式对话。每收到一段增量回调一次 */
    suspend fun streamChat(
        apiKey: String,
        baseUrl: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        thinking: Boolean = false,
        onUsage: ((promptTokens: Int, completionTokens: Int) -> Unit)? = null,
        onFinish: ((reason: String) -> Unit)? = null,
        onDelta: (content: String?, reasoning: String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IOException("还没填 API Key，先去设置页填一下")

        val body = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("stream_options", JSONObject().put("include_usage", true))
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            val arr = JSONArray()
            messages.forEach { m -> arr.put(buildMessage(m)) }
            put("messages", arr)

            // 思考模式：flash 默认是开的，要「普通模式」必须显式关掉
            if (model == "deepseek-flash" || model == "deepseek-v4-pro") {
                put("thinking", JSONObject().put("type", if (thinking) "enabled" else "disabled"))
                if (thinking) put("reasoning_effort", "high")
            }
        }.toString().toRequestBody(jsonType)

        val url = "${normalizeBase(baseUrl)}/chat/completions"
        logger?.invoke("POST $url · model=$model · key=${maskKey(apiKey)} (长度 ${apiKey.length})")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            logger?.invoke("HTTP ${response.code}")
            if (!response.isSuccessful) {
                val raw = response.body?.string().orEmpty()
                logger?.invoke("返回：${raw.take(400)}")
                throw IOException(parseError(response.code, raw))
            }
            val source = response.body?.source() ?: throw IOException("没有响应内容")
            var received = false
            while (true) {
                val line = source.readUtf8Line() ?: break
                val trimmed = line.trim()
                if (!trimmed.startsWith("data:")) continue
                val payload = trimmed.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                if (payload.isEmpty()) continue
                val obj = runCatching { JSONObject(payload) }.getOrNull() ?: continue
                obj.optJSONObject("error")?.let { err ->
                    throw IOException(err.optString("message", "接口返回错误"))
                }
                obj.optJSONArray("choices")?.optJSONObject(0)
                    ?.optString("finish_reason")
                    ?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { onFinish?.invoke(it) }

                obj.optJSONObject("usage")?.let { usage ->
                    val prompt = usage.optInt("prompt_tokens", 0)
                    val completion = usage.optInt("completion_tokens", 0)
                    if (prompt > 0 || completion > 0) onUsage?.invoke(prompt, completion)
                }

                val delta = obj.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                    ?: continue
                // 注意：JSON 里字段值可能是 null，optString 会把它变成字符串 "null"，
                // 必须先用 isNull 判掉，否则思考过程里会刷出一堆 nullnullnull
                val content = delta.optStringOrNull("content")
                val reasoning = delta.optStringOrNull("reasoning_content")
                if ((content?.isNotEmpty() == true) || (reasoning?.isNotEmpty() == true)) {
                    received = true
                    onDelta(content, reasoning)
                }
            }
            if (!received) throw IOException("接口没有返回内容，检查 Key、余额或模型名")
        }
    }

    /** 查询余额：GET /user/balance */
    suspend fun balance(apiKey: String, baseUrl: String): BalanceInfo = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IOException("还没填 API Key")
        val request = Request.Builder()
            .url("${normalizeBase(baseUrl)}/user/balance")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            logger?.invoke("GET ${normalizeBase(baseUrl)}/user/balance → HTTP ${response.code}")
            if (!response.isSuccessful) {
                logger?.invoke("返回：${text.take(400)}")
                throw IOException(parseError(response.code, text))
            }
            val obj = JSONObject(text)
            val arr = obj.optJSONArray("balance_infos") ?: JSONArray()
            val first = if (arr.length() > 0) arr.optJSONObject(0) else null
            BalanceInfo(
                available = obj.optBoolean("is_available", true),
                currency = first?.optString("currency").orEmpty().ifBlank { "CNY" },
                total = first?.optString("total_balance").orEmpty(),
                granted = first?.optString("granted_balance").orEmpty(),
                toppedUp = first?.optString("topped_up_balance").orEmpty()
            )
        }
    }

    /** 列出账号可用的模型（过滤成只留 DeepSeek 自己的） */
    suspend fun models(apiKey: String, baseUrl: String): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IOException("还没填 API Key")
        val request = Request.Builder()
            .url("${normalizeBase(baseUrl)}/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(parseError(response.code, text))
            val data = JSONObject(text).optJSONArray("data") ?: JSONArray()
            (0 until data.length())
                .mapNotNull { data.optJSONObject(it)?.optString("id") }
                .filter { it.startsWith("deepseek") }
        }
    }

    private fun buildMessage(message: ChatMessage): JSONObject {
        val texts = message.attachments.filter { it.kind == "text" && !it.text.isNullOrBlank() }
        val images = message.attachments.filter { it.kind == "image" && !it.imageBase64.isNullOrBlank() }

        val textPart = buildString {
            texts.forEach { att ->
                append("【文件：").append(att.name).append("】\n")
                append(att.text).append("\n")
                if (att.truncated) append("（内容较长，只读取了前 ").append(att.chars).append(" 字的前一部分）\n")
                append("\n")
            }
            append(message.content)
        }.trim()

        return if (images.isEmpty()) {
            JSONObject().apply {
                put("role", message.role)
                put("content", textPart)
            }
        } else {
            val parts = JSONArray()
            images.forEach { att ->
                parts.put(
                    JSONObject().apply {
                        put("type", "image_url")
                        put(
                            "image_url",
                            JSONObject().put("url", "data:image/jpeg;base64,${att.imageBase64}")
                        )
                    }
                )
            }
            parts.put(JSONObject().apply { put("type", "text"); put("text", textPart) })
            JSONObject().apply {
                put("role", message.role)
                put("content", parts)
            }
        }
    }

    private fun parseError(code: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()
        return when (code) {
            401 -> "Key 被拒绝（401）：${message.ifBlank { body.take(120) }}"
            402 -> "账户余额不足（402），先去平台充值"
            403 -> "这个 Key 没有权限（403）"
            429 -> "请求太频繁（429），等一会儿再发"
            500, 502, 503 -> "DeepSeek 服务端暂时不可用（$code），稍后重试"
            else -> "请求失败（$code）${if (message.isNotBlank()) "：$message" else ""}"
        }
    }
}

data class BalanceInfo(
    val available: Boolean,
    val currency: String,
    val total: String,
    val granted: String,
    val toppedUp: String
)

/** org.json 的 optString 会把 JSON null 转成 "null" 字符串，这里按真 null 处理 */
private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    val v = optString(name)
    return v.takeIf { it != "null" }
}
