package com.virex.music.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.virex.music.data.currentLyricIndex
import com.virex.music.data.formatDuration
import com.virex.music.player.LoopMode
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.components.GlassIconButton
import com.virex.music.ui.components.PlayButton
import com.virex.music.ui.components.SnakeProgress
import com.virex.music.ui.components.SongRow
import com.virex.music.ui.theme.VirexColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    player: PlayerViewModel,
    onClose: () -> Unit,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by player.current.collectAsState()
    val playing by player.isPlaying.collectAsState()
    val position by player.positionMs.collectAsState()
    val duration by player.durationMs.collectAsState()
    val shuffle by player.shuffle.collectAsState()
    val loop by player.loop.collectAsState()
    val queue by player.queue.collectAsState()
    val index by player.index.collectAsState()
    val lines by player.lyricLines.collectAsState()
    val plainLyrics by player.plainLyrics.collectAsState()
    val lyricsLoading by player.lyricsLoading.collectAsState()
    val hq by player.settings.hq.collectAsState()
    val animSpeed by player.settings.animSpeed.collectAsState()
    val accentFromArtwork by player.settings.accentFromArtwork.collectAsState()
    val sleepLeft by player.sleepMinLeft.collectAsState()
    val error by player.error.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }

    if (song == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing playing yet.", color = VirexColors.Grey)
        }
        return
    }

    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val lyricIdx = currentLyricIndex(lines, position.toLong())
    val currentLine = lines.getOrNull(lyricIdx)?.text
    val glowMs = (600f / animSpeed).toInt().coerceIn(200, 1200)

    Box(modifier.fillMaxSize()) {
        // Soft artwork glow behind everything (crossfades on track change).
        if (accentFromArtwork && !song!!.artworkUrl.isNullOrBlank()) {
            Crossfade(
                targetState = song!!.artworkUrl,
                animationSpec = tween(glowMs),
                label = "artwork glow"
            ) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(2f)
                        .alpha(0.22f)
                )
            }
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close player", tint = VirexColors.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "NOW PLAYING",
                    color = VirexColors.Grey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(12.dp))

            // Circular artwork, no ring: progress lives on the snake bar.
            if (song!!.artworkUrl.isNullOrBlank()) {
                Box(
                    Modifier.size(280.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = VirexColors.Grey,
                        modifier = Modifier.size(72.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = song!!.artworkUrl,
                    contentDescription = song!!.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(280.dp).clip(CircleShape)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                song!!.title,
                color = VirexColors.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song!!.artist,
                color = VirexColors.Grey,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            // Current synced-lyric line; tapping it opens the full lyrics page.
            Text(
                text = when {
                    lyricsLoading -> "Finding lyrics…"
                    currentLine != null -> currentLine
                    plainLyrics != null -> plainLyrics!!.lineSequence().firstOrNull() ?: ""
                    else -> "No synced lyrics for this song"
                },
                color = if (currentLine != null) VirexColors.White else VirexColors.DimGrey,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(onClick = onOpenLyrics)
            )

            Spacer(Modifier.height(8.dp))

            // The snake: animated wave progress bar. Drag or tap to seek.
            SnakeProgress(
                progress = progress,
                positionMs = position,
                onSeek = { fraction ->
                    if (duration > 0) player.seekTo((fraction * duration).toInt())
                }
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatDuration(position / 1000), color = VirexColors.Grey, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassIconButton(
                        icon = Icons.Filled.Replay10,
                        description = "Back 10 seconds",
                        onClick = { player.seekBy(-10_000) },
                        sizeDp = 32,
                        iconSizeDp = 16
                    )
                    GlassIconButton(
                        icon = Icons.Filled.Forward10,
                        description = "Forward 10 seconds",
                        onClick = { player.seekBy(10_000) },
                        sizeDp = 32,
                        iconSizeDp = 16
                    )
                }
                Text(formatDuration(duration / 1000), color = VirexColors.Grey, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Controls: small glass circles, 56dp play in the middle.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Filled.Shuffle,
                    description = "Shuffle",
                    active = shuffle,
                    onClick = { player.toggleShuffle() }
                )
                GlassIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    description = "Previous",
                    onClick = { player.previous() }
                )
                PlayButton(
                    playing = playing,
                    onClick = { player.togglePlay() }
                )
                GlassIconButton(
                    icon = Icons.Filled.SkipNext,
                    description = "Next",
                    onClick = { player.next() }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassIconButton(
                        icon = if (loop == LoopMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        description = "Repeat",
                        active = loop != LoopMode.OFF,
                        onClick = { player.toggleLoop() }
                    )
                    if (loop == LoopMode.ONE) {
                        Spacer(Modifier.height(2.dp))
                        Canvas(Modifier.size(4.dp)) {
                            drawCircle(color = VirexColors.White, radius = 2.dp.toPx())
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerTextButton("Queue", Icons.Filled.QueueMusic) { showQueue = true }
                PlayerTextButton("Lyrics", Icons.Filled.MusicNote) { onOpenLyrics() }
                PlayerTextButton(
                    if (sleepLeft > 0) "Sleep ${sleepLeft}m" else "Sleep",
                    Icons.Filled.Timer
                ) { showSleep = true }
                PlayerTextButton(
                    if (song!!.isLocal) "HQ FILE" else "HQ ${song!!.qualityLabel(hq)}",
                    null
                ) { if (!song!!.isLocal) player.toggleHq() }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { player.clearError() }) {
                    Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp)
                }
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = VirexColors.Surface
        ) {
            Text(
                "UP NEXT",
                color = VirexColors.Grey,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(queue.size) { i ->
                    val item = queue[i]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            SongRow(
                                song = item,
                                onClick = {
                                    player.playAt(i)
                                    showQueue = false
                                },
                                onMore = { player.removeFromQueue(i) },
                                showDuration = false
                            )
                        }
                        if (i == index) {
                            Text("Playing", color = VirexColors.Grey, fontSize = 11.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            containerColor = VirexColors.Surface,
            title = { Text("Sleep timer", color = VirexColors.White) },
            text = {
                Column {
                    Text(
                        "Playback pauses when the timer ends.",
                        color = VirexColors.Grey,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    for (mins in listOf(0, 10, 20, 30)) {
                        TextButton(onClick = {
                            player.setSleepTimer(mins)
                            showSleep = false
                        }) {
                            Text(
                                if (mins == 0) "Off" else "$mins minutes",
                                color = if (sleepLeft == mins) VirexColors.White else VirexColors.Grey
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleep = false }) {
                    Text("Close", color = VirexColors.Grey)
                }
            }
        )
    }
}

@Composable
private fun PlayerTextButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = VirexColors.Grey, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = VirexColors.White, fontSize = 13.sp, maxLines = 1)
    }
}
