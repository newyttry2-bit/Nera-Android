package com.virex.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.virex.music.data.AppDatabase
import com.virex.music.data.PlaylistEntity
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import kotlinx.coroutines.launch

/**
 * Playlists backed by Room (web app used localStorage). Create a
 * playlist, tap it to play its songs, and add songs from Search via
 * the pending-song picker in MainActivity.
 */
@Composable
fun PlaylistsScreen(
    db: AppDatabase,
    player: PlayerViewModel,
    pendingSong: Song?,
    onPendingHandled: () -> Unit
) {
    val playlists by db.playlistDao().playlists().collectAsState(initial = emptyList())
    var newName by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val songs by db.playlistDao().let { dao ->
        if (selectedId == null) remember { kotlinx.coroutines.flow.flowOf(emptyList<com.virex.music.data.PlaylistSongEntity>()) }
        else dao.songsFor(selectedId!!)
    }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Playlist name") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val name = newName.trim().ifBlank { "My Playlist" }
                    scope.launch {
                        db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
                    }
                    newName = ""
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Create") }
        }

        if (pendingSong != null) {
            Text("Add “${pendingSong.title}” to:", modifier = Modifier.padding(top = 12.dp))
            playlists.forEach { playlist ->
                Button(
                    onClick = {
                        scope.launch {
                            val count = songs.size
                            db.playlistDao().addSong(
                                com.virex.music.data.PlaylistSongEntity(
                                    playlistId = playlist.id,
                                    songId = pendingSong.id,
                                    title = pendingSong.title,
                                    artist = pendingSong.artist,
                                    artworkUrl = pendingSong.artworkUrl,
                                    streamUrl = pendingSong.streamUrl,
                                    durationSec = pendingSong.durationSec,
                                    position = count
                                )
                            )
                        }
                        onPendingHandled()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text(playlist.name) }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(playlists) { playlist ->
                Card(onClick = { selectedId = playlist.id }) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(playlist.name)
                    }
                }
            }
            if (selectedId != null) {
                items(songs) { item ->
                    val song = Song(
                        id = item.songId,
                        title = item.title,
                        artist = item.artist,
                        artworkUrl = item.artworkUrl,
                        durationSec = item.durationSec,
                        streamUrl = item.streamUrl
                    )
                    SongRow(song = song, onClick = {
                        val list = songs.map {
                            Song(it.songId, it.title, it.artist, it.artworkUrl, it.durationSec, it.streamUrl)
                        }
                        player.playQueue(list, list.indexOf(song))
                    })
                }
            }
        }
    }
}
