package com.deepseekchat.app.data

import android.content.Context
import com.deepseekchat.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("deepchat_settings", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API, "").orEmpty())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(
        prefs.getString(KEY_BASE, "https://api.deepseek.com").orEmpty()
    )
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _model = MutableStateFlow(
        prefs.getString(KEY_MODEL, "deepseek-chat").orEmpty()
    )
    val model: StateFlow<String> = _model.asStateFlow()

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _temperature = MutableStateFlow(prefs.getFloat(KEY_TEMP, 0.7f))
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _maxTokens = MutableStateFlow(prefs.getInt(KEY_TOKENS, 8192))
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _webSearch = MutableStateFlow(prefs.getBoolean(KEY_WEB, true))
    val webSearch: StateFlow<Boolean> = _webSearch.asStateFlow()

    fun setWebSearch(value: Boolean) {
        prefs.edit().putBoolean(KEY_WEB, value).apply()
        _webSearch.value = value
    }

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "zh-CN") ?: "zh-CN")
    val language: StateFlow<String> = _language.asStateFlow()

    fun setLanguage(value: String) {
        prefs.edit().putString(KEY_LANGUAGE, value).apply()
        _language.value = value
    }

    private val _asrBase = MutableStateFlow(
        prefs.getString(KEY_ASR_BASE, "https://api.siliconflow.cn/v1").orEmpty()
    )
    val asrBase: StateFlow<String> = _asrBase.asStateFlow()

    fun setAsrBase(v: String) {
        prefs.edit().putString(KEY_ASR_BASE, v).apply()
        _asrBase.value = v
    }

    private val _asrKey = MutableStateFlow(prefs.getString(KEY_ASR_KEY, "").orEmpty())
    val asrKey: StateFlow<String> = _asrKey.asStateFlow()

    fun setAsrKey(v: String) {
        prefs.edit().putString(KEY_ASR_KEY, v).apply()
        _asrKey.value = v
    }

    private val _asrMode = MutableStateFlow(prefs.getString(KEY_ASR_MODE, "local") ?: "local")
    val asrMode: StateFlow<String> = _asrMode.asStateFlow()

    fun setAsrMode(v: String) {
        prefs.edit().putString(KEY_ASR_MODE, v).apply()
        _asrMode.value = v
    }

    private val _asrModel = MutableStateFlow(
        prefs.getString(KEY_ASR_MODEL, "FunAudioLLM/SenseVoiceSmall").orEmpty()
    )
    val asrModel: StateFlow<String> = _asrModel.asStateFlow()

    fun setAsrModel(v: String) {
        prefs.edit().putString(KEY_ASR_MODEL, v).apply()
        _asrModel.value = v
    }

    private val _spent = MutableStateFlow(prefs.getFloat(KEY_SPENT, 0f).toDouble())
    val spent: StateFlow<Double> = _spent.asStateFlow()

    fun addSpend(value: Double) {
        val next = _spent.value + value
        prefs.edit().putFloat(KEY_SPENT, next.toFloat()).apply()
        _spent.value = next
    }

    fun resetSpend() {
        prefs.edit().putFloat(KEY_SPENT, 0f).apply()
        _spent.value = 0.0
    }

    private val _thinking = MutableStateFlow(prefs.getBoolean(KEY_THINKING, false))
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    fun setThinking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_THINKING, enabled).apply()
        _thinking.value = enabled
    }

    private val _systemPrompt = MutableStateFlow(prefs.getString(KEY_PROMPT, "").orEmpty())
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_API, value.trim()).apply()
        _apiKey.value = value.trim()
    }

    fun setBaseUrl(value: String) {
        val normalized = value.trim().ifBlank { "https://api.deepseek.com" }.trimEnd('/')
        prefs.edit().putString(KEY_BASE, normalized).apply()
        _baseUrl.value = normalized
    }

    fun setModel(value: String) {
        prefs.edit().putString(KEY_MODEL, value).apply()
        _model.value = value
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setTemperature(value: Float) {
        prefs.edit().putFloat(KEY_TEMP, value).apply()
        _temperature.value = value
    }

    fun setMaxTokens(value: Int) {
        prefs.edit().putInt(KEY_TOKENS, value).apply()
        _maxTokens.value = value
    }

    fun setSystemPrompt(value: String) {
        prefs.edit().putString(KEY_PROMPT, value).apply()
        _systemPrompt.value = value
    }

    private companion object {
        const val KEY_API = "api_key"
        const val KEY_BASE = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_TEMP = "temperature"
        const val KEY_TOKENS = "max_tokens"
        const val KEY_PROMPT = "system_prompt"
        const val KEY_THINKING = "thinking_enabled"
        const val KEY_SPENT = "total_spent"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_WEB = "web_search_enabled"
        const val KEY_ASR_BASE = "asr_base"
        const val KEY_ASR_KEY = "asr_key"
        const val KEY_ASR_MODEL = "asr_model"
        const val KEY_ASR_MODE = "asr_mode"
    }
}

/** 对话记录持久化（本地 SharedPreferences 存 JSON） */
class ConversationStore(context: Context) {

    private val prefs = context.getSharedPreferences("deepchat_conversations", Context.MODE_PRIVATE)

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<Conversation>> = _items.asStateFlow()

    private fun load(): List<Conversation> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { Conversation.fromJson(it) }
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<Conversation>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    @Synchronized
    fun save(conversation: Conversation) {
        if (conversation.messages.isEmpty()) return
        val next = (listOf(conversation) + _items.value.filterNot { it.id == conversation.id })
            .sortedByDescending { it.updatedAt }
            .take(80)
        _items.value = next
        persist(next)
    }

    @Synchronized
    fun remove(id: String) {
        val next = _items.value.filterNot { it.id == id }
        _items.value = next
        persist(next)
    }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
        persist(emptyList())
    }

    private companion object {
        const val KEY = "conversations"
    }
}

/** 运行轨迹存储（最多 200 条） */
class TraceStore(context: Context) {

    private val prefs = context.getSharedPreferences("deepchat_traces", Context.MODE_PRIVATE)

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<TraceRecord>> = _items.asStateFlow()

    private fun load(): List<TraceRecord> = runCatching {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { TraceRecord.fromJson(it) }
        }.sortedByDescending { it.at }
    }.getOrDefault(emptyList())

    @Synchronized
    fun add(record: TraceRecord) {
        val next = (listOf(record) + _items.value).take(200)
        _items.value = next
        val arr = JSONArray()
        next.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "traces"
    }
}
