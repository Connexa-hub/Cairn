package com.cairn.app.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// True AMOLED black (not #121212) — saves real power on OLED and grounds
// the whole palette. Deliberately not "generic Material dark theme" grey.
private val CairnBlack = Color(0xFF000000)
private val CairnSurface = Color(0xFF0C0C0D)
private val CairnSurfaceElevated = Color(0xFF19191B)
private val CairnHairline = Color(0xFF2A2A2D) // thin-stroke outlines, Nothing-OS style

// Signature accent: a warm ember tone, not a generic Google purple/blue and
// not a direct lift of any competitor's brand color (Truecaller blue/green,
// Cash App green). Ties into the brand concept — warm light on stone.
private val CairnEmber = Color(0xFFFF7A33)
private val CairnEmberDim = Color(0xFFB85A26)
private val CairnEmberSoft = Color(0xFFFFC9A3)

// Stone neutrals for text — warm-tinted off-whites/greys rather than pure
// grey, matching the "cairn" material concept.
private val CairnStone = Color(0xFFEDEAE6)
private val CairnStoneMuted = Color(0xFF9A9591)

private val CairnDanger = Color(0xFFFF5C5C)   // missed calls
private val CairnSuccess = Color(0xFF3DDC84)  // incoming/connected — deliberately far from Ember on the wheel

private val DarkColors = darkColorScheme(
    primary = CairnEmber,
    onPrimary = Color.Black,
    secondary = CairnEmberSoft,
    background = CairnBlack,
    onBackground = CairnStone,
    surface = CairnSurface,
    onSurface = CairnStone,
    surfaceVariant = CairnSurfaceElevated,
    onSurfaceVariant = CairnStoneMuted,
    outline = CairnHairline,
    error = CairnDanger
)

private val LightColors = lightColorScheme(
    primary = CairnEmberDim,
    onPrimary = Color.White,
    secondary = CairnEmber,
    error = CairnDanger
)

object CairnColors {
    val danger = CairnDanger
    val success = CairnSuccess
    val accent = CairnEmber
    val accentDim = CairnEmberDim
    val accentSoft = CairnEmberSoft
    val hairline = CairnHairline
    val stoneMuted = CairnStoneMuted

    // Cash-App-style flat color blocks for quick-action tiles — each tile
    // gets its own confident, distinct flat color rather than one repeated
    // accent, while staying inside the same warm/muted family so nothing
    // clashes with the ember signature color.
    val tileContacts = Color(0xFF3A6B8A)
    val tileTimeline = Color(0xFF4A8A6B)
    val tileStats = Color(0xFF8A5A3A)
    val tileFavorites = CairnEmber
    val tileBackup = Color(0xFF6B5A8A)
}

// System font for now (no bundled variable font asset) — but pushed toward
// a more considered, branded feel via deliberate letter-spacing rather than
// stock Material defaults everywhere.
private val CairnFont = FontFamily.Default

val CairnTypography = Typography(
    displayLarge = TextStyle(fontFamily = CairnFont, fontWeight = FontWeight.Bold, fontSize = 42.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = CairnFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = CairnFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = CairnFont, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = CairnFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = CairnFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // Nothing-OS-style tracked-out uppercase label — used for section
    // headers ("RECENT ACTIVITY") instead of plain sentence-case Material
    // defaults, which is a big part of what reads as "generic" otherwise.
    labelSmall = TextStyle(fontFamily = CairnFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.6.sp)
)

// Rounded, soft-depth geometry — larger radii than stock Material for a
// premium, confident feel closer to Cash App's card language.
val CairnShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

@Composable
fun CairnTheme(
    darkTheme: Boolean = true, // app defaults to AMOLED dark regardless of system, user can override in Appearance
    // Defaults OFF, not on: Material You's dynamicDarkColorScheme() silently
    // overrides Cairn's entire designed palette with colors auto-generated
    // from the device wallpaper on API 31+, which is what was actually
    // causing the app to look like generic stock Material instead of a
    // deliberately designed product. Real opt-in toggle lives in Appearance.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CairnTypography,
        shapes = CairnShapes,
        content = content
    )
}
