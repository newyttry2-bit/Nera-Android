package com.virex.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virex.music.data.AppDatabase
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.SectionHeader
import com.virex.music.ui.components.VirexChip
import com.virex.music.ui.theme.VirexColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    player: PlayerViewModel,
    db: AppDatabase,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hq by player.settings.hq.collectAsState()
    val crossfade by player.settings.crossfadeSec.collectAsState()
    val animSpeed by player.settings.animSpeed.collectAsState()
    val pureBlack by player.settings.pureBlack.collectAsState()
    val accentArtwork by player.settings.accentFromArtwork.collectAsState()
    val sleepDefault by player.settings.sleepDefaultMin.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VirexColors.White)
                }
                Text(
                    "Settings",
                    color = VirexColors.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        item {
            SectionHeader("Playback", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        item {
            SettingRow(
                icon = Icons.Filled.HighQuality,
                title = "HQ quality",
                description = "Play 320kbps when available, otherwise 160kbps."
            ) {
                Switch(
                    checked = hq,
                    onCheckedChange = { player.toggleHq() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = VirexColors.Grey,
                        uncheckedTrackColor = VirexColors.GlassStrong
                    )
                )
            }
            Divider(color = VirexColors.Hairline, thickness = 1.dp)
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, null, tint = VirexColors.Grey, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Crossfade", color = VirexColors.White, fontSize = 15.sp)
                        Text(
                            if (crossfade == 0) "Off — tracks start at full volume."
                            else "$crossfade seconds — new tracks fade in.",
                            color = VirexColors.Grey,
                            fontSize = 13.sp
                        )
                    }
                }
                Slider(
                    value = crossfade.toFloat(),
                    onValueChange = { player.settings.setCrossfadeSec(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = VirexColors.GlassStrong
                    )
                )
            }
            Divider(color = VirexColors.Hairline, thickness = 1.dp)
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, null, tint = VirexColors.Grey, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Sleep timer default", color = VirexColors.White, fontSize = 15.sp)
                        Text("Applied when you open the timer in the player.", color = VirexColors.Grey, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    for (mins in listOf(0, 10, 20, 30)) {
                        VirexChip(
                            text = if (mins == 0) "Off" else "${mins}m",
                            selected = sleepDefault == mins,
                            onClick = { player.settings.setSleepDefaultMin(mins) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
            Divider(color = VirexColors.Hairline, thickness = 1.dp)
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Animation speed", color = VirexColors.White, fontSize = 15.sp)
                Text("How fast the artwork glow and transitions move.", color = VirexColors.Grey, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row {
                    for (speed in listOf(0.5f, 1f, 1.5f)) {
                        VirexChip(
                            text = "${speed}x",
                            selected = animSpeed == speed,
                            onClick = { player.settings.setAnimSpeed(speed) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
            Divider(color = VirexColors.Hairline, thickness = 1.dp)
        }

        item {
            SectionHeader("Appearance", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        item {
            SettingRow(
                icon = Icons.Filled.Palette,
                title = "Accent from artwork",
                description = "Let the song artwork glow softly behind the glass."
            ) {
                Switch(
                    checked = accentArtwork,
                    onCheckedChange = { player.settings.setAccentFromArtwork(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = VirexColors.Grey,
                        uncheckedTrackColor = VirexColors.GlassStrong
                    )
                )
            }
            Divider(color = VirexColors.Hairline, thickness = 1.dp)
        }
        item {
            SettingRow(
                icon = Icons.Filled.Palette,
                title = "Pure black",
                description = "True black background. Off uses a very dark grey."
            ) {
                Switch(
                    checked = pureBlack,
                    onCheckedChange = { player.settings.setPureBlack(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = VirexColors.Grey,
                        uncheckedTrackColor = VirexColors.GlassStrong
                    )
                )
            }
            Divider(color = VirexColors.Hairline, thickness = 1.dp)
        }

        item {
            SectionHeader("Library & files", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Clear recent searches", color = VirexColors.White, fontSize = 15.sp)
                    Text("Removes all saved search history.", color = VirexColors.Grey, fontSize = 13.sp)
                }
                TextButton(onClick = { scope.launch { db.recentDao().clearSearches() } }) {
                    Text("Clear", color = VirexColors.White)
                }
            }
            Divider(color = VirexColors.Hairline, thickness = 1.dp)
        }

        item {
            SectionHeader("About", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Icon(Icons.Filled.Info, null, tint = VirexColors.Grey, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Virex Music 1.0", color = VirexColors.White, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Search and stream music, build playlists, follow synced lyrics, and play audio files already on your phone.",
                        color = VirexColors.Grey,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Music data: JioSaavn API. Lyrics: LRCLIB.",
                        color = VirexColors.DimGrey,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = VirexColors.Grey, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = VirexColors.White, fontSize = 15.sp)
            Text(description, color = VirexColors.Grey, fontSize = 13.sp)
        }
        control()
    }
}
