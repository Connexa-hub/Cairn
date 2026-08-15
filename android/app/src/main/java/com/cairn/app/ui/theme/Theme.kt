package com.cairn.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// AMOLED-true dark palette — pure black background (not #121212) to actually
// save power on OLED panels and match the "vault" mood: calm, dense, black.
private val VaultBlack = Color(0xFF000000)
private val VaultSurface = Color(0xFF0D0D0F)
private val VaultSurfaceElevated = Color(0xFF17171B)
private val VaultAccent = Color(0xFF7C5CFF)      // violet — Cash-App-adjacent confidence, distinct from Truecaller blue/green
private val VaultAccentSoft = Color(0xFFB6A6FF)
private val VaultOnSurface = Color(0xFFEDEDEF)
private val VaultOnSurfaceMuted = Color(0xFF9A9AA2)
private val VaultDanger = Color(0xFFFF6B6B)       // missed calls
private val VaultSuccess = Color(0xFF4ADE80)      // incoming/connected

private val DarkColors = darkColorScheme(
    primary = VaultAccent,
    onPrimary = Color.White,
    secondary = VaultAccentSoft,
    background = VaultBlack,
    onBackground = VaultOnSurface,
    surface = VaultSurface,
    onSurface = VaultOnSurface,
    surfaceVariant = VaultSurfaceElevated,
    onSurfaceVariant = VaultOnSurfaceMuted,
    error = VaultDanger
)

private val LightColors = lightColorScheme(
    primary = VaultAccent,
    onPrimary = Color.White,
    secondary = VaultAccentSoft,
    error = VaultDanger
)

object CairnColors {
    val danger = VaultDanger
    val success = VaultSuccess
    val accent = VaultAccent
    val accentSoft = VaultAccentSoft
}

private val VaultFont = FontFamily.Default // swap for a licensed variable font (e.g. Inter/Söhne) in production

val CairnTypography = Typography(
    displayLarge = TextStyle(fontFamily = VaultFont, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = VaultFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = VaultFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = VaultFont, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = VaultFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = VaultFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = VaultFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp)
)

// Rounded, soft-depth geometry — larger radii than stock Material for the "premium vault" feel
val CairnShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

@Composable
fun CairnTheme(
    darkTheme: Boolean = true, // app defaults to AMOLED dark regardless of system, user can override in Appearance
    dynamicColor: Boolean = true,
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
