package com.virex.music.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small SharedPreferences-backed settings store.
 * Exposed as StateFlows so the UI updates the moment a setting changes.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("virex_music_prefs", Context.MODE_PRIVATE)

    private val _hq = MutableStateFlow(prefs.getBoolean(KEY_HQ, true))
    val hq: StateFlow<Boolean> = _hq.asStateFlow()

    private val _crossfadeSec = MutableStateFlow(prefs.getInt(KEY_CROSSFADE, 0))
    val crossfadeSec: StateFlow<Int> = _crossfadeSec.asStateFlow()

    /** 0.5f, 1f or 1.5f — multiplies UI animation durations. */
    private val _animSpeed = MutableStateFlow(prefs.getFloat(KEY_ANIM_SPEED, 1f))
    val animSpeed: StateFlow<Float> = _animSpeed.asStateFlow()

    private val _pureBlack = MutableStateFlow(prefs.getBoolean(KEY_PURE_BLACK, true))
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _accentFromArtwork = MutableStateFlow(prefs.getBoolean(KEY_ACCENT_ARTWORK, true))
    val accentFromArtwork: StateFlow<Boolean> = _accentFromArtwork.asStateFlow()

    private val _sleepDefaultMin = MutableStateFlow(prefs.getInt(KEY_SLEEP_DEFAULT, 0))
    val sleepDefaultMin: StateFlow<Int> = _sleepDefaultMin.asStateFlow()

    fun setHq(value: Boolean) {
        prefs.edit().putBoolean(KEY_HQ, value).apply()
        _hq.value = value
    }

    fun setCrossfadeSec(value: Int) {
        prefs.edit().putInt(KEY_CROSSFADE, value).apply()
        _crossfadeSec.value = value
    }

    fun setAnimSpeed(value: Float) {
        prefs.edit().putFloat(KEY_ANIM_SPEED, value).apply()
        _animSpeed.value = value
    }

    fun setPureBlack(value: Boolean) {
        prefs.edit().putBoolean(KEY_PURE_BLACK, value).apply()
        _pureBlack.value = value
    }

    fun setAccentFromArtwork(value: Boolean) {
        prefs.edit().putBoolean(KEY_ACCENT_ARTWORK, value).apply()
        _accentFromArtwork.value = value
    }

    fun setSleepDefaultMin(value: Int) {
        prefs.edit().putInt(KEY_SLEEP_DEFAULT, value).apply()
        _sleepDefaultMin.value = value
    }

    companion object {
        private const val KEY_HQ = "hq"
        private const val KEY_CROSSFADE = "crossfade_sec"
        private const val KEY_ANIM_SPEED = "anim_speed"
        private const val KEY_PURE_BLACK = "pure_black"
        private const val KEY_ACCENT_ARTWORK = "accent_artwork"
        private const val KEY_SLEEP_DEFAULT = "sleep_default_min"
    }
}
