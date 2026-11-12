package com.virex.music.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virex.music.data.ApiClient
import com.virex.music.data.AppDatabase
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.AddToPlaylistDialog
import com.virex.music.ui.components.ArtworkImage
import com.virex.music.ui.components.GlassIconButton
import com.virex.music.ui.components.SectionHeader
import com.virex.music.ui.components.SongRow
import com.virex.music.ui.components.VirexChip
import com.virex.music.ui.theme.VirexColors

private val Categories = listOf(
    "Trending" to "trending bollywood songs",
    "Hindi" to "hindi songs",
    "English" to "english pop songs",
    "Punjabi" to "punjabi songs",
    "Lo-Fi" to "lofi songs",
    "Workout" to "workout songs",
    "Classics" to "old hindi classics"
)

@Composable
fun HomeScreen(
    player: PlayerViewModel,
    db: AppDatabase,
    onOpenSettings: () -> Unit,
    onOpenFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(0) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var moreSong by remember { mutableStateOf<Song?>(null) }

    val recentPlays by db.recentDao().recentlyPlayed().collectAsState(initial = emptyList())

    LaunchedEffect(selected) {
        loading = true
        loadFailed = false
        try {
            val res = ApiClient.search.searchSongs(Categories[selected].second, 20)
            songs = res.songs
            if (songs.isEmpty()) loadFailed = true
        } catch (_: Exception) {
            songs = emptyList()
            loadFailed = true
        } finally {
            loading = false
        }
    }

    val trending = songs.take(10)
    val quickPicks = songs.drop(10)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Top bar: thin wordmark left, settings gear right — always visible.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Virex Music",
                    color = VirexColors.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(
                    icon = Icons.Filled.Settings,
                    description = "Settings",
                    onClick = onOpenSettings
                )
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Categories.size) { i ->
                    VirexChip(
                        text = Categories[i].first,
                        selected = selected == i,
                        onClick = { selected = i }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = VirexColors.White) }
            }
        } else if (loadFailed && songs.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "You're offline or the music service didn't respond.",
                        color = VirexColors.Grey,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onOpenFiles) {
                        Text("Open Your Files", color = VirexColors.White)
                    }
                }
            }
        } else {
            item {
                SectionHeader(
                    "Trending Mix",
                    Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trending.size) { i ->
                        val song = trending[i]
                        val cardSize = if (i == 0) 168.dp else 132.dp
                        Column(
                            Modifier.width(cardSize).clickable {
                                player.playQueue(trending, i)
                            }
                        ) {
                            ArtworkImage(
                                url = song.artworkUrl,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(cardSize)
                                    .border(1.dp, VirexColors.Hairline, RoundedCornerShape(14.dp)),
                                cornerDp = 14
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                song.title,
                                color = VirexColors.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.artist,
                                color = VirexColors.Grey,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            if (quickPicks.isNotEmpty()) {
                item {
                    SectionHeader("Quick picks", Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                }
                items(quickPicks.chunked(2)) { pair ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (song in pair) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clickable {
                                        val idx = quickPicks.indexOf(song)
                                        player.playQueue(quickPicks, idx)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkImage(
                                    url = song.artworkUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.size(44.dp),
                                    cornerDp = 8
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        song.title,
                                        color = VirexColors.White,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        song.artist,
                                        color = VirexColors.Grey,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            if (recentPlays.isNotEmpty()) {
                item {
                    SectionHeader("Recently played", Modifier.padding(horizontal = 16.dp))
                }
                items(recentPlays) { play ->
                    val song = play.toSong()
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SongRow(
                            song = song,
                            onClick = {
                                val list = recentPlays.map { it.toSong() }
                                player.playQueue(list, list.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
                            },
                            onMore = { moreSong = song }
                        )
                    }
                }
            }
        }
    }

    moreSong?.let { song ->
        AddToPlaylistDialog(song = song, db = db, onDismiss = { moreSong = null })
    }
}
