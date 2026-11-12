package com.virex.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * "Black glass, thin lines" — our own design.
 * Pure black base, one glass material, hairline borders, small precise
 * buttons. The app has almost no colour of its own; colour comes from
 * the song artwork glowing behind the glass.
 */
object VirexColors {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0A0A0A)
    val Glass = Color.White.copy(alpha = 0.08f)
    val GlassStrong = Color.White.copy(alpha = 0.12f)
    val Hairline = Color.White.copy(alpha = 0.14f)
    val White = Color(0xFFFFFFFF)
    val Grey = Color(0xFF9A9A9A)
    val DimGrey = Color(0xFF666666)
    val ProgressRed = Color(0xFFE53935)
}

private val VirexColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = VirexColors.Grey,
    background = VirexColors.Black,
    surface = VirexColors.Surface,
    onBackground = Color.White,
    onSurface = Color.White
)

private val VirexTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp
    )
)

@Composable
fun VirexMusicTheme(
    pureBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = if (pureBlack) {
        VirexColorScheme
    } else {
        VirexColorScheme.copy(
            background = Color(0xFF101010),
            surface = Color(0xFF161616)
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = VirexTypography,
        content = content
    )
}
