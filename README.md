# DeepChat

**中文** | [English](#english)

---

## 中文

纯原生 Android 的 DeepSeek 第三方客户端：Kotlin + Jetpack Compose + Material 3，填入自己的 DeepSeek API Key 后直连官方接口，不经过任何中转。

### 下载

最新版：**[DeepChat 1.3.1 APK](https://github.com/imsz250/DeepChat/releases/latest)**

历史版本见 [Releases](https://github.com/imsz250/DeepChat/releases)（1.0.3 到 1.3.1 每个版本都带 APK）。release 包使用 debug 签名，同一签名可直接覆盖升级。

### 功能

- 流式对话，边生成边跟随滚动；思考过程可折叠
- 多模型切换（deepseek-flash / deepseek-v4-pro / deepseek-chat / deepseek-reasoner），每个对话单独记录花费
- Markdown 渲染：加粗、斜体、内联代码、标题、列表、引用、分割线、表格、代码块
- 代码块：HTML / CSS / JS / SVG 可在本机 WebView 直接运行；其他语言只提供语法高亮查看、保存、复制
- 语音输入：**按住说话、松手识别**；内置录音 + **本地离线识别**（Vosk 中文模型随 APK 打包，断网可用、免费，输出自动去掉汉字间空格）；也可切到在线识别（任意 OpenAI 兼容的 transcription 接口）
- 附件：气泡内直接渲染图片缩略图，点开是应用内图片查看器（双指缩放/双击复位）；文本附件为卡片，点开是应用内文本查看器（可选中、可复制全文）
- 联网搜索：Bing（手机端 / 桌面端）→ 百度依次兜底，搜不到自动退回普通回答
- 三语界面：简体中文、繁體中文（中国香港 / 中国台湾）、English（美国 / 英国）
- 长按选择文本时使用 Material 3 风格的选择浮条（复制 / 全选图标 + 溢出菜单），全屏代码页同样是自制浮条；消息里的 http(s) 链接可点开
- 深色 / 浅色 / 跟随系统；诊断日志；对话本地保存

### 环境

- minSdk 26（Android 8.0），targetSdk 34
- JDK 17、Android SDK 34、Gradle 8.9（仓库已带 wrapper）
- 依赖：Compose BOM 2024.09.03、Material 3、OkHttp 4.12、kotlinx-coroutines

### 构建

```bash
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

> 仓库不含 `local.properties`，SDK 路径按本机环境配置。

### 关于 API Key

API Key 只保存在本机应用私有目录，**不写进源码、不上传仓库**。首次使用在「设置」里粘贴自己的 Key；官方余额与账单以 https://platform.deepseek.com 为准。

### 免责声明

本项目为个人学习用途，与 DeepSeek 官方无关。接口名称、模型名称、价格版权归其权利人所有。

---

## English

A native Android third-party client for DeepSeek, built with Kotlin + Jetpack Compose + Material 3. Plug in your own DeepSeek API key and talk to the official API directly — no proxy, no relay.

### Download

Latest: **[DeepChat 1.3.1 APK](https://github.com/imsz250/DeepChat/releases/latest)**

Older builds: [Releases](https://github.com/imsz250/DeepChat/releases) — every version from 1.0.3 to 1.3.1 ships an APK. Release builds are signed with the debug key, so they upgrade over each other directly.

### Features

- Streaming chat with auto-follow scrolling; collapsible reasoning traces
- Model switching (deepseek-flash / deepseek-v4-pro / deepseek-chat / deepseek-reasoner) with per-conversation cost tracking
- Markdown rendering: bold, italic, inline code, headings, lists, quotes, rules, tables, code blocks
- Code blocks: HTML / CSS / JS / SVG run in a local WebView; every other language gets syntax-highlight view, save and copy only
- Voice input: **hold to talk, release to transcribe**; built-in recorder plus **on-device recognition** (Vosk Chinese model bundled in the APK, works offline and free, spaces between Chinese characters stripped); optionally switch to online transcription (any OpenAI-compatible endpoint)
- Attachments: inline image thumbnails opening into a built-in zoomable image viewer; text files open in a built-in text viewer (selectable, copy-all); files copied into app storage so they survive restarts
- Web search: Bing (mobile / desktop) then Baidu as fallbacks; falls back to a plain answer when nothing comes back
- Three UI languages: Simplified Chinese, Traditional Chinese (Hong Kong / Taiwan), English (US / UK)
- Material 3 text-selection toolbar (copy / select-all icons plus overflow) everywhere including the full-screen code viewer; http(s) links in messages are tappable
- Dark / light / system theme; diagnostic log; local conversation storage

### Requirements

- minSdk 26 (Android 8.0), targetSdk 34
- JDK 17, Android SDK 34, Gradle 8.9 (wrapper included)
- Dependencies: Compose BOM 2024.09.03, Material 3, OkHttp 4.12, kotlinx-coroutines

### Build

```bash
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

> `local.properties` is not in the repo — set your SDK path locally.

### About the API key

Your key lives only in the app's private storage. It is **never committed and never uploaded**. Paste your own key in Settings on first launch. Official balance and billing: https://platform.deepseek.com

### Disclaimer

Personal learning project, not affiliated with DeepSeek. API names, model names and pricing belong to their respective owners.
