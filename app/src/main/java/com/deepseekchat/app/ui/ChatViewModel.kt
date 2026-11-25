package com.deepseekchat.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepseekchat.app.data.AppPrefs
import com.deepseekchat.app.data.BalanceInfo
import com.deepseekchat.app.data.ChatMessage
import com.deepseekchat.app.data.Conversation
import com.deepseekchat.app.data.ConversationStore
import com.deepseekchat.app.data.TraceRecord
import com.deepseekchat.app.data.TraceStore
import android.net.Uri
import com.deepseekchat.app.data.Attachment
import com.deepseekchat.app.data.DeepSeekApi
import com.deepseekchat.app.data.DEEPSEEK_MODELS
import com.deepseekchat.app.data.DeepSeekModel
import com.deepseekchat.app.data.WebSearch
import com.deepseekchat.app.data.FileIngest
import com.deepseekchat.app.data.modelById
import com.deepseekchat.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatUi(
    val conversationId: String = UUID.randomUUID().toString(),
    val title: String = "新的对话",
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val model: String = "deepseek-chat",
    val lastError: String? = null,
    /** 这段对话自己花了多少钱 */
    val spent: Double = 0.0
)

data class BalanceUi(
    val loading: Boolean = false,
    val info: BalanceInfo? = null,
    val error: String? = null
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPrefs(app)
    private val store = ConversationStore(app)
    private val traceStore = TraceStore(app)

    val apiKey: StateFlow<String> = prefs.apiKey
    val baseUrl: StateFlow<String> = prefs.baseUrl
    val model: StateFlow<String> = prefs.model
    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
    val dynamicColor: StateFlow<Boolean> = prefs.dynamicColor
    val themeSeed: StateFlow<String> = prefs.themeSeed
    val temperature: StateFlow<Float> = prefs.temperature
    val maxTokens: StateFlow<Int> = prefs.maxTokens
    val conversations: StateFlow<List<Conversation>> = store.items
    val thinking: StateFlow<Boolean> = prefs.thinking
    val language: StateFlow<String> = prefs.language
    val webSearch: StateFlow<Boolean> = prefs.webSearch

    val traces: StateFlow<List<TraceRecord>> = traceStore.items

    /** 还没发出去的附件 */
    private val _pending = MutableStateFlow<List<Attachment>>(emptyList())
    val pending: StateFlow<List<Attachment>> = _pending.asStateFlow()

    private val _chat = MutableStateFlow(ChatUi(model = prefs.model.value))
    val chat: StateFlow<ChatUi> = _chat.asStateFlow()

    private val _balance = MutableStateFlow(BalanceUi())
    val balance: StateFlow<BalanceUi> = _balance.asStateFlow()

    /** 调试日志：请求地址、状态码、服务端返回的原始片段 */
    private val _diagnostics = MutableStateFlow<List<String>>(emptyList())
    val diagnostics: StateFlow<List<String>> = _diagnostics.asStateFlow()

    /** 测试连接的结果，原样显示服务端返回 */
    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val messages = _messages.asSharedFlow()

    private var sendJob: Job? = null
    private var nextId = 1L
    private var preparing = false

    init {
        DeepSeekApi.logger = { line -> appendDiagnostic(line) }
    }

    @Synchronized
    private fun appendDiagnostic(line: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _diagnostics.update { current -> (current + "[$stamp] $line").takeLast(60) }
    }

    fun clearDiagnostics() {
        _diagnostics.value = emptyList()
        _testResult.value = null
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    // ---------------- 设置 ----------------

    val asrBase: StateFlow<String> = prefs.asrBase
    val asrKey: StateFlow<String> = prefs.asrKey
    val asrModel: StateFlow<String> = prefs.asrModel
    val asrMode: StateFlow<String> = prefs.asrMode

    fun setAsrBase(v: String) = prefs.setAsrBase(v)
    fun setAsrKey(v: String) = prefs.setAsrKey(v)
    fun setAsrModel(v: String) = prefs.setAsrModel(v)
    fun setAsrMode(v: String) = prefs.setAsrMode(v)

    fun setApiKey(v: String) = prefs.setApiKey(v)

    fun setBaseUrl(v: String) = prefs.setBaseUrl(v)

    fun setTemperature(v: Float) = prefs.setTemperature(v)

    fun setMaxTokens(v: Int) = prefs.setMaxTokens(v)

    fun setLanguage(code: String) = prefs.setLanguage(code)

    fun toggleWebSearch() = prefs.setWebSearch(!prefs.webSearch.value)

    fun setThemeMode(v: ThemeMode) = prefs.setThemeMode(v)

    fun setDynamicColor(v: Boolean) = prefs.setDynamicColor(v)

    fun setThemeSeed(seed: String) = prefs.setThemeSeed(seed)

    fun setModel(v: String) {
        val allowed = DEEPSEEK_MODELS.any { it.id == v }
        if (!allowed) {
            _messages.tryEmit("只能选 DeepSeek 的模型")
            return
        }
        prefs.setModel(v)
        _chat.update { it.copy(model = v) }
    }

    fun setThinking(enabled: Boolean) {
        prefs.setThinking(enabled)
        _messages.tryEmit(if (enabled) "已开启深度思考" else "已关闭深度思考")
    }

    /** 深度思考开关：直接控制 thinking 参数，不动模型 */
    fun toggleThinking() {
        setThinking(!prefs.thinking.value)
    }

    // ---------------- 附件 ----------------

    /** 用户从系统文件选择器挑了一堆文件（文字或图片） */
    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty() || preparing) return
        preparing = true
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        val mime = getApplication<Application>().contentResolver.getType(uri)
                        FileIngest.read(getApplication(), uri, mime)
                    }.getOrNull()
                }
            }
            preparing = false
            if (added.isEmpty()) {
                _messages.tryEmit("这个文件读不出来（可能不是文本或图片）")
                return@launch
            }
            _pending.update { (it + added).take(6) }
            added.filter { it.kind == "text" && it.truncated }.firstOrNull()?.let {
                _messages.tryEmit("${it.name} 太长了，已截断到前 1.2 万字")
            }
        }
    }

    /** 拍照后直接加进来 */
    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            val (att, savedPath) = withContext(Dispatchers.IO) {
                runCatching {
                    val ctx = getApplication<Application>()
                    val realName = FileIngest.queryName(ctx, uri)
                        .ifBlank { "照片_" + stamp() + ".jpg" }
                    val parsed = FileIngest.readImage(ctx, uri, realName) ?: return@runCatching null
                    // 复制一份到私有目录，历史记录里也能出缩略图
                    val dir = java.io.File(ctx.filesDir, "images").apply { mkdirs() }
                    val file = java.io.File(dir, UUID.randomUUID().toString() + ".jpg")
                    val bytes = android.util.Base64.decode(parsed.imageBase64, android.util.Base64.DEFAULT)
                    file.writeBytes(bytes)
                    parsed.copy(path = file.absolutePath) to file.absolutePath
                }.getOrNull()
            } ?: run {
                _messages.tryEmit("照片读取失败")
                return@launch
            }
            _pending.update { (it + att).take(6) }
        }
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun removeAttachment(index: Int) {
        _pending.update { list -> list.filterIndexed { i, _ -> i != index } }
    }

    fun clearAttachments() {
        _pending.value = emptyList()
    }

    fun notify(text: String) {
        _messages.tryEmit(text)
    }

    /** 用最小请求测 Key 到底能不能用，返回结果原样展示 */
    fun testConnection() {
        val key = prefs.apiKey.value.trim()
        if (key.isBlank()) {
            _testResult.value = "还没填 API Key"
            return
        }
        _testResult.value = "测试中…"
        appendDiagnostic("—— 测试连接：POST ${prefs.baseUrl.value}/chat/completions ——")
        viewModelScope.launch {
            runCatching {
                DeepSeekApi.streamChat(
                    apiKey = key,
                    baseUrl = prefs.baseUrl.value,
                    model = "deepseek-chat",
                    messages = listOf(ChatMessage(id = 0, role = "user", content = "ping")),
                    temperature = 0.0,
                    maxTokens = 16,
                    thinking = false
                ) { _, _ -> }
            }.onSuccess {
                _testResult.value = "Key 可用，模型能正常返回内容"
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                _testResult.value = error.message.orEmpty().ifBlank { "请求失败" }
            }
        }
    }

    /** 只查余额，不聊天 */
    fun refreshBalance() {
        if (_balance.value.loading) return
        val key = prefs.apiKey.value.trim()
        if (key.isBlank()) {
            _balance.value = BalanceUi(loading = false, error = "还没填 API Key")
            return
        }
        _balance.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { DeepSeekApi.balance(key, prefs.baseUrl.value) }
            }.onSuccess { info ->
                _balance.value = BalanceUi(loading = false, info = info)
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                _balance.value = BalanceUi(
                    loading = false,
                    error = error.message.orEmpty().ifBlank { "余额查询失败" }
                )
            }
        }
    }

    // ---------------- 对话 ----------------

    /** 立即把当前对话存下来（切页面、退出、打开别的对话时调用） */
    fun saveNow() {
        persistCurrent()
    }

    fun newChat() {
        persistCurrent()
        sendJob?.cancel()
        _chat.value = ChatUi(model = prefs.model.value)
    }

    fun openConversation(conversation: Conversation) {
        persistCurrent()
        sendJob?.cancel()
        nextId = (conversation.messages.maxOfOrNull { it.id } ?: 0L) + 1
        _chat.value = ChatUi(
            conversationId = conversation.id,
            title = conversation.title,
            messages = conversation.messages,
            model = conversation.model.ifBlank { prefs.model.value },
            spent = conversation.spent
        )
    }

    fun deleteConversation(id: String) {
        store.remove(id)
        if (_chat.value.conversationId == id) newChat()
    }

    /** 「继续生成」：把断掉的地方接着写，界面不显示这条内部指令 */
    fun continueLast() {
        val last = _chat.value.messages.lastOrNull() ?: return
        if (_chat.value.sending) return
        send("接着上面被截断的地方继续写完，从中断处直接续写，不要重复已经输出的内容。", hidden = true)
    }

    fun send(text: String, hidden: Boolean = false) {
        val input = text.trim()
        if (_chat.value.sending) return
        if (input.isEmpty() && _pending.value.isEmpty()) return
        val key = prefs.apiKey.value.trim()
        if (key.isBlank()) {
            _messages.tryEmit("先去「设置」填 DeepSeek 的 API Key")
            return
        }

        val attachments = _pending.value
        val modelInfo = modelById(_chat.value.model)
        if (attachments.any { it.kind == "image" } && !modelInfo.vision) {
            _messages.tryEmit("${modelInfo.label} 不支持看图，图片请换 deepseek-flash 再发")
            return
        }

        val userMessage = ChatMessage(
            id = nextId++,
            role = "user",
            content = input,
            attachments = attachments
        )
        _pending.value = emptyList()
        val assistantId = nextId++
        val assistant = ChatMessage(id = assistantId, role = "assistant", content = "")
        val history = _chat.value.messages + userMessage
        val uiMessages = if (hidden) _chat.value.messages else history

        _chat.update {
            it.copy(
                messages = uiMessages + assistant,
                sending = true,
                lastError = null,
                title = if (it.messages.isEmpty()) {
                    makeTitle(if (input.isNotBlank()) input else attachments.firstOrNull()?.name ?: "对话")
                } else it.title
            )
        }

        launchStream(
            key = key,
            modelName = _chat.value.model,
            modelInfo = modelInfo,
            history = history,
            assistantId = assistantId,
            useSearch = !hidden && prefs.webSearch.value && input.isNotBlank()
        )
    }

    /** 重新生成：扔掉最后一条回答，用相同的上下文再请求一遍 */
    fun regenerate() {
        val state = _chat.value
        if (state.sending) return
        val lastUser = state.messages.indexOfLast { it.role == "user" }
        if (lastUser < 0) return
        val key = prefs.apiKey.value.trim()
        if (key.isBlank()) {
            _messages.tryEmit("先去「设置」填 DeepSeek 的 API Key")
            return
        }
        val modelInfo = modelById(state.model)
        val assistantId = nextId++
        val history = state.messages.take(lastUser + 1)
        _chat.update {
            it.copy(
                messages = history + ChatMessage(id = assistantId, role = "assistant", content = ""),
                sending = true,
                lastError = null
            )
        }
        launchStream(
            key = key,
            modelName = state.model,
            modelInfo = modelInfo,
            history = history,
            assistantId = assistantId,
            useSearch = prefs.webSearch.value
        )
    }

    /** 流式请求的公共部分：send 和 regenerate 都走这里 */
    private fun launchStream(
        key: String,
        modelName: String,
        modelInfo: DeepSeekModel,
        history: List<ChatMessage>,
        assistantId: Long,
        useSearch: Boolean = false
    ) {
        sendJob = viewModelScope.launch {
            try {

            var finalHistory = history
            var searched = false
            if (useSearch) {
                val query = history.lastOrNull { it.role == "user" }?.content.orEmpty()
                if (query.isNotBlank()) {
                    updateAssistant(assistantId, "正在联网搜索…", null)
                    val hits = runCatching { WebSearch.search(query) }.getOrDefault(emptyList())
                    if (hits.isEmpty()) {
                        _messages.tryEmit("联网搜索这轮没拿到结果，先按普通回答发")
                    } else {
                        finalHistory = history.dropLast(1) + history.last().copy(
                            content = history.last().content + "\n\n" + WebSearch.asContext(query, hits)
                        )
                        searched = true
                    }
                }
            }

            val buffer = StringBuilder()
            val reasoningBuffer = StringBuilder()
            val startedAt = System.currentTimeMillis()
            var promptTokens = 0
            var completionTokens = 0
            var callCost = 0.0
            var failure: String? = null

            fun consume(content: String?, reasoning: String?) {
                if (content != null) buffer.append(content)
                if (reasoning != null) reasoningBuffer.append(reasoning)
                updateAssistant(assistantId, buffer.toString(), reasoningBuffer.toString().ifBlank { null })
            }

            suspend fun streamOnce(hist: List<ChatMessage>): Boolean {
                runCatching {
                    DeepSeekApi.streamChat(
                        apiKey = key,
                        baseUrl = prefs.baseUrl.value,
                        model = modelName,
                        messages = hist,
                        temperature = prefs.temperature.value.toDouble(),
                        maxTokens = prefs.maxTokens.value,
                        thinking = prefs.thinking.value && modelInfo.supportsThinkingToggle,
                        onFinish = { reason ->
                            if (reason == "length") {
                                updateAssistant(
                                    assistantId,
                                    buffer.toString(),
                                    reasoningBuffer.toString().ifBlank { null },
                                    truncated = true
                                )
                            }
                        },
                        onUsage = { prompt, completion ->
                            promptTokens = prompt
                            completionTokens = completion
                            val cost = prompt / 1_000_000.0 * modelInfo.inputPerM +
                                completion / 1_000_000.0 * modelInfo.outputPerM
                            callCost += cost
                            _chat.update { it.copy(spent = it.spent + cost) }
                        }
                    ) { content, reasoning -> consume(content, reasoning) }
                }.onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) {
                        persistCurrent()
                        throw error
                    }
                    val msg = error.message.orEmpty().ifBlank { "网络请求失败" }
                    _chat.update { it.copy(lastError = msg) }
                    updateAssistant(
                        assistantId,
                        buffer.toString(),
                        reasoningBuffer.toString().ifBlank { null },
                        error = buffer.isEmpty(),
                        errorText = msg
                    )
                    failure = msg
                }
                return failure == null
            }

            var ok = streamOnce(finalHistory)

            // 联网注入后如果一句都没回，自动退回普通回答再试一次（不需要用户重发）
            if (ok && buffer.isEmpty() && searched) {
                updateAssistant(assistantId, "联网这轮没接到回复，改用普通回答重试…", null)
                finalHistory = history
                ok = streamOnce(history)
            }

            if (ok && buffer.isEmpty() && failure == null) {
                updateAssistant(
                    assistantId, "", null,
                    error = true,
                    errorText = "模型这次没返回内容，点「重新生成」或再发一次试试"
                )
                failure = "empty"
            }

            traceStore.add(
                TraceRecord(
                    at = startedAt,
                    model = modelName,
                    thinking = prefs.thinking.value && modelInfo.supportsThinkingToggle,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    cost = callCost,
                    durationMs = System.currentTimeMillis() - startedAt,
                    ok = failure == null,
                    error = failure.orEmpty(),
                    chars = buffer.length + reasoningBuffer.length
                )
            )
            persistCurrent()
            } finally {
                // 不管成功、失败还是被取消，都不能把「发送中」留着，否则界面会一直卡在转圈
                _chat.update { it.copy(sending = false) }
            }
        }
    }

    private fun updateAssistant(
        id: Long,
        content: String,
        reasoning: String?,
        error: Boolean = false,
        errorText: String? = null,
        truncated: Boolean = false
    ) {
        _chat.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == id) {
                        msg.copy(
                            content = if (error && errorText != null) errorText else content,
                            reasoning = reasoning,
                            error = error,
                            truncated = truncated || msg.truncated
                        )
                    } else msg
                }
            )
        }
    }

    fun clearTraces() {
        traceStore.clear()
    }

    fun stop() {
        sendJob?.cancel()
        _chat.update { it.copy(sending = false) }
        persistCurrent()
    }

    private fun persistCurrent() {
        val state = _chat.value
        if (state.messages.isEmpty()) return
        if (state.messages.all { it.role != "user" }) return
        store.save(
            Conversation(
                id = state.conversationId,
                title = state.title,
                model = state.model,
                messages = state.messages.filter { it.content.isNotBlank() },
                updatedAt = System.currentTimeMillis(),
                spent = state.spent
            )
        )
    }

    private fun makeTitle(input: String): String {
        val oneLine = input.replace("\n", " ").trim()
        return if (oneLine.length <= 18) oneLine else oneLine.take(18) + "…"
    }
}
