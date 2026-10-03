package com.ifts18.unadecisionmas.ui.screens

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.JuegoLimpioTheme
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.delay

@Composable
fun FirstDecisionScreen(
    playerName: String = "Mateo",
    onApostarClick: () -> Unit = {},
    onNavigateToInfo: () -> Unit = {},
    onNoApostarClick: () -> Unit = onNavigateToInfo,
    onTimeout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current

    // SoundPool para confirmacion sonora inmediata y tactil al clickear botones
    val soundPool = remember {
        if (!isPreview) {
            try {
                SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val clickSoundId = remember(soundPool) {
        if (!isPreview && soundPool != null) {
            try {
                soundPool.load(context, R.raw.button_click, 1)
            } catch (_: Exception) {
                0
            }
        } else 0
    }

    val playClickFeedback: () -> Unit = {
        try {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}

        try {
            if (!isPreview && soundPool != null && clickSoundId != 0) {
                soundPool.play(clickSoundId, 0.40f, 0.40f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    // Estado de carga y llegada del mensaje
    var isMessageLoaded by remember { mutableStateOf(false) }

    // Control del temporizador de 15 segundos
    var isTimerActive by remember { mutableStateOf(false) }
    val timerProgress = remember { Animatable(1f) }

    // Determinar si estamos en los ultimos 5 segundos con derivedStateOf (evita recomposicion innecesaria)
    val isLast5Seconds by remember {
        derivedStateOf {
            isMessageLoaded && isTimerActive && timerProgress.value <= (5f / 15f)
        }
    }

    // Animacion de latido (heartbeat / pulso) para los botones en los ultimos 5 segundos
    val pulseTransition = rememberInfiniteTransition(label = "buttonHeartbeat")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Reproductores de audio: musica de fondo en suspense y latido de corazon
    val bgMusicPlayer = remember {
        if (!isPreview) {
            try {
                MediaPlayer().apply {
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
                    setVolume(0.45f, 0.45f)
                }
            } catch (_: Exception) {
                try {
                    MediaPlayer.create(context, R.raw.bg_music_suspense)?.apply {
                        isLooping = true
                        setVolume(0.45f, 0.45f)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    val heartbeatPlayer = remember {
        if (!isPreview) {
            try {
                MediaPlayer().apply {
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
                    setVolume(0.35f, 0.35f) // Levemente audible como fue solicitado
                }
            } catch (_: Exception) {
                try {
                    MediaPlayer.create(context, R.raw.heartbeat)?.apply {
                        isLooping = true
                        setVolume(0.35f, 0.35f)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    val stopAllAudio: () -> Unit = {
        isTimerActive = false
        try {
            if (bgMusicPlayer?.isPlaying == true) {
                bgMusicPlayer.pause()
            }
            if (heartbeatPlayer?.isPlaying == true) {
                heartbeatPlayer.pause()
            }
        } catch (_: Exception) {}
    }

    // Liberar recursos de audio cuando la pantalla se desmonta
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (bgMusicPlayer?.isPlaying == true) {
                    bgMusicPlayer.stop()
                }
                bgMusicPlayer?.release()
            } catch (_: Exception) {}

            try {
                if (heartbeatPlayer?.isPlaying == true) {
                    heartbeatPlayer.stop()
                }
                heartbeatPlayer?.release()
            } catch (_: Exception) {}

            try {
                soundPool?.release()
            } catch (_: Exception) {}
        }
    }

    // 1. Simular carga del mensaje, reproducir sonido de notificacion y activar animacion
    LaunchedEffect(Unit) {
        delay(700L) // Breve tiempo de anticipacion / carga del mensaje

        if (!isPreview) {
            // Reproducir sonido de notificacion del sistema (con fallback a ToneGenerator)
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (_: Exception) {
                try {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                } catch (_: Exception) {}
            }
        }

        // Marcar mensaje como entregado para disparar animacion de entrada
        isMessageLoaded = true
    }

    // 2. Temporizador real de 15 segundos y musica de fondo: arrancan 1.5s DESPUES de llegar el mensaje
    LaunchedEffect(isMessageLoaded) {
        if (isMessageLoaded) {
            // Pausa de 1.5 segundos para que el usuario pueda leer el mensaje antes de iniciar el estres
            delay(1500L)

            // Iniciar musica de fondo a velocidad normal (1.0x)
            try {
                if (bgMusicPlayer != null && !bgMusicPlayer.isPlaying) {
                    bgMusicPlayer.start()
                }
            } catch (_: Exception) {}

            isTimerActive = true
            timerProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 15_000,
                    easing = LinearEasing
                )
            )

            if (isTimerActive && timerProgress.value <= 0.001f) {
                stopAllAudio()
                onTimeout()
            }
        }
    }

    // 3. En los ultimos 5 segundos: acelerar musica de fondo y reproducir latido leve sincronizado
    LaunchedEffect(isLast5Seconds) {
        if (isLast5Seconds && isTimerActive) {
            // Acelerar la musica de fondo a 1.40x
            try {
                bgMusicPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(1.40f)
                    }
                }
            } catch (_: Exception) {}

            // Iniciar el latido tenue
            try {
                heartbeatPlayer?.let { player ->
                    if (!player.isPlaying) {
                        player.start()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Gradiente difuminado verde/azulado en la esquina superior izquierda
    val backgroundBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E676).copy(alpha = 0.28f),
                Color(0xFF00E5FF).copy(alpha = 0.15f),
                FondoGeneral
            ),
            center = Offset(x = 100f, y = 350f),
            radius = 900f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BARRA SUPERIOR: Titulo "Lunes" y Foto de perfil circular
            HeaderSection(
                title = "Lunes"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. TARJETA CENTRAL: Carga y animacion de entrada del mensaje
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isMessageLoaded) {
                    // Indicador sutil de carga / mensaje entrante
                    TypingIndicatorCard(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = isMessageLoaded,
                    enter = fadeIn(animationSpec = tween(350)) +
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.75f,
                                    stiffness = 380f
                                )
                            ) { -it / 2 } +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = spring(
                                    dampingRatio = 0.75f,
                                    stiffness = 380f
                                )
                            )
                ) {
                    ChatMessageCard(
                        playerName = playerName,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. BARRA DE PROGRESO - TIMER REAL DE 15 SEGUNDOS (Verde se reduce, Rojo aumenta)
            SplitProgressBar(
                progressProvider = { timerProgress.value },
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4. BOTONES DE ACCION INFERIORES (Laten en los ultimos 5 segundos)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GradientPillButton(
                    text = "Apostar $1.000",
                    onClick = {
                        playClickFeedback()
                        stopAllAudio()
                        onApostarClick()
                    },
                    isPulsing = isLast5Seconds,
                    pulseScale = { pulseScale },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                GradientPillButton(
                    text = "No apostar",
                    onClick = {
                        playClickFeedback()
                        stopAllAudio()
                        onNoApostarClick()
                    },
                    isPulsing = isLast5Seconds,
                    pulseScale = { pulseScale },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

/**
 * Indicador de mensaje cargando / escribiendo con puntos animados
 */
@Composable
private fun TypingIndicatorCard(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val transition = rememberInfiniteTransition(label = "typingDots")
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "d1"
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 150), RepeatMode.Reverse), label = "d2"
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 300), RepeatMode.Reverse), label = "d3"
    )

    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = BordeCyan.copy(alpha = 0.3f),
                shape = shape
            )
            .clip(shape)
            .background(BurbujaChat.copy(alpha = 0.6f))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Entrando mensaje",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Box(modifier = Modifier.size(6.dp).background(BordeCyan.copy(alpha = dot1Alpha), CircleShape))
            Box(modifier = Modifier.size(6.dp).background(BordeCyan.copy(alpha = dot2Alpha), CircleShape))
            Box(modifier = Modifier.size(6.dp).background(BordeCyan.copy(alpha = dot3Alpha), CircleShape))
        }
    }
}

/**
 * Encabezado con el titulo centrado "Lunes" y foto de perfil alineada a la derecha.
 */
@Composable
private fun HeaderSection(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        // Texto Centrado "Lunes"
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        // Imagen de perfil circular a la derecha
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(50.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(BordeCyan, BordeMagenta)
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(Color(0xFF2A2038)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Foto de perfil",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Tarjeta de mensaje estilo chat con bordes de gradiente (magenta a verde),
 * fondo translúcido, icono tilde verde y texto exacto.
 */
@Composable
private fun ChatMessageCard(
    playerName: String,
    modifier: Modifier = Modifier
) {
    val borderGradient = Brush.horizontalGradient(
        colors = listOf(BordeMagenta, VerdeExito)
    )

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .border(width = 1.5.dp, brush = borderGradient, shape = shape)
            .clip(shape)
            .background(BurbujaChat.copy(alpha = 0.85f))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icono pequeño de tilde/check verde en la esquina superior derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Mensaje entregado",
                    tint = VerdeExito,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Texto exacto según requerimiento
            Text(
                text = "Hola $playerName! Querés ganar hoy mismo $2.000.- ? Solo tenés que pagar $ 1.000.- ahora, en menos de 10 segudos!!! Te espero !!!",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * Barra de progreso lineal dividida en dos partes: Verde a la izquierda (tiempo restante) y Roja a la derecha.
 * Opera como un temporizador real de 15 segundos optimizado en Canvas (solo corre en la fase de dibujo).
 */
@Composable
private fun SplitProgressBar(
    progressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val cornerRadius = with(LocalDensity.current) { 8.dp.toPx() }
    val spacerWidth = with(LocalDensity.current) { 3.dp.toPx() }

    Canvas(
        modifier = modifier
            .height(14.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        val progress = progressProvider().coerceIn(0f, 1f)
        val w = size.width
        val h = size.height

        val greenW = w * progress
        val redW = w - greenW

        if (greenW > 1f) {
            val drawGreenW = if (redW > 1f) (greenW - spacerWidth / 2f).coerceAtLeast(0f) else w
            drawRoundRect(
                color = VerdeExito,
                topLeft = Offset.Zero,
                size = Size(drawGreenW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
        }

        if (redW > 1f) {
            val redStart = if (greenW > 1f) (greenW + spacerWidth / 2f).coerceAtMost(w) else 0f
            drawRoundRect(
                color = RojoAlerta,
                topLeft = Offset(redStart, 0f),
                size = Size(w - redStart, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

/**
 * Boton redondeado (pill-shaped) con soporte para efecto de latido / pulso en los ultimos segundos.
 * Optimizado con lambda de escala para ejecutar la transformacion en graphicsLayer sin recomponer el boton.
 */
@Composable
private fun GradientPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    pulseScale: () -> Float = { 1.0f }
) {
    val shape = CircleShape
    val borderBrush = if (isPulsing) {
        Brush.horizontalGradient(
            colors = listOf(RojoAlerta, BordeMagenta, BordeCyan)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(BordeMagenta, BordeCyan)
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = if (isPulsing) pulseScale() else 1f
                scaleX = scale
                scaleY = scale
            }
            .height(54.dp)
            .border(
                width = if (isPulsing) 2.2.dp else 1.5.dp,
                brush = borderBrush,
                shape = shape
            )
            .clip(shape)
            .background(Color(0x33121212))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun FirstDecisionScreenPreview() {
    JuegoLimpioTheme {
        FirstDecisionScreen(
            playerName = "Mateo"
        )
    }
}
