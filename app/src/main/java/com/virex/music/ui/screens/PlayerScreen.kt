package com.virex.music.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.virex.music.ui.components.SongRow
import com.virex.music.ui.theme.VirexColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    player: PlayerViewModel,
    onClose: () -> Unit,
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
    var showLyrics by remember { mutableStateOf(false) }
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

    fun seekFromOffset(offset: Offset, boxSizePx: Int) {
        if (duration <= 0) return
        val cx = boxSizePx / 2f
        val cy = boxSizePx / 2f
        val angleDeg = Math.toDegrees(atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())).toFloat()
        val fraction = ((angleDeg + 90f + 360f) % 360f) / 360f
        player.seekTo((fraction * duration).toInt())
    }

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

            Spacer(Modifier.height(16.dp))

            // Circular artwork with a thin progress ring + red dot head.
            // Dragging on the ring seeks; tapping the ring jumps there.
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .pointerInput(duration) {
                        coroutineScope {
                            launch {
                                detectTapGestures { offset ->
                                    seekFromOffset(offset, size.width)
                                }
                            }
                            launch {
                                detectDragGestures { change, _ ->
                                    seekFromOffset(change.position, size.width)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    val inset = stroke / 2f + 4.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = size.minDimension / 2f - inset,
                        style = Stroke(width = stroke)
                    )
                    if (progress > 0f) {
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    // Red progress head dot.
                    val angleRad = Math.toRadians((-90f + 360f * progress).toDouble())
                    val r = size.minDimension / 2f - inset
                    val dotX = size.width / 2f + r * kotlin.math.cos(angleRad).toFloat()
                    val dotY = size.height / 2f + r * kotlin.math.sin(angleRad).toFloat()
                    drawCircle(
                        color = VirexColors.ProgressRed,
                        radius = 5.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
                if (song!!.artworkUrl.isNullOrBlank()) {
                    Box(
                        Modifier
                            .size(240.dp)
                            .clip(CircleShape),
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
                        modifier = Modifier.size(240.dp).clip(CircleShape)
                    )
                }
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
            // Current synced-lyric line. LRC gives line-level timing, so the
            // whole current line shows in white; the full view highlights it.
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
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(position / 1000), color = VirexColors.Grey, fontSize = 12.sp)
                Text(formatDuration(duration / 1000), color = VirexColors.Grey, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

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
                PlayerTextButton("Lyrics", Icons.Filled.MusicNote) { showLyrics = true }
                PlayerTextButton(
                    if (sleepLeft > 0) "Sleep ${sleepLeft}m" else "Sleep",
                    Icons.Filled.Timer
                ) { showSleep = true }
                PlayerTextButton(
                    if (song!!.isLocal) "HQ · FILE" else "HQ · ${song!!.qualityLabel(hq)}",
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

    if (showLyrics) {
        ModalBottomSheet(
            onDismissRequest = { showLyrics = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = VirexColors.Surface
        ) {
            Text(
                "LYRICS",
                color = VirexColors.Grey,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (lines.isNotEmpty()) {
                val listState = rememberLazyListState()
                LaunchedEffect(lyricIdx) {
                    if (lyricIdx >= 0) {
                        listState.animateScrollToItem((lyricIdx - 2).coerceAtLeast(0))
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    items(lines.size) { i ->
                        Text(
                            lines[i].text,
                            color = if (i == lyricIdx) VirexColors.White else VirexColors.Grey,
                            fontSize = if (i == lyricIdx) 17.sp else 15.sp,
                            fontWeight = if (i == lyricIdx) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            } else if (plainLyrics != null) {
                LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    item {
                        Text(plainLyrics!!, color = VirexColors.White, fontSize = 15.sp)
                        Spacer(Modifier.height(32.dp))
                    }
                }
            } else {
                Text(
                    if (lyricsLoading) "Finding lyrics…" else "No lyrics found for this song.",
                    color = VirexColors.Grey,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
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
        Text(label, color = VirexColors.White, fontSize = 13.sp)
    }
}
