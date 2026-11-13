package com.ifts18.unadecisionmas.ui.screens

import android.media.RingtoneManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.sensor.HapticFeedbackHelper
import com.ifts18.unadecisionmas.sensor.rememberAccelerometerMonitor
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
    var previewImageResId by remember(gameState.currentDayIndex) { mutableStateOf<Int?>(null) }

    val totalTimeMillis = currentScenario.decisionTimeSeconds * 1000L

    // Control de carga secuencial de mensajes del escenario actual
    var loadedMessagesCount by remember(gameState.currentDayIndex) { mutableIntStateOf(0) }
    var isTimerActive by remember(gameState.currentDayIndex) { mutableStateOf(false) }
    var elapsedDecisionMillis by remember(gameState.currentDayIndex) { mutableLongStateOf(0L) }
    val timerProgress = remember(gameState.currentDayIndex) { Animatable(1f) }

    // Evento de sensor en el Jueves: sacudir el teléfono para bloquear el pop-up de préstamo
    var isShakeChallengeActive by remember(gameState.currentDayIndex) {
        mutableStateOf(false)
    }
    var shakeProgress by remember(gameState.currentDayIndex) { mutableFloatStateOf(0f) }

    rememberAccelerometerMonitor(
        enabled = isShakeChallengeActive,
        shakeThresholdG = 1.38f,
        minIntervalMs = 100L
    ) {
        if (isShakeChallengeActive) {
            HapticFeedbackHelper.vibrateShort(context, 35L)
            SoundManager.playClick()
            shakeProgress = (shakeProgress + 0.34f).coerceAtMost(1f)
            if (shakeProgress >= 1f) {
                isShakeChallengeActive = false
                viewModel.unlockAchievement(AchievementsCatalog.ID_EVITANDO_TENTACION)
            }
        }
    }

    // En el último 28% del tiempo: se activa latido y pulso
    val isUrgent by remember(gameState.currentDayIndex) {
        derivedStateOf {
            isTimerActive && timerProgress.value <= 0.28f
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
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

    // Secuencia de mensajes y temporizador de 25 segundos por cada día
    LaunchedEffect(gameState.currentDayIndex, gameState.isPunishedNoPhone) {
        if (gameState.isPunishedNoPhone) {
            isTimerActive = false
            return@LaunchedEffect
        }
        previewImageResId = null
        SoundManager.startBgMusic()
        loadedMessagesCount = 0
        isTimerActive = false
        elapsedDecisionMillis = 0L
        timerProgress.snapTo(1f)

        fun playNotification() {
            if (SoundManager.isMuted.value) return
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (_: Exception) {}
        }

        currentScenario.messages.forEachIndexed { index, chatMsg ->
            delay(if (index == 0) 450L else chatMsg.delayAfterMillis)
            playNotification()
            loadedMessagesCount = index + 1
            try {
                scrollState.animateScrollTo(scrollState.maxValue)
            } catch (_: Throwable) {}
        }

        while (previewImageResId != null) {
            delay(100L)
        }

        delay(900L)

        // Si el escenario tiene alerta invasiva de préstamo/influencer, activar desafío de sensor
        if (currentScenario.triggersSpamShakeAlert) {
            HapticFeedbackHelper.vibrateAlertPattern(context)
            isShakeChallengeActive = true
        }

        isTimerActive = true

        var remainingTimeMillis = totalTimeMillis
        val tickStep = 50L

        while (remainingTimeMillis > 0L && isTimerActive) {
            if (previewImageResId != null) {
                delay(100L)
                continue
            }
            delay(tickStep)
            if (previewImageResId == null && isTimerActive) {
                remainingTimeMillis -= tickStep
                elapsedDecisionMillis = totalTimeMillis - remainingTimeMillis
                timerProgress.snapTo((remainingTimeMillis.toFloat() / totalTimeMillis.toFloat()).coerceIn(0f, 1f))
            }
        }

        if (isTimerActive && remainingTimeMillis <= 0L) {
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
    ) {
        if (!gameState.isPunishedNoPhone) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // 1. TOP BAR FIJA CON AHORRO, DEUDA, BIENESTAR Y DÍA
            GameTopBar(
                gameState = gameState,
                currentDayName = currentScenario.dayName
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. CHAT DE MENSAJES SECUENCIALES
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentScenario.messages.forEachIndexed { index, msg ->
                    val isVisible = index < loadedMessagesCount
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300)) +
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
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = msg.sender,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        if (msg.isForwarded) {
                                            Text(
                                                text = "• ↪ Publicidad / Reenviado",
                                                fontFamily = SpaceGrotesk,
                                                fontStyle = FontStyle.Italic,
                                                fontSize = 11.5.sp,
                                                color = BordeCyan
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Entregado",
                                        tint = VerdeExito,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                if (msg.imageResId != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.35f))
                                            .clickable { previewImageResId = msg.imageResId }
                                    ) {
                                        Image(
                                            painter = painterResource(id = msg.imageResId),
                                            contentDescription = "Captura reenviada",
                                            contentScale = ContentScale.Crop,
                                            alignment = Alignment.TopCenter,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(175.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "Tocar para ampliar",
                                                fontFamily = SpaceGrotesk,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }

                                if (msg.message.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.message,
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 22.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                if (!gameState.isPunishedNoPhone && loadedMessagesCount < currentScenario.messages.size) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = BordeCyan.copy(alpha = 0.3f), shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(BurbujaChat.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Escribiendo...",
                            fontFamily = SpaceGrotesk,
                            color = BordeCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. ZONA CALIENTE DEL PULGAR: TEMPORIZADOR DE 25s Y LAS 3 OPCIONES DE DECISIÓN
            AnimatedVisibility(
                visible = isTimerActive,
                enter = fadeIn(animationSpec = tween(260)) +
                        slideInVertically(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 400f
                            )
                        ) { it / 2 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Si está activo el desafío de sensor del Jueves, pedir sacudir el teléfono
                    if (isShakeChallengeActive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E132B))
                                .border(1.5.dp, BordeCyan, RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = BordeCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "¡POP-UP DE PRÉSTAMO! AGITÁ EL CELU PARA DESBLOQUEAR",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = BordeCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { shakeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = VerdeExito,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "O tocá acá si estás en emulador (${(shakeProgress * 100).toInt()}%)",
                                fontFamily = SpaceGrotesk,
                                fontSize = 10.5.sp,
                                color = VerdeExito,
                                modifier = Modifier.clickable {
                                    shakeProgress = (shakeProgress + 0.5f).coerceAtMost(1f)
                                    if (shakeProgress >= 1f) {
                                        isShakeChallengeActive = false
                                        viewModel.unlockAchievement(AchievementsCatalog.ID_EVITANDO_TENTACION)
                                    }
                                }
                            )
                        }
                    }

                    // Barra de temporizador en Canvas de 25s
                    val remainingSecs = ((totalTimeMillis - elapsedDecisionMillis) / 1000L).coerceAtLeast(0L)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SplitProgressBar(
                            progressProvider = { timerProgress.value },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${remainingSecs}s",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isUrgent) RojoAlerta else Color.White
                        )
                    }

                    // Las 3 alternativas de decisión del escenario en orden aleatorio y mismo color
                    val options = remember(gameState.currentDayIndex, gameState.decisionHistory.size) {
                        currentScenario.getAllOptions().shuffled()
                    }
                    options.forEach { option ->
                        DecisionButton(
                            text = option.text,
                            enabled = !isShakeChallengeActive,
                            onClick = {
                                if (isTimerActive && !isShakeChallengeActive) {
                                    isTimerActive = false
                                    viewModel.selectOption(
                                        option = option,
                                        responseTimeMillis = elapsedDecisionMillis
                                    )
                                }
                            },
                            isPulsing = isUrgent,
                            pulseScale = { pulseScale },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        }

        // Pantalla / Overlay integrado de Castigo ("Saltó la ficha: Sin Celu hasta el próximo Lunes")
        AnimatedVisibility(
            visible = gameState.isPunishedNoPhone,
            enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(180))
        ) {
            val punishmentScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(punishmentScrollState)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF181224))
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(listOf(RojoAlerta, BordeMagenta, BordeCyan)),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📵",
                        fontSize = 48.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = gameState.punishmentReasonTitle.ifBlank { "📵 ¡SALTÓ LA FICHA! SIN CELULAR" },
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = RojoAlerta,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = gameState.punishmentReasonBody,
                        fontFamily = SpaceGrotesk,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Insignia de Trofeo Desbloqueado en el acto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF142229))
                            .border(1.2.dp, VerdeExito, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🏅", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TROFEO DESBLOQUEADO (#06)",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = VerdeExito
                            )
                            Text(
                                text = "Sin Celu hasta el Lunes (+400 pts)",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resumen de cómo quedó la semana truncada
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF121620))
                                .border(1.dp, BordeCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Ahorro Restante",
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.sp,
                                color = BordeCyan
                            )
                            Text(
                                text = "$${gameState.currentSavings}",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF121620))
                                .border(1.dp, RojoAlerta.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Deuda Acumulada",
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.sp,
                                color = RojoAlerta
                            )
                            Text(
                                text = "$${gameState.currentDebt}",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Pasó el fin de semana en penitencia... Un nuevo lunes, una nueva oportunidad para terminar la semana sin caer en la trampa.",
                        fontFamily = SpaceGrotesk,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = BordeCyan,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botón 1: Teletransportar al próximo Lunes (Reiniciar semana)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                            .clickable {
                                viewModel.restartToNextMondayAfterPunishment()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔄 Pasar al Lunes siguiente (Nuevo comienzo)",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón 2: Ir directo al Balance Final / Menú Principal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF222838))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.finishPunishedWeekToResults()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📊 Ver Balance Final y Trofeos",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Overlay integrado para ver la captura ampliada en pantalla completa
        AnimatedVisibility(
            visible = previewImageResId != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160))
        ) {
            previewImageResId?.let { imgRes ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.94f))
                        .clickable { previewImageResId = null },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = imgRes),
                        contentDescription = "Captura ampliada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 40.dp)
                    )
                    IconButton(
                        onClick = { previewImageResId = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
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
            .height(11.dp)
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
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    pulseScale: () -> Float = { 1.0f }
) {
    val shape = RoundedCornerShape(13.dp)
    val borderBrush = if (isPulsing) {
        Brush.horizontalGradient(listOf(RojoAlerta, BordeMagenta, BordeCyan))
    } else {
        Brush.horizontalGradient(listOf(BordeCyan, BordeMagenta.copy(alpha = 0.75f)))
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = if (isPulsing) pulseScale() else 1f
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .height(46.dp)
            .border(
                width = if (isPulsing) 2.dp else 1.3.dp,
                brush = borderBrush,
                shape = shape
            )
            .clip(shape)
            .background(Color(0xFF172030).copy(alpha = 0.90f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = SpaceGrotesk,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 16.sp
        )
    }
}
