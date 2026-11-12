package com.virex.music.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.virex.music.data.LocalTrack
import com.virex.music.data.Song
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.SongRow
import com.virex.music.ui.theme.VirexColors

@Composable
fun FilesScreen(
    player: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var tracks by remember { mutableStateOf<List<LocalTrack>>(emptyList()) }
    var askedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        askedOnce = true
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val song = Song(
                id = "file:${uri}",
                title = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio file",
                artist = "Your file",
                streamUrl = uri.toString(),
                isLocal = true
            )
            player.playQueue(listOf(song), 0)
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val list = mutableListOf<LocalTrack>()
        try {
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            context.contentResolver.query(
                uri, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = android.content.ContentUris.withAppendedId(uri, id)
                    list.add(
                        LocalTrack(
                            id = id,
                            title = cursor.getString(titleCol) ?: "Unknown",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            uri = contentUri.toString(),
                            durationMs = cursor.getLong(durCol)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        tracks = list
    }

    Column(modifier.fillMaxSize()) {
        Text(
            text = "Your Files",
            color = VirexColors.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        Text(
            text = "This plays audio files already on your phone. Streamed songs are not downloaded.",
            color = VirexColors.Grey,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { filePicker.launch(arrayOf("audio/*")) },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text("Pick files", color = VirexColors.White)
        }

        if (!hasPermission) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "To list your audio files, Virex Music needs permission to read audio on this device.",
                    color = VirexColors.Grey,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { permissionLauncher.launch(permission) }) {
                    Text(
                        if (askedOnce) "Grant permission" else "Allow access to audio",
                        color = VirexColors.White
                    )
                }
                Text(
                    "You can still use Pick files above without this permission.",
                    color = VirexColors.DimGrey,
                    fontSize = 13.sp
                )
            }
        } else if (tracks.isEmpty()) {
            Text(
                "No audio files found on this device.",
                color = VirexColors.Grey,
                fontSize = 14.sp,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tracks.size) { i ->
                    val track = tracks[i]
                    SongRow(
                        song = track.toSong(),
                        onClick = {
                            val list = tracks.map { it.toSong() }
                            player.playQueue(list, i)
                        },
                        onMore = {}
                    )
                }
            }
        }
    }
}
