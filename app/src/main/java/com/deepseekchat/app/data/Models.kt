package com.deepseekchat.app.data

import org.json.JSONArray
import org.json.JSONObject

/** 附件（文本文件 / 图片） */
data class Attachment(
    val kind: String,          // "text" / "image"
    val name: String,
    val text: String? = null,  // 文本文件内容（可能已截断）
    val imageBase64: String? = null, // 图片 base64（只在内存里，不持久化）
    val chars: Int = 0,
    val truncated: Boolean = false,
    /** 图片存到 App 私有目录后的绝对路径（缩略图用，持久化） */
    val path: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("kind", kind)
        put("name", name)
        put("text", text ?: "")
        put("chars", chars)
        put("truncated", truncated)
        put("path", path ?: "")
    }

    companion object {
        fun fromJson(o: JSONObject) = Attachment(
            kind = o.optString("kind", "text"),
            name = o.optString("name"),
            text = o.optString("text").ifBlank { null },
            chars = o.optInt("chars"),
            truncated = o.optBoolean("truncated"),
            path = o.optString("path").ifBlank { null }
        )
    }
}

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
    val reasoning: String? = null,
    val error: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    /** 被 max_tokens 截断，可以点「继续生成」 */
    val truncated: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role)
        put("content", content)
        put("reasoning", reasoning ?: "")
        put("error", error)
        put("truncated", truncated)
        val arr = JSONArray()
        attachments.forEach { arr.put(it.toJson()) }
        put("attachments", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): ChatMessage {
            val arr = o.optJSONArray("attachments")
            val list = if (arr == null) emptyList() else (0 until arr.length())
                .mapNotNull { i -> arr.optJSONObject(i)?.let { Attachment.fromJson(it) } }
            return ChatMessage(
                id = o.optLong("id"),
                role = o.optString("role", "user"),
                content = o.optString("content"),
                reasoning = o.optString("reasoning").ifBlank { null },
                error = o.optBoolean("error"),
                attachments = list,
                truncated = o.optBoolean("truncated")
            )
        }
    }
}

data class Conversation(
    val id: String,
    val title: String,
    val model: String,
    val messages: List<ChatMessage>,
    val updatedAt: Long,
    /** 这段对话累计花了多少钱（元） */
    val spent: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("model", model)
        put("updatedAt", updatedAt)
        put("spent", spent)
        val arr = JSONArray()
        messages.forEach { arr.put(it.toJson()) }
        put("messages", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): Conversation {
            val arr = o.optJSONArray("messages") ?: JSONArray()
            val list = (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { ChatMessage.fromJson(it) }
            }
            return Conversation(
                id = o.optString("id"),
                title = o.optString("title"),
                model = o.optString("model"),
                messages = list,
                updatedAt = o.optLong("updatedAt"),
                spent = o.optDouble("spent", 0.0)
            )
        }
    }
}

/**
 * 官方开放平台当前可调的模型（按 2026-11 官网「模型 & 价格」页）：
 *  deepseek-flash   → DeepSeek-V4.1-Flash-1017，支持图片理解
 *  deepseek-v4-pro  → DeepSeek-V4-Pro-0813，不支持图片
 * 下面两个是过渡期旧名字，官方说明会逐步下线，留着兼容老 Key。
 */
data class DeepSeekModel(
    val id: String,
    val label: String,
    val hint: String,
    val thinking: Boolean = false,
    val vision: Boolean = false,
    val price: String = "",
    val priceDetail: String = "",
    val legacy: Boolean = false,
    /** 是否支持通过 thinking 参数开关思考模式（旧 reasoner 天然思考，不能关） */
    val supportsThinkingToggle: Boolean = false,
    /** 输入单价：元 / 百万 tokens（闲时） */
    val inputPerM: Double = 0.0,
    /** 输出单价：元 / 百万 tokens（闲时） */
    val outputPerM: Double = 0.0
)

val DEEPSEEK_MODELS = listOf(
    DeepSeekModel(
        id = "deepseek-flash",
        label = "deepseek-flash（V4.1 Flash）",
        hint = "主力模型：文字 + 看图，1M 上下文，最便宜",
        thinking = false,
        vision = true,
        price = "闲时 输入¥1 / 输出¥4",
        priceDetail = "高峰时段（工作日 9-12、14-18）翻倍：输入¥2 / 输出¥8；缓存命中 0.02-0.04 元/百万 tokens。支持思考模式和图片输入。",
        supportsThinkingToggle = true,
        inputPerM = 1.0,
        outputPerM = 4.0
    ),
    DeepSeekModel(
        id = "deepseek-v4-pro",
        label = "deepseek-v4-pro（V4 Pro）",
        hint = "高配：长任务、复杂工程更强，不支持看图，贵一些",
        thinking = false,
        vision = false,
        price = "闲时 输入¥4.5 / 输出¥13.5",
        priceDetail = "高峰时段翻倍：输入¥9 / 输出¥27；缓存命中 0.15-0.3 元/百万。1M 上下文、最大输出 384K，不支持图片。",
        supportsThinkingToggle = true,
        inputPerM = 4.5,
        outputPerM = 13.5
    ),
    DeepSeekModel(
        id = "deepseek-chat",
        label = "deepseek-chat（旧名 · V3.2 非思考）",
        hint = "过渡期旧名字，官方已计划下线",
        thinking = false,
        vision = false,
        price = "旧价：输入¥2 / 输出¥3",
        priceDetail = "V3.2 非思考模式，128K 上下文。新任务建议改用 deepseek-flash。",
        legacy = true,
        inputPerM = 2.0,
        outputPerM = 3.0
    ),
    DeepSeekModel(
        id = "deepseek-reasoner",
        label = "deepseek-reasoner（旧名 · R1 思考）",
        hint = "过渡期旧名字，先推理再回答，也就是「深度思考」",
        thinking = true,
        vision = false,
        price = "旧价：输入¥2 / 输出¥3",
        priceDetail = "V3.2 思考模式，思考过程也计费。新任务建议改用 deepseek-flash 的思考模式。",
        legacy = true,
        inputPerM = 2.0,
        outputPerM = 3.0
    )
)

fun modelById(id: String): DeepSeekModel =
    DEEPSEEK_MODELS.firstOrNull { it.id == id } ?: DEEPSEEK_MODELS.first()

/** 一次请求的运行轨迹（对应 Harness 的 session log 思路） */
data class TraceRecord(
    val at: Long,
    val model: String,
    val thinking: Boolean,
    val promptTokens: Int,
    val completionTokens: Int,
    val cost: Double,
    val durationMs: Long,
    val ok: Boolean,
    val error: String = "",
    val chars: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("at", at)
        put("model", model)
        put("thinking", thinking)
        put("promptTokens", promptTokens)
        put("completionTokens", completionTokens)
        put("cost", cost)
        put("durationMs", durationMs)
        put("ok", ok)
        put("error", error)
        put("chars", chars)
    }

    companion object {
        fun fromJson(o: JSONObject) = TraceRecord(
            at = o.optLong("at"),
            model = o.optString("model"),
            thinking = o.optBoolean("thinking"),
            promptTokens = o.optInt("promptTokens"),
            completionTokens = o.optInt("completionTokens"),
            cost = o.optDouble("cost", 0.0),
            durationMs = o.optLong("durationMs"),
            ok = o.optBoolean("ok", true),
            error = o.optString("error"),
            chars = o.optInt("chars")
        )
    }
}
