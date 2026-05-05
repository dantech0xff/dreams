package com.example.theme.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Primitive shape tokens — RoundedCornerShape scale.
 * Replace {Prefix} with your project prefix (e.g., App, Qzds).
 * Usage: {Prefix}PrimitiveShape.Md  →  RoundedCornerShape(8.dp)
 * Note:  Full uses percent (50) — produces pill/circle regardless of size.
 */
object {Prefix}PrimitiveShape {
    val None = RoundedCornerShape(0.dp)
    val Xs   = RoundedCornerShape(2.dp)
    val Sm   = RoundedCornerShape(4.dp)
    val Md   = RoundedCornerShape(8.dp)
    val Lg   = RoundedCornerShape(16.dp)
    val Xl   = RoundedCornerShape(24.dp)
    val Xxl  = RoundedCornerShape(32.dp)
    val Full = RoundedCornerShape(50)
}
