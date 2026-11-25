package com.deepseekchat.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Material 3 风格的文本选择浮条（对标 Android 14/15/16 的原生外观）：
 * 圆角胶囊、surfaceContainerHigh 底、抬起阴影，图标按钮 + 右侧溢出菜单。
 * 用 LocalTextToolbar 接管系统默认的长按菜单，避免出现旧系统的「复制 全选」样式。
 */
class Md3TextToolbarState {
    var visible by mutableStateOf(false)
    internal var rect by mutableStateOf(Rect.Zero)
    internal var onCopy: (() -> Unit)? = null
    internal var onSelectAll: (() -> Unit)? = null
    internal var onPaste: (() -> Unit)? = null
    internal var onCut: (() -> Unit)? = null
}

class Md3TextToolbar(private val state: Md3TextToolbarState) : TextToolbar {
    override val status: TextToolbarStatus
        get() = if (state.visible) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun hide() {
        state.visible = false
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        state.rect = rect
        state.onCopy = onCopyRequested
        state.onSelectAll = onSelectAllRequested
        state.onPaste = onPasteRequested
        state.onCut = onCutRequested
        state.visible = true
    }
}

/** 把整个页面的文本选择统一接管成 MD3 浮条，页面内所有 SelectionContainer 都会生效 */
@Composable
fun Md3TextSelectionHost(language: String = "zh-CN", content: @Composable () -> Unit) {
    val state = remember { Md3TextToolbarState() }
    val toolbar = remember(state) { Md3TextToolbar(state) }
    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        content()
        Md3SelectionToolbar(state = state, language = language)
    }
}

/** 页面内联的浮条本体（不再用独立 Popup 窗口，避免和系统浮条重复、点击失效） */
@Composable
fun Md3SelectionBar(state: Md3TextToolbarState, language: String = "zh-CN", modifier: Modifier = Modifier) {
    val copyLabel = L10n.t(language, "复制", "複製", "Copy")
    val selectAllLabel = L10n.t(language, "全选", "全選", "Select all")
    val pasteLabel = L10n.t(language, "粘贴", "貼上", "Paste")
    val cutLabel = L10n.t(language, "剪切", "剪下", "Cut")
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            listOf(
                cutLabel to state.onCut,
                copyLabel to state.onCopy,
                pasteLabel to state.onPaste,
                selectAllLabel to state.onSelectAll
            ).forEach { (label, action) ->
                if (action != null) {
                    TextButton(onClick = {
                        action.invoke()
                        if (label != selectAllLabel) state.visible = false
                    }) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Md3SelectionToolbar(state: Md3TextToolbarState, language: String = "zh-CN") {
    val copyLabel = L10n.t(language, "复制", "複製", "Copy")
    val selectAllLabel = L10n.t(language, "全选", "全選", "Select all")
    val pasteLabel = L10n.t(language, "粘贴", "貼上", "Paste")
    val cutLabel = L10n.t(language, "剪切", "剪下", "Cut")
    if (!state.visible) return
    // Popup 的 offset 是相对“父布局”的，这里按窗口坐标换算，避免第二次长按时浮条跑到别处
    val anchorRect = state.rect
    val bottomReserve = with(LocalDensity.current) { 132.dp.toPx() }
    val provider = remember(anchorRect, bottomReserve) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val rect = anchorRect
                @Suppress("UNUSED_VARIABLE") val unusedAnchor = anchorBounds
                val gap = 10
                var x = (rect.center.x - popupContentSize.width / 2f).toInt()
                x = x.coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
                var y = (rect.top - popupContentSize.height - gap).toInt()
                if (y < 8) y = (rect.bottom + gap).toInt()
                // 上半屏放不下时才放选区下方，但不能压住底部输入区，否则浮条会挡住发送/麦克风按钮
                val safeBottom = (windowSize.height - bottomReserve).toInt()
                y = y.coerceIn(8, (safeBottom - popupContentSize.height).coerceAtLeast(8))
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = {
            // 点浮条外面任意空白处就收掉
            state.visible = false
        },
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                listOf(
                    cutLabel to state.onCut,
                    copyLabel to state.onCopy,
                    pasteLabel to state.onPaste,
                    selectAllLabel to state.onSelectAll
                ).forEach { (label, action) ->
                    if (action != null) {
                        TextButton(onClick = {
                            action.invoke()
                            if (label != selectAllLabel) state.visible = false
                        }) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
