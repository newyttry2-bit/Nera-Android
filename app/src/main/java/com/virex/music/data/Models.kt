package com.virex.music.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * API shapes for https://jiosavan-api2.vercel.app/ (note the spelling).
 * Parsed defensively: artists may arrive as an object with a primary list,
 * a plain string, or a list, and any field may be missing.
 *
 * downloadUrl entries are used ONLY as transient streaming URLs for the
 * in-app player. Nothing is ever saved to device storage from them.
 */
data class ApiImage(
    val quality: String? = null,
    val url: String? = null
)

data class ApiDownloadUrl(
    val quality: String? = null,
    val url: String? = null
)

data class ApiSong(
    val id: String? = null,
    val name: String? = null,
    val artists: JsonElement? = null,
    val image: List<ApiImage>? = null,
    val duration: Int? = null,
    val downloadUrl: List<ApiDownloadUrl>? = null
) {
    val artistName: String
        get() {
            val el = artists ?: return "Unknown Artist"
            try {
                if (el.isJsonPrimitive) {
                    return el.asString.ifBlank { "Unknown Artist" }
                }
                if (el.isJsonArray) {
                    val arr = el.asJsonArray
                    if (arr.size() == 0) return "Unknown Artist"
                    val first = arr[0]
                    if (first.isJsonPrimitive) return first.asString
                    if (first.isJsonObject && first.asJsonObject.has("name")) {
                        return first.asJsonObject.get("name").asString
                    }
                    return "Unknown Artist"
                }
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("primary")) {
                        val primary = obj.get("primary")
                        if (primary.isJsonArray && primary.asJsonArray.size() > 0) {
                            val first = primary.asJsonArray[0]
                            if (first.isJsonObject && first.asJsonObject.has("name")) {
                                return first.asJsonObject.get("name").asString
                            }
                            if (first.isJsonPrimitive) return first.asString
                        }
                    }
                    if (obj.has("name")) return obj.get("name").asString
                }
            } catch (_: Exception) {
                return "Unknown Artist"
            }
            return "Unknown Artist"
        }

    val artworkUrl: String?
        get() = image?.lastOrNull { !it.url.isNullOrBlank() }?.url

    /** Highest-quality URL available (320kbps when present). */
    val urlHq: String?
        get() = downloadUrl?.firstOrNull { it.quality == "320kbps" }?.url
            ?: downloadUrl?.lastOrNull()?.url

    /** Standard-quality URL (160kbps when present). */
    val urlStd: String?
        get() = downloadUrl?.firstOrNull { it.quality == "160kbps" }?.url
            ?: downloadUrl?.firstOrNull()?.url

    fun urlFor(hq: Boolean): String? = if (hq) urlHq else urlStd

    fun toSong(): Song = Song(
        id = id ?: "",
        title = (name ?: "Unknown").ifBlank { "Unknown" },
        artist = artistName,
        artworkUrl = artworkUrl,
        durationSec = duration ?: 0,
        streamUrl = urlStd,
        streamUrlHq = urlHq,
        streamUrlStd = urlStd
    )
}

data class SearchData(
    val results: List<ApiSong>? = null
)

data class SearchResponse(
    val data: SearchData? = null
) {
    val songs: List<Song>
        get() = data?.results?.mapNotNull { api ->
            if (api.id.isNullOrBlank() && api.name.isNullOrBlank()) null else api.toSong()
        } ?: emptyList()
}

/** LRCLIB lyrics: https://lrclib.net/api/get and /api/search. */
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
    /** Currently selected transient stream URL, or a local content URI. */
    val streamUrl: String? = null,
    val streamUrlHq: String? = null,
    val streamUrlStd: String? = null,
    val isLocal: Boolean = false
) {
    fun urlFor(hq: Boolean): String? {
        if (isLocal) return streamUrl
        return if (hq) (streamUrlHq ?: streamUrl) else (streamUrlStd ?: streamUrl)
    }

    fun qualityLabel(hq: Boolean): String {
        if (isLocal) return "FILE"
        return if (hq) "320" else "160"
    }
}

/** A locally stored audio file found on device storage. */
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
    if (seconds <= 0) return "0:00"
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
