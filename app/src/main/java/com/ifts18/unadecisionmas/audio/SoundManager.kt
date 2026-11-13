package com.ifts18.unadecisionmas.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.ifts18.unadecisionmas.R
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundManager {

    private var appContext: Context? = null
    private var bgMusicPlayer: MediaPlayer? = null
    private var heartbeatPlayer: MediaPlayer? = null
    private var jinglePlayer: MediaPlayer? = null
    private var endCreditsMusicPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    private var clickSoundId: Int = 0
    private var slotSpinStartSoundId: Int = 0
    private var slotReelsMoveSoundId: Int = 0
    private var slotReelStopSoundId: Int = 0
    private var slotIntrigueSoundId: Int = 0
    private var slotWinSoundId: Int = 0
    private var slotNearMissSoundId: Int = 0

    @Volatile
    private var activeReelsMoveStreamId: Int = 0
    @Volatile
    private var activeIntrigueStreamId: Int = 0

    // Estado reactivo de mute observable por Jetpack Compose
    private val _isMutedState = mutableStateOf(false)
    val isMuted: State<Boolean> = _isMutedState

    private var currentSpeed: Float = 1.0f
    @Volatile
    private var isMusicPausedByLifecycle = false
    @Volatile
    private var isGameOverActive = false
    @Volatile
    private var isAppInForeground = false

    fun onAppForeground() {
        isAppInForeground = true
        resumeBgMusic()
    }

    fun onAppBackground() {
        isAppInForeground = false
        pauseBgMusic()
    }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        val ctx = appContext ?: return

        // Configuración de SoundPool
        try {
            soundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()

            clickSoundId = soundPool?.load(ctx, R.raw.button_click, 1) ?: 0
            initSlotSounds(ctx)
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
                stopSlotSpinLoop()
            } catch (_: Exception) {}
        } else {
            try {
                bgMusicPlayer?.setVolume(0.40f, 0.40f)
                heartbeatPlayer?.setVolume(0.30f, 0.30f)
                jinglePlayer?.setVolume(0.70f, 0.70f)
                endCreditsMusicPlayer?.setVolume(0.40f, 0.40f)
                if (!isGameOverActive && isAppInForeground && bgMusicPlayer?.isPlaying == false && !isMusicPausedByLifecycle) {
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
            if (!isAppInForeground) {
                isMusicPausedByLifecycle = true
                return
            }
            isMusicPausedByLifecycle = false
            if (bgMusicPlayer?.isPlaying == false) {
                resetMusicSpeed()
                bgMusicPlayer?.start()
            }
        } catch (_: Exception) {}
    }

    fun pauseBgMusic() {
        try {
            var hadAudioPlaying = false
            if (bgMusicPlayer?.isPlaying == true) {
                bgMusicPlayer?.pause()
                hadAudioPlaying = true
            }
            if (endCreditsMusicPlayer?.isPlaying == true) {
                endCreditsMusicPlayer?.pause()
                hadAudioPlaying = true
            }
            if (jinglePlayer?.isPlaying == true) {
                jinglePlayer?.pause()
                hadAudioPlaying = true
            }
            if (heartbeatPlayer?.isPlaying == true) {
                hadAudioPlaying = true
            }
            if (hadAudioPlaying) {
                isMusicPausedByLifecycle = true
            }
            stopHeartbeat()
            stopSlotSpinLoop()
        } catch (_: Exception) {}
    }

    fun resumeBgMusic() {
        if (!isAppInForeground) return
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
                    } else if (endCreditsMusicPlayer == null && jinglePlayer == null) {
                        isMusicPausedByLifecycle = false
                        startEndCreditsMusic()
                    }
                } else {
                    if (bgMusicPlayer == null && appContext != null) {
                        initBgMusic(appContext!!)
                    }
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
            stopSlotSpinLoop()
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
        if (!isAppInForeground) return
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
        if (_isMutedState.value || !isAppInForeground) return
        try {
            if (clickSoundId != 0) {
                soundPool?.play(clickSoundId, 0.5f, 0.5f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun playSlotSpinStart(turbo: Boolean) {
        if (_isMutedState.value || !isAppInForeground) return
        try {
            stopSlotSpinLoop()
            if (slotSpinStartSoundId != 0) {
                soundPool?.play(slotSpinStartSoundId, 0.65f, 0.65f, 2, 0, if (turbo) 1.15f else 1.0f)
            }
            if (slotReelsMoveSoundId != 0) {
                activeReelsMoveStreamId = soundPool?.play(
                    slotReelsMoveSoundId,
                    0.50f,
                    0.50f,
                    2,
                    -1,
                    if (turbo) 1.28f else 1.0f
                ) ?: 0
            }
        } catch (_: Exception) {}
    }

    fun playSlotIntrigueLoop() {
        if (_isMutedState.value || !isAppInForeground) return
        try {
            stopSlotIntrigueLoop()
            if (slotIntrigueSoundId != 0) {
                activeIntrigueStreamId = soundPool?.play(slotIntrigueSoundId, 0.55f, 0.55f, 2, -1, 1.05f) ?: 0
            }
        } catch (_: Exception) {}
    }

    fun stopSlotIntrigueLoop() {
        try {
            val sid = activeIntrigueStreamId
            activeIntrigueStreamId = 0
            if (sid != 0) {
                soundPool?.stop(sid)
            }
        } catch (_: Exception) {}
    }

    fun playSlotReelStop(columnIndex: Int) {
        if (_isMutedState.value || !isAppInForeground) return
        try {
            if (slotReelStopSoundId != 0) {
                val pitchRate = (0.95f + columnIndex.coerceIn(0, 2) * 0.06f)
                soundPool?.play(slotReelStopSoundId, 0.72f, 0.72f, 2, 0, pitchRate)
            }
        } catch (_: Exception) {}
    }

    fun stopSlotSpinLoop() {
        try {
            val moveSid = activeReelsMoveStreamId
            activeReelsMoveStreamId = 0
            if (moveSid != 0) {
                soundPool?.stop(moveSid)
            }
        } catch (_: Exception) {}
        stopSlotIntrigueLoop()
    }

    fun playSlotWinChime() {
        if (_isMutedState.value || !isAppInForeground) return
        try {
            if (slotWinSoundId != 0) {
                soundPool?.play(slotWinSoundId, 0.75f, 0.75f, 2, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun playSlotNearMiss() {
        if (_isMutedState.value || !isAppInForeground) return
        try {
            if (slotNearMissSoundId != 0) {
                soundPool?.play(slotNearMissSoundId, 0.65f, 0.65f, 2, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    private fun initSlotSounds(context: Context) {
        val sampleRate = 22050
        val cacheDir = File(context.cacheDir, "slot_sfx").apply { mkdirs() }

        fun buildAndLoad(fileName: String, durationMs: Int, synth: (Double, Double, Double) -> Double): Int {
            return try {
                val totalSamples = ((sampleRate * durationMs) / 1000.0).toInt().coerceAtLeast(1)
                val samples = ShortArray(totalSamples)
                val durationSec = durationMs / 1000.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / totalSamples
                    val value = synth(t, progress, durationSec).coerceIn(-1.0, 1.0)
                    samples[i] = (value * 0.65 * Short.MAX_VALUE).toInt().toShort()
                }
                val outFile = File(cacheDir, fileName)
                writePcmWavFile(outFile, sampleRate, samples)
                soundPool?.load(outFile.absolutePath, 1) ?: 0
            } catch (_: Exception) {
                0
            }
        }

        // spin.ogg equivalente: barrido ascendente corto al pulsar Girar
        var spinPhase = 0.0
        slotSpinStartSoundId = buildAndLoad("slot_spin_start.wav", 110) { _, progress, _ ->
            val freq = 240.0 + (620.0 - 240.0) * progress
            spinPhase += 2.0 * PI * freq / sampleRate
            val env = (progress * 10.0).coerceAtMost(1.0) * exp(-2.8 * progress)
            (sin(spinPhase) * 0.72 + sin(spinPhase * 2.0) * 0.28) * env
        }

        // reels_move.ogg equivalente (loopable): pulsos mecánicos rítmicos cada 60ms con bordes en cero
        slotReelsMoveSoundId = buildAndLoad("slot_reels_move.wav", 180) { t, _, _ ->
            val cycleSec = 0.060
            val localT = t % cycleSec
            val norm = localT / cycleSec
            val window = sin(PI * norm)
            val env = exp(-4.5 * norm) * window
            val clickTone = sin(2.0 * PI * 220.0 * localT) * 0.65 + sin(2.0 * PI * 680.0 * localT) * 0.35
            clickTone * env
        }

        // reel_stop.ogg equivalente: golpe amortiguado + trinquete metálico corto
        var stopPhase = 0.0
        slotReelStopSoundId = buildAndLoad("slot_reel_stop.wav", 95) { t, progress, _ ->
            val thudFreq = 190.0 - (190.0 - 68.0) * progress
            stopPhase += 2.0 * PI * thudFreq / sampleRate
            val thudEnv = exp(-5.0 * progress)
            val pingEnv = exp(-14.0 * progress)
            val fadeIn = (progress * 15.0).coerceAtMost(1.0)
            (sin(stopPhase) * thudEnv * 0.78 + sin(2.0 * PI * 1350.0 * t) * pingEnv * 0.28) * fadeIn
        }

        // intrigue.ogg equivalente (loopable): pulsación dual de tensión en último rodillo
        slotIntrigueSoundId = buildAndLoad("slot_intrigue.wav", 240) { t, _, _ ->
            val cycleSec = 0.120
            val localNorm = (t % cycleSec) / cycleSec
            val pulseEnv = sin(PI * localNorm)
            val tone = sin(2.0 * PI * 440.0 * t) * 0.52 + sin(2.0 * PI * 660.0 * t) * 0.48
            tone * pulseEnv * 0.75
        }

        // win_1.ogg equivalente: arpegio mayor brillante (Do-Mi-Sol-Do)
        slotWinSoundId = buildAndLoad("slot_win_chime.wav", 360) { t, progress, _ ->
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            val stepIdx = (progress * notes.size).toInt().coerceIn(0, notes.lastIndex)
            val localProg = (progress * notes.size) - stepIdx
            val env = (localProg * 8.0).coerceAtMost(1.0) * exp(-2.4 * localProg)
            val freq = notes[stepIdx]
            (sin(2.0 * PI * freq * t) * 0.72 + sin(2.0 * PI * (freq * 2.0) * t) * 0.28) * env
        }

        // Casi-acierto (tiradas 3..6): doble nota descendente de tensión
        slotNearMissSoundId = buildAndLoad("slot_near_miss.wav", 220) { t, progress, _ ->
            val freq = if (progress < 0.45) 659.25 else 466.16
            val localProg = if (progress < 0.45) progress / 0.45 else (progress - 0.45) / 0.55
            val env = (localProg * 10.0).coerceAtMost(1.0) * exp(-3.2 * localProg)
            (sin(2.0 * PI * freq * t) * 0.75 + sin(2.0 * PI * (freq * 1.5) * t) * 0.25) * env
        }
    }

    private fun writePcmWavFile(file: File, sampleRate: Int, samples: ShortArray) {
        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        for (sample in samples) {
            buffer.putShort(sample)
        }
        FileOutputStream(file).use { out ->
            out.write(buffer.array())
        }
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

        if (!isAppInForeground) {
            isMusicPausedByLifecycle = true
            return
        }

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
                if (isAppInForeground) {
                    player.start()
                } else {
                    isMusicPausedByLifecycle = true
                }
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
            stopSlotSpinLoop()
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
