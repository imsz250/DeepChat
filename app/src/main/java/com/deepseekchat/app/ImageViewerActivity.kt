package com.deepseekchat.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepseekchat.app.ui.theme.DeepChatTheme

/**
 * 应用内的图片查看器：黑底、可双指缩放/拖动、双击复位。
 * 不再依赖系统相册 App，避免机型差异导致的“打不开”。
 */
class ImageViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_PATH)
        val base64 = intent.getStringExtra(EXTRA_BASE64)
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()

        setContent {
            DeepChatTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    val bitmap = remember(path, base64) {
                        val fromFile = runCatching {
                            if (path.isNullOrBlank()) null else BitmapFactory.decodeFile(path)
                        }.getOrNull()
                        fromFile ?: runCatching {
                            if (base64.isNullOrBlank()) null
                            else {
                                val bytes = Base64.decode(base64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                        }.getOrNull()
                    }

                    if (bitmap != null) {
                        var scale by remember { mutableFloatStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        val state = rememberTransformableState { zoomChange, panChange, _ ->
                            scale = (scale * zoomChange).coerceIn(1f, 6f)
                            offset = if (scale <= 1f) Offset.Zero else offset + panChange
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .transformable(state)
                                .pointerInput(Unit) {
                                    detectTapGestures(onDoubleTap = {
                                        scale = if (scale > 1f) 1f else 2.5f
                                        offset = Offset.Zero
                                    })
                                }
                        )
                    } else {
                        Text(
                            text = "图片读取失败（可能已被清理）",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    if (name.isNotBlank()) {
                        Text(
                            text = name,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
                        )
                    }

                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 36.dp, start = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PATH = "extra_path"
        private const val EXTRA_BASE64 = "extra_base64"
        private const val EXTRA_NAME = "extra_name"

        fun open(context: Context, path: String?, base64: String?, name: String) {
            context.startActivity(
                Intent(context, ImageViewerActivity::class.java)
                    .putExtra(EXTRA_PATH, path)
                    .putExtra(EXTRA_BASE64, base64)
                    .putExtra(EXTRA_NAME, name)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        }
    }
}
