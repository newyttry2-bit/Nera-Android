package com.virex.music.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.virex.music.data.ApiClient
import com.virex.music.data.AppDatabase
import com.virex.music.data.LyricLine
import com.virex.music.data.SettingsStore
import com.virex.music.data.Song
import com.virex.music.data.parseLrc
import com.virex.music.data.toRecentPlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoopMode { OFF, ALL, ONE }

/**
 * ExoPlayer-based player: queue, play/pause, next/previous, seek,
 * +/-10s skip, shuffle, repeat (off/all/one), sleep timer, HQ quality
 * switching, synced lyrics (LRCLIB) and suggestions auto-queued into
 * the tail.
 *
 * ExoPlayer (not MediaPlayer) because it retries and resumes stalled
 * network streams instead of ending the song early on a flaky
 * connection, and quality switches don't need a re-prepare dance.
 *
 * Playback is transient streaming, or local-file playback. This class
 * never writes audio to device storage.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val settings = SettingsStore(application)
    private val db = AppDatabase.get(application)

    private val exoPlayer: ExoPlayer = run {
        // Browser-like UA: the CDN treats plain library clients worse.
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
        // DefaultDataSource routes content:// and file:// locally and
        // http(s):// through the factory above.
        val dataSourceFactory = DefaultDataSource.Factory(application, httpFactory)
        ExoPlayer.Builder(application)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    private var progressJob: Job? = null
    private var sleepJob: Job? = null
    private var fadeJob: Job? = null
    private var suggestionsForId: String? = null
    private var triedFallbackQuality = false
    private var fadePending = false

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _current = MutableStateFlow<Song?>(null)
    val current: StateFlow<Song?> = _current.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0)
    val positionMs: StateFlow<Int> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _loop = MutableStateFlow(LoopMode.OFF)
    val loop: StateFlow<LoopMode> = _loop.asStateFlow()

    /** Remaining sleep-timer minutes, 0 = off. */
    private val _sleepMinLeft = MutableStateFlow(0)
    val sleepMinLeft: StateFlow<Int> = _sleepMinLeft.asStateFlow()

    private val _lyricLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyricLines: StateFlow<List<LyricLine>> = _lyricLines.asStateFlow()

    private val _plainLyrics = MutableStateFlow<String?>(null)
    val plainLyrics: StateFlow<String?> = _plainLyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    val d = exoPlayer.duration
                    _durationMs.value = if (d > 0) d.toInt() else 0
                    if (fadePending) {
                        fadePending = false
                        applyCrossfadeIn()
                    }
                    _isPlaying.value = exoPlayer.isPlaying
                }
                Player.STATE_ENDED -> onTrackEnded()
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlayerError()
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    fun clearError() { _error.value = null }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        suggestionsForId = null
        _queue.value = songs
        playAt(startIndex.coerceIn(songs.indices))
    }

    fun playAt(newIndex: Int, resumePositionMs: Int = 0) {
        val songs = _queue.value
        if (songs.isEmpty() || newIndex !in songs.indices) return
        val song = songs[newIndex]
        // Validate the stream URL BEFORE touching playback state. A song
        // with no URL must never hijack the UI while the old song keeps
        // playing underneath it.
        val hq = settings.hq.value
        val url = song.urlFor(hq) ?: song.streamUrl
        if (url.isNullOrBlank()) {
            _error.value = "No playable audio for this song."
            return
        }
        triedFallbackQuality = false
        _index.value = newIndex
        _current.value = song
        playInternal(url, resumePositionMs)
        recordRecentPlay(song)
        loadLyrics(song)
        loadSuggestions(song)
    }

    private fun playInternal(url: String, resumePositionMs: Int) {
        fadeJob?.cancel()
        progressJob?.cancel()
        fadePending = true
        _positionMs.value = resumePositionMs
        _durationMs.value = 0
        _error.value = null
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        if (resumePositionMs > 0) exoPlayer.seekTo(resumePositionMs.toLong())
        exoPlayer.prepare()
        exoPlayer.play()
        startProgress()
    }

    /** On a playback error, retry the same song once at the other
     *  quality before giving up — one CDN URL failing does not mean
     *  the song is unplayable. */
    private fun handlePlayerError() {
        val song = _current.value
        if (song != null && !song.isLocal && !triedFallbackQuality) {
            triedFallbackQuality = true
            val fallbackUrl = song.urlFor(!settings.hq.value)
            if (!fallbackUrl.isNullOrBlank()) {
                playInternal(fallbackUrl, _positionMs.value)
                return
            }
        }
        _isPlaying.value = false
        _error.value = "Couldn't play this song."
    }

    /** HQ toggle really switches the stream: replays the current song
     *  at the same position with the other quality's URL. */
    fun toggleHq() {
        val newHq = !settings.hq.value
        settings.setHq(newHq)
        val song = _current.value
        if (song != null && !song.isLocal && _index.value >= 0) {
            val url = song.urlFor(newHq) ?: song.streamUrl
            if (url.isNullOrBlank()) return
            val pos = _positionMs.value
            val wasPlaying = _isPlaying.value
            triedFallbackQuality = false
            fadeJob?.cancel()
            progressJob?.cancel()
            fadePending = true
            _positionMs.value = pos
            _durationMs.value = 0
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            if (pos > 0) exoPlayer.seekTo(pos.toLong())
            exoPlayer.prepare()
            if (wasPlaying) exoPlayer.play() else exoPlayer.pause()
            startProgress()
        }
    }

    fun togglePlay() {
        if (_queue.value.isEmpty()) return
        if (_index.value < 0) {
            playAt(0)
            return
        }
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun next() {
        val songs = _queue.value
        if (songs.isEmpty()) return
        if (_loop.value == LoopMode.ONE) {
            playAt(_index.value)
            return
        }
        val nextIndex = if (_shuffle.value && songs.size > 1) {
            (songs.indices - _index.value).random()
        } else {
            _index.value + 1
        }
        if (nextIndex in songs.indices) playAt(nextIndex)
        else if (_loop.value == LoopMode.ALL) playAt(0)
        else _isPlaying.value = false
    }

    fun previous() {
        if (_positionMs.value > 3000) {
            seekTo(0)
            return
        }
        val prev = _index.value - 1
        if (prev >= 0) playAt(prev)
    }

    fun seekTo(ms: Int) {
        val d = exoPlayer.duration
        val clamped = if (d > 0) ms.coerceIn(0, d.toInt()) else ms.coerceAtLeast(0)
        exoPlayer.seekTo(clamped.toLong())
        _positionMs.value = clamped
    }

    /** Skip forward/back by a few seconds, e.g. +/-10s buttons. */
    fun seekBy(deltaMs: Int) {
        seekTo(_positionMs.value + deltaMs)
    }

    fun toggleShuffle() { _shuffle.value = !_shuffle.value }

    fun toggleLoop() {
        _loop.value = when (_loop.value) {
            LoopMode.OFF -> LoopMode.ALL
            LoopMode.ALL -> LoopMode.ONE
            LoopMode.ONE -> LoopMode.OFF
        }
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun removeFromQueue(pos: Int) {
        val list = _queue.value.toMutableList()
        if (pos !in list.indices) return
        if (pos == _index.value) return
        list.removeAt(pos)
        _queue.value = list
        if (pos < _index.value) _index.value = _index.value - 1
    }

    /** Sleep timer in minutes; 0 cancels. Pauses playback when it fires. */
    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        _sleepMinLeft.value = minutes
        if (minutes <= 0) return
        sleepJob = viewModelScope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000)
                left -= 1
                _sleepMinLeft.value = left
            }
            exoPlayer.pause()
            _isPlaying.value = false
        }
    }

    private fun onTrackEnded() {
        if (_loop.value == LoopMode.ONE) playAt(_index.value) else next()
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                if (exoPlayer.isPlaying) {
                    _positionMs.value = exoPlayer.currentPosition.toInt()
                    val d = exoPlayer.duration
                    if (d > 0) _durationMs.value = d.toInt()
                }
                delay(250)
            }
        }
    }

    /** Simple volume fade-in when a crossfade duration is set. */
    private fun applyCrossfadeIn() {
        val secs = settings.crossfadeSec.value
        if (secs <= 0) {
            exoPlayer.volume = 1f
            return
        }
        fadeJob = viewModelScope.launch {
            exoPlayer.volume = 0f
            val steps = 20
            val stepMs = (secs * 1000L / steps).coerceAtLeast(25L)
            for (i in 1..steps) {
                exoPlayer.volume = i / steps.toFloat()
                delay(stepMs)
            }
        }
    }

    private fun recordRecentPlay(song: Song) {
        if (song.isLocal) return
        viewModelScope.launch {
            try {
                db.recentDao().saveRecentPlay(song.toRecentPlay())
                db.recentDao().trimRecentPlays()
            } catch (_: Exception) {
            }
        }
    }

    private fun loadLyrics(song: Song) {
        if (song.isLocal) {
            _lyricLines.value = emptyList()
            _plainLyrics.value = null
            return
        }
        _lyricsLoading.value = true
        _lyricLines.value = emptyList()
        _plainLyrics.value = null
        viewModelScope.launch {
            try {
                // 1) Exact match, with duration.
                val res = ApiClient.lyrics.getLyrics(
                    trackName = song.title,
                    artistName = song.artist,
                    duration = song.durationSec.takeIf { it > 0 }
                )
                if (applyLyrics(res.syncedLyrics, res.plainLyrics)) return@launch
                // 2) Search by track + artist.
                val list = ApiClient.lyrics.searchLyrics(song.title, song.artist)
                val first = list.firstOrNull()
                if (first != null && applyLyrics(first.syncedLyrics, first.plainLyrics)) return@launch
                // 3) Search by track only.
                val list2 = ApiClient.lyrics.searchLyrics(song.title, "")
                val first2 = list2.firstOrNull()
                if (first2 != null) applyLyrics(first2.syncedLyrics, first2.plainLyrics)
            } catch (_: Exception) {
                // The exact lookup threw (usually network): one more
                // attempt through search before giving up.
                try {
                    val list = ApiClient.lyrics.searchLyrics(song.title, song.artist)
                    val first = list.firstOrNull()
                    if (first != null) applyLyrics(first.syncedLyrics, first.plainLyrics)
                } catch (_: Exception) {
                    _plainLyrics.value = null
                }
            } finally {
                _lyricsLoading.value = false
            }
        }
    }

    /** Stores the lyrics and reports whether anything usable was found.
     *  Falls back to timestamps embedded in the plain text when no
     *  synced block exists (LRCLIB entries often carry them). */
    private fun applyLyrics(synced: String?, plain: String?): Boolean {
        val lines = parseLrc(synced).ifEmpty { parseLrc(plain) }
        _lyricLines.value = lines
        _plainLyrics.value = if (lines.isEmpty()) plain?.ifBlank { null } else null
        return lines.isNotEmpty() || !plain.isNullOrBlank()
    }

    /** Suggestions for the current song go into the queue tail (site behaviour). */
    private fun loadSuggestions(song: Song) {
        if (song.isLocal || song.id.isBlank()) return
        if (suggestionsForId == song.id) return
        suggestionsForId = song.id
        viewModelScope.launch {
            try {
                val res = ApiClient.search.suggestions(song.id, 20)
                val fresh = res.songs.filter { s ->
                    _queue.value.none { it.id == s.id }
                }
                if (fresh.isNotEmpty()) _queue.value = _queue.value + fresh
            } catch (_: Exception) {
            }
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        sleepJob?.cancel()
        fadeJob?.cancel()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
        super.onCleared()
    }
}
