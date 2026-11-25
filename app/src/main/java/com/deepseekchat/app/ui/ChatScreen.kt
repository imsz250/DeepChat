package com.deepseekchat.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    LaunchedEffect(chat.messages.size, chat.messages.lastOrNull()?.content?.length) {
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

    val selState = remember { Md3TextToolbarState() }
    val selToolbar = remember(selState) { Md3TextToolbar(selState) }
    val focus = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        // 任何触摸（滑动切页、点输入框、点按钮）先收掉选择浮条，避免残留导致掉帧
                        if (event.changes.any { it.pressed } && selState.visible) {
                            selState.visible = false
                            focus.clearFocus()
                        }
                    }
                }
            }
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

        Md3SelectionToolbar(
            state = selState,
            copyLabel = L10n.t(language, "复制", "複製", "Copy"),
            selectAllLabel = L10n.t(language, "全选", "全選", "Select all")
        )

        if (!autoFollow && chat.messages.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 96.dp)
                    .clickable {
                        autoFollow = true
                        scope.launch { listState.scrollToItem(chat.messages.lastIndex, Int.MAX_VALUE) }
                    }
            ) {
                Text(
                    "↓ " + L10n.latest(language),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // 底部浮层：附件 + 输入框，永远贴在页面底部，不会被挤掉
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(L10n.sendPlaceholder(language)) },
                    maxLines = 5,
                    shape = RoundedCornerShape(22.dp)
                )
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
