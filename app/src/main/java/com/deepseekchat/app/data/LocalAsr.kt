package com.deepseekchat.app.data

import android.content.Context
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 离线语音识别（Vosk small-cn，随 APK 内置）。
 * 首次调用时把 assets 里的模型 zip 解到 filesDir/asr-model。
 */
object LocalAsr {
    private const val ASSET = "vosk-model-small-cn.zip"
    private const val DIR = "asr-model/vosk-model-small-cn-0.22"

    @Volatile
    private var model: Model? = null

    private fun dirOf(ctx: Context) = File(ctx.filesDir, DIR)

    fun isReady(ctx: Context): Boolean = dirOf(ctx).exists()

    /** 解压模型；返回是否可用。 */
    fun installIfNeeded(ctx: Context): Boolean {
        val target = dirOf(ctx)
        if (target.exists()) return true
        return runCatching {
            val root = File(ctx.filesDir, "asr-model")
            root.mkdirs()
            ctx.assets.open(ASSET).use { input ->
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
            target.exists()
        }.getOrDefault(false)
    }

    private fun modelOf(ctx: Context): Model? {
        model?.let { return it }
        if (!installIfNeeded(ctx)) return null
        val created = runCatching { Model(dirOf(ctx).absolutePath) }.getOrNull() ?: return null
        model = created
        return created
    }

    /** Vosk 会按词切空格输出中文，这里把汉字之间的空格去掉，只保留英文词之间的空格。 */
    private fun cleanUp(raw: String): String {
        var t = raw.replace(Regex("\\s+"), " ").trim()
        val han = "\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef"
        t = t.replace(Regex("([$han])\\s+"), "$1")
        t = t.replace(Regex("\\s+([$han])"), "$1")
        return t.trim()
    }

    /** pcm：16kHz 单声道 16bit 裸 PCM。 */
    fun recognize(ctx: Context, pcm: ByteArray): String? {
        val m = modelOf(ctx) ?: return null
        return runCatching {
            val rec = Recognizer(m, 16000.0f)
            try {
                rec.acceptWaveForm(pcm, pcm.size)
                cleanUp(JSONObject(rec.finalResult).optString("text"))
            } finally {
                runCatching { rec.close() }
            }
        }.getOrNull()
    }
}
