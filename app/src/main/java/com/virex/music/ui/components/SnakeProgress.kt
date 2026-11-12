package com.virex.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.virex.music.ui.theme.VirexColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * The "snake": a horizontal progress bar drawn as a smooth sine wave.
 * The played part is bright white, the rest is dim, and the wave's
 * phase is driven by the playback position, so the snake slithers
 * forward while the song plays and rests when paused. A glowing red
 * head dot sits at the current position. Press or drag anywhere on
 * the bar to seek.
 */
@Composable
fun SnakeProgress(
    progress: Float,
    positionMs: Int,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // The seek lambda changes every recomposition (position ticks 4x/s);
    // hold the latest in a state holder so the gesture block never
    // restarts mid-drag.
    val currentOnSeek by rememberUpdatedState(onSeek)
    val safeProgress = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Wait for a press (awaitPointerEvent takes a pass,
                        // not a type — filter on the event's type instead).
                        val press = awaitPointerEvent()
                        if (press.type != PointerEventType.Press) continue
                        val down = press.changes.firstOrNull() ?: continue
                        down.consume()
                        currentOnSeek((down.position.x / size.width).coerceIn(0f, 1f))
                        // Follow the finger until it lifts.
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            currentOnSeek((change.position.x / size.width).coerceIn(0f, 1f))
                        }
                    }
                }
            }
    ) {
        val amplitude = 5.dp.toPx()
        val wavelength = 56.dp.toPx()
        val centerY = size.height / 2f
        // Phase advances with playback: ~1 wave per 3.5s of music.
        val phase = positionMs * 0.0018f * (2f * PI.toFloat())

        fun waveY(x: Float): Float =
            centerY + amplitude * sin(2f * PI.toFloat() * x / wavelength + phase).toFloat()

        val wave = Path().apply {
            moveTo(0f, waveY(0f))
            var x = 4f
            while (x <= size.width) {
                lineTo(x, waveY(x))
                x += 4f
            }
            lineTo(size.width, waveY(size.width))
        }

        // Unplayed snake: thin and dim, full width.
        drawPath(
            path = wave,
            color = Color.White.copy(alpha = 0.22f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        // Played snake: bright and thicker, clipped to the progress point.
        if (safeProgress > 0f) {
            clipRect(right = size.width * safeProgress) {
                drawPath(
                    path = wave,
                    color = Color.White,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Head: red dot with a soft white glow, riding the wave.
            val headX = size.width * safeProgress
            val head = Offset(headX, waveY(headX))
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = 8.dp.toPx(),
                center = head
            )
            drawCircle(
                color = VirexColors.ProgressRed,
                radius = 4.5.dp.toPx(),
                center = head
            )
        }
    }
}
