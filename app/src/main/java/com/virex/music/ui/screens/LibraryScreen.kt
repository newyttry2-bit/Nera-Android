package com.virex.music.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virex.music.data.AppDatabase
import com.virex.music.data.PlaylistEntity
import com.virex.music.data.PlaylistSongEntity
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.ArtworkImage
import com.virex.music.ui.components.GlassIconButton
import com.virex.music.ui.components.SongRow
import com.virex.music.ui.theme.VirexColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    player: PlayerViewModel,
    db: AppDatabase,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by db.playlistDao().playlists().collectAsState(initial = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<PlaylistEntity?>(null) }
    var previews by remember { mutableStateOf<Map<Long, List<PlaylistSongEntity>>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    // First artworks per playlist, for the glass-folder peek effect.
    LaunchedEffect(playlists) {
        val map = mutableMapOf<Long, List<PlaylistSongEntity>>()
        for (p in playlists) {
            try {
                map[p.id] = db.playlistDao().songsFor(p.id).first().take(3)
            } catch (_: Exception) {
            }
        }
        previews = map
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                color = VirexColors.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.weight(1f)
            )
            GlassIconButton(
                icon = Icons.Filled.Add,
                description = "Create playlist",
                onClick = { showCreate = true }
            )
        }

        if (playlists.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "No playlists yet.",
                    color = VirexColors.White,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap + to create one, then add songs from Search or Home with the ••• button.",
                    color = VirexColors.Grey,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists.chunked(2)) { pair ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (playlist in pair) {
                            PlaylistFolderCard(
                                playlist = playlist,
                                preview = previews[playlist.id] ?: emptyList(),
                                onClick = { onOpenPlaylist(playlist.id) },
                                onDelete = { deleteTarget = playlist },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = VirexColors.Surface,
            title = { Text("New playlist", color = VirexColors.White) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Playlist name", color = VirexColors.DimGrey) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VirexColors.Glass,
                        unfocusedContainerColor = VirexColors.Glass,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isEmpty()) return@TextButton
                    scope.launch {
                        db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
                        newName = ""
                        showCreate = false
                    }
                }) { Text("Create", color = VirexColors.White) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel", color = VirexColors.Grey) }
            }
        )
    }

    deleteTarget?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = VirexColors.Surface,
            title = { Text("Delete playlist?", color = VirexColors.White) },
            text = { Text("\"${playlist.name}\" will be removed.", color = VirexColors.Grey) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { db.playlistDao().deletePlaylist(playlist) }
                    deleteTarget = null
                }) { Text("Delete", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Keep", color = VirexColors.Grey) }
            }
        )
    }
}

/** Glass folder card with the playlist's first artworks peeking inside. */
@Composable
private fun PlaylistFolderCard(
    playlist: PlaylistEntity,
    preview: List<PlaylistSongEntity>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VirexColors.Glass)
            .border(1.dp, VirexColors.Hairline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box(Modifier.height(56.dp)) {
                if (preview.isEmpty()) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VirexColors.GlassStrong)
                    )
                } else {
                    preview.forEachIndexed { i, song ->
                        ArtworkImage(
                            url = song.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .offset(x = (i * 28).dp, y = (i * 4).dp),
                            cornerDp = 8
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    playlist.name,
                    color = VirexColors.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete playlist",
                        tint = VirexColors.DimGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                "${preview.size}${if (preview.size == 3) "+" else ""} songs",
                color = VirexColors.Grey,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    player: PlayerViewModel,
    db: AppDatabase,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by db.playlistDao().songsFor(playlistId).collectAsState(initial = emptyList())
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playlistId) {
        name = db.playlistDao().playlistById(playlistId)?.name ?: "Playlist"
    }

    Column(modifier.fillMaxSize()) {
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
                text = name,
                color = VirexColors.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (songs.isNotEmpty()) {
                GlassIconButton(
                    icon = Icons.Filled.PlayArrow,
                    description = "Play all",
                    onClick = {
                        val list = songs.map { it.toSong() }
                        player.playQueue(list, 0)
                    }
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        if (songs.isEmpty()) {
            Text(
                "Nothing here yet. Add songs from Search or Home with the ••• button.",
                color = VirexColors.Grey,
                fontSize = 14.sp,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(songs.size) { i ->
                    val entity = songs[i]
                    val song: Song = entity.toSong()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            SongRow(
                                song = song,
                                onClick = {
                                    val list = songs.map { it.toSong() }
                                    player.playQueue(list, i)
                                },
                                onMore = {
                                    scope.launch {
                                        db.playlistDao().removeSong(playlistId, song.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
