package com.virex.music.data

import com.google.gson.annotations.SerializedName

/**
 * Mirrors the shape used by Virex Music's web app (index.html):
 * results live at data.data.results[], each song has id, name,
 * artists.primary[0].name, image[] (last entry is largest),
 * duration (seconds) and downloadUrl[] (quality + url).
 *
 * In this Android scaffold, downloadUrl is used ONLY as a transient
 * streaming/preview URL for the in-app player. Nothing is saved to
 * device storage from it. See README.md.
 */
data class ApiImage(
    val quality: String? = null,
    val url: String? = null
)

data class ApiArtist(
    val name: String? = null
)

data class ApiArtists(
    val primary: List<ApiArtist>? = null
)

data class ApiDownloadUrl(
    val quality: String? = null,
    val url: String? = null
)

data class ApiSong(
    val id: String? = null,
    val name: String? = null,
    val artists: ApiArtists? = null,
    val image: List<ApiImage>? = null,
    val duration: Int? = null,
    val downloadUrl: List<ApiDownloadUrl>? = null
) {
    val artistName: String
        get() = artists?.primary?.firstOrNull()?.name ?: "Unknown"

    val artworkUrl: String?
        get() = image?.lastOrNull()?.url

    /** Prefer 160kbps for preview streaming; fall back to the last entry. */
    val streamUrl: String?
        get() = downloadUrl?.firstOrNull { it.quality == "160kbps" }?.url
            ?: downloadUrl?.lastOrNull()?.url

    fun toSong(): Song = Song(
        id = id ?: "",
        title = name ?: "Unknown",
        artist = artistName,
        artworkUrl = artworkUrl,
        durationSec = duration ?: 0,
        streamUrl = streamUrl
    )
}

data class SearchData(
    val results: List<ApiSong>? = null
)

data class SearchResponse(
    val data: SearchData? = null
)

/** LRCLIB lyrics: https://lrclib.net/api/get and /api/search (same as site). */
data class LyricsResponse(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val name: String? = null,
    @SerializedName("artistName") val artist: String? = null
)

/** App-level song, used by player, queue and playlists. */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val durationSec: Int = 0,
    /** Transient preview stream URL, or a local content:// / file path. */
    val streamUrl: String? = null,
    val isLocal: Boolean = false
)

/** A locally stored / imported audio file found on device storage. */
data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: String,
    val durationMs: Long = 0
) {
    fun toSong(): Song = Song(
        id = "local:$id",
        title = title,
        artist = artist,
        durationSec = (durationMs / 1000).toInt(),
        streamUrl = uri,
        isLocal = true
    )
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
