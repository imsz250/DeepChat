package com.deepseekchat.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** 少量自绘图标（material-icons-core 里没有合适的，为避免引入 extended 包体积） */
object AppIcons {

    val Chat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                verticalLineTo(16f)
                curveTo(2f, 17.1f, 2.9f, 18f, 4f, 18f)
                horizontalLineTo(18f)
                lineTo(22f, 22f)
                verticalLineTo(4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                close()
                moveTo(6f, 9f)
                horizontalLineTo(18f)
                verticalLineTo(11f)
                horizontalLineTo(6f)
                close()
                moveTo(6f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(8f)
                horizontalLineTo(6f)
                close()
                moveTo(6f, 12f)
                horizontalLineTo(14f)
                verticalLineTo(14f)
                horizontalLineTo(6f)
                close()
            }
        }.build()
    }

    val Wallet: ImageVector by lazy {
        ImageVector.Builder(
            name = "Wallet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(21f, 7.28f)
                verticalLineTo(5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                horizontalLineTo(5f)
                curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.89f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(16.72f)
                curveTo(21.6f, 16.24f, 22f, 15.5f, 22f, 14.67f)
                verticalLineTo(9.33f)
                curveTo(22f, 8.5f, 21.6f, 7.76f, 21f, 7.28f)
                close()
                moveTo(5f, 6f)
                horizontalLineTo(19f)
                verticalLineTo(8f)
                horizontalLineTo(5f)
                close()
                moveTo(20f, 14f)
                lineTo(15f, 14f)
                curveTo(14.45f, 14f, 14f, 13.55f, 14f, 13f)
                curveTo(14f, 12.45f, 14.45f, 12f, 15f, 12f)
                lineTo(20f, 12f)
                close()
            }
        }.build()
    }


    /** 相机 */
    val Camera: ImageVector by lazy {
        ImageVector.Builder(
            name = "Camera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 3f)
                lineTo(7.5f, 5f)
                horizontalLineTo(5f)
                curveTo(3.9f, 5f, 3f, 5.9f, 3f, 7f)
                verticalLineTo(18f)
                curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
                horizontalLineTo(19f)
                curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f)
                verticalLineTo(7f)
                curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
                horizontalLineTo(16.5f)
                lineTo(15f, 3f)
                close()
                moveTo(12f, 17.5f)
                curveTo(9.7f, 17.5f, 7.8f, 15.6f, 7.8f, 13.3f)
                curveTo(7.8f, 11f, 9.7f, 9.1f, 12f, 9.1f)
                curveTo(14.3f, 9.1f, 16.2f, 11f, 16.2f, 13.3f)
                curveTo(16.2f, 15.6f, 14.3f, 17.5f, 12f, 17.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFF000000)), stroke = SolidColor(Color.White)) {
                // 镜头挖空效果靠两个圆叠加，不重要，保持简洁
            }
        }.build()
    }

    /** 相册 */
    val Photo: ImageVector by lazy {
        ImageVector.Builder(
            name = "Photo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 3f)
                horizontalLineTo(4f)
                curveTo(2.9f, 3f, 2f, 3.9f, 2f, 5f)
                verticalLineTo(19f)
                curveTo(2f, 20.1f, 2.9f, 21f, 4f, 21f)
                horizontalLineTo(20f)
                curveTo(21.1f, 21f, 22f, 20.1f, 22f, 19f)
                verticalLineTo(5f)
                curveTo(22f, 3.9f, 21.1f, 3f, 20f, 3f)
                close()
                moveTo(9f, 8f)
                moveTo(5f, 18f)
                lineTo(9.5f, 12.5f)
                lineTo(13f, 16f)
                lineTo(15.5f, 13f)
                lineTo(19f, 18f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(8.6f, 7.4f)
                curveTo(8.6f, 8.5f, 7.7f, 9.4f, 6.6f, 9.4f)
                curveTo(5.5f, 9.4f, 4.6f, 8.5f, 4.6f, 7.4f)
                curveTo(4.6f, 6.3f, 5.5f, 5.4f, 6.6f, 5.4f)
                curveTo(7.7f, 5.4f, 8.6f, 6.3f, 8.6f, 7.4f)
                close()
            }
        }.build()
    }

    /** 文件 */
    val File: ImageVector by lazy {
        ImageVector.Builder(
            name = "File",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(14f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(8f)
                lineTo(14f, 2f)
                close()
                moveTo(13f, 9f)
                verticalLineTo(3.5f)
                lineTo(18.5f, 9f)
                close()
            }
        }.build()
    }

    val Copy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(16f, 1f)
                horizontalLineTo(9f)
                curveTo(7.9f, 1f, 7f, 1.9f, 7f, 3f)
                verticalLineTo(15f)
                curveTo(7f, 16.1f, 7.9f, 17f, 9f, 17f)
                horizontalLineTo(19f)
                curveTo(20.1f, 17f, 21f, 16.1f, 21f, 15f)
                verticalLineTo(3f)
                curveTo(21f, 1.9f, 20.1f, 1f, 19f, 1f)
                close()
                moveTo(19f, 15f)
                horizontalLineTo(9f)
                verticalLineTo(3f)
                horizontalLineTo(19f)
                close()
                moveTo(4f, 5f)
                verticalLineTo(21f)
                curveTo(4f, 22.1f, 4.9f, 23f, 6f, 23f)
                horizontalLineTo(17f)
                verticalLineTo(21f)
                horizontalLineTo(6f)
                verticalLineTo(5f)
                close()
            }
        }.build()
    }

    val SelectAll: ImageVector by lazy {
        ImageVector.Builder(
            name = "SelectAll",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // 四个角括号
                moveTo(3f, 3f); lineTo(9f, 3f); lineTo(9f, 5f); lineTo(3f, 5f); close()
                moveTo(3f, 3f); lineTo(5f, 3f); lineTo(5f, 9f); lineTo(3f, 9f); close()
                moveTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 5f); lineTo(15f, 5f); close()
                moveTo(19f, 3f); lineTo(21f, 3f); lineTo(21f, 9f); lineTo(19f, 9f); close()
                moveTo(3f, 15f); lineTo(9f, 15f); lineTo(9f, 17f); lineTo(3f, 17f); close()
                moveTo(3f, 19f); lineTo(5f, 19f); lineTo(5f, 21f); lineTo(3f, 21f); close()
                moveTo(15f, 19f); lineTo(21f, 19f); lineTo(21f, 21f); lineTo(15f, 21f); close()
                moveTo(19f, 15f); lineTo(21f, 15f); lineTo(21f, 19f); lineTo(19f, 19f); close()
                // 中间的对勾
                moveTo(8.8f, 12.5f); lineTo(11.3f, 15f); lineTo(16.3f, 10f)
                lineTo(14.9f, 8.6f); lineTo(11.3f, 12.2f); lineTo(10.2f, 11.1f); close()
            }
        }.build()
    }
}
