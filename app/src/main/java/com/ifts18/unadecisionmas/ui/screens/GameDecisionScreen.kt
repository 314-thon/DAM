package com.ifts18.unadecisionmas.ui.screens

import android.media.RingtoneManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.DecisionOption
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.ui.components.GameTopBar
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import com.ifts18.unadecisionmas.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun GameDecisionScreen(
    viewModel: GameViewModel,
    gameState: GameState,
    onNavigateToResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScenario = viewModel.getCurrentScenario()
    val scrollState = rememberScrollState()

    // Control de carga secuencial de mensajes del escenario actual
    var loadedMessagesCount by remember(gameState.currentDayIndex) { mutableIntStateOf(0) }
    var isTimerActive by remember(gameState.currentDayIndex) { mutableStateOf(false) }
    val timerProgress = remember(gameState.currentDayIndex) { Animatable(1f) }

    // En los últimos 4 segundos de 10s: se activa latido y pulso
    val isUrgent by remember(gameState.currentDayIndex) {
        derivedStateOf {
            isTimerActive && timerProgress.value <= 0.40f
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Si ya terminó el juego, navegar a resultados
    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            onNavigateToResult()
        }
    }

    // Efecto de audio y aceleración en últimos segundos
    LaunchedEffect(isUrgent) {
        if (isUrgent) {
            SoundManager.setMusicSpeed(1.35f)
            SoundManager.startHeartbeat()
        } else {
            SoundManager.resetMusicSpeed()
            SoundManager.stopHeartbeat()
        }
    }

    // Secuencia de mensajes y temporizador por cada día
    LaunchedEffect(gameState.currentDayIndex) {
        SoundManager.startBgMusic()
        loadedMessagesCount = 0
        isTimerActive = false
        timerProgress.snapTo(1f)

        // Reproducir sonido sutil de notificación
        fun playNotification() {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (_: Exception) {}
        }

        // Desplegar cada mensaje con su delay
        currentScenario.messages.forEachIndexed { index, chatMsg ->
            delay(if (index == 0) 500L else chatMsg.delayAfterMillis)
            playNotification()
            loadedMessagesCount = index + 1
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        // Breve pausa para leer el último mensaje antes de arrancar los 10 segundos
        delay(1200L)
        isTimerActive = true

        // Animación estricta de 10 segundos (10_000 ms)
        timerProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 10_000,
                easing = LinearEasing
            )
        )

        // Si expiró el tiempo sin respuesta
        if (isTimerActive && timerProgress.value <= 0.005f) {
            isTimerActive = false
            viewModel.onTimeout()
        }
    }

    val backgroundBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E676).copy(alpha = 0.15f),
                Color(0xFF00E5FF).copy(alpha = 0.08f),
                FondoGeneral
            ),
            center = Offset(x = 120f, y = 250f),
            radius = 850f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP BAR FIJA CON SALDO, AVATAR Y HISTORIAL DE DECISIONES
            GameTopBar(
                gameState = gameState,
                currentDayName = currentScenario.dayName
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. CHAT DE MENSAJES SECUENCIALES (OCUPA EL CUERPO PRINCIPAL)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                currentScenario.messages.forEachIndexed { index, msg ->
                    val isVisible = index < loadedMessagesCount
                    AnimatedVisibility(
                        visible = isVisible,
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(BurbujaChat.copy(alpha = 0.85f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.sender,
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Entregado",
                                        tint = VerdeExito,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = msg.message,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 18.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 24.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Indicador de escritura si aún faltan mensajes por llegar
                if (loadedMessagesCount < currentScenario.messages.size) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = BordeCyan.copy(alpha = 0.3f), shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(BurbujaChat.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Escribiendo...",
                            fontFamily = SpaceGrotesk,
                            color = BordeCyan,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. ZONA CALIENTE DEL PULGAR: TEMPORIZADOR DE 10s Y BOTONES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra de temporizador en Canvas de 10s
                SplitProgressBar(
                    progressProvider = { timerProgress.value },
                    modifier = Modifier.fillMaxWidth()
                )

                // Botones de acción: Opción preventiva vs Opción de riesgo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Seguro (Preventivo)
                    DecisionButton(
                        text = currentScenario.safeOption.text,
                        isPrimary = false,
                        onClick = {
                            if (isTimerActive) {
                                isTimerActive = false
                                viewModel.selectOption(currentScenario.safeOption)
                            }
                        },
                        isPulsing = isUrgent,
                        pulseScale = { pulseScale },
                        modifier = Modifier.weight(1f)
                    )

                    // Botón Riesgoso (Apostar / Endeudarse)
                    DecisionButton(
                        text = currentScenario.riskyOption.text,
                        isPrimary = true,
                        onClick = {
                            if (isTimerActive) {
                                isTimerActive = false
                                viewModel.selectOption(currentScenario.riskyOption)
                            }
                        },
                        isPulsing = isUrgent,
                        pulseScale = { pulseScale },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitProgressBar(
    progressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val cornerRadius = with(LocalDensity.current) { 8.dp.toPx() }
    val spacerWidth = with(LocalDensity.current) { 3.dp.toPx() }

    Canvas(
        modifier = modifier
            .height(13.dp)
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

@Composable
private fun DecisionButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    pulseScale: () -> Float = { 1.0f }
) {
    val shape = RoundedCornerShape(14.dp)
    val borderBrush = if (isPulsing) {
        Brush.horizontalGradient(listOf(RojoAlerta, BordeMagenta, BordeCyan))
    } else {
        if (isPrimary) {
            Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan))
        } else {
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.20f)))
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
            .background(if (isPrimary) Color(0x33121212) else Color(0x22FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = SpaceGrotesk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}
