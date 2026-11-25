package com.deepseekchat.app.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepseekchat.app.data.DEEPSEEK_MODELS
import com.deepseekchat.app.WebPageActivity
import com.deepseekchat.app.ui.theme.ThemeMode

@Composable
fun SettingsScreen(vm: ChatViewModel) {
    val apiKey by vm.apiKey.collectAsStateWithLifecycle()
    val baseUrl by vm.baseUrl.collectAsStateWithLifecycle()
    val model by vm.model.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.dynamicColor.collectAsStateWithLifecycle()
    val temperature by vm.temperature.collectAsStateWithLifecycle()
    val maxTokens by vm.maxTokens.collectAsStateWithLifecycle()

    val asrBase by vm.asrBase.collectAsStateWithLifecycle()
    val asrKey by vm.asrKey.collectAsStateWithLifecycle()
    val asrModel by vm.asrModel.collectAsStateWithLifecycle()
    var asrBaseText by remember(asrBase) { mutableStateOf(asrBase) }
    var asrKeyText by remember(asrKey) { mutableStateOf(asrKey) }
    var asrModelText by remember(asrModel) { mutableStateOf(asrModel) }

    var keyText by remember(apiKey) { mutableStateOf(apiKey) }
    var baseText by remember(baseUrl) { mutableStateOf(baseUrl) }
    val diagnostics by vm.diagnostics.collectAsStateWithLifecycle()
    val testResult by vm.testResult.collectAsStateWithLifecycle()
    val traces by vm.traces.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var keySaved by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    var tokensText by remember(maxTokens) { mutableStateOf(maxTokens.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 28.dp)
    ) {
        SectionHeader(L10n.t(language,"接口","介面","API"))

        OutlinedTextField(
            value = keyText,
            onValueChange = {
                keyText = it
                vm.setApiKey(it)
                keySaved = true
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("DeepSeek API Key") },
            placeholder = { Text("sk-…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                androidx.compose.material3.TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "隐藏" else "显示", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (keySaved) "边打字边保存，粘贴完不用再点按钮（首尾空格会自动去掉）"
            else "粘贴后自动保存，会自动去掉首尾空格和换行",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::testConnection) { Text(L10n.t(language,"测试连接","測試連線","Test connection")) }
        }
        if (!testResult.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = testResult.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = baseText,
            onValueChange = { baseText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(L10n.t(language,"接口地址","介面網址","API base URL")) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            supportingText = { Text("默认 https://api.deepseek.com，一般不用改") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = { vm.setBaseUrl(baseText); vm.notify("接口地址已保存") }) { Text(L10n.t(language,"保存地址","儲存網址","Save URL")) }

        Spacer(Modifier.height(16.dp))
        SectionHeader(L10n.t(language,"模型（只能选 DeepSeek 自己的）","模型（只能選 DeepSeek 自己的）","Model (DeepSeek only)"))

        DEEPSEEK_MODELS.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = model == item.id, onClick = { vm.setModel(item.id) })
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = model == item.id, onClick = { vm.setModel(item.id) })
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(item.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        item.hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader(L10n.t(language,"生成参数","生成參數","Generation"))

        Text(
            text = "温度 ${"%.2f".format(temperature)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = temperature,
            onValueChange = vm::setTemperature,
            valueRange = 0f..2f,
            steps = 19
        )
        Text(
            text = "低一点更稳定严谨，高一点更有发散。日常 0.7 就行。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = tokensText,
            onValueChange = { tokensText = it.filter { c -> c.isDigit() }.take(5) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(L10n.t(language,"单次最大输出 tokens","單次最大輸出 tokens","Max output tokens")) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            supportingText = { Text(L10n.t(language,"默认 8192，范围 256–32768。写长代码就拉到 16000 以上","預設 8192，範圍 256–32768。寫長程式就拉到 16000 以上","Default 8192, range 256–32768. Use 16000+ for long code.")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = {
            val value = tokensText.toIntOrNull()?.coerceIn(256, 32768) ?: 8192
            tokensText = value.toString()
            vm.setMaxTokens(value)
            vm.notify("已设为 $value tokens")
        }) { Text("保存") }


        Spacer(Modifier.height(18.dp))
        SectionHeader(L10n.t(language,"语音识别","語音識別","Speech recognition"))
        val asrMode by vm.asrMode.collectAsStateWithLifecycle()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                    selected = asrMode != "online",
                    onClick = { vm.setAsrMode("local"); vm.notify("已切换：本地离线识别") },
                    label = { Text(L10n.t(language,"本地离线（内置）","本地離線（內建）","On-device")) }
            )
            FilterChip(
                selected = asrMode == "online",
                onClick = { vm.setAsrMode("online"); vm.notify("已切换：在线识别接口") },
                label = { Text(L10n.t(language,"在线接口","線上介面","Online API")) }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = L10n.t(
                language,
                "本地离线：模型已经打进 APK（中文小模型，42MB），装好就能断网识别，不花钱、不走服务器。在线接口：填一个 OpenAI 兼容的语音接口，识别更准、中英混说更好，默认走硅基流动（cloud.siliconflow.cn 有免费额度）。",
                "本地離線：模型已打進 APK（中文小模型，42MB），裝好就能斷網識別，不收費、不走伺服器。線上介面：填一個 OpenAI 相容的語音介面，識別更準、中英混說更好，預設走硅基流動（cloud.siliconflow.cn 有免費額度）。",
                "On-device: a small Chinese model ships inside the APK (42 MB), so recognition works offline, free, with no server. Online API: any OpenAI-compatible transcription endpoint - more accurate on mixed Chinese/English, defaults to SiliconFlow (free tier at cloud.siliconflow.cn)."
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = asrBaseText,
            onValueChange = { asrBaseText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(L10n.t(language,"语音接口地址","語音介面網址","ASR base URL")) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = asrKeyText,
            onValueChange = { asrKeyText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(L10n.t(language,"语音接口 Key","語音介面 Key","ASR API key")) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = asrModelText,
            onValueChange = { asrModelText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(L10n.t(language,"语音模型名","語音模型名稱","ASR model")) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            supportingText = { Text("硅基流动：FunAudioLLM/SenseVoiceSmall；OpenAI：whisper-1") }
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = {
            vm.setAsrBase(asrBaseText.trim())
            vm.setAsrKey(asrKeyText.trim())
            vm.setAsrModel(asrModelText.trim().ifBlank { "FunAudioLLM/SenseVoiceSmall" })
            vm.notify("语音识别设置已保存")
        }) { Text(L10n.t(language,"保存语音设置","儲存語音設定","Save speech settings")) }

        Spacer(Modifier.height(18.dp))
        SectionHeader(L10n.t(language,"语言 / 地区","語言 / 地區","Language / Region"))

        Text(
            text = L10n.languageHint(language),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        L10n.OPTIONS.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = language == option.code, onClick = { vm.setLanguage(option.code) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = language == option.code, onClick = { vm.setLanguage(option.code) })
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(option.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        L10n.hint(option.code, language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(L10n.t(language,"外观","外觀","Appearance"))

        ThemeRow(L10n.t(language,"跟随系统","跟隨系統","System"), themeMode == ThemeMode.SYSTEM) { vm.setThemeMode(ThemeMode.SYSTEM) }
        ThemeRow(L10n.t(language,"浅色","淺色","Light"), themeMode == ThemeMode.LIGHT) { vm.setThemeMode(ThemeMode.LIGHT) }
        ThemeRow(L10n.t(language,"深色","深色","Dark"), themeMode == ThemeMode.DARK) { vm.setThemeMode(ThemeMode.DARK) }

        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(L10n.t(language,"跟随壁纸取色","跟隨桌布取色","Wallpaper colors"), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        "用壁纸色调当主题色"
                    else "系统版本不支持",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onCheckedChange = vm::setDynamicColor
            )
        }


        Spacer(Modifier.height(18.dp))
        SectionHeader("DeepSeek Harness")

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "本地运行轨迹",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "每次请求的模型、思考开关、token 用量、耗时、花费都记在这里，对应 Harness 的 session log。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (traces.isEmpty()) {
                    Text(
                        "还没有记录，发一条消息就有了。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    traces.take(8).forEach { trace ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${if (trace.ok) "" else "✕ "}${trace.model}" +
                                        if (trace.thinking) " · 思考" else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (trace.ok) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "${trace.promptTokens}↑ / ${trace.completionTokens}↓ tokens · " +
                                        "${trace.durationMs / 1000.0}s · ¥" +
                                        String.format(java.util.Locale.US, "%.3f", trace.cost) +
                                        " · " + formatTraceTime(trace.at),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!trace.ok && trace.error.isNotBlank()) {
                                    Text(
                                        trace.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = vm::clearTraces, contentPadding = PaddingValues(0.dp)) {
                        Text("清空轨迹", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        WebPageActivity.open(
                            context,
                            "https://www.deepseek.com/harness/",
                            "DeepSeek Harness"
                        )
                    }
                ) {
                    Text("打开 DeepSeek Harness")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Harness 本体是 Node 端程序（npx @deepseek-ai/dsh web），手机上只能这样看官网、插件和文档。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(L10n.t(language,"关于","關於","About"))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "● 聊天走 api.deepseek.com 官方接口，用你自己的 API Key，不经过任何第三方服务器。\n" +
                        "● 不做网页版登录，也不用账号密码；Key 只存在这台手机上。\n" +
                        "● 模型只开放 deepseek-chat / deepseek-reasoner 两个。\n" +
                        "● 余额页直接调 /user/balance；下面的网页只是官方控制台，用来看用量明细。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionHeader(L10n.t(language,"诊断日志","診斷日誌","Diagnostics"))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                if (diagnostics.isEmpty()) {
                    Text(
                        "还没请求过。填好 Key 点「测试连接」，这里会显示请求地址、状态码和服务器原文。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = diagnostics.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.TextButton(onClick = vm::clearDiagnostics) {
                    Text("清空日志", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "DeepChat " + appVersion(context),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatTraceTime(at: Long): String =
    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(at))

private fun appVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}.getOrDefault("?")
