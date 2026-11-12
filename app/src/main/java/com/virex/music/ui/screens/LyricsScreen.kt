package com.virex.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.virex.music.data.currentLyricIndex
import com.virex.music.player.PlayerViewModel
import com.virex.music.ui.theme.VirexColors

/**
 * Full-screen lyrics page: big synced lines over the blurred artwork,
 * the current line bright and centred, the rest dim. Tapping any line
 * jumps the song to it.
 */
@Composable
fun LyricsScreen(
    player: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by player.current.collectAsState()
    val position by player.positionMs.collectAsState()
    val lines by player.lyricLines.collectAsState()
    val plainLyrics by player.plainLyrics.collectAsState()
    val lyricsLoading by player.lyricsLoading.collectAsState()

    val lyricIdx = currentLyricIndex(lines, position.toLong())
    val listState = rememberLazyListState()

    // Keep the current line near the top third of the screen. Don't
    // fight the user while they're dragging the list themselves.
    LaunchedEffect(lyricIdx) {
        if (lyricIdx >= 0 && !listState.isScrollInProgress) {
            val viewport = listState.layoutInfo.viewportSize.height
            val offset = if (viewport > 0) -(viewport / 3) else 0
            listState.animateScrollToItem(lyricIdx, scrollOffset = offset)
        }
    }

    Box(modifier.fillMaxSize().background(VirexColors.Black)) {
        // Blurred artwork behind everything, dimmed for readability.
        if (!song?.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = song!!.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp)
                    .alpha(0.45f)
            )
        }
        Box(
            Modifier.fillMaxSize().background(VirexColors.Black.copy(alpha = 0.55f))
        )

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close lyrics", tint = VirexColors.White)
                }
                Column(Modifier.weight(1f).padding(end = 48.dp)) {
                    Text(
                        text = song?.title ?: "Lyrics",
                        color = VirexColors.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = song?.artist ?: "",
                        color = VirexColors.Grey,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            when {
                lines.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 200.dp)
                    ) {
                        items(lines.size) { i ->
                            val current = i == lyricIdx
                            Text(
                                text = lines[i].text,
                                color = if (current) VirexColors.White else VirexColors.Grey,
                                fontSize = if (current) 22.sp else 18.sp,
                                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { player.seekTo(lines[i].timeMs.toInt()) }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                plainLyrics != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        item {
                            Text(
                                plainLyrics!!,
                                color = VirexColors.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(120.dp))
                        Text(
                            text = if (lyricsLoading) "Finding lyrics…" else "No lyrics found for this song.",
                            color = VirexColors.Grey,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        if (!lyricsLoading) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { player.retryLyrics() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VirexColors.White,
                                    contentColor = VirexColors.Black
                                )
                            ) {
                                Text("Try again")
                            }
                        }
                    }
                }
            }
        }
    }
}
