package com.dantech.dreams.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.dantech.dreams.R

// Downloadable Roboto Slab via the GMS Fonts provider. Certs are declared in
// res/values/font_certs.xml. First app launch fetches the font asynchronously;
// system falls back to a serif face until the download completes (typically
// once, persisted by the system font cache).
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val RobotoSlabName = GoogleFont("Roboto Slab")

val RobotoSlab: FontFamily = FontFamily(
    Font(googleFont = RobotoSlabName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = RobotoSlabName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = RobotoSlabName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = RobotoSlabName, fontProvider = provider, weight = FontWeight.Bold),
)

// Roboto Slab is the brand face for prose, titles, and labels. Numeric readouts
// and shader source still use Tokens.mono (system monospace) — slab serif glyphs
// don't read as well for digits at small sizes.
private val Brand: FontFamily = RobotoSlab

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
