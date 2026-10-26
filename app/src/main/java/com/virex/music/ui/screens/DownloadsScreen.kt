package com.virex.music.ui.screens

import android.content.ContentUris
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.virex.music.data.LocalTrack
import com.virex.music.player.PlayerViewModel

/**
 * Downloads tab — local files ONLY.
 *
 * This screen lists audio already on the device (MediaStore) and lets
 * the user pick a file via the Storage Access Framework. It does NOT
 * download tracks from any streaming service, and it never writes
 * streamed audio to storage. That is a deliberate scope limit of this
 * Android scaffold versus the web PWA's download feature.
 */
@Composable
fun DownloadsScreen(player: PlayerViewModel) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf<List<LocalTrack>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val song = com.virex.music.data.Song(
                id = "picked:${uri}",
                title = "Picked audio file",
                artist = "Local file",
                streamUrl = uri.toString(),
                isLocal = true
            )
            player.playQueue(listOf(song), 0)
            message = "Playing picked local file."
        }
    }

    fun loadFromMediaStore() {
        val out = mutableListOf<LocalTrack>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                out += LocalTrack(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown",
                    uri = ContentUris.withAppendedId(uri, id).toString(),
                    durationMs = cursor.getLong(durCol)
                )
            }
        }
        tracks = out
        if (out.isEmpty()) message = "No local audio found on device storage."
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Downloads — your local files")
        Button(
            onClick = { loadFromMediaStore() },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) { Text("Scan device audio") }
        Button(
            onClick = { picker.launch(arrayOf("audio/*")) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Pick a local audio file") }
        message?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tracks) { track ->
                val song = track.toSong()
                SongRow(song = song, onClick = {
                    val list = tracks.map { it.toSong() }
                    player.playQueue(list, list.indexOf(song))
                })
            }
        }
    }
}
