package com.deepseekchat.app.data

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

/**
 * 离线语音识别：sherpa-onnx + 中英双语 streaming zipformer（k2-fsa 训练、国产开源引擎）。
 * 中英混说一条链路，不再出现「英文词被中文模型硬拆」的问题。
 * 模型随 APK 内置在 assets/asr 下，不联网、不收钱。
 */
object LocalAsr {
    @Volatile
    private var recognizer: OnlineRecognizer? = null

    private fun engine(ctx: Context): OnlineRecognizer? {
        recognizer?.let { return it }
        return synchronized(this) {
            recognizer?.let { return it }
            runCatching {
                val config = OnlineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                    modelConfig = OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = "asr/encoder.onnx",
                            decoder = "asr/decoder.onnx",
                            joiner = "asr/joiner.onnx"
                        ),
                        tokens = "asr/tokens.txt",
                        numThreads = 2,
                        modelType = "zipformer"
                    ),
                    decodingMethod = "greedy_search",
                    enableEndpoint = false
                )
                OnlineRecognizer(ctx.assets, config).also { recognizer = it }
            }.getOrNull()
        }
    }

    fun isReady(ctx: Context): Boolean = engine(ctx) != null

    /** pcm：16kHz 单声道 16bit 裸 PCM。 */
    fun recognize(ctx: Context, pcm: ByteArray): String? {
        val rec = engine(ctx) ?: return null
        val samples = FloatArray(pcm.size / 2) { i ->
            val v = (pcm[i * 2 + 1].toInt() shl 8) or (pcm[i * 2].toInt() and 0xFF)
            v / 32768f
        }
        if (samples.isEmpty()) return null
        return synchronized(this) {
            runCatching {
                val stream = rec.createStream()
                try {
                    var i = 0
                    val chunk = 8000 // 0.5s
                    while (i < samples.size) {
                        val n = minOf(chunk, samples.size - i)
                        stream.acceptWaveform(samples.copyOfRange(i, i + n), 16000)
                        while (rec.isReady(stream)) rec.decode(stream)
                        i += n
                    }
                    stream.inputFinished()
                    while (rec.isReady(stream)) rec.decode(stream)
                    cleanUp(rec.getResult(stream).text)
                } finally {
                    runCatching { stream.release() }
                }
            }.getOrNull()
        }
    }

    /** 常见识别错字 → 正确写法（同音/近音、品牌名、技术词） */
    private val fixes = listOf(
        "the seek" to "DeepSeek",
        "de seek" to "DeepSeek",
        "deep sick" to "DeepSeek",
        "deep seek" to "DeepSeek",
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
        "康波斯" to "Compose",
        "酷木" to "Kotlin",
        "靠特林" to "Kotlin",
        "艾迪bi" to "ADB",
        "缓存吖" to "缓存",
        "点几" to "点击"
    )

    /** 汉字之间去掉空格；被拆开的单个字母合回；再走一遍错字修正 */
    private fun cleanUp(raw: String): String {
        var t = raw.replace(Regex("\\s+"), " ").trim()
        val han = "\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef"
        t = t.replace(Regex("([$han])\\s+"), "$1")
        t = t.replace(Regex("\\s+([$han])"), "$1")
        repeat(3) {
            t = t.replace(Regex("(?i)\\b([a-z])\\s+([a-z])\\b"), "$1$2")
        }
        fixes.forEach { (wrong, right) ->
            if (wrong != right) {
                t = t.replace(Regex(Regex.escape(wrong), RegexOption.IGNORE_CASE), right)
            }
        }
        return t.trim()
    }
}
