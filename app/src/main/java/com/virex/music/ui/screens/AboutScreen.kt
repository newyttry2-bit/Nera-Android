package com.virex.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Mirrors the web About + Guide sections, with the Android scope
 * difference called out explicitly (local-file Downloads only).
 */
@Composable
fun AboutScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("About Virex Music", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Virex Music is an advanced, privacy-focused music player app. " +
                "This Android version mirrors the web app's Home, Search, " +
                "Playlists, Downloads and player screens."
        )
        Text("• No Ads, No Tracking")
        Text("• High Quality Audio (where the source provides it)")
        Text("• Offline playback of audio already on your device")
        Text("• Queue, shuffle, loop, seek and synced lyrics")
        Text("Developed by Virex Devs • v4.0", style = MaterialTheme.typography.bodySmall)

        Text("How It Works", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("1. Search & History — quickly re-visit past queries with 1 tap.")
        Text("2. Local Library — the Downloads tab plays audio already on your device (MediaStore / file picker). It does not download streaming-service tracks.")
        Text("3. Playlists — create playlists in-app; they are stored locally with Room.")
        Text("4. Player — mini-player and full player with queue, shuffle, loop and lyrics.")
    }
}
