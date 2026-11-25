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
        Md3SelectionToolbar(
            state = state,
            copyLabel = L10n.t(language, "复制", "複製", "Copy"),
            selectAllLabel = L10n.t(language, "全选", "全選", "Select all")
        )
    }
}

@Composable
fun Md3SelectionToolbar(state: Md3TextToolbarState, copyLabel: String, selectAllLabel: String) {
    if (!state.visible) return
    // Popup 的 offset 是相对“父布局”的，这里按窗口坐标换算，避免第二次长按时浮条跑到别处
    val anchorRect = state.rect
    val provider = remember(anchorRect) {
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
                y = y.coerceIn(8, (windowSize.height - popupContentSize.height - 8).coerceAtLeast(8))
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = { state.visible = false },
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                TextButton(
                    onClick = {
                        state.onCopy?.invoke()
                        state.visible = false
                    },
                    enabled = state.onCopy != null
                ) {
                    Text(
                        copyLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = { state.onSelectAll?.invoke() },
                    enabled = state.onSelectAll != null
                ) {
                    Text(
                        selectAllLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
