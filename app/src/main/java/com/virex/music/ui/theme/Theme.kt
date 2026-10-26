package com.virex.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Virex Music web colours: black #000000, surface #121212, accent #1DB954. */
val VirexBlack = Color(0xFF000000)
val VirexSurface = Color(0xFF121212)
val VirexCard = Color(0xFF111111)
val VirexGreen = Color(0xFF1DB954)
val VirexText = Color(0xFFFFFFFF)
val VirexSubtext = Color(0xFFA1A1A1)

private val DarkScheme = darkColorScheme(
    primary = VirexGreen,
    onPrimary = Color.Black,
    background = VirexBlack,
    surface = VirexSurface,
    onBackground = VirexText,
    onSurface = VirexText
)

@Composable
fun VirexMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        // Outfit is a Google Font; the web app loads it from fonts.googleapis.com.
        // This scaffold uses the system sans-serif stack with the same weights
        // so no font file needs to be bundled. Replace with a bundled Outfit
        // font in res/font/ for a pixel-identical look.
        content = content
    )
}
