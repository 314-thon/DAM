package com.ifts18.unadecisionmas.ui.screens

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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.ifts18.unadecisionmas.ui.components.BalanceWithdrawPill
import com.ifts18.unadecisionmas.ui.components.CashierDepositModal
import com.ifts18.unadecisionmas.ui.components.WithdrawBonusModal
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sin

private val GoldBezelLight = Color(0xFFFFE082)
private val GoldBezelDark = Color(0xFFB45309)
private val CrimsonCabinetTop = Color(0xFF4A0710)
private val CrimsonCabinetBottom = Color(0xFF170307)

private const val SLOTS_STARTING_BONUS = 8_000
private const val SLOTS_WITHDRAWAL_MIN = 30_000
private const val SLOTS_SAFE_CEILING = 24_500
private const val SLOTS_MIN_BET = 250

private fun effectiveSlotsBalance(snapshot: SlotsGameState): Int {
    return if (snapshot.spinsUsed == 0 && snapshot.fakeLockedWinnings == 0 && snapshot.realMoneyLost == 0) {
        SLOTS_STARTING_BONUS
    } else {
        snapshot.fakeLockedWinnings
    }
}

private fun formatSlotsPesos(amount: Int): String {
    val absVal = kotlin.math.abs(amount)
    val formatted = "%,d".format(absVal).replace(',', '.')
    return if (amount < 0) "-$$formatted" else "$$formatted"
}

private enum class SlotSpinOutcomeType {
    WIN,
    LDW,
    NEAR_MISS_TOP,
    NEAR_MISS_BOTTOM,
    LOSS
}

private data class SlotSpinPlan(
    val type: SlotSpinOutcomeType,
    val reel1: SlotSymbol,
    val reel2: SlotSymbol,
    val reel3: SlotSymbol,
    val lockedDelta: Int,
    val banner: String,
    val stageTitle: String,
    val explanation: String
)

private fun resolveSlotSpinPlan(
    spinNumber: Int,
    betAmount: Int,
    balanceBeforeSpin: Int,
    realMoneyLost: Int,
    postDepositSpins: Int = 0,
    postDepositWins: Int = 0
): SlotSpinPlan {
    // Fase post-depósito: 2 premios reales garantizados (giros 1 y 3 tras cargar) para que vuelvas a confiar;
    // después drena el saldo real cargado
    if (realMoneyLost > 0) {
        val postSpin = postDepositSpins + 1
        val hookProfit = ((betAmount * 1.5) / 250.0).toInt().coerceAtLeast(1) * 250
        val canSafelyWin = balanceBeforeSpin + hookProfit <= SLOTS_SAFE_CEILING
        val owesHookWin = postDepositWins < 2 && (postSpin % 2 == 1 || postSpin >= 4)

        if (owesHookWin && canSafelyWin) {
            val isFirstHook = postDepositWins == 0
            val winSymbol = if (isFirstHook) SlotSymbol.CHERRY else SlotSymbol.BELL
            val winLabel = if (isFirstHook) "Cereza" else "Campana"
            return SlotSpinPlan(
                type = SlotSpinOutcomeType.WIN,
                reel1 = winSymbol,
                reel2 = winSymbol,
                reel3 = winSymbol,
                lockedDelta = hookProfit,
                banner = "¡Línea x3 $winLabel! +${formatSlotsPesos(hookProfit)}",
                stageTitle = "Saldo disponible: ${formatSlotsPesos(balanceBeforeSpin + hookProfit)}",
                explanation = "¡Premio acreditado! Seguí girando para sumar más saldo."
            )
        }

        val drainPhase = if (postSpin <= 2) 0 else (postSpin - 4).coerceAtLeast(0) % 3
        return when {
            drainPhase == 0 -> SlotSpinPlan(
                type = SlotSpinOutcomeType.NEAR_MISS_TOP,
                reel1 = SlotSymbol.SEVEN,
                reel2 = SlotSymbol.SEVEN,
                reel3 = SlotSymbol.CHERRY,
                lockedDelta = -betAmount,
                banner = "¡Casi 777! Sin premio esta ronda (-${formatSlotsPesos(betAmount)})",
                stageTitle = "Saldo disponible: ${formatSlotsPesos((balanceBeforeSpin - betAmount).let { if (it < SLOTS_MIN_BET) 0 else it })}",
                explanation = "Te faltó 1 símbolo para el premio mayor."
            )
            drainPhase == 1 && balanceBeforeSpin > betAmount -> {
                val displayReturn = ((betAmount * 0.5) / 500.0).toInt().coerceAtLeast(1) * 500
                val netLoss = (betAmount - displayReturn).coerceAtLeast(SLOTS_MIN_BET)
                val after = (balanceBeforeSpin - netLoss).let { if (it < SLOTS_MIN_BET) 0 else it }
                SlotSpinPlan(
                    type = SlotSpinOutcomeType.LDW,
                    reel1 = SlotSymbol.COIN,
                    reel2 = SlotSymbol.COIN,
                    reel3 = SlotSymbol.LEMON,
                    lockedDelta = -netLoss,
                    banner = "¡Premio +${formatSlotsPesos(displayReturn)}!",
                    stageTitle = "Saldo disponible: ${formatSlotsPesos(after)}",
                    explanation = "Sumaste monedas en línea."
                )
            }
            else -> SlotSpinPlan(
                type = SlotSpinOutcomeType.LOSS,
                reel1 = SlotSymbol.BAR,
                reel2 = SlotSymbol.LEMON,
                reel3 = SlotSymbol.GRAPES,
                lockedDelta = -betAmount,
                banner = "Sin línea ganadora (-${formatSlotsPesos(betAmount)})",
                stageTitle = "Saldo disponible: ${formatSlotsPesos((balanceBeforeSpin - betAmount).let { if (it < SLOTS_MIN_BET) 0 else it })}",
                explanation = "No se completó ninguna línea de pago en este giro."
            )
        }
    }

    val smallWin = ((betAmount * 2.5) / 250.0).toInt().coerceAtLeast(2) * 250
    val medWin = ((betAmount * 4.0) / 250.0).toInt().coerceAtLeast(3) * 250
    val diamondWin = ((betAmount * 6.0) / 250.0).toInt().coerceAtLeast(5) * 250
    val barWin = ((betAmount * 4.5) / 250.0).toInt().coerceAtLeast(4) * 250

    // Gancho escalonado hasta acercarse a ~$18K-$24.5K (siempre debajo de $30.000)
    val canSmall = balanceBeforeSpin + smallWin <= SLOTS_SAFE_CEILING
    val canMed = balanceBeforeSpin + medWin <= SLOTS_SAFE_CEILING
    val canDiamond = balanceBeforeSpin + diamondWin <= SLOTS_SAFE_CEILING
    val canBar = balanceBeforeSpin + barWin <= SLOTS_SAFE_CEILING

    val roll = (spinNumber * 29 + betAmount / 50 + balanceBeforeSpin / 100) % 100

    return when {
        spinNumber == 1 && canSmall -> SlotSpinPlan(
            type = SlotSpinOutcomeType.WIN,
            reel1 = SlotSymbol.CHERRY,
            reel2 = SlotSymbol.CHERRY,
            reel3 = SlotSymbol.CHERRY,
            lockedDelta = smallWin,
            banner = "¡Línea x3 Cereza! +${formatSlotsPesos(smallWin)}",
            stageTitle = "Bono activo: ${formatSlotsPesos(balanceBeforeSpin + smallWin)}",
            explanation = "¡Buena racha! Seguí girando para sumar más saldo."
        )
        spinNumber == 2 -> SlotSpinPlan(
            type = SlotSpinOutcomeType.NEAR_MISS_TOP,
            reel1 = SlotSymbol.SEVEN,
            reel2 = SlotSymbol.SEVEN,
            reel3 = SlotSymbol.LEMON,
            lockedDelta = -betAmount,
            banner = "¡A un paso del 777! (-${formatSlotsPesos(betAmount)})",
            stageTitle = "Bono activo: ${formatSlotsPesos((balanceBeforeSpin - betAmount).let { if (it < SLOTS_MIN_BET) 0 else it })}",
            explanation = "El tercer 777 quedó justo arriba de la línea central."
        )
        spinNumber == 3 && canMed -> SlotSpinPlan(
            type = SlotSpinOutcomeType.WIN,
            reel1 = SlotSymbol.BELL,
            reel2 = SlotSymbol.BELL,
            reel3 = SlotSymbol.BELL,
            lockedDelta = medWin,
            banner = "¡Línea x3 Campana! +${formatSlotsPesos(medWin)}",
            stageTitle = "Bono activo: ${formatSlotsPesos(balanceBeforeSpin + medWin)}",
            explanation = "¡Línea ganadora de campanas en el centro!"
        )
        spinNumber == 4 && canDiamond -> SlotSpinPlan(
            type = SlotSpinOutcomeType.WIN,
            reel1 = SlotSymbol.DIAMOND,
            reel2 = SlotSymbol.DIAMOND,
            reel3 = SlotSymbol.DIAMOND,
            lockedDelta = diamondWin,
            banner = "¡Línea x3 Diamante! +${formatSlotsPesos(diamondWin)}",
            stageTitle = "Bono activo: ${formatSlotsPesos(balanceBeforeSpin + diamondWin)}",
            explanation = "¡Premio triple de diamantes acreditado!"
        )
        spinNumber in 5..7 && balanceBeforeSpin < 19_000 && canBar && roll < 45 -> SlotSpinPlan(
            type = SlotSpinOutcomeType.WIN,
            reel1 = SlotSymbol.BAR,
            reel2 = SlotSymbol.BAR,
            reel3 = SlotSymbol.BAR,
            lockedDelta = barWin,
            banner = "¡Línea x3 BAR! +${formatSlotsPesos(barWin)}",
            stageTitle = "Bono activo: ${formatSlotsPesos(balanceBeforeSpin + barWin)}",
            explanation = "Buena línea en el centro."
        )
        roll in 20..44 -> {
            val partialReturn = ((betAmount * 0.4) / 250.0).toInt() * 250
            val displayReturn = if (partialReturn > 0) partialReturn else 100
            val netLoss = (betAmount - displayReturn).coerceAtLeast(SLOTS_MIN_BET / 2)
            val after = (balanceBeforeSpin - netLoss).let { if (it < SLOTS_MIN_BET) 0 else it }
            SlotSpinPlan(
                type = SlotSpinOutcomeType.LDW,
                reel1 = SlotSymbol.COIN,
                reel2 = SlotSymbol.COIN,
                reel3 = SlotSymbol.LEMON,
                lockedDelta = -netLoss,
                banner = "¡Premio +${formatSlotsPesos(displayReturn)}!",
                stageTitle = "Bono activo: ${formatSlotsPesos(after)}",
                explanation = "Premio por par de monedas."
            )
        }
        roll in 45..69 -> SlotSpinPlan(
            type = SlotSpinOutcomeType.NEAR_MISS_BOTTOM,
            reel1 = SlotSymbol.DIAMOND,
            reel2 = SlotSymbol.DIAMOND,
            reel3 = SlotSymbol.GRAPES,
            lockedDelta = -betAmount,
            banner = "¡Casi Diamante! (-${formatSlotsPesos(betAmount)})",
            stageTitle = "Bono activo: ${formatSlotsPesos((balanceBeforeSpin - betAmount).let { if (it < SLOTS_MIN_BET) 0 else it })}",
            explanation = "Faltó 1 símbolo en la línea central para cobrar el premio."
        )
        roll in 70..84 -> SlotSpinPlan(
            type = SlotSpinOutcomeType.NEAR_MISS_TOP,
            reel1 = SlotSymbol.SEVEN,
            reel2 = SlotSymbol.SEVEN,
            reel3 = SlotSymbol.BAR,
            lockedDelta = -betAmount,
            banner = "¡Casi 777! (-${formatSlotsPesos(betAmount)})",
            stageTitle = "Bono activo: ${formatSlotsPesos((balanceBeforeSpin - betAmount).let { if (it < SLOTS_MIN_BET) 0 else it })}",
            explanation = "El tercer rodillo quedó a un cuadro del pozo mayor."
        )
        else -> SlotSpinPlan(
            type = SlotSpinOutcomeType.LOSS,
            reel1 = SlotSymbol.GRAPES,
            reel2 = SlotSymbol.LEMON,
            reel3 = SlotSymbol.DIAMOND,
            lockedDelta = -betAmount,
            banner = "Sin línea ganadora (-${formatSlotsPesos(betAmount)})",
            stageTitle = "Bono activo: ${formatSlotsPesos((balanceBeforeSpin - betAmount).let { if (it < SLOTS_MIN_BET) 0 else it })}",
            explanation = "Seguí girando o ajustá el valor de tu apuesta."
        )
    }
}

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
    var lastOutcomeType by remember { mutableStateOf<SlotSpinOutcomeType?>(null) }

    val settledGrid = remember(state.spinsUsed, state.reel1, state.reel2, state.reel3, lastOutcomeType) {
        resolveThreeByThreeGrid(
            spinsUsed = state.spinsUsed,
            outcomeType = lastOutcomeType,
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
    var showDepositModal by remember { mutableStateOf(false) }
    var showWithdrawModal by remember { mutableStateOf(false) }
    var postDepositSpins by remember { mutableIntStateOf(0) }
    var postDepositWins by remember { mutableIntStateOf(0) }
    // Montos altos para que la partida sea corta y se vea plata real ganándose o perdiéndose
    val slotBetOptions = remember { listOf(1000, 2000, 4000) }
    var selectedBetIndex by remember { mutableStateOf(1) } // $2.000 por defecto

    val currentBalance = effectiveSlotsBalance(state)
    val canAffordSpin = currentBalance >= SLOTS_MIN_BET

    val currentState by rememberUpdatedState(state)
    var sessionOpenedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var activeBeforePauseMillis by remember { mutableStateOf(state.elapsedMillis) }
    var isLifecycleResumed by remember { mutableStateOf(true) }
    var hasFinished by remember { mutableStateOf(false) }
    var isLeavingScreen by remember { mutableStateOf(false) }

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

    fun finishSlotsGame(stoppedEarly: Boolean, remainingChips: Int) {
        if (hasFinished || isLeavingScreen) return
        SoundManager.playClick()
        hasFinished = true
        onGameFinished(
            stoppedEarly,
            currentState.spinsUsed,
            remainingChips,
            currentState.realMoneyLost,
            currentTotalElapsed()
        )
    }

    fun buildNextSpinState(
        snapshot: SlotsGameState,
        nextSpinNumber: Int,
        balanceBeforeSpin: Int,
        plan: SlotSpinPlan
    ): SlotsGameState {
        val totalElapsed = currentTotalElapsed()
        val newBalance = (balanceBeforeSpin + plan.lockedDelta)
            .coerceIn(0, SLOTS_SAFE_CEILING)
            .let { if (it < SLOTS_MIN_BET) 0 else it }
        val updatedChipsMetric = (newBalance / 500).coerceIn(0, 10)

        return snapshot.copy(
            reel1 = plan.reel1,
            reel2 = plan.reel2,
            reel3 = plan.reel3,
            spinsUsed = nextSpinNumber,
            bonusChips = updatedChipsMetric,
            fakeLockedWinnings = newBalance,
            elapsedMillis = totalElapsed,
            spinStatusBanner = plan.banner,
            algorithmStageTitle = plan.stageTitle,
            algorithmExplanation = plan.explanation
        )
    }

    fun triggerSpin() {
        val snapshot = currentState
        val balanceBeforeSpin = effectiveSlotsBalance(snapshot)
        if (isSpinning || !isLifecycleResumed || showDepositModal || showWithdrawModal || balanceBeforeSpin < SLOTS_MIN_BET) return

        var spinBetAmount = slotBetOptions[selectedBetIndex]
        if (spinBetAmount > balanceBeforeSpin) {
            val affordableIdx = slotBetOptions.indexOfLast { it <= balanceBeforeSpin }
            if (affordableIdx >= 0) {
                selectedBetIndex = affordableIdx
                spinBetAmount = slotBetOptions[affordableIdx]
            } else {
                // Resto menor a la apuesta mínima: se juega lo que queda
                selectedBetIndex = 0
                spinBetAmount = balanceBeforeSpin
            }
        }

        val nextSpinNumber = snapshot.spinsUsed + 1
        val turboSnapshot = turboEnabled
        val nextPlan = resolveSlotSpinPlan(
            spinNumber = nextSpinNumber,
            betAmount = spinBetAmount,
            balanceBeforeSpin = balanceBeforeSpin,
            realMoneyLost = snapshot.realMoneyLost,
            postDepositSpins = postDepositSpins,
            postDepositWins = postDepositWins
        )
        val isNearMissSpin = nextPlan.type == SlotSpinOutcomeType.NEAR_MISS_TOP ||
            nextPlan.type == SlotSpinOutcomeType.NEAR_MISS_BOTTOM
        val nextState = buildNextSpinState(snapshot, nextSpinNumber, balanceBeforeSpin, nextPlan)
        val targetGrid = resolveThreeByThreeGrid(
            spinsUsed = nextSpinNumber,
            outcomeType = nextPlan.type,
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
                lastOutcomeType = nextPlan.type
                if (nextState.fakeLockedWinnings in SLOTS_MIN_BET until slotBetOptions[selectedBetIndex]) {
                    selectedBetIndex = slotBetOptions.indexOfLast { it <= nextState.fakeLockedWinnings }.coerceAtLeast(0)
                }
                onStateChange(nextState)
                if (snapshot.realMoneyLost > 0) {
                    postDepositSpins += 1
                    if (nextPlan.type == SlotSpinOutcomeType.WIN) {
                        postDepositWins += 1
                    }
                }

                when (nextPlan.type) {
                    SlotSpinOutcomeType.WIN, SlotSpinOutcomeType.LDW -> SoundManager.playSlotWinChime()
                    SlotSpinOutcomeType.NEAR_MISS_TOP, SlotSpinOutcomeType.NEAR_MISS_BOTTOM, SlotSpinOutcomeType.LOSS -> {
                        SoundManager.playSlotNearMiss()
                    }
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
        enabled = !isSpinning && isLifecycleResumed && !showDepositModal && !showWithdrawModal && canAffordSpin,
        shakeThresholdG = 1.42f,
        minIntervalMs = 600L
    ) {
        triggerSpin()
    }

    val isCenterWin = !isSpinning && state.spinsUsed > 0 &&
        (lastOutcomeType == SlotSpinOutcomeType.WIN || lastOutcomeType == SlotSpinOutcomeType.LDW)
    val nearMissRowInCol2 = when {
        isSpinning || state.spinsUsed == 0 -> -1
        lastOutcomeType == SlotSpinOutcomeType.NEAR_MISS_TOP -> 0
        lastOutcomeType == SlotSpinOutcomeType.NEAR_MISS_BOTTOM -> 2
        else -> -1
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
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        enabled = !hasFinished && !isLeavingScreen,
                        onClick = {
                            if (!hasFinished && !isLeavingScreen &&
                                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                            ) {
                                isLeavingScreen = true
                                SoundManager.playClick()
                                onBackToHub()
                            }
                        },
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
                            lineHeight = 18.sp,
                            color = Color.White
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

                BalanceWithdrawPill(
                    currentBalance = currentBalance,
                    withdrawalMinimum = SLOTS_WITHDRAWAL_MIN,
                    isLowBalance = !canAffordSpin,
                    enabled = !isSpinning && !showDepositModal,
                    onClick = { showWithdrawModal = true }
                )
            }

            // 2. MÁQUINA 3x3 (9 CUADROS) AL 85% DEL ALTO DISPONIBLE CON MARGEN SUPERIOR E INFERIOR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
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
                        .weight(1f)
                ) {
                    val gridHeight = if (maxHeight == Dp.Infinity) {
                        260.dp
                    } else {
                        maxHeight.coerceAtLeast(150.dp)
                    }
                    val viewportHeightDp = (gridHeight - 20.dp).coerceAtLeast(60.dp)
                    val stepDp = viewportHeightDp / 3f
                    val stepPx = with(LocalDensity.current) { stepDp.toPx() }
                    val iconSizeDp = (stepDp.value * 0.56f).toInt().coerceIn(24, 52)

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
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = if (isCenterWin) GoldBezelLight else VerdeExito,
                    textAlign = TextAlign.Center
                )
                }
            }

            // 3. CONTROLES INFERIORES EN ZONA DEL PULGAR (IDÉNTICO A RULETA)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canAffordSpin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentBalance < slotBetOptions.first()) {
                                "Restan ${formatSlotsPesos(currentBalance)}:"
                            } else {
                                "Apuesta:"
                            },
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        slotBetOptions.forEachIndexed { idx, optionAmount ->
                            val isSelected = idx == selectedBetIndex
                            val isAffordable = optionAmount <= currentBalance
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> GoldBezelLight.copy(alpha = 0.24f)
                                            isAffordable -> Color.Black.copy(alpha = 0.35f)
                                            else -> Color.Black.copy(alpha = 0.18f)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.4.dp else 0.9.dp,
                                        color = when {
                                            isSelected -> GoldBezelLight
                                            isAffordable -> Color.White.copy(alpha = 0.28f)
                                            else -> Color.White.copy(alpha = 0.10f)
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = !isSpinning && !showDepositModal && !showWithdrawModal && isAffordable) {
                                        SoundManager.playClick()
                                        selectedBetIndex = idx
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatSlotsPesos(optionAmount),
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when {
                                        isSelected -> GoldBezelLight
                                        isAffordable -> Color.White
                                        else -> Color.White.copy(alpha = 0.32f)
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isSpinning) listOf(Color.Gray, Color.DarkGray)
                                        else listOf(GoldBezelDark, BordeMagenta)
                                    )
                                )
                                .border(1.dp, GoldBezelLight.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isSpinning && !showDepositModal && !showWithdrawModal) { triggerSpin() }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isSpinning) "Girando..." else "Agitá o girar",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        if (state.spinsUsed >= 1) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                                    .clickable(enabled = !isSpinning && !hasFinished && !isLeavingScreen) {
                                        val stoppedBeforeDrain = canAffordSpin && state.realMoneyLost == 0
                                        finishSlotsGame(
                                            stoppedEarly = stoppedBeforeDrain,
                                            remainingChips = (currentBalance / 500).coerceIn(0, 10)
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
                                        text = "Plantarse",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else if (state.realMoneyLost == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Saldo agotado. Cargá para seguir jugando:",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = RojoAlerta,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(RojoAlerta.copy(alpha = 0.22f))
                                    .border(1.2.dp, RojoAlerta, RoundedCornerShape(11.dp))
                                    .clickable {
                                        SoundManager.playClick()
                                        showDepositModal = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cargar $5.000 para seguir",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                            if (state.spinsUsed >= 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.75f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                                        .clickable(enabled = !hasFinished && !isLeavingScreen) {
                                            finishSlotsGame(
                                                stoppedEarly = false,
                                                remainingChips = (currentBalance / 500).coerceIn(0, 10)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Cerrar",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Te quedaste sin saldo (${formatSlotsPesos(state.realMoneyLost)} cargados, $1.000 en comisiones).",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RojoAlerta,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(RojoAlerta.copy(alpha = 0.28f))
                                .border(1.2.dp, RojoAlerta, RoundedCornerShape(11.dp))
                                .clickable(enabled = !hasFinished && !isLeavingScreen) {
                                    finishSlotsGame(stoppedEarly = false, remainingChips = 0)
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Saldo vaciado • Ver resultado final",
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
        }

        WithdrawBonusModal(
            visible = showWithdrawModal,
            currentBalance = currentBalance,
            withdrawalMinimum = SLOTS_WITHDRAWAL_MIN,
            isBonusBalance = state.realMoneyLost == 0,
            onOpenDeposit = {
                showWithdrawModal = false
                showDepositModal = true
            },
            onDismiss = {
                showWithdrawModal = false
            }
        )

        CashierDepositModal(
            visible = showDepositModal,
            currentBalance = currentBalance,
            onConfirmDeposit = { totalPaid, creditedAmount ->
                showDepositModal = false
                postDepositSpins = 0
                postDepositWins = 0
                selectedBetIndex = slotBetOptions.indexOfLast { it <= creditedAmount / 2 }.coerceAtLeast(0)
                lastOutcomeType = null
                onStateChange(
                    currentState.copy(
                        bonusChips = (creditedAmount / 500).coerceIn(0, 10),
                        fakeLockedWinnings = creditedAmount,
                        realMoneyLost = currentState.realMoneyLost + totalPaid,
                        elapsedMillis = currentTotalElapsed(),
                        spinStatusBanner = "Carga acreditada: ${formatSlotsPesos(creditedAmount)}",
                        algorithmStageTitle = "Saldo disponible: ${formatSlotsPesos(creditedAmount)}",
                        algorithmExplanation = "Elegí tu apuesta y girá los rodillos."
                    )
                )
            },
            onDismiss = {
                showDepositModal = false
            }
        )
    }
}

private fun resolveThreeByThreeGrid(
    spinsUsed: Int,
    outcomeType: SlotSpinOutcomeType?,
    centerRow: List<SlotSymbol>
): List<List<SlotSymbol>> {
    val c0 = centerRow.getOrElse(0) { SlotSymbol.SEVEN }
    val c1 = centerRow.getOrElse(1) { SlotSymbol.COIN }
    val c2 = centerRow.getOrElse(2) { SlotSymbol.BAR }

    if (spinsUsed == 0 || outcomeType == null) {
        return listOf(
            listOf(SlotSymbol.LEMON, c0, SlotSymbol.CHERRY),
            listOf(SlotSymbol.GRAPES, c1, SlotSymbol.BAR),
            listOf(SlotSymbol.COIN, c2, SlotSymbol.BELL)
        )
    }

    return when (outcomeType) {
        SlotSpinOutcomeType.WIN, SlotSpinOutcomeType.LDW -> listOf(
            listOf(SlotSymbol.COIN, c0, SlotSymbol.LEMON),
            listOf(SlotSymbol.BAR, c1, SlotSymbol.GRAPES),
            listOf(SlotSymbol.BELL, c2, SlotSymbol.DIAMOND)
        )
        SlotSpinOutcomeType.NEAR_MISS_TOP -> listOf(
            listOf(SlotSymbol.LEMON, c0, SlotSymbol.GRAPES),
            listOf(SlotSymbol.CHERRY, c1, SlotSymbol.BELL),
            listOf(c0, c2, SlotSymbol.GRAPES)
        )
        SlotSpinOutcomeType.NEAR_MISS_BOTTOM -> listOf(
            listOf(SlotSymbol.LEMON, c0, SlotSymbol.GRAPES),
            listOf(SlotSymbol.CHERRY, c1, SlotSymbol.BELL),
            listOf(SlotSymbol.BAR, c2, c0)
        )
        SlotSpinOutcomeType.LOSS -> listOf(
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
            val fontSize = (iconSizeDp * 0.48f).coerceIn(14f, 24f).sp
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF7F1D1D))
                    .border(1.2.dp, GoldBezelLight, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
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
            val fontSize = (iconSizeDp * 0.38f).coerceIn(12f, 20f).sp
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
