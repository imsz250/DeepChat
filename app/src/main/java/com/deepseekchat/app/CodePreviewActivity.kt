package com.deepseekchat.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepseekchat.app.ui.ChatViewModel
import com.deepseekchat.app.ui.theme.DeepChatTheme

/** 代码预览：HTML/CSS/JS 真渲染，其余语言带行号与高亮的只读视图 */
class CodePreviewActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_LANG = "extra_lang"
        private const val EXTRA_CODE = "extra_code"
        private const val EXTRA_MODE = "extra_mode"

        fun open(context: Context, lang: String, code: String, mode: String = "run") {
            context.startActivity(
                Intent(context, CodePreviewActivity::class.java)
                    .putExtra(EXTRA_LANG, lang)
                    .putExtra(EXTRA_CODE, code)
                    .putExtra(EXTRA_MODE, mode)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lang = intent.getStringExtra(EXTRA_LANG).orEmpty().lowercase()
        val code = intent.getStringExtra(EXTRA_CODE).orEmpty()
        val mode = intent.getStringExtra(EXTRA_MODE).orEmpty().ifBlank { "run" }

        setContent {
            val vm: ChatViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by vm.dynamicColor.collectAsStateWithLifecycle()
            val seedKey by vm.themeSeed.collectAsStateWithLifecycle()
            DeepChatTheme(themeMode = themeMode, dynamicColor = dynamicColor, seedKey = seedKey) {
                val html = remember(lang, code, mode) { buildHtml(lang, code, mode) }
                var pageTitle by remember { mutableStateOf(displayLang(lang)) }
                val saveLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("text/plain")
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            contentResolver.openOutputStream(uri)?.use { it.write(code.toByteArray()) }
                        }.onSuccess {
                            android.widget.Toast.makeText(this@CodePreviewActivity, "已保存", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        if (mode == "code") "全屏代码" else "运行 / 预览",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        pageTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    val clipboard = this@CodePreviewActivity.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("code", code)
                                    )
                                    android.widget.Toast.makeText(this@CodePreviewActivity, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                                }) { Text("复制") }
                                TextButton(onClick = { saveLauncher.launch(fileNameFor(lang)) }) {
                                    Text("保存")
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                        )
                    }
                ) { padding ->
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(Color.parseColor("#111318"))
                                settings.javaScriptEnabled = html.contains("<script")
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        view?.evaluateJavascript(
                                            "(function(){var t=document.title;if(t){window.__dcTitle=t;}})();"
                                        ) { }
                                        view?.evaluateJavascript("window.__dcTitle") { value ->
                                            val title = value?.trim()?.trim('"')
                                            if (!title.isNullOrBlank() && title != "null") {
                                                pageTitle = title
                                            }
                                        }
                                    }
                                }
                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun displayLang(lang: String): String = when (lang) {
    "html", "xml" -> "HTML"
    "css" -> "CSS"
    "javascript", "js" -> "JavaScript"
    "typescript", "ts" -> "TypeScript"
    "python", "py" -> "Python"
    "java" -> "Java"
    "kotlin", "kt" -> "Kotlin"
    "c" -> "C"
    "c++", "cpp", "cxx" -> "C++"
    "c#", "csharp", "cs" -> "C#"
    "sql" -> "SQL"
    "shell", "bash", "sh" -> "Shell"
    "go" -> "Go"
    "rust", "rs" -> "Rust"
    else -> lang.ifBlank { "code" }
}

private fun escapeHtml(s: String): String = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun buildHtml(lang: String, code: String, mode: String = "run"): String {
    // 本身就是一份完整 HTML（模型经常直接给整页），原样交给 WebView，别再套一层
    val isHtml = lang in setOf("html", "xml", "svg")
    if (mode != "code" && isHtml &&
        (code.contains("<html", true) || code.contains("<!doctype", true))
    ) return code

    val body = if (mode == "code") plainCodeBody(code) else when (lang) {
        "html", "xml", "svg" -> code
        "css" -> """
            <style>${code}</style>
            <div class="dc-demo">
              <h1>CSS 预览</h1>
              <p>这段是示例内容，用来展示你的样式效果：颜色、字体、间距、圆角、动画都会生效。</p>
              <button class="btn">按钮</button>
              <div class="card"><span>卡片</span></div>
            </div>
        """.trimIndent()

        "javascript", "js", "typescript", "ts" -> """
            <style>
              body{font-family:system-ui,sans-serif;padding:16px}
              #out{margin-top:12px;padding:12px;border-radius:10px;background:#1b1f27;color:#d7e0ea;font-family:ui-monospace,monospace;white-space:pre-wrap}
            </style>
            <h2>JS 运行结果</h2>
            <div id="out">运行中…</div>
            <script>
              (function(){
                var out = document.getElementById('out');
                var logs = [];
                var originLog = console.log;
                console.log = function(){ logs.push(Array.prototype.slice.call(arguments).join(' ')); originLog.apply(console, arguments); };
                try {
                  (function(){ ${code} })();
                  out.textContent = logs.length ? logs.join('\n') : '脚本已执行（没有输出）';
                } catch (e) {
                  out.textContent = '报错：' + e;
                }
              })();
            </script>
        """.trimIndent()

        else -> plainCodeBody(code)
    }
    return """
        <!doctype html>
        <html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>${displayLang(lang)} 预览</title>
        <style>html,body{margin:0}</style>
        </head><body>$body</body></html>
    """.trimIndent()
}

private fun withLineNumbers(html: String): String =
    html.split("\n").joinToString("\n") { "<span class=\"ln\">$it</span>" }

private val KEYWORDS = setOf(
    "class", "fun", "def", "import", "from", "return", "if", "else", "for", "while",
    "public", "private", "protected", "static", "void", "int", "long", "float", "double",
    "bool", "boolean", "char", "new", "package", "namespace", "include", "using", "struct",
    "auto", "val", "var", "let", "const", "function", "func", "async", "await", "try",
    "catch", "throw", "switch", "case", "break", "continue", "true", "false", "null",
    "None", "self", "this", "select", "where", "insert", "update", "delete", "echo", "print",
    "std", "cout", "cin", "endl", "string", "String", "List", "dict", "lambda", "elif"
)

/** 逐字符扫描做高亮，避免把生成的 HTML 标签再匹配一遍 */
private fun highlight(code: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < code.length) {
        val c = code[i]
        // 行注释
        if ((c == '/' && i + 1 < code.length && code[i + 1] == '/') ||
            c == '#' || (c == '-' && i + 1 < code.length && code[i + 1] == '-')
        ) {
            val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
            sb.append("<span class=\"cm\">").append(escapeHtml(code.substring(i, end))).append("</span>")
            i = end
            continue
        }
        // 字符串
        if (c == '"' || c == '\'' || c == '`') {
            var j = i + 1
            while (j < code.length && code[j] != c) {
                if (code[j] == '\\') j++
                j++
            }
            val end = (j + 1).coerceAtMost(code.length)
            sb.append("<span class=\"str\">").append(escapeHtml(code.substring(i, end))).append("</span>")
            i = end
            continue
        }
        // 数字
        if (c.isDigit()) {
            var j = i
            while (j < code.length && (code[j].isDigit() || code[j] == '.')) j++
            sb.append("<span class=\"num\">").append(code.substring(i, j)).append("</span>")
            i = j
            continue
        }
        // 标识符 / 关键字
        if (c.isLetter() || c == '_') {
            var j = i
            while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
            val word = code.substring(i, j)
            if (KEYWORDS.contains(word)) {
                sb.append("<span class=\"kw\">").append(word).append("</span>")
            } else {
                sb.append(escapeHtml(word))
            }
            i = j
            continue
        }
        sb.append(escapeHtml(c.toString()))
        i++
    }
    return sb.toString()
}

private fun plainCodeBody(code: String): String = """
    <style>
      body{margin:0;background:#111318;color:#dbe3ec;font:14px/1.6 ui-monospace,SFMono-Regular,Menlo,monospace}
      pre{margin:0;padding:14px 12px;white-space:pre;overflow:auto;counter-reset:line}
      .ln{display:block;counter-increment:line}
      .ln:before{content:counter(line);display:inline-block;width:2.6em;color:#5c6675;text-align:right;padding-right:12px}
      .kw{color:#ff9d6b}.str{color:#a8d78a}.cm{color:#6c7686}.num{color:#d6b3ff}
      .selbar{position:fixed;display:none;align-items:center;background:#2b303b;border-radius:24px;
              padding:2px 4px;box-shadow:0 8px 22px rgba(0,0,0,.5);z-index:99}
      .selbar button{background:transparent;border:0;color:#dbe3ec;font:15px system-ui,Roboto,sans-serif;
              padding:10px 16px;border-radius:22px}
      .selbar button:active{background:#3b4252}
    </style>
    <pre>${withLineNumbers(highlight(code))}</pre>
    <div class="selbar" id="selbar">
      <button id="btnCopy">复制</button>
      <button id="btnAll">全选</button>
    </div>
    <script>
    (function(){
      var bar=document.getElementById('selbar');
      var pre=document.querySelector('pre');
      function getSel(){var s=window.getSelection();return (s&&s.toString().trim().length>0)?s:null;}
      function place(){
        var s=getSel();
        if(!s){bar.style.display='none';return;}
        var r=s.getRangeAt(0).getBoundingClientRect();
        var w=bar.offsetWidth,h=bar.offsetHeight;
        var x=Math.max(8,Math.min(window.innerWidth-w-8, r.left+r.width/2-w/2));
        var y=r.top-h-10; if(y<8){y=r.bottom+10;}
        if(y+h>window.innerHeight-8){y=window.innerHeight-h-8;}
        bar.style.left=x+'px'; bar.style.top=y+'px'; bar.style.display='flex';
      }
      document.addEventListener('selectionchange',function(){setTimeout(place,50);});
      document.addEventListener('touchend',function(){setTimeout(place,50);});
      document.addEventListener('mousedown',function(e){
        if(e.target.tagName==='BUTTON'){e.preventDefault();}
      },true);
      document.getElementById('btnCopy').addEventListener('click',function(){
        var s=getSel(); if(!s){return;}
        var t=s.toString();
        var ok=false; try{ok=document.execCommand('copy');}catch(e){ok=false;}
        if(!ok&&navigator.clipboard){navigator.clipboard.writeText(t);ok=true;}
        var b=document.getElementById('btnCopy');
        b.textContent=ok?'已复制':'复制失败';
        setTimeout(function(){b.textContent='复制';},1200);
      });
      document.getElementById('btnAll').addEventListener('click',function(){
        var s=window.getSelection(); var r=document.createRange();
        r.selectNodeContents(pre); s.removeAllRanges(); s.addRange(r);
        setTimeout(place,60);
      });
    })();
    </script>
""".trimIndent()

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
    return "deepchat_code.$ext"
}
