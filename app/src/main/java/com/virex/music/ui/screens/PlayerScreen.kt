package com.virex.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.virex.music.data.ApiClient
import com.virex.music.data.formatDuration
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.theme.VirexCard
import kotlinx.coroutines.launch

/** Mini-player bar shown above the bottom navigation. */
@Composable
fun MiniPlayer(player: PlayerViewModel, onExpand: () -> Unit) {
    val song = player.current
    val playing by player.isPlaying.collectAsState()
    if (song == null) return
    Card(
        onClick = onExpand,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = VirexCard)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(song.artist, maxLines = 1)
            }
            Button(onClick = { player.togglePlay() }) {
                Text(if (playing) "Pause" else "Play")
            }
        }
    }
}

/** Full player: artwork, seek bar, queue controls, lyrics, settings toggles. */
@Composable
fun FullPlayerScreen(player: PlayerViewModel) {
    val song = player.current
    val playing by player.isPlaying.collectAsState()
    val position by player.positionSec.collectAsState()
    val duration by player.durationSec.collectAsState()
    val shuffle by player.shuffle.collectAsState()
    val loop by player.loop.collectAsState()
    val hq by player.hq.collectAsState()
    var lyrics by remember { mutableStateOf("Lyrics will load here.") }
    val scope = rememberCoroutineScope()

    if (song == null) {
        Text("Nothing playing yet. Pick a song from Home or Search.")
        return
    }

    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(280.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Text(song.title, fontWeight = FontWeight.Bold)
        Text(song.artist)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(position))
            Text(formatDuration(if (duration > 0) duration else song.durationSec))
        }
        Slider(
            value = position.toFloat(),
            onValueChange = { player.seekTo(it.toInt()) },
            valueRange = 0f..(if (duration > 0) duration else song.durationSec).toFloat().coerceAtLeast(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { player.toggleShuffle() }) {
                Text(if (shuffle) "Shuffle: On" else "Shuffle")
            }
            Button(onClick = { player.previous() }) { Text("Prev") }
            Button(onClick = { player.togglePlay() }) {
                Text(if (playing) "Pause" else "Play")
            }
            Button(onClick = { player.next() }) { Text("Next") }
            Button(onClick = { player.toggleLoop() }) { Text("Loop: $loop") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HQ (placeholder — stream quality is chosen by the API)")
            Switch(checked = hq, onCheckedChange = { player.toggleHq() })
        }
        Button(onClick = {
            scope.launch {
                try {
                    val res = ApiClient.lyrics.getLyrics(
                        song.title, song.artist, song.durationSec.takeIf { it > 0 }
                    )
                    lyrics = res.plainLyrics ?: res.syncedLyrics ?: "No lyrics found."
                } catch (e: Exception) {
                    lyrics = "Could not load lyrics. Try again."
                }
            }
        }) { Text("Load lyrics") }
        Text(lyrics)
        Text(
            "Note: the web app's real-time visualizer and crossfade are not " +
                "implemented in this native scaffold yet — see README."
        )
    }
}
