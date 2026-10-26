package com.virex.music.player

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virex.music.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoopMode { OFF, ALL, ONE }

/**
 * Simple MediaPlayer-based player mirroring the web app's queue behaviour:
 * play/pause, next/previous, shuffle, loop, seek. Crossfade and the
 * canvas visualizer from the web app are NOT implemented natively here
 * (see README — they are stubbed as toggles in Settings).
 *
 * Playback is transient streaming/preview, or local-file playback.
 * This ViewModel never writes audio to device storage.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionSec = MutableStateFlow(0)
    val positionSec: StateFlow<Int> = _positionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _loop = MutableStateFlow(LoopMode.OFF)
    val loop: StateFlow<LoopMode> = _loop.asStateFlow()

    /** HQ toggle from the site: kept as a user preference placeholder. */
    private val _hq = MutableStateFlow(true)
    val hq: StateFlow<Boolean> = _hq.asStateFlow()

    val current: Song?
        get() = _queue.value.getOrNull(_index.value)

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.value = songs
        playAt(startIndex)
    }

    fun playAt(newIndex: Int) {
        val songs = _queue.value
        if (songs.isEmpty() || newIndex !in songs.indices) return
        _index.value = newIndex
        val song = songs[newIndex]
        val url = song.streamUrl ?: return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (song.isLocal) {
                    setDataSource(getApplication(), Uri.parse(url))
                } else {
                    setDataSource(url)
                }
                setOnPreparedListener { player ->
                    _durationSec.value = (player.duration / 1000)
                    player.start()
                    _isPlaying.value = true
                    startProgress()
                }
                setOnCompletionListener { onTrackEnded() }
                prepareAsync()
            }
            _positionSec.value = 0
        } catch (e: Exception) {
            _isPlaying.value = false
        }
    }

    fun togglePlay() {
        val player = mediaPlayer ?: return
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
        val nextIndex = if (_shuffle.value) {
            (songs.indices - _index.value).random()
        } else {
            _index.value + 1
        }
        if (nextIndex in songs.indices) playAt(nextIndex)
        else if (_loop.value == LoopMode.ALL) playAt(0)
        else _isPlaying.value = false
    }

    fun previous() {
        if (_positionSec.value > 3) {
            seekTo(0)
            return
        }
        val prev = _index.value - 1
        if (prev >= 0) playAt(prev)
    }

    fun seekTo(seconds: Int) {
        mediaPlayer?.seekTo(seconds * 1000)
        _positionSec.value = seconds
    }

    fun toggleShuffle() { _shuffle.value = !_shuffle.value }

    fun toggleLoop() {
        _loop.value = when (_loop.value) {
            LoopMode.OFF -> LoopMode.ALL
            LoopMode.ALL -> LoopMode.ONE
            LoopMode.ONE -> LoopMode.OFF
        }
    }

    fun toggleHq() { _hq.value = !_hq.value }

    private fun onTrackEnded() {
        if (_loop.value == LoopMode.ONE) playAt(_index.value) else next()
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    _positionSec.value = player.currentPosition / 1000
                }
                delay(500)
            }
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        mediaPlayer?.release()
        super.onCleared()
    }
}
