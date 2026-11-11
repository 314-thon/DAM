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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
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
 * JUEGO 3: "La Trampa del Jackpot 🎰"
 * Simulador interactivo de Máquina Tragamonedas de 3 Emojis con 10 tiros de bono que se acciona
 * agitando el celular (Acelerómetro). Muestra en vivo cómo opera el algoritmo (Gancho inicial,
 * Casi-Acierto y Vaciado Estadístico).
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
        mutableStateOf("Bono Cebo Activo: Tenés 10 fichas gratis")
    }
    var algorithmExplanation by remember {
        mutableStateOf("Agitá el celular o tocá el botón para hacer girar los 3 rodillos y ver cómo responde el algoritmo del tragamonedas.")
    }
    var spinStatusBanner by remember {
        mutableStateOf("🎰 Máquina lista: 3 iguales = Premio")
    }

    fun triggerSpin() {
        if (isSpinning || spinsUsed >= maxSpins || bonusChips <= 0) return
        isSpinning = true
        SoundManager.playClick()
        HapticFeedbackHelper.vibrateShort(context, 55L)

        scope.launch {
            val nextSpinNumber = spinsUsed + 1
            repeat(12) {
                reel1 = symbols.random()
                reel2 = symbols.random()
                reel3 = symbols.random()
                delay(65L)
            }

            spinsUsed = nextSpinNumber
            bonusChips = (bonusChips - 1).coerceAtLeast(0)

            when (nextSpinNumber) {
                1, 2 -> {
                    // ETAPA 1: Gancho Inicial (Dopamina + Rollover falso)
                    reel1 = "🍒"
                    reel2 = "🍒"
                    reel3 = "🍒"
                    fakeLockedWinnings += 3500
                    spinStatusBanner = "🍒 ¡3 CEREZAS! +$3.500 (Retenidos por Rollover x40)"
                    algorithmStageTitle = "Etapa 1 (Tiro $nextSpinNumber/10): El Gancho Inicial"
                    algorithmExplanation = "¡El algoritmo te dejó sacar 3 iguales a propósito! Te acreditó $$fakeLockedWinnings 'bonificados', pero la letra chica exige apostar 40 veces más (Rollover) para poder retirarlos."
                }
                3, 4, 5, 6 -> {
                    // ETAPA 2: Casi-Acierto Programado (Near-Miss Effect)
                    val pairSymbol = if (nextSpinNumber % 2 == 0) "7️⃣" else "💎"
                    reel1 = pairSymbol
                    reel2 = pairSymbol
                    reel3 = "🍋"
                    spinStatusBanner = "⚡ ¡CASI! Salieron dos $pairSymbol y el 3.° frenó al lado"
                    algorithmStageTitle = "Etapa 2 (Tiro $nextSpinNumber/10): Efecto 'Casi Ganás' (Near-Miss)"
                    algorithmExplanation = "Salieron dos $pairSymbol iguales y el tercer rodillo frenó a un milímetro. No es mala suerte: está programado para disparar dopamina en tu cerebro y que no puedas soltar el teléfono."
                }
                else -> {
                    // ETAPA 3: Vaciado Estadístico (La Casa Siempre Gana)
                    reel1 = "🍋"
                    reel2 = "🔔"
                    reel3 = "🍒"
                    fakeLockedWinnings = (fakeLockedWinnings - 1800).coerceAtLeast(0)
                    spinStatusBanner = "📉 Sin combinación • El RTP (<95%) consume tu saldo"
                    algorithmStageTitle = "Etapa 3 (Tiro $nextSpinNumber/10): Vaciado Estadístico"
                    algorithmExplanation = "A medida que sumás tiros, el Retorno al Jugador (RTP < 95%) garantiza matemáticamente que el casino recupere el bono y te empuje a cargar plata de tu propio bolsillo."
                }
            }

            HapticFeedbackHelper.vibrateShort(context, 40L)
            isSpinning = false
        }
    }

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
            // 1. CABECERA CORREGIDA (Con weight(1f) y badge de Fichas en una sola línea)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Juego 3: Tragamonedas",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Agitá para girar • ${"%.2f".format(sensorReading.gForce)} G",
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.5.sp,
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
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🪙 Fichas: $bonusChips/$maxSpins",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. TABLERO DE ESTADO DE FICHAS Y SALDO RETENIDO POR ROLLOVER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BurbujaChat.copy(alpha = 0.90f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121622))
                            .border(1.dp, BordeCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FICHAS RESTANTES",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = BordeCyan
                        )
                        Text(
                            text = "$bonusChips de $maxSpins tiros",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121622))
                            .border(1.dp, BordeMagenta.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BONO RETENIDO (ROLLOVER)",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = BordeMagenta,
                            maxLines = 1
                        )
                        Text(
                            text = "$$fakeLockedWinnings 🔒",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barra visual de fichas disponibles
                LinearProgressIndicator(
                    progress = { bonusChips.toFloat() / maxSpins.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (bonusChips > 3) BordeCyan else RojoAlerta,
                    trackColor = Color.White.copy(alpha = 0.12f)
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
                    text = "🎰 TRAGAMONEDAS ALGORÍTMICO (3 EMOJIS)",
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
                                .height(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B0E14))
                                .border(
                                    width = 1.8.dp,
                                    color = if (isSpinning) BordeMagenta else BordeCyan.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 44.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = spinStatusBanner,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = VerdeExito,
                    textAlign = TextAlign.Center
                )

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "Girando rodillos..." else "¡AGITÁ EL CELULAR O TOCÁ PARA GIRAR! ($bonusChips fichas)",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                maxLines = 1
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
