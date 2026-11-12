package com.virex.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virex.music.data.AppDatabase
import com.virex.music.data.PlaylistEntity
import com.virex.music.data.Song
import com.virex.music.data.toPlaylistSong
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.theme.VirexColors
import kotlinx.coroutines.launch

/** Thin glass strip above the bottom bar. Tapping expands the full player. */
@Composable
fun MiniPlayer(
    player: PlayerViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by player.current.collectAsState()
    val playing by player.isPlaying.collectAsState()
    val position by player.positionMs.collectAsState()
    val duration by player.durationMs.collectAsState()
    if (song == null) return
    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .background(VirexColors.Surface)
            .border(1.dp, VirexColors.Hairline, RoundedCornerShape(0.dp))
    ) {
        // 2dp progress line along the top edge.
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.12f))) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(Color.White)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkImage(
                url = song!!.artworkUrl,
                contentDescription = song!!.title,
                modifier = Modifier.size(40.dp),
                cornerDp = 8
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song!!.title,
                    color = VirexColors.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song!!.artist,
                    color = VirexColors.Grey,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            GlassIconButton(
                icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = if (playing) "Pause" else "Play",
                onClick = { player.togglePlay() },
                sizeDp = 36
            )
            Spacer(Modifier.width(8.dp))
            GlassIconButton(
                icon = Icons.Filled.SkipNext,
                description = "Next",
                onClick = { player.next() },
                sizeDp = 36
            )
        }
    }
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val BottomDestinations = listOf(
    BottomDestination("home", "Home", Icons.Filled.Home),
    BottomDestination("search", "Search", Icons.Filled.Search),
    BottomDestination("library", "Library", Icons.Filled.LibraryMusic),
    BottomDestination("files", "Your Files", Icons.Filled.Folder)
)

/** Bottom bar: black, hairline top border, 4 labelled destinations. */
@Composable
fun VirexBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(VirexColors.Black)
            .border(1.dp, VirexColors.Hairline, RoundedCornerShape(0.dp))
            .padding(vertical = 6.dp)
    ) {
        for (dest in BottomDestinations) {
            val selected = currentRoute == dest.route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(dest.route) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = dest.icon,
                    contentDescription = dest.label,
                    tint = if (selected) VirexColors.White else VirexColors.Grey,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dest.label,
                    color = if (selected) VirexColors.White else VirexColors.Grey,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Dialog to add a song to one of the user's playlists (or a new one). */
@Composable
fun AddToPlaylistDialog(
    song: Song,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val playlists by db.playlistDao().playlists().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newName by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VirexColors.Surface,
        title = { Text("Add to playlist", color = VirexColors.White) },
        text = {
            Column {
                Text(
                    text = "${song.title} — ${song.artist}",
                    color = VirexColors.Grey,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                if (playlists.isEmpty()) {
                    Text("No playlists yet.", color = VirexColors.DimGrey, fontSize = 13.sp)
                } else {
                    LazyColumn(Modifier.height(220.dp)) {
                        items(playlists) { playlist: PlaylistEntity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        scope.launch {
                                            val dao = db.playlistDao()
                                            val pos = dao.songCount(playlist.id)
                                            dao.addSong(song.toPlaylistSong(playlist.id, pos))
                                            onDismiss()
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = VirexColors.Grey,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(playlist.name, color = VirexColors.White, fontSize = 15.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (creating) {
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
                } else {
                    TextButton(
                        onClick = { creating = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = VirexColors.White)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New playlist")
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isEmpty()) return@TextButton
                        scope.launch {
                            val id = db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
                            db.playlistDao().addSong(song.toPlaylistSong(id, 0))
                            onDismiss()
                        }
                    }
                ) { Text("Create & add", color = VirexColors.White) }
            } else {
                TextButton(onClick = onDismiss) { Text("Close", color = VirexColors.Grey) }
            }
        },
        dismissButton = {
            if (creating) {
                TextButton(onClick = { creating = false }) { Text("Back", color = VirexColors.Grey) }
            }
        }
    )
}
