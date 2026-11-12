package com.virex.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.virex.music.data.ApiClient
import com.virex.music.data.AppDatabase
import com.virex.music.data.PlaylistEntity
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.AddToPlaylistDialog
import com.virex.music.ui.components.ArtworkImage
import com.virex.music.ui.components.GlassIconButton
import com.virex.music.ui.components.Pressable
import com.virex.music.ui.components.SectionHeader
import com.virex.music.ui.components.VirexChip
import com.virex.music.ui.theme.VirexColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

// Each tab merges several queries: one query returns tracks from the
// same compilation over and over (same artwork everywhere), while a
// merge of artist + chart queries gives real songs with real covers.
private val Categories = listOf(
    "Trending" to listOf("trending hindi songs", "trending english songs", "arijit singh", "trending punjabi songs"),
    "Hindi" to listOf("arijit singh", "hindi love songs", "bollywood hits"),
    "English" to listOf("english pop songs", "ed sheeran", "imagine dragons"),
    "Punjabi" to listOf("punjabi songs", "diljit dosanjh", "punjabi hits"),
    "Lo-Fi" to listOf("lofi songs", "lofi hindi", "chill lofi"),
    "Workout" to listOf("workout songs", "gym songs", "workout hindi"),
    "Classics" to listOf("old hindi classics", "kishore kumar", "lata mangeshkar")
)

@Composable
fun HomeScreen(
    player: PlayerViewModel,
    db: AppDatabase,
    onOpenSettings: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(0) }
    var reloadKey by remember { mutableStateOf(0) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var moreSong by remember { mutableStateOf<Song?>(null) }
    var seedSong by remember { mutableStateOf<Song?>(null) }
    var suggested by remember { mutableStateOf<List<Song>>(emptyList()) }
    var entered by remember { mutableStateOf(false) }

    val recentPlays by db.recentDao().recentlyPlayed().collectAsState(initial = emptyList())
    val playlists by db.playlistDao().playlists().collectAsState(initial = emptyList())

    LaunchedEffect(selected, reloadKey) {
        loading = true
        loadFailed = false
        try {
            val queries = Categories[selected].second
            val lists = queries.map { q ->
                async {
                    try {
                        ApiClient.search.searchSongs(q, 12).songs
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll()
            // Dedupe by song, then by artwork: compilation tracks share
            // one cover, and the same cover repeated looks broken.
            val seenArt = mutableSetOf<String>()
            val merged = lists.flatten()
                .distinctBy { it.id }
                .filter { song ->
                    val art = song.artworkUrl
                    if (art.isNullOrBlank()) true else seenArt.add(art)
                }
                .take(24)
            if (merged.isNotEmpty()) {
                songs = merged
                entered = true
            } else if (songs.isEmpty()) {
                loadFailed = true
            }
        } catch (_: Exception) {
            if (songs.isEmpty()) loadFailed = true
        } finally {
            loading = false
        }
    }

    // "Because you played": suggestions for the most recent song.
    // Local files have no server id, so they can't seed suggestions.
    LaunchedEffect(recentPlays) {
        val seed = recentPlays.firstOrNull()?.toSong()
        if (seed == null || seed.isLocal) {
            seedSong = null
            suggested = emptyList()
            return@LaunchedEffect
        }
        seedSong = seed
        try {
            val res = ApiClient.search.suggestions(seed.id, 12)
            val seenArt = mutableSetOf<String>()
            suggested = res.songs
                .distinctBy { it.id }
                .filter { it.id != seed.id }
                .filter { song ->
                    val art = song.artworkUrl
                    if (art.isNullOrBlank()) true else seenArt.add(art)
                }
                .take(10)
        } catch (_: Exception) {
            suggested = emptyList()
        }
    }

    val recentSongs = recentPlays.map { it.toSong() }

    Column(modifier.fillMaxSize()) {
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
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Categories.size) { i ->
                VirexChip(
                    text = Categories[i].first,
                    selected = selected == i,
                    onClick = {
                        if (selected == i) reloadKey++ else selected = i
                    }
                )
            }
        }
        // Thin refresh line while a new tab loads over old content.
        if (loading && songs.isNotEmpty()) {
            LinearProgressIndicator(
                color = VirexColors.White,
                trackColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }

        if (loading && songs.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = VirexColors.White) }
        } else if (loadFailed && songs.isEmpty()) {
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
        } else {
            // The mix crossfades when its songs change (new tab loaded).
            Crossfade(
                targetState = songs,
                animationSpec = tween(300),
                label = "home-mix"
            ) { list ->
                val hero = list.firstOrNull()
                val trending = list.drop(1).take(10)
                val quickPicks = list.drop(11)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Hero: the top song of the mix as a big feature card.
                    if (hero != null) {
                        item {
                            StaggeredSection(index = 0, visible = entered) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { player.playQueue(list, 0) }
                                ) {
                                    ArtworkImage(
                                        url = hero.artworkUrl,
                                        contentDescription = hero.title,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerDp = 20
                                    )
                                    // Bottom scrim so the text reads over any artwork.
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.55f),
                                                        Color.Black.copy(alpha = 0.92f)
                                                    )
                                                )
                                            )
                                    )
                                    Column(
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = if (selected == 0) "TRENDING NOW" else "${Categories[selected].first.uppercase()} MIX",
                                            color = VirexColors.Grey,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.5.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = hero.title,
                                            color = VirexColors.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = hero.artist,
                                            color = VirexColors.Grey,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Pressable(onClick = { player.playQueue(list, 0) }) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(VirexColors.White)
                                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.PlayArrow,
                                                    contentDescription = null,
                                                    tint = VirexColors.Black,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "Play",
                                                    color = VirexColors.Black,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    // Because you played: real suggestions for the last song.
                    if (suggested.isNotEmpty() && seedSong != null) {
                        item {
                            StaggeredSection(index = 1, visible = entered) {
                                SectionHeader("Because you played", Modifier.padding(horizontal = 16.dp))
                                Text(
                                    text = "${seedSong!!.title} — ${seedSong!!.artist}",
                                    color = VirexColors.White,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(suggested.size) { i ->
                                        val song = suggested[i]
                                        Column(
                                            Modifier.width(132.dp).clickable {
                                                player.playQueue(suggested, i)
                                            }
                                        ) {
                                            ArtworkImage(
                                                url = song.artworkUrl,
                                                contentDescription = song.title,
                                                modifier = Modifier
                                                    .size(132.dp)
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
                        }
                    }

                    // Your playlists as glass folders (AETHER's signature).
                    if (playlists.isNotEmpty()) {
                        item {
                            StaggeredSection(index = 2, visible = entered) {
                                SectionHeader("Your playlists", Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(playlists.size) { i ->
                                        FolderCard(
                                            playlist = playlists[i],
                                            db = db,
                                            onClick = { onOpenPlaylist(playlists[i].id) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    // Recently played as square cards.
                    if (recentSongs.isNotEmpty()) {
                        item {
                            StaggeredSection(index = 3, visible = entered) {
                                SectionHeader("Recently played", Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentSongs.size) { i ->
                                        val song = recentSongs[i]
                                        Column(
                                            Modifier.width(120.dp).clickable {
                                                player.playQueue(recentSongs, i)
                                            }
                                        ) {
                                            ArtworkImage(
                                                url = song.artworkUrl,
                                                contentDescription = song.title,
                                                modifier = Modifier
                                                    .size(120.dp)
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
                        }
                    }

                    item {
                        StaggeredSection(index = 4, visible = entered) {
                            SectionHeader(
                                "${Categories[selected].first} Mix",
                                Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(trending.size) { i ->
                                    val song = trending[i]
                                    Column(
                                        Modifier.width(132.dp).clickable {
                                            player.playQueue(list, i + 1)
                                        }
                                    ) {
                                        ArtworkImage(
                                            url = song.artworkUrl,
                                            contentDescription = song.title,
                                            modifier = Modifier
                                                .size(132.dp)
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
                    }

                    if (quickPicks.isNotEmpty()) {
                        item {
                            StaggeredSection(index = 5, visible = entered) {
                                SectionHeader("Quick picks", Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(8.dp))
                            }
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
                }
            }
        }
    }

    moreSong?.let { song ->
        AddToPlaylistDialog(song = song, db = db, onDismiss = { moreSong = null })
    }
}

/** A home section that fades and slides in, staggered by its index. */
@Composable
private fun StaggeredSection(
    index: Int,
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400, delayMillis = index * 90)) +
            slideInVertically(animationSpec = tween(400, delayMillis = index * 90)) { it / 3 },
        exit = fadeOut()
    ) {
        Column { content() }
    }
}

/** A playlist as a glass folder: tab on top, the first song's artwork
 *  peeking out, name and count on the solid front face. */
@Composable
private fun FolderCard(
    playlist: PlaylistEntity,
    db: AppDatabase,
    onClick: () -> Unit
) {
    val songs by db.playlistDao().songsFor(playlist.id).collectAsState(initial = emptyList())
    val art = songs.firstOrNull()?.artworkUrl
    Box(
        Modifier
            .width(150.dp)
            .height(158.dp)
            .clickable(onClick = onClick)
    ) {
        // Folder tab.
        Box(
            Modifier
                .offset(x = 12.dp)
                .width(56.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(VirexColors.GlassStrong)
        )
        // Back panel.
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(VirexColors.GlassStrong)
        )
        // Artwork peeking out above the front face.
        if (!art.isNullOrBlank()) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .size(104.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        // Solid front face with the name and song count.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(VirexColors.Surface)
                .border(1.dp, VirexColors.Hairline, RoundedCornerShape(16.dp))
        ) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = playlist.name,
                    color = VirexColors.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${songs.size} songs",
                    color = VirexColors.Grey,
                    fontSize = 12.sp
                )
            }
        }
    }
}
