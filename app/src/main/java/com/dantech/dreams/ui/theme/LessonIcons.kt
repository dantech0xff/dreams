package com.dantech.dreams.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Inline icons we need that aren't in androidx.compose.material:material-icons-core.
// Defining them here avoids pulling material-icons-extended (~24MB APK bloat).
object LessonIcons {

    /** Lightning bolt — used as a "complexity / intensity" indicator on lesson cards. */
    val Bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            stroke = null,
            strokeAlpha = 1f,
            fillAlpha = 1f,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero,
            pathBuilder = boltPath,
        ).build()
    }

    private val boltPath: PathBuilder.() -> Unit = {
        // Material Symbols "bolt" filled glyph, traced.
        moveTo(11f, 21f)
        horizontalLineToRelative(-1f)
        lineToRelative(1f, -7f)
        horizontalLineTo(7.5f)
        curveToRelative(-0.58f, 0f, -0.57f, -0.32f, -0.38f, -0.66f)
        curveToRelative(0.19f, -0.34f, 0.05f, -0.08f, 0.07f, -0.12f)
        curveTo(8.48f, 10.94f, 10.42f, 7.54f, 13f, 3f)
        horizontalLineToRelative(1f)
        lineToRelative(-1f, 7f)
        horizontalLineToRelative(3.5f)
        curveToRelative(0.49f, 0f, 0.56f, 0.33f, 0.47f, 0.51f)
        lineToRelative(-0.07f, 0.15f)
        curveTo(12.96f, 17.55f, 11f, 21f, 11f, 21f)
        close()
    }
}
