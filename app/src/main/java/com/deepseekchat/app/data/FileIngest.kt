package com.deepseekchat.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream

/** 把用户挑的文件 / 拍的照片读成能发出去的东西 */
object FileIngest {

    private const val MAX_TEXT_CHARS = 12000      // 超过就截断（相当于转成 txt 读前一段）
    private const val MAX_IMAGE_SIDE = 1280       // 图片最长边，太大就压缩

    fun queryName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull() ?: (uri.lastPathSegment ?: "文件")

    fun sizeOf(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null }
    }.getOrNull() ?: 0L

    /** 文本文件：读全文，超长截断并标记 */
    fun readText(context: Context, uri: Uri, name: String): Attachment? {
        val body = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        }.getOrNull() ?: return null

        val text = runCatching { body.toString(Charsets.UTF_8) }.getOrDefault("")
        if (text.isBlank()) return null

        val truncated = text.length > MAX_TEXT_CHARS
        val body2 = if (truncated) text.take(MAX_TEXT_CHARS) else text
        return Attachment(
            kind = "text",
            name = name,
            text = body2,
            chars = text.length,
            truncated = truncated
        )
    }

    /** 图片：缩到 1280 长边，转 JPG base64 */
    fun readImage(context: Context, uri: Uri, name: String): Attachment? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        }.getOrNull() ?: return null

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val longSide = maxOf(options.outWidth, options.outHeight)
        var sample = 1
        while (longSide / sample > MAX_IMAGE_SIDE) sample *= 2

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        bitmap.recycle()
        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)

        return Attachment(
            kind = "image",
            name = name,
            imageBase64 = base64
        )
    }

    /** 按 mimeType 决定怎么读 */
    fun read(context: Context, uri: Uri, mimeType: String?): Attachment? {
        val name = queryName(context, uri)
        return if (mimeType?.startsWith("image") == true) readImage(context, uri, name)
        else readText(context, uri, name)
    }
}
