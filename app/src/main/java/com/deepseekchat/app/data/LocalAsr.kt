package com.deepseekchat.app.data

import android.content.Context
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 离线语音识别（Vosk，模型随 offline 包内置）。
 * 同时装了中文和英文两个小模型：同一段录音两个都跑一遍，谁的平均置信度高用谁，
 * 这样「MD」这种英文词不会被中文模型硬拆成「儿木D儿」。
 */
object LocalAsr {
    private data class Pack(val asset: String, val dir: String)

    private val packs = listOf(
        Pack("vosk-model-small-cn.zip", "asr-model/vosk-model-small-cn-0.22"),
        Pack("vosk-model-small-en.zip", "asr-model/vosk-model-small-en-us-0.15")
    )

    private val models = HashMap<String, Model>()

    private fun dirOf(ctx: Context, pack: Pack) = File(ctx.filesDir, pack.dir)

    fun isReady(ctx: Context): Boolean = packs.any { dirOf(ctx, it).exists() }

    private fun unzip(ctx: Context, pack: Pack): Boolean = runCatching {
        val root = File(ctx.filesDir, "asr-model")
        root.mkdirs()
        ctx.assets.open(pack.asset).use { input ->
            ZipInputStream(input).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    val out = File(root, entry.name)
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zin.copyTo(it) }
                    }
                    entry = zin.nextEntry
                }
            }
        }
        dirOf(ctx, pack).exists()
    }.getOrDefault(false)

    /** 解压模型；返回是否至少有一个模型可用。 */
    fun installIfNeeded(ctx: Context): Boolean {
        packs.forEach { pack ->
            if (!dirOf(ctx, pack).exists()) unzip(ctx, pack)
        }
        return isReady(ctx)
    }

    private fun modelOf(ctx: Context, pack: Pack): Model? {
        synchronized(models) {
            models[pack.dir]?.let { return it }
        }
        val created = runCatching { Model(dirOf(ctx, pack).absolutePath) }.getOrNull() ?: return null
        synchronized(models) { models[pack.dir] = created }
        return created
    }

    /** 返回 Pair(文本, 平均置信度) */
    private fun runModel(model: Model, pcm: ByteArray): Pair<String, Float>? = runCatching {
        val rec = Recognizer(model, 16000.0f)
        try {
            rec.setWords(true)
            rec.acceptWaveForm(pcm, pcm.size)
            val json = JSONObject(rec.finalResult)
            val text = cleanUp(json.optString("text"))
            val words = json.optJSONArray("result")
            var conf = 0f
            if (words != null && words.length() > 0) {
                for (i in 0 until words.length()) {
                    conf += words.optJSONObject(i)?.optDouble("conf", 0.0)?.toFloat() ?: 0f
                }
                conf /= words.length()
            } else {
                conf = if (text.isNotBlank()) 0.3f else -1f
            }
            text to conf
        } finally {
            runCatching { rec.close() }
        }
    }.getOrNull()

    /** 常见识别错字 → 正确写法（同音/近音、品牌名、技术词） */
    private val fixes = listOf(
        "the seek" to "DeepSeek",
        "de seek" to "DeepSeek",
        "deep sick" to "DeepSeek",
        "deep seek" to "DeepSeek",
        "the seek" to "DeepSeek",
        "deepchat" to "DeepChat",
        "deep chat" to "DeepChat",
        "弗莱仕" to "Flex",
        "弗莱克斯" to "Flex",
        "福莱克斯" to "Flex",
        "迪普西克" to "DeepSeek",
        "迪普塞克" to "DeepSeek",
        "深度求所" to "深度求索",
        "给他哈勃" to "GitHub",
        "给特哈勃" to "GitHub",
        "杰特哈勃" to "GitHub",
        "杰特" to "Git",
        "陪对" to "配对",
        "配队" to "配对",
        "在现" to "在线",
        "离线接口" to "离线接口",
        "马特瑞尔" to "Material",
        "康波斯" to "Compose",
        "酷木" to "Kotlin",
        "靠特林" to "Kotlin",
        "安卓" to "安卓",
        "艾迪bi" to "ADB",
        "缓存吖" to "缓存",
        "点几" to "点击",
        "设值" to "设置"
    )

    /** Vosk 会按词切空格输出中文，这里把汉字之间的空格去掉，只保留英文词之间的空格。 */
    private fun cleanUp(raw: String): String {
        var t = raw.replace(Regex("\\s+"), " ").trim()
        val han = "\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef"
        t = t.replace(Regex("([$han])\\s+"), "$1")
        t = t.replace(Regex("\\s+([$han])"), "$1")
        // M D、G i t 这种被拆成单个字母的，合并回单词
        repeat(3) {
            t = t.replace(Regex("(?i)\\b([a-z])\\s+([a-z])\\b"), "$1$2")
        }
        // 错字/同音字修正（英文品牌名忽略大小写）
        fixes.forEach { (wrong, right) ->
            if (wrong != right) {
                t = t.replace(Regex(Regex.escape(wrong), RegexOption.IGNORE_CASE), right)
            }
        }
        return t.trim()
    }

    /** pcm：16kHz 单声道 16bit 裸 PCM。 */
    fun recognize(ctx: Context, pcm: ByteArray): String? {
        installIfNeeded(ctx)
        var best: String? = null
        var bestScore = Float.NEGATIVE_INFINITY
        for (pack in packs) {
            if (!dirOf(ctx, pack).exists()) continue
            val model = modelOf(ctx, pack) ?: continue
            val (text, score) = runModel(model, pcm) ?: continue
            if (text.isNotBlank() && score > bestScore) {
                bestScore = score
                best = text
            }
        }
        return best
    }
}
