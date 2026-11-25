package com.deepseekchat.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Material 3 风格的文本选择浮条（对标 Android 13/14/15 的原生外观）：
 * 胶囊形、圆角、带一点抬升阴影，只放图标按钮：复制、全选、收起。
 * 用 LocalTextToolbar 接管系统默认的长按菜单，避免在旧系统上出现“复制 全选”那种老样式。
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

@Composable
fun Md3SelectionToolbar(state: Md3TextToolbarState, copyLabel: String, selectAllLabel: String) {
    if (!state.visible) return
    val density = LocalDensity.current
    val estimatedWidthPx = with(density) { 132.dp.toPx() }
    val barHeightPx = with(density) { 46.dp.toPx() }
    val x = (state.rect.center.x - estimatedWidthPx / 2f).coerceAtLeast(8f)
    val y = (state.rect.top - barHeightPx - with(density) { 8.dp.toPx() }).coerceAtLeast(8f)

    Popup(
        offset = IntOffset(x.toInt(), y.toInt()),
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
                IconButton(
                    onClick = {
                        state.onCopy?.invoke()
                        state.visible = false
                    },
                    enabled = state.onCopy != null
                ) {
                    Icon(AppIcons.Copy, contentDescription = copyLabel)
                }
                IconButton(
                    onClick = {
                        state.onSelectAll?.invoke()
                    },
                    enabled = state.onSelectAll != null
                ) {
                    Icon(AppIcons.SelectAll, contentDescription = selectAllLabel)
                }
            }
        }
    }
}
