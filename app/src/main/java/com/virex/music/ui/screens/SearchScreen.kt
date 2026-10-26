package com.virex.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.virex.music.data.ApiClient
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(player: PlayerViewModel, onAddToPlaylist: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search(q: String) {
        if (q.isBlank()) return
        recent = (listOf(q) + recent).distinct().take(8)
        loading = true
        error = null
        scope.launch {
            try {
                val res = ApiClient.search.searchSongs(q, 20)
                results = res.data?.results.orEmpty().map { it.toSong() }
                if (results.isEmpty()) error = "No results found."
            } catch (e: Exception) {
                error = "Could not load songs. Try again."
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search songs or artists") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { search(query) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Search") }

        if (recent.isNotEmpty()) {
            Text("Recent searches", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            recent.forEach { term ->
                AssistChip(onClick = { query = term; search(term) }, label = { Text(term) })
            }
        }
        if (loading) CircularProgressIndicator(Modifier.padding(top = 16.dp))
        error?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { song ->
                SongRow(song = song, onClick = {
                    player.playQueue(results, results.indexOf(song))
                }) {
                    Button(onClick = { onAddToPlaylist(song) }) { Text("+") }
                }
            }
        }
    }
}
