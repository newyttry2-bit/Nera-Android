package com.virex.music.data

/** One synced lyric line. timeMs is the start time of the line. */
data class LyricLine(val timeMs: Long, val text: String)

/**
 * Parses LRC text ("[mm:ss.xx] words") into timed lines.
 * Lines without a timestamp and metadata tags ([ar:], [ti:]...) are skipped.
 * Returns an empty list when there is nothing timed to show.
 */
fun parseLrc(lrc: String?): List<LyricLine> {
    if (lrc.isNullOrBlank()) return emptyList()
    val out = mutableListOf<LyricLine>()
    val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")
    for (raw in lrc.lines()) {
        val line = raw.trim()
        if (line.isEmpty()) continue
        val match = regex.find(line) ?: continue
        val min = match.groupValues[1].toLongOrNull() ?: continue
        val sec = match.groupValues[2].toLongOrNull() ?: continue
        val fracRaw = match.groupValues[3]
        val fracMs = when (fracRaw.length) {
            1 -> fracRaw.toLongOrNull()?.times(100) ?: 0L
            2 -> fracRaw.toLongOrNull()?.times(10) ?: 0L
            3 -> fracRaw.toLongOrNull() ?: 0L
            else -> 0L
        }
        val text = line.substring(match.range.last + 1).trim()
        if (text.isEmpty()) continue
        out.add(LyricLine(min * 60_000 + sec * 1_000 + fracMs, text))
    }
    return out.sortedBy { it.timeMs }
}

/** Index of the line playing at [positionMs], or -1 when none has started. */
fun currentLyricIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var idx = -1
    for (i in lines.indices) {
        if (lines[i].timeMs <= positionMs) idx = i else break
    }
    return idx
}
