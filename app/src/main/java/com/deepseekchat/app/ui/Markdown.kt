package com.deepseekchat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import com.deepseekchat.app.CodePreviewActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Markdown 块级元素 */
private sealed interface MdNode {
    data class Block(val text: String) : MdNode
    data class Heading(val level: Int, val text: String) : MdNode
    data class Bullet(val text: String, val depth: Int) : MdNode
    data class Ordered(val number: String, val text: String) : MdNode
    data class Quote(val text: String) : MdNode
    data class Code(val lang: String, val code: String) : MdNode
    data class Table(val rows: List<List<String>>, val header: Boolean) : MdNode
    data object Rule : MdNode
}

@Composable
fun MarkdownText(text: String, color: Color) {
    val nodes = remember(text) { parseMarkdown(text) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        nodes.forEach { node ->
            when (node) {
                is MdNode.Heading -> Text(
                    text = buildInline(node.text, color),
                    style = when (node.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                is MdNode.Block -> Text(
                    text = buildInline(node.text, color),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )

                is MdNode.Bullet -> Row(Modifier.padding(start = (node.depth * 12).dp)) {
                    Text(
                        "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buildInline(node.text, color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                }

                is MdNode.Ordered -> Row(Modifier) {
                    Text(
                        "${node.number}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buildInline(node.text, color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                }

                is MdNode.Quote -> Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = buildInline(node.text, color),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is MdNode.Table -> MdTable(node)

                is MdNode.Code -> {
                    if (isMarkdownLang(node.lang)) {
                        MarkdownText(node.code, color)
                    } else {
                        CodeBlock(node.lang, node.code)
                    }
                }

                MdNode.Rule -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun MdTable(node: MdNode.Table) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            node.rows.forEachIndexed { index, row ->
                val isHeaderRow = node.header && index == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHeaderRow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else Color.Transparent
                        )
                        .padding(vertical = 6.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isHeaderRow) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
                if (index < node.rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun CodeAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp)
    )
}

@Composable
private fun CodeBlock(lang: String, code: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var pendingSave by remember { mutableStateOf<String?>(null) }
    var pendingName by remember { mutableStateOf("deepchat_code.txt") }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val body = pendingSave
        pendingSave = null
        if (uri != null && body != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(body.toByteArray())
                }
            }.onSuccess { Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "保存失败：${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }
    val trimmed = code.trim()
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val lines = trimmed.lines().size

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (lang.ifBlank { "code" }) + " · " + lines + " 行",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (isRunnableLang(lang)) {
                    IconButton(
                        onClick = { CodePreviewActivity.open(context, lang, trimmed, "run") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "运行",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    CodeAction("全屏") {
                        CodePreviewActivity.open(context, lang, trimmed, "code")
                    }
                } else {
                    // 本机没有对应运行时，只能看语法高亮，不摆三角号
                    CodeAction("打开") {
                        CodePreviewActivity.open(context, lang, trimmed, "code")
                    }
                }
                CodeAction("保存") {
                    pendingSave = trimmed
                    pendingName = fileNameFor(lang)
                    saveLauncher.launch(pendingName)
                }
                CodeAction("复制") {
                    clipboard.setText(AnnotatedString(trimmed))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(vScroll)
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Box(modifier = Modifier.horizontalScroll(hScroll)) {
                Text(
                    text = trimmed,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false
                )
                }
            }
        }
    }
}

private val codeRegex = Regex("`([^`]+)`")
private val boldRegex = Regex("\\*\\*([^\\*]+)\\*\\*")

/** 行内格式：**加粗**、*斜体*、`代码` */
private fun buildInline(source: String, baseColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    val text = source
    while (i < text.length) {
        val rest = text.substring(i)
        val bold = boldRegex.find(rest)
        val code = codeRegex.find(rest)
        val first = listOfNotNull(bold, code).minByOrNull { it.range.first }
        if (first == null) {
            append(text.substring(i))
            break
        }
        append(text.substring(i, i + first.range.first))
        when (first) {
            bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(first.groupValues[1])
            }
            else -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = baseColor.copy(alpha = 0.12f),
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(first.groupValues[1])
            }
        }
        i += first.range.last + 1
    }
}

/** 极简 Markdown 解析：标题 / 列表 / 引用 / 分割线 / 代码块 */
private fun parseMarkdown(source: String): List<MdNode> {
    val out = ArrayList<MdNode>()
    val lines = source.replace("\r\n", "\n").split("\n")
    var index = 0
    while (index < lines.size) {
        val raw = lines[index]
        val line = raw.trimEnd()

        when {
            line.trimStart().startsWith("```") -> {
                val lang = line.trim().removePrefix("```").trim()
                val body = StringBuilder()
                index++
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    body.append(lines[index]).append('\n')
                    index++
                }
                out.add(MdNode.Code(lang, body.toString()))
                index++
            }

            line.isBlank() -> index++

            line.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length.coerceIn(1, 3)
                out.add(MdNode.Heading(level, line.drop(level).trim()))
                index++
            }

            Regex("^[-*+]\\s+").containsMatchIn(line) -> {
                val indent = line.length - line.trimStart().length
                out.add(
                    MdNode.Bullet(
                        line.trimStart().replaceFirst(Regex("^[-*+]\\s+"), ""),
                        (indent / 2).coerceIn(0, 3)
                    )
                )
                index++
            }

            Regex("^\\d+[.)]\\s+").containsMatchIn(line) -> {
                val number = line.trimStart().takeWhile { it.isDigit() }
                out.add(
                    MdNode.Ordered(
                        number,
                        line.trimStart().replaceFirst(Regex("^\\d+[.)]\\s+"), "")
                    )
                )
                index++
            }

            line.startsWith(">") -> {
                out.add(MdNode.Quote(line.removePrefix(">").trim()))
                index++
            }

            line.trimStart().startsWith("|") -> {
                val rows = ArrayList<List<String>>()
                var hasHeader = false
                while (index < lines.size && lines[index].trimStart().startsWith("|")) {
                    val cells = lines[index].trim().trim('|').split("|").map { it.trim() }
                    val isSeparator = cells.all { cell ->
                        cell.isEmpty() || cell.all { it == '-' || it == ':' || it == ' ' }
                    }
                    if (isSeparator && rows.isNotEmpty()) {
                        hasHeader = true
                    } else {
                        rows.add(cells)
                    }
                    index++
                }
                if (rows.isNotEmpty()) out.add(MdNode.Table(rows, hasHeader))
            }

            line.matches(Regex("^[-_*]{3,}$")) -> {
                out.add(MdNode.Rule)
                index++
            }

            else -> {
                // 连续的普通行合并成一段（保留换行）
                val sb = StringBuilder(line)
                index++
                while (index < lines.size) {
                    val next = lines[index]
                    if (next.isBlank() || next.trimStart().startsWith("```") ||
                        next.startsWith("#") || Regex("^[-*+]\\s+").containsMatchIn(next) ||
                        Regex("^\\d+[.)]\\s+").containsMatchIn(next) || next.startsWith(">")
                    ) break
                    sb.append('\n').append(next.trimEnd())
                    index++
                }
                out.add(MdNode.Block(sb.toString()))
            }
        }
    }
    return out
}

/** 只有能在本机 WebView 里真跑起来的语言才给三角号 */
private fun isRunnableLang(lang: String): Boolean =
    lang.lowercase() in setOf("html", "htm", "xhtml", "css", "javascript", "js", "svg")

private fun isMarkdownLang(lang: String): Boolean =
    lang.lowercase() in setOf("markdown", "md", "text", "plain", "txt")

private fun fileNameFor(lang: String): String {
    val ext = when (lang.lowercase()) {
        "python", "py" -> "py"
        "java" -> "java"
        "kotlin", "kt" -> "kt"
        "c++", "cpp", "cxx" -> "cpp"
        "c" -> "c"
        "c#", "csharp", "cs" -> "cs"
        "javascript", "js" -> "js"
        "typescript", "ts" -> "ts"
        "html", "xml" -> "html"
        "css" -> "css"
        "sql" -> "sql"
        "shell", "bash", "sh" -> "sh"
        "json" -> "json"
        "go" -> "go"
        "rust", "rs" -> "rs"
        "swift" -> "swift"
        else -> "txt"
    }
    return "deepchat_code." + ext
}
