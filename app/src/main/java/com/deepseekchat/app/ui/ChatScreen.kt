package com.deepseekchat.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepseekchat.app.data.AsrClient
import com.deepseekchat.app.data.LocalAsr
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepseekchat.app.data.Attachment
import com.deepseekchat.app.data.ChatMessage
import com.deepseekchat.app.data.DEEPSEEK_MODELS
import com.deepseekchat.app.data.modelById
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val chat by vm.chat.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val webSearch by vm.webSearch.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var autoFollow by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var modelMenu by remember { mutableStateOf(false) }
    var attachMenu by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> vm.addFiles(uris) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.addPhoto(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri?.let { vm.addPhoto(it) }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val said = result.data
                ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (said.isNotBlank()) {
                input = if (input.isBlank()) said else input + " " + said
            } else {
                vm.notify(L10n.t(language, "没听清，再说一次", "沒聽清，再說一次", "Didn't catch that"))
            }
        }
    }

    // ---- 内置语音识别：点麦克风进入「按住说话」语音条 ----
    var recording by remember { mutableStateOf(false) }
    var recorderRef by remember { mutableStateOf<android.media.AudioRecord?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var recordThread by remember { mutableStateOf<Thread?>(null) }
    var voiceMode by remember { mutableStateOf(false) }
    var voiceStatus by remember { mutableStateOf("") }
    var voiceCancelArmed by remember { mutableStateOf(false) }
    val voiceAmp = remember { mutableFloatStateOf(0f) }
    val view = LocalView.current
    var doStartRecord: () -> Unit = {}
    var doStopRecord: (Boolean) -> Unit = {}

    var enterVoiceMode: () -> Unit = {}

    fun hasMicPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) voiceMode = false }

    // 松手：false=发送、true=上滑取消
    doStopRecord = { cancel ->
        recording = false
        runCatching { recorderRef?.stop() }
        runCatching { recorderRef?.release() }
        recorderRef = null
        val file = recordFile
        scope.launch {
            val pcm = withContext(Dispatchers.IO) {
                runCatching { file?.readBytes() ?: ByteArray(0) }.getOrDefault(ByteArray(0))
            }
            runCatching { file?.delete() }
            when {
                cancel -> voiceStatus = ""
                pcm.size < 6400 -> voiceStatus = L10n.t(
                    language, "太短了，按住再说", "太短了，按住再說", "Too short, hold and speak"
                )
                else -> {
                    voiceStatus = L10n.t(language, "识别中…", "識別中…", "Transcribing…")
                    val mode = if (vm.asrMode.value == "online" && vm.asrKey.value.isBlank()) "local" else vm.asrMode.value
                    val said = withContext(Dispatchers.IO) {
                        if (mode == "online") {
                            AsrClient.transcribe(
                                vm.asrBase.value, vm.asrKey.value, vm.asrModel.value, AsrClient.pcmToWav(pcm)
                            )
                        } else {
                            LocalAsr.recognize(context, pcm)
                        }
                    }
                    voiceStatus = ""
                    if (said.isNullOrBlank()) {
                        voiceStatus = L10n.t(
                            language, "没听清，再说一次", "沒聽清，再說一次", "Didn't catch that"
                        )
                    } else {
                        input = ""
                        vm.send(said)
                    }
                }
            }
        }
    }

    doStartRecord = {
        if (!recording) {
            val minBuf = android.media.AudioRecord.getMinBufferSize(
                16000,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            val record = runCatching {
                android.media.AudioRecord(
                    android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
            }.getOrNull()
            if (record == null || record.state != android.media.AudioRecord.STATE_INITIALIZED) {
                voiceStatus = L10n.t(
                    language, "麦克风打不开（可能被别的 App 占着）", "麥克風打不開（可能被別的 App 佔着）",
                    "Microphone unavailable (maybe another app is using it)"
                )
            } else {
                if (vm.asrMode.value != "online") {
                    // 边录边把引擎加载起来，说完就不用等
                    scope.launch(Dispatchers.IO) { runCatching { LocalAsr.isReady(context) } }
                }
                val file = File(context.cacheDir, "voice_" + System.currentTimeMillis() + ".pcm")
                val out = file.outputStream()
                recorderRef = record
                recordFile = file
                recording = true
                record.startRecording()
                recordThread = Thread {
                    val buf = ByteArray(minBuf)
                    while (recording) {
                        val n = runCatching { record.read(buf, 0, buf.size) }.getOrDefault(0)
                        if (n > 0) {
                            runCatching { out.write(buf, 0, n) }
                            // 算 RMS 给波形用
                            var sum = 0.0
                            var count = 0
                            var i = 0
                            while (i + 1 < n) {
                                val v = (buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)
                                sum += (v * v).toDouble()
                                count++
                                i += 2
                            }
                            if (count > 0) {
                                val rms = kotlin.math.sqrt(sum / count)
                                val level = (rms / 6000.0).toFloat().coerceIn(0.06f, 1f)
                                voiceAmp.floatValue = level
                            }
                        }
                    }
                    runCatching { out.close() }
                    voiceAmp.floatValue = 0f
                }.also { it.start() }
                voiceStatus = ""
            }
        }
    }

    fun onVoiceHoldStart(): Boolean {
        if (!hasMicPermission()) {
            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            return false
        }
        doStartRecord()
        return recording
    }

    fun onVoiceHoldEnd(cancel: Boolean) {
        if (recording) doStopRecord(cancel)
    }

    fun startVoice() {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE,
                if (language.startsWith("en")) "en-US" else "zh-CN"
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, L10n.t(language, "说话，正在识别…", "說話，正在識別…", "Speak now…"))
        }
        runCatching { voiceLauncher.launch(intent) }.onFailure {
            vm.notify(L10n.t(language, "这台设备没装语音识别服务", "這台設備沒裝語音識別服務", "No speech service on this device"))
        }
    }

    fun launchCamera() {
        runCatching {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { vm.notify("这台机器调不起相机，用「选择文件」挑照片吧") }
    }

    // 边写边跟；只要是自己发了新消息，强制回到最新并继续跟随
    var lastUserId by remember { mutableStateOf<Long?>(null) }
    // 注意：思考过程的字数也要算进 key，否则开着深度思考时只会跟 content，
    // 思考阶段内容涨了不滚，看起来就像“不自动跟随”
    val lastBar = chat.messages.lastOrNull()
    LaunchedEffect(
        chat.messages.size,
        lastBar?.content?.length,
        lastBar?.reasoning?.length
    ) {
        val newestUser = chat.messages.lastOrNull { it.role == "user" }?.id
        if (newestUser != null && newestUser != lastUserId) {
            lastUserId = newestUser
            autoFollow = true
            if (chat.messages.isNotEmpty()) {
                listState.scrollToItem(chat.messages.lastIndex, Int.MAX_VALUE)
            }
        }
        if (chat.messages.isNotEmpty() && autoFollow) {
            listState.scrollToItem(chat.messages.lastIndex, Int.MAX_VALUE)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()
                autoFollow = last == null ||
                    (last.index >= chat.messages.lastIndex &&
                        last.offset + last.size <= info.viewportEndOffset + 24)
            }
        }
    }

    // 改回系统自带的文本选择浮条（不再自己画，省得越改越崩）
    val selToolbar = LocalTextToolbar.current
    val focus = LocalFocusManager.current

    enterVoiceMode = {
        voiceMode = true
        voiceStatus = ""
        keyboard?.hide()
        focus.clearFocus()
        if (!hasMicPermission()) {
            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CompositionLocalProvider(LocalTextToolbar provides selToolbar) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 86.dp)
        ) {
        // 顶部：深度思考 + 模型 + 价格 + 新对话
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val modelInfo = modelById(chat.model)
            FilterChip(
                selected = thinking,
                enabled = modelInfo.supportsThinkingToggle || modelInfo.thinking,
                onClick = { vm.toggleThinking() },
                label = {
                    Text(
                        if (thinking) L10n.deepThinking(language) else L10n.normalMode(language),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            )

            Box {
                FilterChip(
                    selected = false,
                    onClick = { modelMenu = true },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                shortModelName(modelInfo.id),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
                DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        DEEPSEEK_MODELS.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.label, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            item.hint,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            item.price,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    vm.setModel(item.id)
                                    modelMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 联网开关：单个圆角按钮，不占文字宽度
            val onColor = MaterialTheme.colorScheme.primary
            val offColor = MaterialTheme.colorScheme.onSurfaceVariant
            Surface(
                shape = CircleShape,
                color = if (webSearch) onColor.copy(alpha = 0.16f) else Color.Transparent,
                border = if (webSearch) null else BorderStroke(1.dp, offColor.copy(alpha = 0.5f)),
                modifier = Modifier.size(34.dp)
            ) {
                IconButton(
                    onClick = { vm.toggleWebSearch() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        if (webSearch) Icons.Filled.Search else Icons.Outlined.Search,
                        contentDescription = if (webSearch) L10n.t(language, "联网已开", "聯網已開", "Web search on")
                        else L10n.t(language, "联网已关", "聯網已關", "Web search off"),
                        tint = if (webSearch) onColor else offColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            PriceChip(chat.spent, language)

            Spacer(Modifier.weight(1f))
            IconButton(onClick = vm::newChat, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = L10n.newChat(language),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (chat.messages.isEmpty()) {
            EmptyChatHint(language)
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.pressed }) autoFollow = false
                            }
                        }
                    },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chat.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        streaming = chat.sending && message.id == chat.messages.lastOrNull()?.id,
                        isLast = message.id == chat.messages.lastOrNull()?.id,
                        language = language,
                        onContinue = { vm.continueLast() },
                        onRegenerate = { vm.regenerate() }
                    )
                }
            }
        }

        }
        }

        // 向上翻历史后，右下角冒出一个向下箭头，点一下回到最新消息
        androidx.compose.animation.AnimatedVisibility(
            visible = !autoFollow && chat.messages.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 94.dp),
            enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(38.dp)
                    .clickable {
                        autoFollow = true
                        if (chat.messages.isNotEmpty()) {
                            scope.launch {
                                listState.animateScrollToItem(chat.messages.lastIndex, Int.MAX_VALUE)
                            }
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "回到最新",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 底部浮层：附件 + 输入框（永远贴在底部，不会被挤掉）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (pending.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pending) { att ->
                        AttachmentChip(
                            att = att,
                            onRemove = { vm.removeAttachment(pending.indexOf(att)) }
                        )
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = recording,
                        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
                    ) {
                        RecordingPanel(language = language, armed = voiceCancelArmed, amp = voiceAmp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 加号：打字模式和语音模式都在同一个位置
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { showAttachSheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "添加附件",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            androidx.compose.animation.Crossfade(
                                targetState = voiceMode,
                                animationSpec = tween(180),
                                label = "composer"
                            ) { isVoice ->
                                if (isVoice) {
                                    VoiceBar(
                                        modifier = Modifier.fillMaxWidth(),
                                        language = language,
                                        status = voiceStatus,
                                        recording = recording,
                                        armed = voiceCancelArmed,
                                        onArmChange = { voiceCancelArmed = it },
                                        onKeyboard = { voiceMode = false },
                                        onHoldStart = { onVoiceHoldStart() },
                                        onHoldEnd = { onVoiceHoldEnd(it) },
                                        onTick = { heavy ->
                                            view.performHapticFeedback(
                                                if (heavy) android.view.HapticFeedbackConstants.LONG_PRESS
                                                else android.view.HapticFeedbackConstants.KEYBOARD_TAP
                                            )
                                        }
                                    )
                                } else {
                                    CompositionLocalProvider(LocalTextToolbar provides selToolbar) {
                                        OutlinedTextField(
                                            value = input,
                                            onValueChange = { input = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerInput(input) {
                                                    // 没字（只有空格也算没字）时长按＝直接进录音；有字就走粘贴/全选
                                                    awaitEachGesture {
                                                        val down = awaitFirstDown(
                                                            requireUnconsumed = false,
                                                            pass = PointerEventPass.Initial
                                                        )
                                                        if (input.isNotBlank()) return@awaitEachGesture
                                                        val startedAt = System.currentTimeMillis()
                                                        while (true) {
                                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                                            val change = event.changes
                                                                .firstOrNull { it.id == down.id }
                                                                ?: return@awaitEachGesture
                                                            if (!change.pressed) return@awaitEachGesture
                                                            if ((change.position - down.position).getDistance() > 28f)
                                                                return@awaitEachGesture
                                                            if (System.currentTimeMillis() - startedAt > 420) {
                                                                change.consume()
                                                                enterVoiceMode()
                                                                return@awaitEachGesture
                                                            }
                                                        }
                                                    }
                                                },
                                            placeholder = { Text(L10n.sendPlaceholder(language)) },
                                            maxLines = 5,
                                            shape = RoundedCornerShape(22.dp),
                                            trailingIcon = {
                                                IconButton(onClick = { enterVoiceMode() }) {
                                                    Icon(
                                                        AppIcons.Mic,
                                                        contentDescription = L10n.t(
                                                            language, "语音输入", "語音輸入", "Voice input"
                                                        ),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // 语音模式下只有带了附件才露出发送按钮
                        if (!voiceMode || pending.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            if (chat.sending) {
                                FilledIconButton(onClick = vm::stop) {
                                    Icon(Icons.Filled.Close, contentDescription = "停止")
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        val text = input
                                        input = ""
                                        keyboard?.hide()
                                        vm.send(text)
                                    },
                                    enabled = input.isNotBlank() || pending.isNotEmpty()
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = "发送")
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Text(
                    L10n.addAttachment(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachTile(L10n.camera(language), AppIcons.Camera) {
                        showAttachSheet = false
                        launchCamera()
                    }
                    AttachTile(L10n.gallery(language), AppIcons.Photo) {
                        showAttachSheet = false
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    AttachTile(L10n.files(language), AppIcons.File) {
                        showAttachSheet = false
                        filePicker.launch(
                            arrayOf("text/*", "image/*", "application/json", "application/octet-stream")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceBar(
    language: String,
    status: String,
    recording: Boolean,
    armed: Boolean,
    modifier: Modifier = Modifier,
    onArmChange: (Boolean) -> Unit,
    onKeyboard: () -> Unit,
    onHoldStart: () -> Boolean,
    onHoldEnd: (Boolean) -> Unit,
    onTick: (Boolean) -> Unit
) {
    val label = when {
        status.isNotBlank() -> status
        armed -> L10n.t(language, "松开手指，取消发送", "鬆開手指，取消傳送", "Release to cancel")
        recording -> L10n.t(language, "松手发送，上移取消", "放手傳送，上移取消", "Release to send, slide up to cancel")
        else -> L10n.t(language, "按住说话", "按住說話", "Hold to talk")
    }
    val barScale by animateFloatAsState(
        targetValue = if (recording) 0.975f else 1f,
        animationSpec = tween(180),
        label = "voiceBarScale"
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (armed) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .height(48.dp)
            .scale(barScale)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            onArmChange(false)
                            val started = onHoldStart()
                            if (!started) return@awaitEachGesture
                            onTick(false)
                            val threshold = 72.dp.toPx()
                            var cancel = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val dy = change.position.y - down.position.y
                                val c = dy < -threshold
                                if (c != cancel) {
                                    cancel = c
                                    onArmChange(c)
                                    onTick(c)
                                }
                                change.consume()
                            }
                            onHoldEnd(cancel)
                            onTick(false)
                            onArmChange(false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (armed) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onKeyboard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    AppIcons.Keyboard,
                    contentDescription = L10n.t(language, "切回键盘", "切回鍵盤", "Back to keyboard"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** 按住录音时从打底条上面顶起来的录音面板：提示文字 + 实时波形 */
@Composable
private fun RecordingPanel(language: String, armed: Boolean, amp: State<Float>, modifier: Modifier = Modifier) {
    val level = amp.value
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val barColor = if (armed) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = if (armed) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (armed) L10n.t(language, "松开手指，取消发送", "鬆開手指，取消傳送", "Release to cancel")
                else L10n.t(language, "松手发送，上移取消", "放手傳送，上移取消", "Release to send, slide up to cancel"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (armed) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                val bars = 36
                val step = size.width / bars
                for (i in 0 until bars) {
                    val wobble = kotlin.math.abs(
                        kotlin.math.sin((phase * 6.283f + i * 0.55f).toDouble())
                    ).toFloat()
                    val h = (size.height * (0.10f + 0.90f * level * (0.45f + 0.55f * wobble)))
                        .coerceIn(4f, size.height)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(i * step + step * 0.22f, (size.height - h) / 2f),
                        size = Size(step * 0.42f, h),
                        cornerRadius = CornerRadius(step * 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachTile(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PriceChip(spent: Double, lang: String = "zh-CN") {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clickable { open = true }
        ) {
            Text(
                text = "¥" + formatCost(spent),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    L10n.priceTitle(lang) + " ¥" + formatCost(spent),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    L10n.priceSub(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                DEEPSEEK_MODELS.forEach { item ->
                    Text(
                        "${item.label} · ${item.price}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        item.priceDetail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "最终以 platform.deepseek.com 价格页为准。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AttachmentChip(att: Attachment, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    att.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (att.kind == "image") "图片" else "已读取 ${att.chars} 字" + if (att.truncated) "（截断）" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyChatHint(lang: String = "zh-CN") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AppIcons.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(L10n.startChat(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = L10n.startChatHint(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    streaming: Boolean,
    isLast: Boolean = false,
    language: String = "zh-CN",
    onContinue: () -> Unit = {},
    onRegenerate: () -> Unit = {}
) {
    val isUser = message.role == "user"
    var showReasoning by remember(message.id) { mutableStateOf(message.reasoning.isNullOrBlank()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        AppIcons.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isUser) 22.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 22.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 1f)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                message.attachments.forEach { att ->
                    if (att.kind == "image") {
                        val bitmap = remember(att.name, att.path) {
                            decodeBase64(att.imageBase64) ?: decodeFile(att.path)
                        }
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            if (bitmap != null) {
                                val ctx = LocalContext.current
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = att.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .heightIn(min = 80.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            com.deepseekchat.app.ImageViewerActivity.open(
                                                ctx, att.path, att.imageBase64, att.name
                                            )
                                        }
                                )
                                Text(
                                    att.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    "🖼 " + att.name + "（图片已过期，重新发一次才能显示）",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val ctx = LocalContext.current
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .widthIn(max = 260.dp)
                                .clickable {
                                    com.deepseekchat.app.TextViewerActivity.open(
                                        ctx, att.name, att.text.orEmpty()
                                    )
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "📄 " + att.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    att.chars.toString() + " 字 · 点开查看",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!att.text.isNullOrBlank()) {
                                    Text(
                                        att.text.take(90),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (message.attachments.isNotEmpty()) Spacer(Modifier.height(6.dp))

                if (!isUser && !message.reasoning.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.clickable { showReasoning = !showReasoning },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showReasoning) L10n.t(language, "收起思考过程", "收合思考過程", "Hide reasoning")
                            else L10n.t(language, "查看思考过程", "查看思考過程", "Show reasoning"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (showReasoning) {
                        Text(
                            text = message.reasoning.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                        )
                    }
                }

                val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else if (message.error) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface

                if (message.content.isBlank()) {
                    if (streaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (message.attachments.isEmpty()) {
                        Text("（没有内容）", color = textColor, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    SelectionContainer {
                        MarkdownText(message.content, textColor)
                    }
                }

                if (!isUser && !streaming && isLast) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onRegenerate,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(L10n.regenerate(language), style = MaterialTheme.typography.labelMedium)
                        }
                        if (message.truncated) {
                            Text(
                                "已截断",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = onContinue,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(L10n.continueGen(language), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCost(value: Double): String =
    if (value < 0.01) String.format(Locale.US, "%.3f", value) else String.format(Locale.US, "%.2f", value)

private fun decodeFile(path: String?): android.graphics.Bitmap? = runCatching {
    if (path.isNullOrBlank()) null else android.graphics.BitmapFactory.decodeFile(path)
}.getOrNull()

private fun decodeBase64(data: String?): android.graphics.Bitmap? {
    if (data.isNullOrBlank()) return null
    return runCatching {
        val bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun shortModelName(id: String): String = when (id) {
    "deepseek-flash" -> "flash"
    "deepseek-v4-pro" -> "v4-pro"
    "deepseek-chat" -> "chat"
    "deepseek-reasoner" -> "reasoner"
    else -> id.removePrefix("deepseek-")
}
