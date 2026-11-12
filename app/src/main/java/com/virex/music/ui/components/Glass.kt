package com.virex.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.virex.music.data.Song
import com.virex.music.data.formatDuration
import com.virex.music.ui.theme.VirexColors

/** One glass material: white 8% fill, 1dp hairline border. */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerDp: Int = 16,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerDp.dp))
            .background(VirexColors.Glass)
            .border(1.dp, VirexColors.Hairline, RoundedCornerShape(cornerDp.dp)),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** A pressable wrapper with a quick shrink on touch-down, so every tap
 *  feels answered the moment a finger lands. No ripple: the scale is
 *  the feedback, keeping the monochrome look clean. */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "press-scale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Small precise icon button: 40dp glass circle, thin icon. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    sizeDp: Int = 40,
    iconSizeDp: Int = 20
) {
    Pressable(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(if (active) VirexColors.GlassStrong else VirexColors.Glass)
                .border(1.dp, VirexColors.Hairline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (active) VirexColors.White else VirexColors.Grey,
                modifier = Modifier.size(iconSizeDp.dp)
            )
        }
    }
}

/** Primary play/pause: 56dp glass circle. */
@Composable
fun PlayButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Pressable(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(VirexColors.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon ?: if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = VirexColors.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Small uppercase letter-spaced section header, e.g. "TRENDING MIX". */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = VirexColors.Grey,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
        modifier = modifier
    )
}

/** Artwork with a music-note fallback when there is no image. */
@Composable
fun ArtworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerDp: Int = 12
) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerDp.dp))
                .background(VirexColors.Glass)
                .border(1.dp, VirexColors.Hairline, RoundedCornerShape(cornerDp.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = contentDescription,
                tint = VirexColors.Grey
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(cornerDp.dp))
        )
    }
}

/** Slim song row: artwork, title, artist, duration, small more-button. */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showDuration: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            url = song.artworkUrl,
            contentDescription = song.title,
            modifier = Modifier.size(48.dp),
            cornerDp = 10
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = VirexColors.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = VirexColors.Grey,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showDuration && song.durationSec > 0) {
            Text(
                text = formatDuration(song.durationSec),
                color = VirexColors.DimGrey,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = VirexColors.Grey,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Hairline-outline chip; selected = white fill, black text. */
@Composable
fun VirexChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Pressable(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) VirexColors.White else VirexColors.Glass)
                .border(1.dp, VirexColors.Hairline, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) VirexColors.Black else VirexColors.White,
                fontSize = 13.sp
            )
        }
    }
}
