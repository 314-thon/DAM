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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
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
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
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
    val scrollState = rememberScrollState()

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

    // Estado de carga secuencial de los mensajes
    var isMessage1Loaded by remember { mutableStateOf(false) }
    var isMessage2Loaded by remember { mutableStateOf(false) }
    var isMessage3Loaded by remember { mutableStateOf(false) }

    // Control del temporizador de 15 segundos
    var isTimerActive by remember { mutableStateOf(false) }
    val timerProgress = remember { Animatable(1f) }

    // Determinar si estamos en los ultimos 5 segundos con derivedStateOf
    val isLast5Seconds by remember {
        derivedStateOf {
            isMessage3Loaded && isTimerActive && timerProgress.value <= (5f / 15f)
        }
    }

    // Animacion de latido (pulso) para los botones en los ultimos 5 segundos
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
                    setVolume(0.35f, 0.35f)
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

    // Secuencia temporal de llegada de mensajes y activacion del temporizador
    LaunchedEffect(Unit) {
        val playMessageTone: () -> Unit = {
            if (!isPreview) {
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
        }

        // 1. Breve tiempo de anticipacion inicial
        delay(600L)
        playMessageTone()
        isMessage1Loaded = true

        // 2. Exactamente 2 segundos de delay para el Mensaje 2
        delay(2000L)
        playMessageTone()
        isMessage2Loaded = true

        // 3. Exactamente 2 segundos de delay para el Mensaje 3
        delay(2000L)
        playMessageTone()
        isMessage3Loaded = true

        // 4. Pausa de 1.5 segundos LUEGO DEL ÚLTIMO MENSAJE antes de arrancar timer y musica
        delay(1500L)

        // Iniciar musica de fondo a velocidad normal
        try {
            if (bgMusicPlayer != null && !bgMusicPlayer.isPlaying) {
                bgMusicPlayer.start()
            }
        } catch (_: Exception) {}

        // Iniciar el temporizador de 15 segundos
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

    // 3. En los ultimos 5 segundos: acelerar musica de fondo y reproducir latido leve
    LaunchedEffect(isLast5Seconds) {
        if (isLast5Seconds) {
            try {
                bgMusicPlayer?.let { player ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val params = player.playbackParams
                        params.speed = 1.35f
                        params.pitch = 1.10f
                        player.playbackParams = params
                    }
                }
            } catch (_: Exception) {}

            try {
                heartbeatPlayer?.let { player ->
                    if (!player.isPlaying) {
                        player.start()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Gradiente difuminado oscuro de fondo
    val backgroundBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E676).copy(alpha = 0.22f),
                Color(0xFF00E5FF).copy(alpha = 0.10f),
                FondoGeneral
            ),
            center = Offset(x = 120f, y = 300f),
            radius = 850f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ENCABEZADO MINIMALISTA: "Lunes" y avatar sutil
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lunes",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(BordeCyan, BordeMagenta)
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(Color(0xFF24142D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. FLUJO SECUENCIAL DE MENSAJES HACIA ABAJO (Ocupa el espacio restante, scrollable si la pantalla es reducida)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mensaje 1: Propuesta del amigo
                AnimatedVisibility(
                    visible = isMessage1Loaded,
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
                    ChatMessageBubble(
                        sender = "Lucas",
                        message = "¡Che $playerName! Me pasaron una fija imperdible para el partido de hoy 🔥",
                        isDelivered = true
                    )
                }

                // Mensaje 2: La oferta económica concreta
                AnimatedVisibility(
                    visible = isMessage2Loaded,
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
                    ChatMessageBubble(
                        sender = "Lucas",
                        message = "¿Ponés $1.000 ahora y nos llevamos $2.000 cada uno al toque?",
                        isDelivered = true
                    )
                }

                // Mensaje 3: Urgencia y presión de tiempo
                AnimatedVisibility(
                    visible = isMessage3Loaded,
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
                    ChatMessageBubble(
                        sender = "Lucas",
                        message = "¡Confirmame ya que en 15 segundos cierran la apuesta y se cae la cuota!",
                        isDelivered = true
                    )
                }

                // Indicador de escritura hasta que caiga el último mensaje
                if (!isMessage3Loaded) {
                    TypingIndicatorBubble()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. ZONA CALIENTE DEL PULGAR (FIJA EN LA BASE DE LA PANTALLA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra de temporizador horizontal con bordes redondeados y resplandor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    SplitProgressBar(
                        progressProvider = { timerProgress.value },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Dos botones de acción grandes y prominentes (lado a lado o apilados, texto blanco puro)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Boton 1: No apostar
                    DecisionActionButton(
                        text = "No apostar",
                        isPrimary = false,
                        onClick = {
                            playClickFeedback()
                            stopAllAudio()
                            onNoApostarClick()
                        },
                        isPulsing = isLast5Seconds,
                        pulseScale = { pulseScale },
                        modifier = Modifier.weight(1f)
                    )

                    // Boton 2: Apostar $1.000
                    DecisionActionButton(
                        text = "Apostar $1.000",
                        isPrimary = true,
                        onClick = {
                            playClickFeedback()
                            stopAllAudio()
                            onApostarClick()
                        },
                        isPulsing = isLast5Seconds,
                        pulseScale = { pulseScale },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Burbuja de mensaje secuencial de chat con texto 100% blanco puro y tipografía uniforme agrandada.
 */
@Composable
private fun ChatMessageBubble(
    sender: String,
    message: String,
    isDelivered: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = shape
            )
            .clip(shape)
            .background(BurbujaChat.copy(alpha = 0.85f))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sender,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )

                if (isDelivered) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Entregado",
                        tint = VerdeExito,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                fontFamily = SpaceGrotesk,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 27.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Indicador de mensaje cargando / escribiendo.
 */
@Composable
private fun TypingIndicatorBubble(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
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
            .fillMaxWidth()
            .border(width = 1.dp, color = BordeCyan.copy(alpha = 0.3f), shape = shape)
            .clip(shape)
            .background(BurbujaChat.copy(alpha = 0.6f))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lucas está escribiendo",
                fontFamily = SpaceGrotesk,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            )
            Box(modifier = Modifier.size(6.dp).background(BordeCyan.copy(alpha = dot1Alpha), CircleShape))
            Box(modifier = Modifier.size(6.dp).background(BordeCyan.copy(alpha = dot2Alpha), CircleShape))
            Box(modifier = Modifier.size(6.dp).background(BordeCyan.copy(alpha = dot3Alpha), CircleShape))
        }
    }
}

/**
 * Barra de temporizador horizontal de 15 segundos en Canvas (sin recomposiciones innecesarias).
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
            .background(Color(0xFF161922))
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
 * Boton de accion prominente con texto en blanco puro y soporte para pulso / latido.
 */
@Composable
private fun DecisionActionButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    pulseScale: () -> Float = { 1.0f }
) {
    val shape = RoundedCornerShape(14.dp)
    val borderBrush = if (isPulsing) {
        Brush.horizontalGradient(
            colors = listOf(RojoAlerta, BordeMagenta, BordeCyan)
        )
    } else {
        if (isPrimary) {
            Brush.horizontalGradient(
                colors = listOf(BordeMagenta, BordeCyan)
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.20f))
            )
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = if (isPulsing) pulseScale() else 1f
                scaleX = scale
                scaleY = scale
            }
            .height(52.dp)
            .border(
                width = if (isPulsing) 2.2.dp else 1.4.dp,
                brush = borderBrush,
                shape = shape
            )
            .clip(shape)
            .background(
                if (isPrimary) Color(0x33121212) else Color(0x22FFFFFF)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = SpaceGrotesk,
            fontSize = 15.5.sp,
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
