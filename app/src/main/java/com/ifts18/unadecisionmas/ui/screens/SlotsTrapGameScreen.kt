package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.SlotSymbol
import com.ifts18.unadecisionmas.data.model.SlotsGameState
import com.ifts18.unadecisionmas.sensor.HapticFeedbackHelper
import com.ifts18.unadecisionmas.sensor.rememberAccelerometerMonitor
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sin

private val GoldBezelLight = Color(0xFFFFE082)
private val GoldBezelDark = Color(0xFFB45309)
private val CrimsonCabinetTop = Color(0xFF4A0710)
private val CrimsonCabinetBottom = Color(0xFF170307)

private fun easeOutBackCustom(t: Float): Float {
    val inv = t.coerceIn(0f, 1f) - 1f
    return 1f + 3.60158f * inv.pow(3) + 2.60158f * inv.pow(2)
}

private fun easeOutBackTurbo(t: Float): Float {
    val inv = t.coerceIn(0f, 1f) - 1f
    return 1f + 2.2f * inv.pow(3) + 1.2f * inv.pow(2)
}

/**
 * JUEGO 3: "La Trampa del Jackpot"
 * Simulador interactivo 3x3 (9 cuadros) inspirado en maquinas tipo Coinburst Sevens,
 * con desplazamiento fisico de rodillos, rebote easeOutBackCustom, modo Intriga,
 * Turbo y audio sintetizado sincronizado mediante SoundPool.
 */
@Composable
fun SlotsTrapGameScreen(
    state: SlotsGameState,
    onStateChange: (SlotsGameState) -> Unit,
    onBackToHub: () -> Unit,
    onGameFinished: (stoppedEarly: Boolean, spinsUsed: Int, remainingChips: Int, realMoneyLost: Int, elapsedMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val symbols = remember { SlotSymbol.entries }

    val settledGrid = remember(state.spinsUsed, state.reel1, state.reel2, state.reel3) {
        resolveThreeByThreeGrid(
            spinsUsed = state.spinsUsed,
            centerRow = listOf(state.reel1, state.reel2, state.reel3)
        )
    }

    var reelStrips by remember {
        mutableStateOf(
            List(3) { col ->
                listOf(
                    SlotSymbol.COIN,
                    settledGrid[col][0],
                    settledGrid[col][1],
                    settledGrid[col][2],
                    SlotSymbol.BAR
                )
            }
        )
    }
    var colOffsets by remember { mutableStateOf(listOf(0f, 0f, 0f)) }
    var colStoppedFlags by remember { mutableStateOf(listOf(true, true, true)) }
    var isSpinning by remember { mutableStateOf(false) }
    var isIntrigueActive by remember { mutableStateOf(false) }
    var intriguePulseAlpha by remember { mutableFloatStateOf(0f) }
    var turboEnabled by remember { mutableStateOf(false) }

    val currentState by rememberUpdatedState(state)
    var sessionOpenedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var activeBeforePauseMillis by remember { mutableStateOf(state.elapsedMillis) }
    var isLifecycleResumed by remember { mutableStateOf(true) }
    var hasFinished by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleResumed = true
                    sessionOpenedAt = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    SoundManager.stopSlotSpinLoop()
                    if (isLifecycleResumed) {
                        activeBeforePauseMillis += (System.currentTimeMillis() - sessionOpenedAt).coerceAtLeast(0L)
                        isLifecycleResumed = false
                        if (!hasFinished && currentState.spinsUsed > 0) {
                            onStateChange(currentState.copy(elapsedMillis = activeBeforePauseMillis))
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            SoundManager.stopSlotSpinLoop()
            if (!hasFinished && currentState.spinsUsed > 0) {
                val extra = if (isLifecycleResumed) (System.currentTimeMillis() - sessionOpenedAt).coerceAtLeast(0L) else 0L
                onStateChange(currentState.copy(elapsedMillis = activeBeforePauseMillis + extra))
            }
        }
    }

    fun currentTotalElapsed(): Long {
        val extra = if (isLifecycleResumed) (System.currentTimeMillis() - sessionOpenedAt).coerceAtLeast(0L) else 0L
        return activeBeforePauseMillis + extra
    }

    fun buildNextSpinState(snapshot: SlotsGameState, nextSpinNumber: Int, turboSnapshot: Boolean): SlotsGameState {
        val updatedChips = (snapshot.bonusChips - 1).coerceAtLeast(0)
        val totalElapsed = currentTotalElapsed()
        return when (nextSpinNumber) {
            1 -> {
                val newLocked = snapshot.fakeLockedWinnings + 3500
                snapshot.copy(
                    reel1 = SlotSymbol.CHERRY,
                    reel2 = SlotSymbol.CHERRY,
                    reel3 = SlotSymbol.CHERRY,
                    spinsUsed = nextSpinNumber,
                    bonusChips = updatedChips,
                    fakeLockedWinnings = newLocked,
                    elapsedMillis = totalElapsed,
                    spinStatusBanner = "Línea central x3 Cereza: +$3.500 (Bono x40)",
                    algorithmStageTitle = "Etapa 1: Gancho inicial",
                    algorithmExplanation = "Ganás fácil al inicio, pero exigen apostar x40 para poder retirar."
                )
            }
            2 -> {
                val newLocked = snapshot.fakeLockedWinnings + 3500
                snapshot.copy(
                    reel1 = SlotSymbol.BAR,
                    reel2 = SlotSymbol.BAR,
                    reel3 = SlotSymbol.BAR,
                    spinsUsed = nextSpinNumber,
                    bonusChips = updatedChips,
                    fakeLockedWinnings = newLocked,
                    elapsedMillis = totalElapsed,
                    spinStatusBanner = "Línea central x3 BAR: +$3.500 (Saldo retenido)",
                    algorithmStageTitle = "Etapa 1: Falsa racha positiva",
                    algorithmExplanation = "El saldo en bono sube mientras los pozos de arriba siguen trabados."
                )
            }
            3, 4, 5, 6 -> {
                val pairSymbol = if (nextSpinNumber % 2 == 0) SlotSymbol.SEVEN else SlotSymbol.COIN
                val missCenter = if (nextSpinNumber % 2 == 0) SlotSymbol.LEMON else SlotSymbol.BELL
                val posText = if (nextSpinNumber % 2 == 1) "arriba" else "abajo"
                snapshot.copy(
                    reel1 = pairSymbol,
                    reel2 = pairSymbol,
                    reel3 = missCenter,
                    spinsUsed = nextSpinNumber,
                    bonusChips = updatedChips,
                    elapsedMillis = totalElapsed,
                    spinStatusBanner = "Casi: salieron 2 ${pairSymbol.label} y el 3ro quedó 1 cuadro $posText",
                    algorithmStageTitle = "Etapa 2: Casi-acierto (Intriga)",
                    algorithmExplanation = "El 3er rodillo desacelera dejando el ${pairSymbol.label} pegado a la línea para tentarte."
                )
            }
            else -> {
                val newLocked = (snapshot.fakeLockedWinnings - 1800).coerceAtLeast(0)
                snapshot.copy(
                    reel1 = if (nextSpinNumber % 2 == 0) SlotSymbol.LEMON else SlotSymbol.GRAPES,
                    reel2 = if (nextSpinNumber % 2 == 0) SlotSymbol.BAR else SlotSymbol.BELL,
                    reel3 = if (nextSpinNumber % 2 == 0) SlotSymbol.CHERRY else SlotSymbol.DIAMOND,
                    spinsUsed = nextSpinNumber,
                    bonusChips = updatedChips,
                    fakeLockedWinnings = newLocked,
                    elapsedMillis = totalElapsed,
                    spinStatusBanner = "Sin línea ganadora • La ventaja matemática es de la casa",
                    algorithmStageTitle = "Etapa 3: Vaciado estadístico",
                    algorithmExplanation = if (turboSnapshot) {
                        "Con Turbo activado gastás las fichas al doble de velocidad y sin pausa."
                    } else {
                        "A más tiradas, la ventaja matemática del algoritmo absorbe el bono."
                    }
                )
            }
        }
    }

    fun triggerSpin() {
        val snapshot = currentState
        if (isSpinning || !isLifecycleResumed || snapshot.spinsUsed >= snapshot.maxSpins || snapshot.bonusChips <= 0) return

        val nextSpinNumber = snapshot.spinsUsed + 1
        val turboSnapshot = turboEnabled
        val isNearMissSpin = nextSpinNumber in 3..6
        val nextState = buildNextSpinState(snapshot, nextSpinNumber, turboSnapshot)
        val targetGrid = resolveThreeByThreeGrid(
            spinsUsed = nextSpinNumber,
            centerRow = listOf(nextState.reel1, nextState.reel2, nextState.reel3)
        )

        isSpinning = true
        isIntrigueActive = false
        intriguePulseAlpha = 0f
        colStoppedFlags = listOf(false, false, false)
        colOffsets = listOf(0f, 0f, 0f)
        reelStrips = List(3) { col ->
            listOf(
                symbols.random(),
                settledGrid[col][0],
                settledGrid[col][1],
                settledGrid[col][2],
                symbols.random()
            )
        }

        SoundManager.playClick()
        SoundManager.playSlotSpinStart(turbo = turboSnapshot)
        HapticFeedbackHelper.vibrateShort(context, 45L)

        scope.launch {
            try {
                // Fase SpinStart: retroceso inicial hacia arriba
                val recoilDurationMs = if (turboSnapshot) 60f else 100f
                var recoilElapsedMs = 0f
                var lastNanos = 0L
                while (recoilElapsedMs < recoilDurationMs) {
                    withFrameNanos { frameNanos ->
                        val dtMs = if (lastNanos == 0L) 16f else ((frameNanos - lastNanos) / 1_000_000f).coerceIn(1f, 40f)
                        lastNanos = frameNanos
                        recoilElapsedMs = (recoilElapsedMs + dtMs).coerceAtMost(recoilDurationMs)
                        val p = (recoilElapsedMs / recoilDurationMs).coerceIn(0f, 1f)
                        val recoil = -0.22f * sin(p * (Math.PI.toFloat() / 2f))
                        colOffsets = listOf(recoil, recoil, recoil)
                    }
                }

                // Fases SpinProgress -> SpinStop con rebote easeOutBackCustom / easeOutBackTurbo e Intriga
                val stopAtMs = if (turboSnapshot) {
                    floatArrayOf(180f, 235f, 290f)
                } else {
                    floatArrayOf(380f, 760f, if (isNearMissSpin) 1850f else 1140f)
                }
                val stopDurMs = if (turboSnapshot) 110f else 210f
                val stepMsNormal = if (turboSnapshot) 40f else 65f

                val colStates = IntArray(3) { 0 } // 0 = girando, 1 = frenando, 2 = detenido
                val brakeElapsed = FloatArray(3)
                var progressMs = 0f
                lastNanos = 0L
                var intrigueTriggered = false
                val workingStrips = reelStrips.map { it.toMutableList() }.toMutableList()
                val workingOffsets = colOffsets.toMutableList()
                val stoppedFlags = mutableListOf(false, false, false)

                while (colStates[2] != 2) {
                    withFrameNanos { frameNanos ->
                        val dtMs = if (lastNanos == 0L) 16f else ((frameNanos - lastNanos) / 1_000_000f).coerceIn(1f, 40f)
                        lastNanos = frameNanos
                        progressMs += dtMs

                        for (c in 0..2) {
                            if (colStates[c] == 0 && progressMs >= stopAtMs[c]) {
                                colStates[c] = 1
                                brakeElapsed[c] = 0f
                                workingStrips[c] = mutableListOf(
                                    symbols.random(),
                                    targetGrid[c][0],
                                    targetGrid[c][1],
                                    targetGrid[c][2],
                                    symbols.random()
                                )
                                SoundManager.playSlotReelStop(c)
                                HapticFeedbackHelper.vibrateShort(context, 30L)
                                if (c == 1 && isNearMissSpin && !turboSnapshot && !intrigueTriggered) {
                                    intrigueTriggered = true
                                    isIntrigueActive = true
                                    SoundManager.playSlotIntrigueLoop()
                                }
                            }

                            when (colStates[c]) {
                                0 -> {
                                    val stepMs = if (c == 2 && isIntrigueActive) 76f else stepMsNormal
                                    workingOffsets[c] += dtMs / stepMs
                                    while (workingOffsets[c] >= 1f) {
                                        workingOffsets[c] -= 1f
                                        val old = workingStrips[c]
                                        workingStrips[c] = mutableListOf(
                                            symbols.random(),
                                            old[0],
                                            old[1],
                                            old[2],
                                            old[3]
                                        )
                                    }
                                }
                                1 -> {
                                    brakeElapsed[c] += dtMs
                                    val p = (brakeElapsed[c] / stopDurMs).coerceIn(0f, 1f)
                                    val eased = if (turboSnapshot) easeOutBackTurbo(p) else easeOutBackCustom(p)
                                    workingOffsets[c] = -0.85f * (1f - eased)
                                    if (p >= 1f) {
                                        colStates[c] = 2
                                        workingOffsets[c] = 0f
                                        stoppedFlags[c] = true
                                    }
                                }
                            }
                        }

                        if (isIntrigueActive) {
                            intriguePulseAlpha = 0.5f + 0.5f * sin(progressMs * 0.018f)
                        }

                        reelStrips = workingStrips.map { it.toList() }
                        colOffsets = workingOffsets.toList()
                        colStoppedFlags = stoppedFlags.toList()
                    }
                }

                SoundManager.stopSlotSpinLoop()
                isIntrigueActive = false
                intriguePulseAlpha = 0f
                onStateChange(nextState)

                if (nextSpinNumber in 1..2) {
                    SoundManager.playSlotWinChime()
                } else if (nextSpinNumber in 3..6) {
                    SoundManager.playSlotNearMiss()
                }
                HapticFeedbackHelper.vibrateShort(context, 40L)
                isSpinning = false
            } finally {
                SoundManager.stopSlotSpinLoop()
                isIntrigueActive = false
                intriguePulseAlpha = 0f
                isSpinning = false
            }
        }
    }

    val sensorReading by rememberAccelerometerMonitor(
        enabled = !isSpinning && isLifecycleResumed && state.spinsUsed < state.maxSpins,
        shakeThresholdG = 1.42f,
        minIntervalMs = 600L
    ) {
        triggerSpin()
    }

    val isCenterWin = !isSpinning && state.spinsUsed in 1..2
    val nearMissRowInCol2 = when {
        isSpinning || state.spinsUsed !in 3..6 -> -1
        state.spinsUsed % 2 == 1 -> 0
        else -> 2
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FondoGeneral)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. CABECERA CON CONTADOR DE FICHAS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161922))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackToHub,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tragamonedas",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Agitá para girar • ${"%.2f".format(sensorReading.gForce)} G",
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.sp,
                            color = BordeCyan,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BordeMagenta.copy(alpha = 0.22f))
                        .border(1.dp, BordeMagenta, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Fichas: ${state.bonusChips}/${state.maxSpins}",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. BARRA COMPACTA DE BONO TRABADO Y PROGRESO DE FICHAS (SIN DUPLICAR CONTADOR)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BurbujaChat.copy(alpha = 0.90f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = BordeMagenta,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Bono trabado:",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = TextoSecundario
                    )
                    Text(
                        text = "$${state.fakeLockedWinnings}",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                LinearProgressIndicator(
                    progress = { state.bonusChips.toFloat() / state.maxSpins.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (state.bonusChips > 3) BordeCyan else RojoAlerta,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. MÁQUINA 3x3 (9 CUADROS) ADAPTABLE SIN SCROLL VERTICAL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF28070F), Color(0xFF11070E))
                        )
                    )
                    .border(
                        width = 1.8.dp,
                        brush = Brush.linearGradient(listOf(GoldBezelLight, GoldBezelDark)),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Corona superior + botón Turbo (inspirado en Evoplay)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "777 COINBURST",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        letterSpacing = 0.7.sp,
                        color = GoldBezelLight
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (turboEnabled) GoldBezelLight.copy(alpha = 0.22f)
                                else Color.Black.copy(alpha = 0.4f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (turboEnabled) GoldBezelLight else Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                SoundManager.playClick()
                                turboEnabled = !turboEnabled
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Turbo",
                            tint = if (turboEnabled) GoldBezelLight else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (turboEnabled) "TURBO ON" else "TURBO",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = if (turboEnabled) GoldBezelLight else Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Fila de 4 pozos señuelo (Mini / Minor / Major / Grand)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    JackpotLureChip("MINI", "$200", Color(0xFF2563EB), Modifier.weight(1f))
                    JackpotLureChip("MINOR", "$350", Color(0xFF16A34A), Modifier.weight(1f))
                    JackpotLureChip("MAJOR", "$1K", Color(0xFF9333EA), Modifier.weight(1f))
                    JackpotLureChip("GRAND", "$10K", Color(0xFFDC2626), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Marco dorado con tablero 3 columnas x 3 filas (9 cuadros) adaptable al alto disponible
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    val gridHeight = if (maxHeight == Dp.Infinity) {
                        192.dp
                    } else {
                        maxHeight.coerceAtMost(196.dp)
                    }
                    val viewportHeightDp = (gridHeight - 20.dp).coerceAtLeast(60.dp)
                    val stepDp = viewportHeightDp / 3f
                    val stepPx = with(LocalDensity.current) { stepDp.toPx() }
                    val iconSizeDp = (stepDp.value * 0.62f).toInt().coerceIn(22, 36)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(listOf(CrimsonCabinetTop, CrimsonCabinetBottom))
                            )
                            .border(
                                width = 1.8.dp,
                                brush = Brush.verticalGradient(listOf(GoldBezelLight, GoldBezelDark)),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (colIndex in 0..2) {
                                val isColStopped = !isSpinning || colStoppedFlags[colIndex]
                                val colOffset = if (isSpinning) colOffsets[colIndex] else 0f
                                val colStrip = if (isSpinning) {
                                    reelStrips[colIndex]
                                } else {
                                    listOf(
                                        SlotSymbol.COIN,
                                        settledGrid[colIndex][0],
                                        settledGrid[colIndex][1],
                                        settledGrid[colIndex][2],
                                        SlotSymbol.BAR
                                    )
                                }
                                val isColIntriga = colIndex == 2 && isSpinning && isIntrigueActive
                                val colBorderColor = if (isColIntriga) {
                                    BordeMagenta.copy(alpha = 0.65f + 0.35f * intriguePulseAlpha)
                                } else {
                                    GoldBezelDark.copy(alpha = 0.55f)
                                }
                                val colBgColor = if (isColIntriga) {
                                    Color(0xFF28071B)
                                } else {
                                    Color(0xFF1A050A)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colBgColor)
                                        .border(
                                            width = if (isColIntriga) (1.5f + 1.0f * intriguePulseAlpha).dp else 1.dp,
                                            color = colBorderColor,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clipToBounds()
                                    ) {
                                        for (k in 0..4) {
                                            val symbol = colStrip[k]
                                            val logicalRow = k - 1
                                            val isCenterRow = isColStopped && logicalRow == 1
                                            val highlightWin = isCenterWin && isCenterRow
                                            val highlightNearMiss = !isSpinning && (
                                                (colIndex == 2 && logicalRow == nearMissRowInCol2) ||
                                                    (nearMissRowInCol2 != -1 && isCenterRow && colIndex < 2)
                                                )

                                            val cellBorder = when {
                                                highlightWin -> GoldBezelLight
                                                highlightNearMiss -> BordeMagenta
                                                isCenterRow -> GoldBezelDark.copy(alpha = 0.65f)
                                                else -> Color.White.copy(alpha = 0.08f)
                                            }
                                            val cellBg = when {
                                                highlightWin -> Color(0xFF472009)
                                                highlightNearMiss -> Color(0xFF33112D)
                                                isCenterRow -> Color(0xFF28070F)
                                                else -> Color(0xFF140509)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(stepDp)
                                                    .graphicsLayer {
                                                        translationY = (logicalRow + colOffset) * stepPx
                                                    }
                                                    .padding(vertical = 2.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(cellBg)
                                                    .border(
                                                        width = if (highlightWin || highlightNearMiss) 1.8.dp else 1.dp,
                                                        color = cellBorder,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SlotSymbolVectorIcon(symbol = symbol, iconSizeDp = iconSizeDp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.spinStatusBanner,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    color = if (isCenterWin) GoldBezelLight else VerdeExito,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(7.dp))

                if (state.spinsUsed < state.maxSpins) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isSpinning) listOf(Color.Gray, Color.DarkGray)
                                    else listOf(GoldBezelDark, BordeMagenta)
                                )
                            )
                            .border(1.dp, GoldBezelLight.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSpinning) { triggerSpin() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "Girando rodillos..." else "Agitá o tocá para girar (${state.bonusChips})",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Sin fichas de bono. El casino te pide depositar:",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = RojoAlerta,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RojoAlerta.copy(alpha = 0.2f))
                                .border(1.dp, RojoAlerta, RoundedCornerShape(12.dp))
                                .clickable {
                                    SoundManager.playClick()
                                    hasFinished = true
                                    onGameFinished(
                                        false,
                                        state.spinsUsed,
                                        state.bonusChips,
                                        state.realMoneyLost + 5000,
                                        currentTotalElapsed()
                                    )
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cargar $5.000 reales para destrabar el bono",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. LECTURA DEL ALGORITMO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141B29))
                    .border(1.dp, BordeCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Text(
                    text = state.algorithmStageTitle,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BordeCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.algorithmExplanation,
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.5.sp,
                    lineHeight = 14.5.sp,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 5. BOTÓN PLANTARSE SIEMPRE VISIBLE DENTRO DEL VIEWPORT SIN SCROLL
            AnimatedVisibility(
                visible = state.spinsUsed >= 1,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                            .clickable {
                                SoundManager.playClick()
                                hasFinished = true
                                val stoppedBeforeDrain = state.spinsUsed < state.maxSpins
                                onGameFinished(
                                    stoppedBeforeDrain,
                                    state.spinsUsed,
                                    state.bonusChips,
                                    state.realMoneyLost,
                                    currentTotalElapsed()
                                )
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.spinsUsed < state.maxSpins) "Plantarse" else "Cerrar sin depositar",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveThreeByThreeGrid(
    spinsUsed: Int,
    centerRow: List<SlotSymbol>
): List<List<SlotSymbol>> {
    val c0 = centerRow.getOrElse(0) { SlotSymbol.SEVEN }
    val c1 = centerRow.getOrElse(1) { SlotSymbol.COIN }
    val c2 = centerRow.getOrElse(2) { SlotSymbol.BAR }

    return when (spinsUsed) {
        0 -> listOf(
            listOf(SlotSymbol.LEMON, c0, SlotSymbol.CHERRY),
            listOf(SlotSymbol.GRAPES, c1, SlotSymbol.BAR),
            listOf(SlotSymbol.COIN, c2, SlotSymbol.BELL)
        )
        1 -> listOf(
            listOf(SlotSymbol.COIN, c0, SlotSymbol.LEMON),
            listOf(SlotSymbol.BAR, c1, SlotSymbol.GRAPES),
            listOf(SlotSymbol.BELL, c2, SlotSymbol.COIN)
        )
        2 -> listOf(
            listOf(SlotSymbol.GRAPES, c0, SlotSymbol.COIN),
            listOf(SlotSymbol.LEMON, c1, SlotSymbol.SEVEN),
            listOf(SlotSymbol.CHERRY, c2, SlotSymbol.DIAMOND)
        )
        in 3..6 -> {
            val topCol2 = if (spinsUsed % 2 == 1) c0 else SlotSymbol.BAR
            val botCol2 = if (spinsUsed % 2 == 0) c0 else SlotSymbol.GRAPES
            listOf(
                listOf(SlotSymbol.LEMON, c0, SlotSymbol.GRAPES),
                listOf(SlotSymbol.CHERRY, c1, SlotSymbol.BELL),
                listOf(topCol2, c2, botCol2)
            )
        }
        else -> listOf(
            listOf(SlotSymbol.COIN, c0, SlotSymbol.BELL),
            listOf(SlotSymbol.SEVEN, c1, SlotSymbol.LEMON),
            listOf(SlotSymbol.GRAPES, c2, SlotSymbol.BAR)
        )
    }
}

@Composable
private fun JackpotLureChip(
    tier: String,
    amount: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(accent.copy(alpha = 0.25f))
            .border(1.dp, GoldBezelLight.copy(alpha = 0.65f), RoundedCornerShape(7.dp))
            .padding(vertical = 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tier,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            color = GoldBezelLight,
            maxLines = 1
        )
        Text(
            text = amount,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun SlotSymbolVectorIcon(symbol: SlotSymbol, iconSizeDp: Int = 34) {
    val iconSize = iconSizeDp.dp
    when (symbol) {
        SlotSymbol.SEVEN -> {
            val fontSize = (iconSizeDp * 0.48f).coerceIn(13f, 18f).sp
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF7F1D1D))
                    .border(1.2.dp, GoldBezelLight, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "777",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize,
                    color = GoldBezelLight,
                    maxLines = 1
                )
            }
        }
        SlotSymbol.BAR -> {
            val fontSize = (iconSizeDp * 0.38f).coerceIn(11f, 14f).sp
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(listOf(GoldBezelLight, Color(0xFFD97706)))
                    )
                    .border(1.2.dp, Color(0xFF451A03), RoundedCornerShape(6.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BAR",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize,
                    letterSpacing = 1.sp,
                    color = Color(0xFF1F0A04),
                    maxLines = 1
                )
            }
        }
        SlotSymbol.COIN -> {
            Canvas(modifier = Modifier.size(iconSize)) {
                val r = size.minDimension * 0.42f
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = r,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFDE047),
                    radius = r * 0.78f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFB45309),
                    radius = r * 0.62f,
                    center = center,
                    style = Stroke(width = size.width * 0.06f)
                )
                drawCircle(
                    color = Color(0xFF92400E),
                    radius = r * 0.22f,
                    center = center
                )
            }
        }
        SlotSymbol.DIAMOND -> {
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = size.width
                val h = size.height
                val outer = Path().apply {
                    moveTo(w * 0.28f, h * 0.22f)
                    lineTo(w * 0.72f, h * 0.22f)
                    lineTo(w * 0.90f, h * 0.44f)
                    lineTo(w * 0.50f, h * 0.86f)
                    lineTo(w * 0.10f, h * 0.44f)
                    close()
                }
                drawPath(path = outer, color = BordeCyan)
                val facet = Path().apply {
                    moveTo(w * 0.10f, h * 0.44f)
                    lineTo(w * 0.90f, h * 0.44f)
                    moveTo(w * 0.36f, h * 0.22f)
                    lineTo(w * 0.50f, h * 0.86f)
                    lineTo(w * 0.64f, h * 0.22f)
                }
                drawPath(
                    path = facet,
                    color = Color.White.copy(alpha = 0.65f),
                    style = Stroke(width = w * 0.055f, join = StrokeJoin.Round)
                )
            }
        }
        SlotSymbol.BELL -> {
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = size.width
                val h = size.height
                val bellBody = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    cubicTo(w * 0.74f, h * 0.16f, w * 0.76f, h * 0.48f, w * 0.82f, h * 0.68f)
                    lineTo(w * 0.18f, h * 0.68f)
                    cubicTo(w * 0.24f, h * 0.48f, w * 0.26f, h * 0.16f, w * 0.50f, h * 0.16f)
                    close()
                }
                drawPath(path = bellBody, color = Color(0xFFFBBF24))
                drawRoundRect(
                    color = GoldBezelLight,
                    topLeft = Offset(w * 0.14f, h * 0.65f),
                    size = Size(w * 0.72f, h * 0.11f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawCircle(
                    color = Color(0xFFD97706),
                    radius = w * 0.10f,
                    center = Offset(w * 0.50f, h * 0.82f)
                )
            }
        }
        SlotSymbol.CHERRY -> {
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = size.width
                val h = size.height
                val stemPath = Path().apply {
                    moveTo(w * 0.30f, h * 0.62f)
                    quadraticBezierTo(w * 0.45f, h * 0.22f, w * 0.56f, h * 0.16f)
                    moveTo(w * 0.70f, h * 0.64f)
                    quadraticBezierTo(w * 0.58f, h * 0.28f, w * 0.56f, h * 0.16f)
                }
                drawPath(
                    path = stemPath,
                    color = VerdeExito,
                    style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = w * 0.19f,
                    center = Offset(w * 0.30f, h * 0.68f)
                )
                drawCircle(
                    color = Color(0xFFFF5252),
                    radius = w * 0.19f,
                    center = Offset(w * 0.70f, h * 0.70f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = w * 0.045f,
                    center = Offset(w * 0.24f, h * 0.61f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = w * 0.045f,
                    center = Offset(w * 0.64f, h * 0.63f)
                )
            }
        }
        SlotSymbol.GRAPES -> {
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = size.width
                val h = size.height
                drawLine(
                    color = VerdeExito,
                    start = Offset(w * 0.50f, h * 0.14f),
                    end = Offset(w * 0.50f, h * 0.30f),
                    strokeWidth = w * 0.09f,
                    cap = StrokeCap.Round
                )
                val grapePurple = Color(0xFFA855F7)
                val grapeHighlight = Color(0xFFC084FC)
                val centers = listOf(
                    Offset(w * 0.34f, h * 0.36f),
                    Offset(w * 0.50f, h * 0.36f),
                    Offset(w * 0.66f, h * 0.36f),
                    Offset(w * 0.42f, h * 0.53f),
                    Offset(w * 0.58f, h * 0.53f),
                    Offset(w * 0.50f, h * 0.71f)
                )
                centers.forEachIndexed { idx, c ->
                    drawCircle(
                        color = if (idx % 2 == 0) grapePurple else grapeHighlight,
                        radius = w * 0.14f,
                        center = c
                    )
                }
            }
        }
        SlotSymbol.LEMON -> {
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = size.width
                val h = size.height
                drawOval(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(w * 0.14f, h * 0.26f),
                    size = Size(w * 0.72f, h * 0.52f)
                )
                drawCircle(
                    color = Color(0xFFFFEE58),
                    radius = w * 0.08f,
                    center = Offset(w * 0.16f, h * 0.52f)
                )
                drawCircle(
                    color = Color(0xFFFFEE58),
                    radius = w * 0.08f,
                    center = Offset(w * 0.84f, h * 0.52f)
                )
                val leaf = Path().apply {
                    moveTo(w * 0.50f, h * 0.26f)
                    quadraticBezierTo(w * 0.64f, h * 0.08f, w * 0.74f, h * 0.22f)
                    quadraticBezierTo(w * 0.60f, h * 0.28f, w * 0.50f, h * 0.26f)
                    close()
                }
                drawPath(path = leaf, color = VerdeExito)
            }
        }
    }
}
