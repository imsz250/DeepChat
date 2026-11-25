package com.deepseekchat.app.data

import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * 在线语音识别：走 OpenAI 兼容的 /audio/transcriptions 接口
 * （硅基流动 SenseVoice、OpenAI whisper-1、任何兼容服务都能用）。
 * 中英混说比系统识别器稳，也不依赖手机里有没有装语音服务。
 */
object AsrClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** PCM 16bit 单声道 → WAV */
    fun pcmToWav(pcm: ByteArray, sampleRate: Int = 16000): ByteArray {
        val out = ByteArrayOutputStream()
        val totalDataLen = pcm.size + 36
        fun le32(v: Int) {
            out.write(v and 0xff); out.write((v shr 8) and 0xff)
            out.write((v shr 16) and 0xff); out.write((v shr 24) and 0xff)
        }
        fun le16(v: Int) {
            out.write(v and 0xff); out.write((v shr 8) and 0xff)
        }
        out.write("RIFF".toByteArray()); le32(totalDataLen); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); le32(16); le16(1); le16(1)
        le32(sampleRate); le32(sampleRate * 2); le16(2); le16(16)
        out.write("data".toByteArray()); le32(pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }

    suspend fun transcribe(
        baseUrl: String,
        apiKey: String,
        model: String,
        wav: ByteArray
    ): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val base = baseUrl.trimEnd('/')
            val url = if (base.endsWith("/audio/transcriptions")) base else base + "/audio/transcriptions"
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "voice.wav",
                    wav.toRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", model.ifBlank { "FunAudioLLM/SenseVoiceSmall" })
                .addFormDataPart("response_format", "json")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@runCatching null
                val json = runCatching { JSONObject(text) }.getOrNull()
                json?.optString("text")?.takeIf { it.isNotBlank() }
                    ?: text.takeIf { it.isNotBlank() && !it.startsWith("{") }
            }
        }.getOrNull()
    }
}
