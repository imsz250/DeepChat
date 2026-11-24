package com.deepseekchat.app.ui

/**
 * 极简三语支持：简体 / 繁體 / English。
 * 语言代码：zh-CN、zh-Hant-HK、zh-Hant-TW、en-US、en-GB
 */
object L10n {

    data class Option(val code: String, val label: String)

    val OPTIONS = listOf(
        Option("zh-CN", "简体中文"),
        Option("zh-Hant-HK", "繁體中文"),
        Option("zh-Hant-TW", "繁體中文"),
        Option("en-US", "English (US)"),
        Option("en-GB", "English (UK)")
    )

    /** 地区说明按当前界面语言走 */
    fun hint(code: String, lang: String): String = when (code) {
        "zh-CN" -> t(lang, "中国大陆", "中國大陸", "Mainland China")
        "zh-Hant-HK" -> t(lang, "中国香港", "中國香港", "Hong Kong SAR")
        "zh-Hant-TW" -> t(lang, "中国台湾", "中國台灣", "Taiwan")
        "en-US" -> t(lang, "美国", "美國", "United States")
        "en-GB" -> t(lang, "英国", "英國", "United Kingdom")
        else -> ""
    }

    fun isTraditional(lang: String) = lang.startsWith("zh-Hant")
    fun isEnglish(lang: String) = lang.startsWith("en")

    /** 三选一取词：简中 / 繁中 / 英文 */
    fun t(lang: String, zh: String, zhHant: String, en: String): String = when {
        isEnglish(lang) -> en
        isTraditional(lang) -> zhHant
        else -> zh
    }

    fun languageName(code: String, lang: String = "zh-CN"): String =
        OPTIONS.firstOrNull { it.code == code }?.let { it.label + " · " + hint(code, lang) }
            ?: "简体中文 · 中国大陆"

    // 常用句子
    fun chat(lang: String) = t(lang, "对话", "對話", "Chat")
    fun history(lang: String) = t(lang, "历史", "歷史", "History")
    fun balance(lang: String) = t(lang, "余额", "餘額", "Balance")
    fun settings(lang: String) = t(lang, "设置", "設定", "Settings")

    fun normalMode(lang: String) = t(lang, "普通模式", "一般模式", "Standard")
    fun deepThinking(lang: String) = t(lang, "深度思考", "深度思考", "Deep think")
    fun newChat(lang: String) = t(lang, "新对话", "新對話", "New chat")
    fun sendPlaceholder(lang: String) = t(lang, "发消息…", "傳送訊息…", "Message…")
    fun addAttachment(lang: String) = t(lang, "添加附件", "加入附件", "Add attachment")
    fun camera(lang: String) = t(lang, "相机", "相機", "Camera")
    fun gallery(lang: String) = t(lang, "相册", "相簿", "Photos")
    fun files(lang: String) = t(lang, "文件", "檔案", "Files")
    fun regenerate(lang: String) = t(lang, "重新生成", "重新生成", "Regenerate")
    fun continueGen(lang: String) = t(lang, "继续生成", "繼續生成", "Continue")
    fun truncated(lang: String) = t(lang, "输出被截断（到单次上限了）", "輸出被截斷（到單次上限了）", "Truncated (hit the limit)")
    fun latest(lang: String) = t(lang, "最新", "最新", "Latest")
    fun stop(lang: String) = t(lang, "停止", "停止", "Stop")
    fun send(lang: String) = t(lang, "发送", "傳送", "Send")
    fun startChat(lang: String) = t(lang, "开始一段对话", "開始一段對話", "Start a conversation")
    fun startChatHint(lang: String) = t(
        lang,
        "用你自己的 DeepSeek API Key。左下角 + 可以拍照、选文件；聊过的内容会自动带上下文。",
        "用你自己的 DeepSeek API Key。左下角 + 可以拍照、選檔案；聊過的內容會自動带上下文。",
        "Bring your own DeepSeek API key. Tap + to attach a photo or file; context carries over automatically."
    )
    fun priceTitle(lang: String) = t(lang, "这段对话已花", "這段對話已花", "Spent in this chat")
    fun priceSub(lang: String) = t(lang, "价格参考（元 / 百万 tokens，闲时价）", "價格參考（元 / 百萬 tokens，離峰價）", "Prices (CNY / 1M tokens, off-peak)")
    fun spent(lang: String) = t(lang, "已花", "已花", "Spent")
    fun languageTitle(lang: String) = t(lang, "语言 / 地区", "語言 / 地區", "Language / Region")
    fun languageHint(lang: String) = t(
        lang,
        "切换后底部导航和对话页的主要文字会跟着变，API 行为不受影响。",
        "切換後底部導覽和對話頁的主要文字會跟著變，API 行為不受影響。",
        "Switches the main UI text. API behaviour stays the same."
    )

    /** 地区小字：跟随当前界面语言 */
    fun regionHint(code: String, lang: String): String {
        val traditional = isTraditional(lang)
        val english = isEnglish(lang)
        return when (code) {
            "zh-CN" -> when {
                english -> "Mainland China"
                traditional -> "中國大陸"
                else -> "中国大陆"
            }
            "zh-Hant-HK" -> when {
                english -> "Hong Kong SAR"
                traditional -> "中國香港"
                else -> "中国香港"
            }
            "zh-Hant-TW" -> when {
                english -> "Taiwan"
                traditional -> "中國台灣"
                else -> "中国台湾"
            }
            "en-US" -> when {
                english -> "United States"
                traditional -> "美國"
                else -> "美国"
            }
            "en-GB" -> when {
                english -> "United Kingdom"
                traditional -> "英國"
                else -> "英国"
            }
            else -> ""
        }
    }
}
