package com.deepseekchat.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepseekchat.app.ui.theme.DeepChatTheme

/** 应用内文本查看器：可滚动、可长按选中复制、整段复制 */
class TextViewerActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()

        setContent {
            DeepChatTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = name.ifBlank { "文本附件" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText(name, text))
                                    android.widget.Toast.makeText(
                                        this@TextViewerActivity,
                                        "已复制全文",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }) { Text("复制") }
                            }
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        SelectionContainer {
                            Text(
                                text = text.ifBlank { "（这个附件没有文本内容）" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_TEXT = "extra_text"

        fun open(context: Context, name: String, text: String) {
            context.startActivity(
                Intent(context, TextViewerActivity::class.java)
                    .putExtra(EXTRA_NAME, name)
                    .putExtra(EXTRA_TEXT, text)
            )
        }
    }
}
