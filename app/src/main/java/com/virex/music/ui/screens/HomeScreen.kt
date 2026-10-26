package com.virex.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.virex.music.R
import com.virex.music.data.ApiClient
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mirrors the web Home tab: a "Trending Mix" title, category chips,
 * and song cards fetched from the same search endpoint (the web app
 * mixes 3 topics per category). Topics here are the scaffold defaults;
 * the web app's CATEGORY_POOLS can be copied in later without changing
 * the UI contract.
 */
@Composable
fun HomeScreen(player: PlayerViewModel, onOpenDownloads: () -> Unit) {
    var category by remember { mutableStateOf("Trending") }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val topics = mapOf(
        "Trending" to listOf("trending songs", "top hits", "viral songs"),
        "Chill" to listOf("chill songs", "lofi", "acoustic"),
        "Party" to listOf("party songs", "dance hits", "bollywood party")
    )

    LaunchedEffect(category) {
        loading = true
        error = null
        try {
            val found = withContext(Dispatchers.IO) {
                val out = mutableListOf<Song>()
                for (topic in topics[category].orEmpty()) {
                    val res = ApiClient.search.searchSongs(topic, 15)
                    out += (res.data?.results.orEmpty()).map { it.toSong() }
                }
                out.shuffled().take(30)
            }
            songs = found
        } catch (e: Exception) {
            error = "Could not load songs. Try again."
        } finally {
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Virex Music logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "$category Mix",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topics.keys.toList()) { chip ->
                    AssistChip(
                        onClick = { category = chip },
                        label = { Text(chip) }
                    )
                }
            }
        }
        item {
            Button(onClick = onOpenDownloads) { Text("Go to Downloads") }
            Spacer(Modifier.height(4.dp))
            Text(
                "Offline playback here is for audio already on your device — see Downloads.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (loading) {
            item { CircularProgressIndicator() }
        }
        error?.let { item { Text(it) } }
        items(songs) { song ->
            SongRow(song = song, onClick = {
                player.playQueue(songs, songs.indexOf(song))
            })
        }
    }
}
