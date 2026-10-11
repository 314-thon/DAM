package com.ifts18.unadecisionmas.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ifts18.unadecisionmas.R

object SoundManager {

    private var appContext: Context? = null
    private var bgMusicPlayer: MediaPlayer? = null
    private var heartbeatPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    private var clickSoundId: Int = 0
    private var winSoundId: Int = 0
    private var loseSoundId: Int = 0

    // Estado reactivo de mute observable por Jetpack Compose
    private val _isMutedState = mutableStateOf(false)
    val isMuted: State<Boolean> = _isMutedState

    private var currentSpeed: Float = 1.0f
    private var isMusicPausedByLifecycle = false

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        val ctx = appContext ?: return

        // Configuración de SoundPool
        try {
            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()

            clickSoundId = soundPool?.load(ctx, R.raw.button_click, 1) ?: 0
            winSoundId = soundPool?.load(ctx, R.raw.win_sound, 1) ?: 0
            loseSoundId = soundPool?.load(ctx, R.raw.lose_sound, 1) ?: 0
        } catch (_: Exception) {}

        // Inicializar MediaPlayer de música de fondo global
        initBgMusic(ctx)
        initHeartbeat(ctx)
    }

    private fun initBgMusic(context: Context) {
        try {
            bgMusicPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(R.raw.bg_music_suspense)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                isLooping = true
                val vol = if (_isMutedState.value) 0f else 0.40f
                setVolume(vol, vol)
            }
        } catch (_: Exception) {
            try {
                bgMusicPlayer = MediaPlayer.create(context, R.raw.bg_music_suspense)?.apply {
                    isLooping = true
                    val vol = if (_isMutedState.value) 0f else 0.40f
                    setVolume(vol, vol)
                }
            } catch (_: Exception) {
                bgMusicPlayer = null
            }
        }
    }

    private fun initHeartbeat(context: Context) {
        try {
            heartbeatPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(R.raw.heartbeat)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                isLooping = true
                val vol = if (_isMutedState.value) 0f else 0.30f
                setVolume(vol, vol)
            }
        } catch (_: Exception) {
            try {
                heartbeatPlayer = MediaPlayer.create(context, R.raw.heartbeat)?.apply {
                    isLooping = true
                    val vol = if (_isMutedState.value) 0f else 0.30f
                    setVolume(vol, vol)
                }
            } catch (_: Exception) {
                heartbeatPlayer = null
            }
        }
    }

    fun toggleMute(): Boolean {
        setMuted(!_isMutedState.value)
        return _isMutedState.value
    }

    fun setMuted(muted: Boolean) {
        _isMutedState.value = muted
        if (muted) {
            try {
                bgMusicPlayer?.setVolume(0f, 0f)
                heartbeatPlayer?.setVolume(0f, 0f)
            } catch (_: Exception) {}
        } else {
            try {
                bgMusicPlayer?.setVolume(0.40f, 0.40f)
                heartbeatPlayer?.setVolume(0.30f, 0.30f)
                if (bgMusicPlayer?.isPlaying == false && !isMusicPausedByLifecycle) {
                    bgMusicPlayer?.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun startBgMusic() {
        try {
            if (bgMusicPlayer == null && appContext != null) {
                initBgMusic(appContext!!)
            }
            val vol = if (_isMutedState.value) 0f else 0.40f
            bgMusicPlayer?.setVolume(vol, vol)
            if (bgMusicPlayer?.isPlaying == false) {
                resetMusicSpeed()
                bgMusicPlayer?.start()
            }
        } catch (_: Exception) {}
    }

    fun pauseBgMusic() {
        try {
            if (bgMusicPlayer?.isPlaying == true) {
                bgMusicPlayer?.pause()
                isMusicPausedByLifecycle = true
            }
            stopHeartbeat()
        } catch (_: Exception) {}
    }

    fun resumeBgMusic() {
        try {
            if (isMusicPausedByLifecycle && bgMusicPlayer != null && !bgMusicPlayer!!.isPlaying) {
                val vol = if (_isMutedState.value) 0f else 0.40f
                bgMusicPlayer?.setVolume(vol, vol)
                bgMusicPlayer?.start()
                isMusicPausedByLifecycle = false
            }
        } catch (_: Exception) {}
    }

    fun stopBgMusic() {
        try {
            if (bgMusicPlayer?.isPlaying == true) {
                bgMusicPlayer?.pause()
                bgMusicPlayer?.seekTo(0)
            }
            stopHeartbeat()
        } catch (_: Exception) {}
    }

    fun setMusicSpeed(speed: Float) {
        currentSpeed = speed
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bgMusicPlayer?.let { player ->
                    if (player.isPlaying) {
                        val params = player.playbackParams
                        params.speed = speed
                        params.pitch = 1.05f
                        player.playbackParams = params
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun resetMusicSpeed() {
        setMusicSpeed(1.0f)
    }

    fun startHeartbeat() {
        try {
            val vol = if (_isMutedState.value) 0f else 0.30f
            heartbeatPlayer?.setVolume(vol, vol)
            if (heartbeatPlayer?.isPlaying == false) {
                heartbeatPlayer?.start()
            }
        } catch (_: Exception) {}
    }

    fun stopHeartbeat() {
        try {
            if (heartbeatPlayer?.isPlaying == true) {
                heartbeatPlayer?.pause()
                heartbeatPlayer?.seekTo(0)
            }
        } catch (_: Exception) {}
    }

    fun playClick() {
        if (_isMutedState.value) return
        try {
            if (clickSoundId != 0) {
                soundPool?.play(clickSoundId, 0.5f, 0.5f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun playWin() {
        if (_isMutedState.value) return
        try {
            if (winSoundId != 0) {
                soundPool?.play(winSoundId, 0.7f, 0.7f, 2, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun playLose() {
        if (_isMutedState.value) return
        try {
            if (loseSoundId != 0) {
                soundPool?.play(loseSoundId, 0.7f, 0.7f, 2, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            bgMusicPlayer?.stop()
            bgMusicPlayer?.release()
            bgMusicPlayer = null

            heartbeatPlayer?.stop()
            heartbeatPlayer?.release()
            heartbeatPlayer = null

            soundPool?.release()
            soundPool = null
            appContext = null
        } catch (_: Exception) {}
    }
}
