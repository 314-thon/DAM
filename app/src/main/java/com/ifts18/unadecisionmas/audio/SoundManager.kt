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
    private var jinglePlayer: MediaPlayer? = null
    private var endCreditsMusicPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    private var clickSoundId: Int = 0

    // Estado reactivo de mute observable por Jetpack Compose
    private val _isMutedState = mutableStateOf(false)
    val isMuted: State<Boolean> = _isMutedState

    private var currentSpeed: Float = 1.0f
    private var isMusicPausedByLifecycle = false
    private var isGameOverActive = false

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
        } catch (_: Exception) {}

        // Inicializar MediaPlayer de música de fondo global
        initBgMusic(ctx)
        initHeartbeat(ctx)
    }

    private fun createMediaPlayer(context: Context, resId: Int, isMusic: Boolean): MediaPlayer? {
        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(if (isMusic) AudioAttributes.CONTENT_TYPE_MUSIC else AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
            }
            player
        } catch (_: Exception) {
            try {
                MediaPlayer.create(context, resId)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun initBgMusic(context: Context) {
        try {
            bgMusicPlayer = createMediaPlayer(context, R.raw.casino_vip_music, isMusic = true)?.apply {
                isLooping = true
                val vol = if (_isMutedState.value) 0f else 0.40f
                setVolume(vol, vol)
            }
        } catch (_: Exception) {
            bgMusicPlayer = null
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
                jinglePlayer?.setVolume(0f, 0f)
                endCreditsMusicPlayer?.setVolume(0f, 0f)
            } catch (_: Exception) {}
        } else {
            try {
                bgMusicPlayer?.setVolume(0.40f, 0.40f)
                heartbeatPlayer?.setVolume(0.30f, 0.30f)
                jinglePlayer?.setVolume(0.70f, 0.70f)
                endCreditsMusicPlayer?.setVolume(0.40f, 0.40f)
                if (!isGameOverActive && bgMusicPlayer?.isPlaying == false && !isMusicPausedByLifecycle) {
                    bgMusicPlayer?.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun startBgMusic() {
        try {
            if (isGameOverActive) {
                stopGameOverSequence()
            }
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
            if (endCreditsMusicPlayer?.isPlaying == true) {
                endCreditsMusicPlayer?.pause()
                isMusicPausedByLifecycle = true
            }
            if (jinglePlayer?.isPlaying == true) {
                jinglePlayer?.pause()
                isMusicPausedByLifecycle = true
            }
            stopHeartbeat()
        } catch (_: Exception) {}
    }

    fun resumeBgMusic() {
        try {
            if (isMusicPausedByLifecycle) {
                if (isGameOverActive) {
                    if (endCreditsMusicPlayer != null && !endCreditsMusicPlayer!!.isPlaying) {
                        val vol = if (_isMutedState.value) 0f else 0.40f
                        endCreditsMusicPlayer?.setVolume(vol, vol)
                        endCreditsMusicPlayer?.start()
                        isMusicPausedByLifecycle = false
                    } else if (jinglePlayer != null && !jinglePlayer!!.isPlaying) {
                        val vol = if (_isMutedState.value) 0f else 0.70f
                        jinglePlayer?.setVolume(vol, vol)
                        jinglePlayer?.start()
                        isMusicPausedByLifecycle = false
                    }
                } else {
                    if (bgMusicPlayer != null && !bgMusicPlayer!!.isPlaying) {
                        val vol = if (_isMutedState.value) 0f else 0.40f
                        bgMusicPlayer?.setVolume(vol, vol)
                        bgMusicPlayer?.start()
                        isMusicPausedByLifecycle = false
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun stopBgMusic() {
        try {
            if (bgMusicPlayer?.isPlaying == true) {
                bgMusicPlayer?.pause()
            }
            bgMusicPlayer?.seekTo(0)
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

    fun playGameOverSequence(isWon: Boolean) {
        val ctx = appContext ?: return
        isGameOverActive = true

        // 1. Cortar música global y latidos inmediatamente
        stopBgMusic()

        // 2. Detener cualquier jingle o créditos anteriores
        try {
            jinglePlayer?.stop()
            jinglePlayer?.release()
        } catch (_: Exception) {}
        jinglePlayer = null

        try {
            endCreditsMusicPlayer?.stop()
            endCreditsMusicPlayer?.release()
        } catch (_: Exception) {}
        endCreditsMusicPlayer = null

        // 3. Jingle de victoria o fail comedia
        val jingleResId = if (isWon) R.raw.victory_trumpet else R.raw.fail_sound_comedy

        try {
            val player = createMediaPlayer(ctx, jingleResId, isMusic = false)
            if (player != null) {
                val vol = if (_isMutedState.value) 0f else 0.70f
                player.setVolume(vol, vol)
                player.setOnCompletionListener { mp ->
                    try {
                        mp.release()
                    } catch (_: Exception) {}
                    jinglePlayer = null
                    if (isGameOverActive) {
                        startEndCreditsMusic()
                    }
                }
                player.setOnErrorListener { mp, _, _ ->
                    try {
                        mp.release()
                    } catch (_: Exception) {}
                    jinglePlayer = null
                    if (isGameOverActive) {
                        startEndCreditsMusic()
                    }
                    true
                }
                jinglePlayer = player
                player.start()
            } else {
                if (isGameOverActive) {
                    startEndCreditsMusic()
                }
            }
        } catch (_: Exception) {
            if (isGameOverActive) {
                startEndCreditsMusic()
            }
        }
    }

    fun startEndCreditsMusic() {
        val ctx = appContext ?: return
        if (!isGameOverActive) return

        try {
            val player = createMediaPlayer(ctx, R.raw.end_credits_music, isMusic = true)
            if (player != null) {
                player.isLooping = true
                val vol = if (_isMutedState.value) 0f else 0.40f
                player.setVolume(vol, vol)
                endCreditsMusicPlayer = player
                player.start()
            }
        } catch (_: Exception) {
            endCreditsMusicPlayer = null
        }
    }

    fun stopGameOverSequence() {
        isGameOverActive = false
        try {
            if (jinglePlayer?.isPlaying == true) {
                jinglePlayer?.stop()
            }
            jinglePlayer?.release()
            jinglePlayer = null
        } catch (_: Exception) {}

        try {
            if (endCreditsMusicPlayer?.isPlaying == true) {
                endCreditsMusicPlayer?.stop()
            }
            endCreditsMusicPlayer?.release()
            endCreditsMusicPlayer = null
        } catch (_: Exception) {}
    }

    fun playWin() {
        playGameOverSequence(isWon = true)
    }

    fun playLose() {
        playGameOverSequence(isWon = false)
    }

    fun release() {
        try {
            stopGameOverSequence()

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
