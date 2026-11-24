# DeepChat

一个纯原生 Android 的 DeepSeek 第三方客户端：Kotlin + Jetpack Compose + Material 3，填入自己的 DeepSeek API Key 后直连官方接口，不经过任何中转。

## 下载

最新版：**[DeepChat 1.1.7 APK](https://github.com/imsz250/DeepChat/releases/latest)**

历史版本在 [Releases](https://github.com/imsz250/DeepChat/releases)（从 1.0.3 到 1.1.7 每个版本都带 APK）。release 包使用 debug 签名，同一签名可直接覆盖升级。

## 功能

- 流式对话，边生成边跟随滚动；思考过程可折叠
- 多模型切换（deepseek-flash / deepseek-v4-pro / deepseek-chat / deepseek-reasoner），每个对话单独记录花费
- Markdown 渲染：加粗、斜体、内联代码、标题、列表、引用、分割线、表格、代码块
- 代码块：HTML/CSS/JS/SVG 可在本机 WebView 直接运行；其他语言只做语法高亮查看、保存、复制
- 附件：图片落盘持久化（重开仍有缩略图），文本附件预览
- 联网搜索：Bing（手机端 / 桌面端）→ 百度依次兜底；搜不到自动退回普通回答
- 三语界面：简体中文、繁體中文（中国香港 / 中国台湾）、English（美国 / 英国）
- 深色 / 浅色 / 跟随系统；诊断日志、对话本地保存

## 环境

- minSdk 26（Android 8.0），targetSdk 34
- JDK 17，Android SDK 34，Gradle 8.9
- 依赖：Compose BOM 2024.09.03、Material 3、OkHttp 4.12、kotlinx-coroutines

## 构建

```bash
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

> 仓库不含 `local.properties`（SDK 路径按本机环境配置）。

## 关于 API Key

API Key 只保存在本机应用私有目录，**不写进源码、不上传仓库**。
首次使用在「设置」里粘贴自己的 Key；官方余额与账单以 https://platform.deepseek.com 为准。

## 免责声明

本项目为个人学习用途，与 DeepSeek 官方无关。接口名称、模型名称、价格版权归其权利人所有。
