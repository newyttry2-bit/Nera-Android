package com.virex.music.player

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
 * MediaPlayer-based player: queue, play/pause, next/previous, seek,
 * shuffle, repeat (off/all/one), sleep timer, HQ quality switching,
 * synced lyrics (LRCLIB) and suggestions auto-queued into the tail.
 *
 * Playback is transient streaming, or local-file playback. This class
 * never writes audio to device storage.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val settings = SettingsStore(application)
    private val db = AppDatabase.get(application)

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var sleepJob: Job? = null
    private var fadeJob: Job? = null
    private var suggestionsForId: String? = null

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
        _index.value = newIndex
        val song = songs[newIndex]
        _current.value = song
        val hq = settings.hq.value
        val url = song.urlFor(hq) ?: song.streamUrl
        if (url.isNullOrBlank()) {
            _error.value = "No playable audio for this song."
            return
        }
        try {
            fadeJob?.cancel()
            progressJob?.cancel()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (song.isLocal) {
                    setDataSource(getApplication(), Uri.parse(url))
                } else {
                    setDataSource(url)
                }
                setOnPreparedListener { player ->
                    _durationMs.value = player.duration
                    if (resumePositionMs > 0 && resumePositionMs < player.duration) {
                        player.seekTo(resumePositionMs)
                        _positionMs.value = resumePositionMs
                    } else {
                        _positionMs.value = 0
                    }
                    applyCrossfadeIn(player)
                    player.start()
                    _isPlaying.value = true
                    startProgress()
                }
                setOnCompletionListener { onTrackEnded() }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    _error.value = "Couldn't play this song."
                    true
                }
                prepareAsync()
            }
            _positionMs.value = resumePositionMs
            recordRecentPlay(song)
            loadLyrics(song)
            loadSuggestions(song)
        } catch (_: Exception) {
            _isPlaying.value = false
            _error.value = "Couldn't play this song."
        }
    }

    /** HQ toggle really switches the stream: replays the current song
     *  at the same position with the other quality's URL. */
    fun toggleHq() {
        val newHq = !settings.hq.value
        settings.setHq(newHq)
        val song = _current.value
        if (song != null && !song.isLocal && _index.value >= 0) {
            val pos = _positionMs.value
            val wasPlaying = _isPlaying.value
            playAt(_index.value, resumePositionMs = pos)
            if (!wasPlaying) {
                viewModelScope.launch {
                    delay(600)
                    mediaPlayer?.pause()
                    _isPlaying.value = false
                }
            }
        }
    }

    fun togglePlay() {
        val player = mediaPlayer
        if (player == null) {
            if (_queue.value.isNotEmpty() && _index.value < 0) playAt(0)
            return
        }
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.start()
            _isPlaying.value = true
            startProgress()
        }
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
        try {
            mediaPlayer?.seekTo(ms.coerceIn(0, _durationMs.value))
            _positionMs.value = ms.coerceIn(0, _durationMs.value)
        } catch (_: Exception) {
        }
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
            mediaPlayer?.pause()
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
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    _positionMs.value = player.currentPosition
                    if (player.duration > 0) _durationMs.value = player.duration
                }
                delay(250)
            }
        }
    }

    /** Simple volume fade-in when a crossfade duration is set. */
    private fun applyCrossfadeIn(player: MediaPlayer) {
        val secs = settings.crossfadeSec.value
        if (secs <= 0) {
            player.setVolume(1f, 1f)
            return
        }
        fadeJob = viewModelScope.launch {
            val steps = 20
            val stepMs = (secs * 1000L / steps).coerceAtLeast(25L)
            for (i in 1..steps) {
                val v = i / steps.toFloat()
                try { player.setVolume(v, v) } catch (_: Exception) { break }
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
                val res = ApiClient.lyrics.getLyrics(
                    trackName = song.title,
                    artistName = song.artist,
                    duration = song.durationSec.takeIf { it > 0 }
                )
                applyLyrics(res.syncedLyrics, res.plainLyrics)
            } catch (_: Exception) {
                try {
                    val list = ApiClient.lyrics.searchLyrics(song.title, song.artist)
                    val first = list.firstOrNull()
                    applyLyrics(first?.syncedLyrics, first?.plainLyrics)
                } catch (_: Exception) {
                    _plainLyrics.value = null
                }
            } finally {
                _lyricsLoading.value = false
            }
        }
    }

    private fun applyLyrics(synced: String?, plain: String?) {
        val lines = parseLrc(synced)
        _lyricLines.value = lines
        _plainLyrics.value = if (lines.isEmpty()) plain?.ifBlank { null } else null
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
        mediaPlayer?.release()
        super.onCleared()
    }
}
