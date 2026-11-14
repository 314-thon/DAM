package com.ifts18.unadecisionmas.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.RouletteGameState
import com.ifts18.unadecisionmas.sensor.HapticFeedbackHelper
import com.ifts18.unadecisionmas.sensor.rememberAccelerometerMonitor
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private data class RoulettePocket(
    val number: Int,
    val colorName: String,
    val pocketColor: Color
)

private data class CasinoChipSpec(
    val label: String,
    val baseColor: Color,
    val rimColor: Color,
    val textColor: Color
)

// Paleta inspirada en European Roulette (Evoplay)
private val CasinoBgTop = Color(0xFF1A0A1E)
private val CasinoBgMid = Color(0xFF280D2C)
private val CasinoBgBottom = Color(0xFF0A1F10)
private val MahoganyLight = Color(0xFF6E2C14)
private val MahoganyDark = Color(0xFF331105)
private val FeltCenter = Color(0xFF0B7D2A)
private val FeltEdge = Color(0xFF044A18)
private val FeltGridBorder = Color(0xFF43B560)
private val PocketGreen = Color(0xFF0D7B3E)
private val PocketRed = Color(0xFFD21417)
private val PocketBlack = Color(0xFF151515)
private val BrassGoldLight = Color(0xFFFFE58F)
private val BrassGold = Color(0xFFE5B338)
private val BrassGoldDark = Color(0xFF8C6214)
private val CrimsonBtnLight = Color(0xFF9C161A)
private val CrimsonBtnDark = Color(0xFF56090B)

private val RedNumbers = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
private val TiersNumbers = setOf(27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33)
private val OrphelinsNumbers = setOf(1, 20, 14, 31, 9, 17, 34, 6)
private val VoisinsNumbers = setOf(22, 18, 29, 7, 28, 12, 35, 3, 26, 0, 32, 15, 19, 4, 21, 2, 25)

private val EuropeanWheelOrder = listOf(
    0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23,
    10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26
)

private fun buildEuropeanPockets(): List<RoulettePocket> = EuropeanWheelOrder.map { num ->
    when {
        num == 0 -> RoulettePocket(0, "Verde", PocketGreen)
        num in RedNumbers -> RoulettePocket(num, "Rojo", PocketRed)
        else -> RoulettePocket(num, "Negro", PocketBlack)
    }
}

private fun doesPocketWinBet(
    pocket: RoulettePocket,
    betMode: String,
    selectedNumber: Int
): Boolean {
    val n = pocket.number
    return when (betMode) {
        "PLENO" -> n == selectedNumber
        "ROJO" -> pocket.colorName == "Rojo"
        "NEGRO" -> pocket.colorName == "Negro"
        "EVEN" -> n != 0 && n % 2 == 0
        "ODD" -> n != 0 && n % 2 == 1
        "LOW_1_18" -> n in 1..18
        "HIGH_19_36" -> n in 19..36
        "DOZEN_1" -> n in 1..12
        "DOZEN_2" -> n in 13..24
        "DOZEN_3" -> n in 25..36
        "COL_1" -> n != 0 && n % 3 == 1
        "COL_2" -> n != 0 && n % 3 == 2
        "COL_3" -> n != 0 && n % 3 == 0
        "TIERS" -> n in TiersNumbers
        "ORPHELINS" -> n in OrphelinsNumbers
        "VOISINS" -> n in VoisinsNumbers
        else -> n == selectedNumber
    }
}

private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

private fun tightStyle(fontSize: TextUnit, lineHeight: TextUnit = fontSize): TextStyle =
    TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = fontSize,
        lineHeight = lineHeight,
        platformStyle = NoFontPadding
    )

private fun betDisplayLabel(betMode: String, selectedNumber: Int): String = when (betMode) {
    "PLENO" -> "Pleno #$selectedNumber"
    "ROJO" -> "Rojo x2"
    "NEGRO" -> "Negro x2"
    "EVEN" -> "Par"
    "ODD" -> "Impar"
    "LOW_1_18" -> "1~18"
    "HIGH_19_36" -> "19~36"
    "DOZEN_1" -> "1.ª Docena"
    "DOZEN_2" -> "2.ª Docena"
    "DOZEN_3" -> "3.ª Docena"
    "COL_1" -> "Columna 1"
    "COL_2" -> "Columna 2"
    "COL_3" -> "Columna 3"
    "TIERS" -> "Tercios"
    "ORPHELINS" -> "Huérfanos"
    "VOISINS" -> "Vecinos 0"
    else -> "Pleno #$selectedNumber"
}

/**
 * JUEGO 4: "La Ruleta del 29"
 * Simulador basado en la arquitectura visual y de flujo de European Roulette (Evoplay):
 * rueda europea de 37 casilleros con bolilla física, racetrack francés (Tiers/Orphelins/Voisins),
 * paño verde esmeralda completo (0-36 + externas), dolly de bronce dorado, historial Previous
 * y explicación pedagógica en popup modal (sin scroll en la pantalla principal).
 */
@Composable
fun RouletteTrapGameScreen(
    state: RouletteGameState,
    onStateChange: (RouletteGameState) -> Unit,
    onBackToHub: () -> Unit,
    onGameFinished: (stoppedEarly: Boolean, spinsUsed: Int, remainingChips: Int, realMoneyLost: Int, elapsedMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val wheelPockets = remember { buildEuropeanPockets() }
    val chipSpecs = remember {
        listOf(
            CasinoChipSpec("0.1", Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFF151515)),
            CasinoChipSpec("0.5", Color(0xFF78909C), Color(0xFFCFD8DC), Color(0xFFFFFFFF)),
            CasinoChipSpec("1", Color(0xFF263238), Color(0xFFECEFF1), Color(0xFFFFFFFF)),
            CasinoChipSpec("5", Color(0xFFC62828), Color(0xFFFFFFFF), Color(0xFFFFFFFF)),
            CasinoChipSpec("10", Color(0xFF1565C0), Color(0xFFFFFFFF), Color(0xFFFFFFFF))
        )
    }
    var selectedChipIndex by remember { mutableIntStateOf(3) } // Ficha "5" por defecto
    val activeChip = chipSpecs[selectedChipIndex]

    val selectedNumberIndex = state.selectedNumberIndex.coerceIn(0, wheelPockets.lastIndex)
    val selectedPocket = wheelPockets[selectedNumberIndex]
    val selectedBetMode = state.selectedBetMode
    val landedPocket = wheelPockets[state.landedPocketIndex.coerceIn(0, wheelPockets.lastIndex)]

    val sliceAngle = 360f / wheelPockets.size
    var wheelRotation by remember { mutableFloatStateOf(0f) }

    var isSpinning by remember { mutableStateOf(false) }
    var showResultPill by remember { mutableStateOf(false) }
    var showResultPopup by remember { mutableStateOf(false) }
    var ballVisible by remember { mutableStateOf(state.spinsUsed > 0) }
    var ballRelativeAngle by remember {
        mutableFloatStateOf(-90f + (state.landedPocketIndex.coerceIn(0, wheelPockets.lastIndex) + 0.5f) * sliceAngle)
    }
    var ballRadiusFraction by remember { mutableFloatStateOf(0.565f) }

    val recentHistory = remember { mutableStateListOf<RoulettePocket>() }
    var previousBetBackup by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var betMultiplier by remember { mutableIntStateOf(1) }

    val spinsUsed = state.spinsUsed
    val maxSpins = state.maxSpins
    val bonusChips = state.bonusChips
    val fakeLockedWinnings = state.fakeLockedWinnings
    val realMoneyLost = state.realMoneyLost
    val resultBannerText = state.resultBannerText
    val algorithmStageTitle = state.algorithmStageTitle
    val algorithmExplanation = state.algorithmExplanation

    val currentState by rememberUpdatedState(state)
    var sessionOpenedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var activeBeforePauseMillis by remember { mutableStateOf(state.elapsedMillis) }
    var isLifecycleResumed by remember { mutableStateOf(true) }
    var hasFinished by remember { mutableStateOf(false) }

    // Giro suave continuo en reposo como Evoplay European Roulette
    LaunchedEffect(isSpinning, isLifecycleResumed) {
        if (!isSpinning && isLifecycleResumed) {
            var lastNanos = 0L
            while (!isSpinning && isLifecycleResumed) {
                val dtSec = withFrameNanos { nanos ->
                    val delta = if (lastNanos != 0L) (nanos - lastNanos) / 1_000_000_000f else 0f
                    lastNanos = nanos
                    delta
                }
                if (!isSpinning && dtSec > 0f) {
                    wheelRotation = (wheelRotation + dtSec * 14f) % 360f
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleResumed = true
                    sessionOpenedAt = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
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

    fun selectBet(mode: String, numberIndex: Int = currentState.selectedNumberIndex) {
        if (isSpinning || showResultPopup) return
        SoundManager.playClick()
        previousBetBackup = currentState.selectedBetMode to currentState.selectedNumberIndex
        showResultPill = false
        onStateChange(
            currentState.copy(
                selectedBetMode = mode,
                selectedNumberIndex = numberIndex.coerceIn(0, wheelPockets.lastIndex),
                elapsedMillis = currentTotalElapsed()
            )
        )
    }

    fun resolveTargetPocketIndex(snapshot: RouletteGameState, nextSpinNumber: Int): Int {
        val selIdx = snapshot.selectedNumberIndex.coerceIn(0, wheelPockets.lastIndex)
        val selNum = wheelPockets[selIdx].number
        val mode = snapshot.selectedBetMode

        return when (nextSpinNumber) {
            // Etapa 1: Gancho inicial (acierta la apuesta elegida)
            1, 2 -> {
                if (mode == "PLENO") {
                    selIdx
                } else {
                    val winningIndices = wheelPockets.indices.filter { idx ->
                        doesPocketWinBet(wheelPockets[idx], mode, selNum)
                    }
                    if (winningIndices.isNotEmpty()) {
                        winningIndices[(nextSpinNumber * 3) % winningIndices.size]
                    } else {
                        selIdx
                    }
                }
            }
            // Etapa 2: Casi-acierto (frena al lado en rueda o falla por margen mínimo)
            3, 4, 5, 6 -> {
                if (mode == "PLENO") {
                    val offset = if (nextSpinNumber % 2 == 1) -1 else 1
                    (selIdx + offset + wheelPockets.size) % wheelPockets.size
                } else {
                    val losingNonZero = wheelPockets.indices.filter { idx ->
                        idx != 0 && !doesPocketWinBet(wheelPockets[idx], mode, selNum)
                    }
                    if (losingNonZero.isNotEmpty()) {
                        losingNonZero[(nextSpinNumber * 5) % losingNonZero.size]
                    } else {
                        (selIdx + 1) % wheelPockets.size
                    }
                }
            }
            // Etapa 3: Ventaja matemática de la banca (0 Verde)
            else -> 0
        }
    }

    fun triggerRouletteSpin() {
        val snapshot = currentState
        if (isSpinning || !isLifecycleResumed || showResultPopup || snapshot.spinsUsed >= snapshot.maxSpins || snapshot.bonusChips <= 0) {
            return
        }
        isSpinning = true
        showResultPopup = false
        showResultPill = false
        ballVisible = true

        SoundManager.playSlotSpinStart(turbo = false)
        HapticFeedbackHelper.vibrateShort(context, 55L)

        scope.launch {
            val nextSpinNumber = snapshot.spinsUsed + 1
            val currentSelIdx = snapshot.selectedNumberIndex.coerceIn(0, wheelPockets.lastIndex)
            val currentSelPocket = wheelPockets[currentSelIdx]
            val targetPocketIndex = resolveTargetPocketIndex(snapshot, nextSpinNumber)

            val startWheel = wheelRotation
            val relStart = ballRelativeAngle
            val relTarget = -90f + (targetPocketIndex + 0.5f) * sliceAngle
            val relMod = (((relStart - relTarget) % 360f) + 360f) % 360f
            val relEnd = relStart - 2880f - relMod

            val animProgress = Animatable(0f)

            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3600, easing = LinearEasing)
            ) {
                val t = value
                // Desaceleración continua del rotor (termina con la misma velocidad residual de reposo ~14°/s)
                val wheelDecel = 1f - (1f - t).pow(2.15f)
                wheelRotation = (startWheel + 1380f * wheelDecel + 50f * t) % 360f

                // Desaceleración progresiva de la bolilla en sentido inverso sin doble curva
                val u = (t / 0.93f).coerceIn(0f, 1f)
                val easedU = 1f - (1f - u).pow(2.35f)
                val settleOscillation = if (t > 0.93f) {
                    val s = (t - 0.93f) / 0.07f
                    (sin(s * PI * 2.0) * (1f - s) * 1.6f).toFloat()
                } else {
                    0f
                }
                ballRelativeAngle = relStart + (relEnd - relStart) * easedU + settleOscillation

                ballRadiusFraction = when {
                    u < 0.68f -> 0.875f
                    u < 1.0f -> {
                        val dropU = (u - 0.68f) / 0.32f
                        val bounce = abs(sin(dropU * PI * 3.0)).toFloat() * (1f - dropU) * 0.068f
                        0.875f - (0.875f - 0.565f) * dropU + bounce
                    }
                    else -> 0.565f
                }
            }

            ballRelativeAngle = relTarget
            ballRadiusFraction = 0.565f

            SoundManager.stopSlotSpinLoop()
            SoundManager.playSlotReelStop(2)

            val landed = wheelPockets[targetPocketIndex]
            recentHistory.add(landed)
            if (recentHistory.size > 5) {
                recentHistory.removeAt(0)
            }

            val updatedChips = (snapshot.bonusChips - 1).coerceAtLeast(0)
            val totalElapsed = currentTotalElapsed()
            val betLabel = betDisplayLabel(snapshot.selectedBetMode, currentSelPocket.number)

            val nextState = when (nextSpinNumber) {
                1, 2 -> {
                    SoundManager.playSlotWinChime()
                    snapshot.copy(
                        landedPocketIndex = targetPocketIndex,
                        spinsUsed = nextSpinNumber,
                        bonusChips = updatedChips,
                        fakeLockedWinnings = snapshot.fakeLockedWinnings + 4000,
                        elapsedMillis = totalElapsed,
                        resultBannerText = "+$4.000 BONO TRABADO",
                        algorithmStageTitle = "Etapa 1: Gancho inicial",
                        algorithmExplanation = "Acierto en $betLabel (#${landed.number} ${landed.colorName}). Te dejan ganar al inicio, pero el premio queda bloqueado bajo rollover."
                    )
                }
                3, 4, 5, 6 -> {
                    SoundManager.playSlotNearMiss()
                    snapshot.copy(
                        landedPocketIndex = targetPocketIndex,
                        spinsUsed = nextSpinNumber,
                        bonusChips = updatedChips,
                        elapsedMillis = totalElapsed,
                        resultBannerText = "CASI ACIERTO • #${landed.number}",
                        algorithmStageTitle = "Etapa 2: Efecto casi-acierto",
                        algorithmExplanation = "Apostaste a $betLabel y cayó #${landed.number} ${landed.colorName}. Cada giro es independiente y no existe racha ni compensación."
                    )
                }
                else -> {
                    SoundManager.playSlotNearMiss()
                    snapshot.copy(
                        landedPocketIndex = targetPocketIndex,
                        spinsUsed = nextSpinNumber,
                        bonusChips = updatedChips,
                        fakeLockedWinnings = (snapshot.fakeLockedWinnings - 2000).coerceAtLeast(0),
                        elapsedMillis = totalElapsed,
                        resultBannerText = "SIN PREMIO • 0 VERDE",
                        algorithmStageTitle = "Etapa 3: Ventaja de la banca",
                        algorithmExplanation = "Cayó el 0 Verde. Ese casillero extra (1/37 = 2,70%) asegura que la casa gane en el largo plazo."
                    )
                }
            }

            onStateChange(nextState)
            HapticFeedbackHelper.vibrateShort(context, 45L)
            isSpinning = false
            showResultPill = true
            delay(850L)
            showResultPopup = true
        }
    }

    val sensorReading by rememberAccelerometerMonitor(
        enabled = !isSpinning && isLifecycleResumed && !showResultPopup && spinsUsed < maxSpins,
        shakeThresholdG = 1.42f,
        minIntervalMs = 600L
    ) {
        triggerRouletteSpin()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CasinoBgTop, CasinoBgMid, CasinoBgBottom)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. CABECERA COMPACTA ESTILO CASINO + LECTURA DE SENSOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF140819).copy(alpha = 0.88f))
                    .border(1.dp, BrassGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Ruleta Europea",
                            style = tightStyle(15.sp, 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = BrassGoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Agitá para girar • ${"%.2f".format(sensorReading.gForce)} G",
                            style = tightStyle(10.5.sp, 12.sp),
                            color = BordeCyan,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(BordeMagenta.copy(alpha = 0.22f))
                        .border(1.dp, BordeMagenta, RoundedCornerShape(9.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Fichas: $bonusChips/$maxSpins",
                        style = tightStyle(11.5.sp, 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // 2. ZONA SUPERIOR: SECTORES + RUEDA EUROPEA 37 CASILLEROS + PANEL DE APUESTA ACTIVA
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Izquierda: Sectores de la rueda (Tercios / Huérfanos / Vecinos 0)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(listOf(FeltCenter, FeltEdge))
                            )
                            .border(1.4.dp, BrassGold.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "SECTORES",
                            style = tightStyle(9.sp, 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = BrassGoldLight,
                            letterSpacing = 0.8.sp
                        )
                        listOf(
                            "TIERS" to "Tercios",
                            "ORPHELINS" to "Huérfanos",
                            "VOISINS" to "Vecinos 0"
                        ).forEach { (modeKey, title) ->
                            val isSelected = selectedBetMode == modeKey
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(29.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (isSelected) BrassGold.copy(alpha = 0.30f)
                                        else Color.Black.copy(alpha = 0.32f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.9.dp,
                                        color = if (isSelected) BrassGoldLight else FeltGridBorder,
                                        shape = RoundedCornerShape(9.dp)
                                    )
                                    .clickable(enabled = !isSpinning && !showResultPopup) {
                                        selectBet(modeKey)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = tightStyle(10.sp, 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BrassGoldLight else Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Centro: Rueda Europea Oficial de 37 Casilleros en Canvas + Bolilla física
                    EuropeanRouletteWheelCanvas(
                        wheelPockets = wheelPockets,
                        wheelRotationDegrees = wheelRotation,
                        ballVisible = ballVisible,
                        ballRelativeAngle = ballRelativeAngle,
                        ballRadiusFraction = ballRadiusFraction,
                        modifier = Modifier.size(184.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Derecha: Placa de Apuesta Activa y Valor de Ficha
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF141519))
                            .border(1.4.dp, BrassGold.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TU APUESTA",
                            style = tightStyle(8.5.sp, 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 0.6.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrassGold.copy(alpha = 0.16f))
                                .border(1.dp, BrassGold.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = betDisplayLabel(selectedBetMode, selectedPocket.number),
                                style = tightStyle(10.5.sp, 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = BrassGoldLight,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${activeChip.label} = 1 Ficha",
                            style = tightStyle(9.5.sp, 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                        Text(
                            text = "Tirada $spinsUsed/$maxSpins",
                            style = tightStyle(9.sp, 10.sp),
                            color = BordeCyan,
                            maxLines = 1
                        )
                    }
                }

                // Barra horizontal de "Últimos:" con 5 círculos perfectos y números centrados
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .border(1.dp, BrassGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Últimos:",
                        style = tightStyle(11.sp, 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = BrassGoldLight
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val paddedHistory = List(5) { idx ->
                            val offset = recentHistory.size - 5 + idx
                            if (offset >= 0) recentHistory[offset] else null
                        }
                        paddedHistory.forEach { pocket ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(pocket?.pocketColor ?: Color.White.copy(alpha = 0.05f))
                                    .border(
                                        width = 1.1.dp,
                                        color = if (pocket != null) Color.White.copy(alpha = 0.85f)
                                        else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pocket != null) {
                                    Text(
                                        text = "${pocket.number}",
                                        style = tightStyle(10.sp, 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. PAÑO EUROPEO COMPLETO DE CAOBAS + FELPA VERDE ESMERALDA (0..36 + EXTERNAS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(listOf(MahoganyLight, MahoganyDark))
                    )
                    .border(1.6.dp, BrassGold.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(FeltCenter, FeltEdge)
                            )
                        )
                        .border(1.dp, FeltGridBorder.copy(alpha = 0.65f), RoundedCornerShape(11.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EuropeanBettingTable(
                        wheelPockets = wheelPockets,
                        selectedBetMode = selectedBetMode,
                        selectedNumber = selectedPocket.number,
                        landedNumber = if (!isSpinning && spinsUsed > 0) landedPocket.number else null,
                        activeChip = activeChip,
                        betMultiplier = betMultiplier,
                        enabled = !isSpinning && !showResultPopup,
                        onSelectPleno = { num ->
                            val idx = wheelPockets.indexOfFirst { it.number == num }
                            if (idx >= 0) selectBet("PLENO", idx)
                        },
                        onSelectOutside = { mode -> selectBet(mode) }
                    )

                    // Píldora flotante al frenar la bolilla
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showResultPill && !showResultPopup,
                        enter = fadeIn(tween(180)) + scaleIn(tween(200), initialScale = 0.9f),
                        exit = fadeOut(tween(180))
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xEE101218))
                                .border(1.5.dp, BrassGold, RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(landedPocket.pocketColor)
                                    .border(1.6.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${landedPocket.number}",
                                    style = tightStyle(14.sp, 14.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = resultBannerText,
                                style = tightStyle(12.sp, 14.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 4. CONTROLES INFERIORES: FICHAS + BOTONES CARMESÍ/ORO + HUD
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (spinsUsed < maxSpins) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Selector de fichas (0.1, 0.5, 1, 5, 10)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            chipSpecs.forEachIndexed { idx, spec ->
                                CasinoChipButton(
                                    spec = spec,
                                    isSelected = idx == selectedChipIndex,
                                    enabled = !isSpinning && !showResultPopup,
                                    size = 36.dp,
                                    onClick = {
                                        SoundManager.playClick()
                                        selectedChipIndex = idx
                                    }
                                )
                            }
                        }

                        // Botones circulares carmesí con bisel dorado (Deshacer, Reiniciar, x2) + Girar Grande
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CrimsonRoundButton(
                                size = 36.dp,
                                enabled = !isSpinning && !showResultPopup && previousBetBackup != null,
                                onClick = {
                                    SoundManager.playClick()
                                    previousBetBackup?.let { (prevMode, prevIdx) ->
                                        previousBetBackup = null
                                        onStateChange(
                                            currentState.copy(
                                                selectedBetMode = prevMode,
                                                selectedNumberIndex = prevIdx,
                                                elapsedMillis = currentTotalElapsed()
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Deshacer",
                                    tint = BrassGoldLight,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            CrimsonRoundButton(
                                size = 36.dp,
                                enabled = !isSpinning && !showResultPopup,
                                onClick = {
                                    SoundManager.playClick()
                                    betMultiplier = 1
                                    val idx29 = wheelPockets.indexOfFirst { it.number == 29 }.coerceAtLeast(0)
                                    selectBet("PLENO", idx29)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reiniciar apuesta al 29",
                                    tint = BrassGoldLight,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            CrimsonRoundButton(
                                size = 36.dp,
                                enabled = !isSpinning && !showResultPopup,
                                onClick = {
                                    SoundManager.playClick()
                                    betMultiplier = if (betMultiplier == 1) 2 else 1
                                }
                            ) {
                                if (spinsUsed > 0 && betMultiplier == 1) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "Repetir apuesta",
                                        tint = BrassGoldLight,
                                        modifier = Modifier.size(17.dp)
                                    )
                                } else {
                                    Text(
                                        text = "×${betMultiplier * 2}",
                                        style = tightStyle(11.sp, 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = BrassGoldLight,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Botón principal GIRAR
                            CrimsonRoundButton(
                                size = 52.dp,
                                enabled = !isSpinning && !showResultPopup,
                                borderWidth = 2.4.dp,
                                onClick = { triggerRouletteSpin() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Girar ruleta",
                                    tint = BrassGoldLight,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Sin fichas: trampa de depósito real
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Sin fichas de bono. El casino te pide cargar plata real:",
                            style = tightStyle(11.5.sp, 14.sp),
                            fontWeight = FontWeight.Bold,
                            color = RojoAlerta,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(RojoAlerta.copy(alpha = 0.22f))
                                .border(1.2.dp, RojoAlerta, RoundedCornerShape(11.dp))
                                .clickable {
                                    SoundManager.playClick()
                                    hasFinished = true
                                    onGameFinished(
                                        false,
                                        spinsUsed,
                                        bonusChips,
                                        realMoneyLost + 5000,
                                        currentTotalElapsed()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cargar $5.000 reales para seguir",
                                style = tightStyle(12.sp, 14.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Píldoras inferiores (APUESTA / BONO TRABADO) + Plantarse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudDarkPill(
                        label = "APUESTA",
                        value = "${activeChip.label} ×$betMultiplier",
                        valueColor = BrassGoldLight,
                        modifier = Modifier.weight(1f)
                    )
                    HudDarkPill(
                        label = "BONO TRABADO",
                        value = "$$fakeLockedWinnings",
                        valueColor = BordeMagenta,
                        showLock = true,
                        modifier = Modifier.weight(1.1f)
                    )

                    if (spinsUsed >= 1) {
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                                .clickable {
                                    SoundManager.playClick()
                                    hasFinished = true
                                    onGameFinished(
                                        spinsUsed < maxSpins,
                                        spinsUsed,
                                        bonusChips,
                                        realMoneyLost,
                                        currentTotalElapsed()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (spinsUsed < maxSpins) "Plantarse" else "Cerrar",
                                    style = tightStyle(12.5.sp, 14.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, BordeCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isSpinning) { triggerRouletteSpin() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = BordeCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isSpinning) "Girando" else "Agitar",
                                    style = tightStyle(12.5.sp, 14.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // OVERLAY MODAL DE EXPLICACIÓN POR CADA TIRADA (APARECE AL TERMINAR EL GIRO Y SE CIERRA PARA SEGUIR)
        AnimatedVisibility(
            visible = showResultPopup,
            enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.92f),
            exit = fadeOut(tween(180))
        ) {
            val stageColor = when {
                spinsUsed <= 2 -> VerdeExito
                spinsUsed <= 6 -> Color(0xFFFFD54F)
                else -> RojoAlerta
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.76f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showResultPopup = false
                        showResultPill = false
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141B29))
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(listOf(stageColor, BrassGold)),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(stageColor.copy(alpha = 0.18f))
                                .border(1.dp, stageColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "TIRADA $spinsUsed / $maxSpins",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = stageColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(landedPocket.pocketColor)
                                .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Salió #${landedPocket.number} ${landedPocket.colorName}",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = resultBannerText,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0E131D))
                            .border(1.dp, BrassGold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = algorithmStageTitle,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = stageColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = algorithmExplanation,
                            fontFamily = SpaceGrotesk,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Brush.horizontalGradient(listOf(CrimsonBtnLight, BrassGoldDark)))
                            .border(1.dp, BrassGold, RoundedCornerShape(13.dp))
                            .clickable {
                                SoundManager.playClick()
                                showResultPopup = false
                                showResultPill = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (spinsUsed < maxSpins) "Seguir tirando (${maxSpins - spinsUsed})" else "Cerrar",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    if (spinsUsed < maxSpins) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VerdeExito.copy(alpha = 0.16f))
                                .border(1.dp, VerdeExito, RoundedCornerShape(12.dp))
                                .clickable {
                                    SoundManager.playClick()
                                    showResultPopup = false
                                    showResultPill = false
                                    hasFinished = true
                                    onGameFinished(
                                        true,
                                        spinsUsed,
                                        bonusChips,
                                        realMoneyLost,
                                        currentTotalElapsed()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Plantarse a tiempo",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = VerdeExito
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EuropeanRouletteWheelCanvas(
    wheelPockets: List<RoulettePocket>,
    wheelRotationDegrees: Float,
    ballVisible: Boolean,
    ballRelativeAngle: Float,
    ballRadiusFraction: Float,
    modifier: Modifier = Modifier
) {
    val numberPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val sweepAngle = 360f / wheelPockets.size

        // 1. Aro exterior de madera caoba y pista de la bolilla
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MahoganyDark, MahoganyLight, MahoganyDark),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = BrassGold,
            radius = radius * 0.985f,
            center = center,
            style = Stroke(width = 2.4f)
        )
        drawCircle(
            color = Color(0xFF1F0A03),
            radius = radius * 0.875f,
            center = center,
            style = Stroke(width = radius * 0.08f)
        )

        // 8 deflectores metálicos en la pista exterior
        for (i in 0 until 8) {
            val angleRad = Math.toRadians(i * 45.0)
            val dx = center.x + (cos(angleRad) * radius * 0.845f).toFloat()
            val dy = center.y + (sin(angleRad) * radius * 0.845f).toFloat()
            drawCircle(
                color = BrassGoldLight,
                radius = radius * 0.025f,
                center = Offset(dx, dy)
            )
        }

        // 2. Rotor con los 37 bolsillos europeos + torreta central dorada
        rotate(degrees = wheelRotationDegrees, pivot = center) {
            val outerPocketR = radius * 0.815f
            val innerPocketR = radius * 0.50f
            val numberRingR = radius * 0.725f
            numberPaint.textSize = radius * 0.095f

            wheelPockets.forEachIndexed { idx, pocket ->
                val startAngle = -90f + idx * sweepAngle
                val midAngle = startAngle + sweepAngle / 2f

                // Sector completo del número
                drawArc(
                    color = pocket.pocketColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - outerPocketR, center.y - outerPocketR),
                    size = Size(outerPocketR * 2f, outerPocketR * 2f)
                )

                // Pozo interior ligeramente sombreado donde cae la bolilla
                drawArc(
                    color = Color.Black.copy(alpha = 0.24f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius * 0.61f, center.y - radius * 0.61f),
                    size = Size(radius * 1.22f, radius * 1.22f)
                )

                // Divisor metálico dorado entre bolsillos
                val rad = Math.toRadians(startAngle.toDouble())
                drawLine(
                    color = BrassGold.copy(alpha = 0.85f),
                    start = Offset(
                        center.x + (cos(rad) * innerPocketR).toFloat(),
                        center.y + (sin(rad) * innerPocketR).toFloat()
                    ),
                    end = Offset(
                        center.x + (cos(rad) * outerPocketR).toFloat(),
                        center.y + (sin(rad) * outerPocketR).toFloat()
                    ),
                    strokeWidth = 1.6f
                )

                // Número rotado radialmente
                rotate(degrees = midAngle + 90f, pivot = center) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${pocket.number}",
                        center.x,
                        center.y - numberRingR + numberPaint.textSize * 0.35f,
                        numberPaint
                    )
                }
            }

            // Aros dorados separadores de banda numérica y pozo
            drawCircle(
                color = BrassGold,
                radius = outerPocketR,
                center = center,
                style = Stroke(width = 2.2f)
            )
            drawCircle(
                color = BrassGold.copy(alpha = 0.75f),
                radius = radius * 0.615f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Cono central dorado cepillado
            val coneR = radius * 0.50f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrassGoldLight, BrassGold, BrassGoldDark),
                    center = Offset(center.x - coneR * 0.22f, center.y - coneR * 0.22f),
                    radius = coneR * 1.3f
                ),
                radius = coneR,
                center = center
            )
            drawCircle(
                color = BrassGoldDark,
                radius = coneR,
                center = center,
                style = Stroke(width = 2f)
            )

            // 4 brazos de la torreta central estilo Evoplay
            val armLength = radius * 0.26f
            for (k in 0 until 4) {
                val aRad = Math.toRadians(k * 90.0 + 45.0)
                val endX = center.x + (cos(aRad) * armLength).toFloat()
                val endY = center.y + (sin(aRad) * armLength).toFloat()
                drawLine(
                    color = BrassGoldDark,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = radius * 0.055f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = BrassGoldLight,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = radius * 0.025f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = BrassGoldLight,
                    radius = radius * 0.045f,
                    center = Offset(endX, endY)
                )
            }

            // Eje central de la torreta
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, BrassGold, BrassGoldDark)),
                radius = radius * 0.11f,
                center = center
            )
        }

        // 3. Bolilla de marfil girando / alojada en el bolsillo ganador
        if (ballVisible) {
            val worldBallRad = Math.toRadians((wheelRotationDegrees + ballRelativeAngle).toDouble())
            val bx = center.x + (cos(worldBallRad) * radius * ballRadiusFraction).toFloat()
            val by = center.y + (sin(worldBallRad) * radius * ballRadiusFraction).toFloat()
            val ballR = radius * 0.044f

            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = ballR * 1.15f,
                center = Offset(bx + 1.5f, by + 2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE0E0E0), Color(0xFF9E9E9E)),
                    center = Offset(bx - ballR * 0.3f, by - ballR * 0.3f),
                    radius = ballR * 1.5f
                ),
                radius = ballR,
                center = Offset(bx, by)
            )
        }
    }
}

@Composable
private fun EuropeanBettingTable(
    wheelPockets: List<RoulettePocket>,
    selectedBetMode: String,
    selectedNumber: Int,
    landedNumber: Int?,
    activeChip: CasinoChipSpec,
    betMultiplier: Int,
    enabled: Boolean,
    onSelectPleno: (Int) -> Unit,
    onSelectOutside: (String) -> Unit
) {
    val pocketByNum = remember(wheelPockets) { wheelPockets.associateBy { it.number } }
    val rowHeight = 34.dp
    val zeroWidth = 26.dp
    val colBetWidth = 28.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Fila superior principal: [ 0 ] + Grilla 3x12 + [ 2~1 ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Casillero 0 Verde
            val zeroPocket = pocketByNum[0] ?: RoulettePocket(0, "Verde", PocketGreen)
            val isZeroHighlighted = isNumberHighlighted(0, selectedBetMode, selectedNumber)
            val isZeroDirectBet = selectedBetMode == "PLENO" && selectedNumber == 0
            TableCellBox(
                backgroundColor = zeroPocket.pocketColor,
                isHighlighted = isZeroHighlighted,
                showChip = isZeroDirectBet,
                showDolly = landedNumber == 0,
                activeChip = activeChip,
                betMultiplier = betMultiplier,
                modifier = Modifier
                    .width(zeroWidth)
                    .height(rowHeight * 3)
                    .clickable(enabled = enabled) { onSelectPleno(0) }
            ) {
                Text(
                    text = "0",
                    style = tightStyle(12.5.sp, 12.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // 12 columnas x 3 filas (fila 0: 3..36, fila 1: 2..35, fila 2: 1..34)
            Column(modifier = Modifier.weight(1f)) {
                for (rowIdx in 0 until 3) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (colIdx in 0 until 12) {
                            val num = (colIdx + 1) * 3 - rowIdx
                            val pocket = pocketByNum[num] ?: RoulettePocket(num, "Negro", PocketBlack)
                            val isDirectBet = selectedBetMode == "PLENO" && selectedNumber == num
                            val isHighlighted = isNumberHighlighted(num, selectedBetMode, selectedNumber)

                            TableCellBox(
                                backgroundColor = pocket.pocketColor,
                                isHighlighted = isHighlighted,
                                showChip = isDirectBet,
                                showDolly = landedNumber == num,
                                activeChip = activeChip,
                                betMultiplier = betMultiplier,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(rowHeight)
                                    .clickable(enabled = enabled) { onSelectPleno(num) }
                            ) {
                                Text(
                                    text = "$num",
                                    style = tightStyle(11.sp, 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Columna derecha: tres botones 2~1
            Column(modifier = Modifier.width(colBetWidth)) {
                val colKeys = listOf("COL_3", "COL_2", "COL_1")
                colKeys.forEach { colKey ->
                    val isBetHere = selectedBetMode == colKey
                    TableCellBox(
                        backgroundColor = FeltEdge.copy(alpha = 0.65f),
                        isHighlighted = isBetHere,
                        showChip = isBetHere,
                        showDolly = false,
                        activeChip = activeChip,
                        betMultiplier = betMultiplier,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .clickable(enabled = enabled) { onSelectOutside(colKey) }
                    ) {
                        Text(
                            text = "2~1",
                            style = tightStyle(9.sp, 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Fila de Docenas (1.ª Docena, 2.ª Docena, 3.ª Docena)
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(zeroWidth))
            Row(modifier = Modifier.weight(1f)) {
                listOf(
                    "DOZEN_1" to "1.ª Docena",
                    "DOZEN_2" to "2.ª Docena",
                    "DOZEN_3" to "3.ª Docena"
                ).forEach { (key, label) ->
                    val isBetHere = selectedBetMode == key
                    TableCellBox(
                        backgroundColor = FeltEdge.copy(alpha = 0.55f),
                        isHighlighted = isBetHere,
                        showChip = isBetHere,
                        showDolly = false,
                        activeChip = activeChip,
                        betMultiplier = betMultiplier,
                        modifier = Modifier
                            .weight(1f)
                            .height(29.dp)
                            .clickable(enabled = enabled) { onSelectOutside(key) }
                    ) {
                        Text(
                            text = label,
                            style = tightStyle(10.5.sp, 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(colBetWidth))
        }

        // Fila inferior de Suertes Simples (1~18, Par, Rojo, Negro, Impar, 19~36)
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(zeroWidth))
            Row(modifier = Modifier.weight(1f)) {
                listOf(
                    "LOW_1_18" to "1~18",
                    "EVEN" to "Par",
                    "ROJO" to "RED_DIAMOND",
                    "NEGRO" to "BLACK_DIAMOND",
                    "ODD" to "Impar",
                    "HIGH_19_36" to "19~36"
                ).forEach { (key, label) ->
                    val isBetHere = selectedBetMode == key
                    TableCellBox(
                        backgroundColor = FeltEdge.copy(alpha = 0.55f),
                        isHighlighted = isBetHere,
                        showChip = isBetHere,
                        showDolly = false,
                        activeChip = activeChip,
                        betMultiplier = betMultiplier,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable(enabled = enabled) { onSelectOutside(key) }
                    ) {
                        if (label == "RED_DIAMOND" || label == "BLACK_DIAMOND") {
                            val diamondColor = if (label == "RED_DIAMOND") PocketRed else PocketBlack
                            Canvas(modifier = Modifier.size(width = 26.dp, height = 15.dp)) {
                                val p = Path().apply {
                                    moveTo(size.width / 2f, 1f)
                                    lineTo(size.width - 1f, size.height / 2f)
                                    lineTo(size.width / 2f, size.height - 1f)
                                    lineTo(1f, size.height / 2f)
                                    close()
                                }
                                drawPath(path = p, color = diamondColor)
                                drawPath(path = p, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = 1.1f))
                            }
                        } else {
                            Text(
                                text = label,
                                style = tightStyle(10.5.sp, 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(colBetWidth))
        }
    }
}

private fun isNumberHighlighted(num: Int, betMode: String, selectedNumber: Int): Boolean {
    return when (betMode) {
        "PLENO" -> num == selectedNumber
        "ROJO" -> num in RedNumbers
        "NEGRO" -> num != 0 && num !in RedNumbers
        "EVEN" -> num != 0 && num % 2 == 0
        "ODD" -> num != 0 && num % 2 == 1
        "LOW_1_18" -> num in 1..18
        "HIGH_19_36" -> num in 19..36
        "DOZEN_1" -> num in 1..12
        "DOZEN_2" -> num in 13..24
        "DOZEN_3" -> num in 25..36
        "COL_1" -> num != 0 && num % 3 == 1
        "COL_2" -> num != 0 && num % 3 == 2
        "COL_3" -> num != 0 && num % 3 == 0
        "TIERS" -> num in TiersNumbers
        "ORPHELINS" -> num in OrphelinsNumbers
        "VOISINS" -> num in VoisinsNumbers
        else -> false
    }
}

@Composable
private fun TableCellBox(
    backgroundColor: Color,
    isHighlighted: Boolean,
    showChip: Boolean,
    showDolly: Boolean,
    activeChip: CasinoChipSpec,
    betMultiplier: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .border(
                width = if (isHighlighted) 1.2.dp else 0.7.dp,
                color = if (isHighlighted) BrassGoldLight else FeltGridBorder
            ),
        contentAlignment = Alignment.Center
    ) {
        content()

        if (isHighlighted && !showChip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrassGold.copy(alpha = 0.16f))
            )
        }

        if (showChip) {
            MiniTableChip(spec = activeChip, showStack = betMultiplier > 1)
        }

        if (showDolly) {
            BrassDollyMarker(modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun MiniTableChip(spec: CasinoChipSpec, showStack: Boolean) {
    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showStack) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(spec.baseColor.copy(alpha = 0.85f))
                    .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
            )
        }
        Canvas(modifier = Modifier.size(18.dp)) {
            val r = size.minDimension / 2f
            drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = r, center = Offset(center.x, center.y + 1.2f))
            drawCircle(color = spec.baseColor, radius = r, center = center)
            drawCircle(
                color = spec.rimColor,
                radius = r - 1.5f,
                center = center,
                style = Stroke(
                    width = 2.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = r * 0.55f,
                center = center,
                style = Stroke(width = 0.9f)
            )
        }
        Text(
            text = spec.label,
            style = tightStyle(6.5.sp, 6.5.sp),
            fontWeight = FontWeight.Bold,
            color = spec.textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun BrassDollyMarker(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.44f
        drawCircle(
            color = Color.Black.copy(alpha = 0.55f),
            radius = r,
            center = Offset(cx + 1f, cy + 2f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BrassGoldLight, BrassGold, BrassGoldDark),
                center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                radius = r * 1.4f
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = r * 0.42f,
            center = Offset(cx - r * 0.12f, cy - r * 0.12f)
        )
    }
}

@Composable
private fun CasinoChipButton(
    spec: CasinoChipSpec,
    isSelected: Boolean,
    enabled: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 2.2.dp else 0.dp,
                color = if (isSelected) BordeCyan else Color.Transparent,
                shape = CircleShape
            )
            .padding(2.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(color = spec.baseColor, radius = r, center = center)
            drawCircle(
                color = spec.rimColor,
                radius = r - 2.5f,
                center = center,
                style = Stroke(
                    width = 3.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                )
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = r * 0.62f,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }
        Text(
            text = spec.label,
            style = tightStyle(10.sp, 10.sp),
            fontWeight = FontWeight.Bold,
            color = spec.textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun CrimsonRoundButton(
    size: Dp,
    enabled: Boolean,
    borderWidth: Dp = 1.6.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    if (enabled) listOf(CrimsonBtnLight, CrimsonBtnDark)
                    else listOf(Color(0xFF382C2E), Color(0xFF1F181A))
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    if (enabled) listOf(BrassGoldLight, BrassGoldDark)
                    else listOf(Color.Gray, Color.DarkGray)
                ),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun HudDarkPill(
    label: String,
    value: String,
    valueColor: Color,
    showLock: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD90A0D14))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = label,
            style = tightStyle(8.5.sp, 10.sp),
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showLock) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = valueColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = value,
                style = tightStyle(12.sp, 14.sp),
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
