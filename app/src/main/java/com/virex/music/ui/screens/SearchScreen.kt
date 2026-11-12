package com.virex.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virex.music.data.ApiClient
import com.virex.music.data.AppDatabase
import com.virex.music.data.RecentSearchEntity
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.AddToPlaylistDialog
import com.virex.music.ui.components.SectionHeader
import com.virex.music.ui.components.SongRow
import com.virex.music.ui.components.VirexChip
import com.virex.music.ui.theme.VirexColors
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    player: PlayerViewModel,
    db: AppDatabase,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var moreSong by remember { mutableStateOf<Song?>(null) }
    val scope = rememberCoroutineScope()

    val recentSearches by db.recentDao().recentSearches().collectAsState(initial = emptyList())

    fun runSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isEmpty()) return
        searching = true
        searched = true
        failed = false
        scope.launch {
            try {
                db.recentDao().saveSearch(RecentSearchEntity(query = trimmed))
                val res = ApiClient.search.searchSongs(trimmed, 20)
                results = res.songs
            } catch (_: Exception) {
                results = emptyList()
                failed = true
            } finally {
                searching = false
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        // One hairline search field at the top, large text, no filled box.
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs", color = VirexColors.DimGrey, fontSize = 18.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = VirexColors.Grey)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = VirexColors.Grey)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch(query) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = VirexColors.Hairline,
                unfocusedIndicatorColor = VirexColors.Hairline,
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )

        if (!searched && recentSearches.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Recent searches", Modifier.weight(1f))
                TextButton(onClick = { scope.launch { db.recentDao().clearSearches() } }) {
                    Text("Clear all", color = VirexColors.Grey, fontSize = 12.sp)
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                items(recentSearches) { item ->
                    VirexChip(
                        text = item.query,
                        selected = false,
                        onClick = {
                            query = item.query
                            runSearch(item.query)
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        when {
            searching -> {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VirexColors.White)
                }
            }
            searched && failed -> {
                Text(
                    "Couldn't reach the music service. Check your connection and try again.",
                    color = VirexColors.Grey,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
            searched && results.isEmpty() -> {
                Text(
                    "No results for \"$query\".",
                    color = VirexColors.Grey,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
            results.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results.size) { i ->
                        val song = results[i]
                        SongRow(
                            song = song,
                            onClick = { player.playQueue(results, i) },
                            onMore = { moreSong = song }
                        )
                    }
                }
            }
            else -> {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "Search for any song, artist or album.",
                        color = VirexColors.Grey,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tapping a result starts playback and loads suggestions into your queue.",
                        color = VirexColors.DimGrey,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    moreSong?.let { song ->
        AddToPlaylistDialog(song = song, db = db, onDismiss = { moreSong = null })
    }
}
