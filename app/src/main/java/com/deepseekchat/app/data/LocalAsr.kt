package com.deepseekchat.app.data

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig

/**
 * 离线语音识别：sherpa-onnx + SenseVoiceSmall（阿里 FunAudioLLM 出品，中/英/日/韩/粤一体）。
 * 这是目前开源里中文+英文混说识别最稳的一档，模型随 APK 内置，断网可用、不花钱。
 */
object LocalAsr {
    @Volatile
    private var recognizer: OfflineRecognizer? = null

    private fun engine(ctx: Context): OfflineRecognizer? {
        recognizer?.let { return it }
        return synchronized(this) {
            recognizer?.let { return it }
            runCatching {
                val config = OfflineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                    modelConfig = OfflineModelConfig(
                        senseVoice = OfflineSenseVoiceModelConfig(
                            model = "asr/model.onnx",
                            language = "auto",
                            useInverseTextNormalization = true
                        ),
                        tokens = "asr/tokens.txt",
                        numThreads = 2,
                        debug = false
                    )
                )
                OfflineRecognizer(ctx.assets, config).also { recognizer = it }
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
                    stream.acceptWaveform(samples, 16000)
                    rec.decode(stream)
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
