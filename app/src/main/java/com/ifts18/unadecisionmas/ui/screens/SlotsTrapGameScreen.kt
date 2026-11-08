package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.audio.SoundManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * JUEGO 3: "La Trampa del Jackpot 🎰" (Idea de Lucía y Evelyn)
 * Simulador interactivo de Tragamonedas + Ruleta con 10 tiros de bono que se acciona
 * agitando o inclinando el celular (Acelerómetro). Demuestra en tiempo real cómo están
 * programados los algoritmos para que la casa siempre gane.
 */
@Composable
fun SlotsTrapGameScreen(
    onBackToHub: () -> Unit,
    onGameFinished: (stoppedEarly: Boolean, spinsUsed: Int, remainingChips: Int, realMoneyLost: Int, elapsedMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val symbols = remember { listOf("💎", "7️⃣", "🍒", "🔔", "🍋") }
    val rouletteOptions = remember { listOf("29 Negro", "Rojo x2", "0 Verde") }

    var selectedRouletteBet by remember { mutableStateOf("29 Negro") }
    var reel1 by remember { mutableStateOf("7️⃣") }
    var reel2 by remember { mutableStateOf("💎") }
    var reel3 by remember { mutableStateOf("🍒") }

    var isSpinning by remember { mutableStateOf(false) }
    var spinsUsed by remember { mutableIntStateOf(0) }
    val maxSpins = 10
    var bonusChips by remember { mutableIntStateOf(10) }
    var fakeLockedWinnings by remember { mutableIntStateOf(0) }
    var realMoneyLost by remember { mutableIntStateOf(0) }
    val startTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var algorithmStageTitle by remember {
        mutableStateOf("Bono Cebo Activo: Tenés 10 tiros gratis")
    }
    var algorithmExplanation by remember {
        mutableStateOf("Elegí tu apuesta (ej. 29 Negro) y AGITÁ EL CELULAR para hacer girar el tragamonedas y la ruleta.")
    }
    var rouletteResultBadge by remember {
        mutableStateOf("Ruleta lista para girar")
    }

    fun triggerSpin() {
        if (isSpinning || spinsUsed >= maxSpins || bonusChips <= 0) return
        isSpinning = true
        SoundManager.playClick()
        HapticFeedbackHelper.vibrateShort(context, 55L)

        scope.launch {
            val nextSpinNumber = spinsUsed + 1
            // Animación rápida de rodillos girando
            repeat(12) {
                reel1 = symbols.random()
                reel2 = symbols.random()
                reel3 = symbols.random()
                delay(65L)
            }

            spinsUsed = nextSpinNumber
            bonusChips = (bonusChips - 1).coerceAtLeast(0)

            // Lógica pedagógica del algoritmo del casino según el número de tiro
            when (nextSpinNumber) {
                1, 2 -> {
                    // ETAPA 1: Gancho Inicial (Dopamina + Rollover falso)
                    reel1 = "🍒"
                    reel2 = "🍒"
                    reel3 = "🍒"
                    fakeLockedWinnings += 3500
                    rouletteResultBadge = "Salió $selectedRouletteBet (Premio bloqueado por Rollover)"
                    algorithmStageTitle = "Etapa 1 (Tiro $nextSpinNumber/10): El Gancho Inicial"
                    algorithmExplanation = "¡El algoritmo te dejó ganar a propósito! Te acreditó $$fakeLockedWinnings 'bonificados', pero la letra chica exige apostar 40 veces más (Rollover) para poder retirarlos."
                }
                3, 4, 5, 6 -> {
                    // ETAPA 2: Casi-Acierto Programado (Near-Miss Effect)
                    val pairSymbol = if (nextSpinNumber % 2 == 0) "7️⃣" else "💎"
                    reel1 = pairSymbol
                    reel2 = pairSymbol
                    reel3 = "🍋"
                    rouletteResultBadge = if (selectedRouletteBet == "29 Negro") "Salió 28 Rojo (¡Al lado del 29!)" else "Salió 00 Banca"
                    algorithmStageTitle = "Etapa 2 (Tiro $nextSpinNumber/10): Efecto 'Casi Ganás' (Near-Miss)"
                    algorithmExplanation = "Salieron dos $pairSymbol iguales y el tercero frenó a un milímetro, mientras en la ruleta salió el número de al lado. Está programado para engañar a tu cerebro y que no puedas frenar."
                }
                else -> {
                    // ETAPA 3: Vaciado Estadístico (La Casa Siempre Gana)
                    reel1 = "🍋"
                    reel2 = "🔔"
                    reel3 = "🍒"
                    fakeLockedWinnings = (fakeLockedWinnings - 1800).coerceAtLeast(0)
                    rouletteResultBadge = "Gana la Banca (Ventaja Matemática)"
                    algorithmStageTitle = "Etapa 3 (Tiro $nextSpinNumber/10): Vaciado Estadístico"
                    algorithmExplanation = "A medida que sumás tiros, el Retorno al Jugador (RTP < 95%) garantiza matemáticamente que el casino recupere el bono y te empuje a cargar plata de tu bolsillo."
                }
            }

            HapticFeedbackHelper.vibrateShort(context, 40L)
            isSpinning = false
        }
    }

    // Sensor Acelerómetro: Agitar o inclinar el celular activa la tirada
    val sensorReading by rememberAccelerometerMonitor(
        enabled = !isSpinning && spinsUsed < maxSpins,
        shakeThresholdG = 1.42f,
        minIntervalMs = 600L
    ) {
        triggerSpin()
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. CABECERA Y SENSOR ACTIVO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161922))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToHub,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Juego 3: La Trampa del Jackpot",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Agitá el celu para girar • ${"%.2f".format(sensorReading.gForce)} G",
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.5.sp,
                            color = BordeCyan
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BordeMagenta.copy(alpha = 0.2f))
                        .border(1.dp, BordeMagenta, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Fichas: $bonusChips/$maxSpins",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. SELECTOR DE RULETA (29 NEGRO / ROJO / VERDE - IDEA DE EVELYN Y LUCÍA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BurbujaChat.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "1. Elegí tu ficha de Ruleta y agitá el teléfono",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextoSecundario
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rouletteOptions.forEach { option ->
                        val isSelected = selectedRouletteBet == option
                        val chipColor = when {
                            option.contains("Negro") -> Color(0xFF2B3040)
                            option.contains("Rojo") -> RojoAlerta.copy(alpha = 0.75f)
                            else -> VerdeExito.copy(alpha = 0.75f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(chipColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) BordeCyan else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    SoundManager.playClick()
                                    selectedRouletteBet = option
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = rouletteResultBadge,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = BordeCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. MÁQUINA TRAGAMONEDAS DE 3 RODILLOS DE EMOJIS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF221233), Color(0xFF121622))
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎰 CASINO ALGORÍTMICO (MODO RADIOGRAFÍA)",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BordeCyan
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Los 3 rodillos con iconos de emojis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                     listOf(reel1, reel2, reel3).forEach { symbol ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B0E14))
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSpinning) BordeMagenta else BordeCyan.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 42.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón de acción por Acelerómetro o toque
                if (spinsUsed < maxSpins) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isSpinning) listOf(Color.Gray, Color.DarkGray)
                                    else listOf(BordeMagenta, BordeCyan)
                                )
                            )
                            .clickable(enabled = !isSpinning) { triggerSpin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "Girando algoritmo..." else "¡AGITÁ EL CELULAR O TOCÁ PARA GIRAR! (${maxSpins - spinsUsed} tiros)",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Cuando se agotan los 10 tiros del bono: tentación de meter plata real vs plantarse
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "¡Se agotaron tus 10 fichas de bono! Así te tienta el casino ahora:",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = RojoAlerta,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RojoAlerta.copy(alpha = 0.2f))
                                .border(1.dp, RojoAlerta, RoundedCornerShape(12.dp))
                                .clickable {
                                    SoundManager.playClick()
                                    realMoneyLost += 5000
                                    val elapsed = System.currentTimeMillis() - startTimeMillis
                                    onGameFinished(false, spinsUsed, bonusChips, realMoneyLost, elapsed)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚠️ Cargar $5.000 de mis ahorros para intentar destrabar el bono",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. RADIOGRAFÍA EN VIVO DEL ALGORITMO (LO QUE PASA DETRÁS DE ESCENA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141B29))
                    .border(1.dp, BordeCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "🔍 Lo que hace el algoritmo por dentro:",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BordeCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = algorithmStageTitle,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = algorithmExplanation,
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. BOTÓN INTELIGENTE: PLANTARSE A TIEMPO Y DESBLOQUEAR MEDALLA "OJO ABIERTO"
            AnimatedVisibility(
                visible = spinsUsed >= 1,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                        .clickable {
                            SoundManager.playClick()
                            val elapsed = System.currentTimeMillis() - startTimeMillis
                            val stoppedBeforeDrain = spinsUsed < maxSpins
                            onGameFinished(
                                stoppedBeforeDrain,
                                spinsUsed,
                                bonusChips,
                                realMoneyLost,
                                elapsed
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (spinsUsed < maxSpins) {
                                "Plantarse a tiempo (No caer en la trampa)"
                            } else {
                                "Rechazar recarga y Cerrar Simulador ($0 Deuda)"
                            },
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
