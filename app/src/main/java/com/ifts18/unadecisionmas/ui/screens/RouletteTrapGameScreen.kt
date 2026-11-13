package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class RoulettePocket(
    val number: Int,
    val colorName: String,
    val pocketColor: Color
)

/**
 * JUEGO 4: "La Ruleta del 29 🎡"
 * Simulador interactivo de Ruleta con rueda giratoria real en Canvas, paño interactivo
 * para elegir número y color, y accionamiento mediante el sensor Acelerómetro.
 */
@Composable
fun RouletteTrapGameScreen(
    onBackToHub: () -> Unit,
    onGameFinished: (stoppedEarly: Boolean, spinsUsed: Int, remainingChips: Int, realMoneyLost: Int, elapsedMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val wheelPockets = remember {
        listOf(
            RoulettePocket(0, "Verde", Color(0xFF00C853)),
            RoulettePocket(32, "Rojo", Color(0xFFD32F2F)),
            RoulettePocket(15, "Negro", Color(0xFF262B38)),
            RoulettePocket(19, "Rojo", Color(0xFFD32F2F)),
            RoulettePocket(4, "Negro", Color(0xFF262B38)),
            RoulettePocket(21, "Rojo", Color(0xFFD32F2F)),
            RoulettePocket(2, "Negro", Color(0xFF262B38)),
            RoulettePocket(28, "Rojo", Color(0xFFD32F2F)),
            RoulettePocket(29, "Negro", Color(0xFF262B38)),
            RoulettePocket(7, "Rojo", Color(0xFFD32F2F)),
            RoulettePocket(20, "Negro", Color(0xFF262B38)),
            RoulettePocket(14, "Rojo", Color(0xFFD32F2F))
        )
    }

    var selectedNumberIndex by remember { mutableIntStateOf(8) } // Por defecto 29 Negro
    val selectedPocket = wheelPockets[selectedNumberIndex]
    var selectedBetMode by remember { mutableStateOf("PLENO") } // "PLENO", "ROJO", "NEGRO"

    var landedPocket by remember { mutableStateOf(wheelPockets[8]) }
    val wheelRotation = remember { Animatable(0f) }

    var isSpinning by remember { mutableStateOf(false) }
    var showResultPopup by remember { mutableStateOf(false) }
    var spinsUsed by remember { mutableIntStateOf(0) }
    val maxSpins = 10
    var bonusChips by remember { mutableIntStateOf(10) }
    var fakeLockedWinnings by remember { mutableIntStateOf(0) }
    var realMoneyLost by remember { mutableIntStateOf(0) }
    val startTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var resultBannerText by remember {
        mutableStateOf("Tocá un número del paño para cambiar tu ficha y agitá el celular")
    }
    var algorithmStageTitle by remember {
        mutableStateOf("Ruleta Lista: 10 fichas de bono activas")
    }
    var algorithmExplanation by remember {
        mutableStateOf("Elegí tu número con los botones del paño (ej. 29 Negro) o apostá a Color, y agitá el teléfono para girar la rueda.")
    }

    fun triggerRouletteSpin() {
        if (isSpinning || showResultPopup || spinsUsed >= maxSpins || bonusChips <= 0) return
        isSpinning = true
        showResultPopup = false
        SoundManager.playClick()
        HapticFeedbackHelper.vibrateShort(context, 55L)

        scope.launch {
            val nextSpinNumber = spinsUsed + 1

            // Determinar casillero de destino según la etapa pedagógica del algoritmo
            val targetPocketIndex = when (nextSpinNumber) {
                1, 2 -> {
                    // Etapa 1: Gancho inicial (sale exactamente lo que el jugador apostó)
                    when (selectedBetMode) {
                        "ROJO" -> wheelPockets.indexOfFirst { it.colorName == "Rojo" }
                        "NEGRO" -> wheelPockets.indexOfFirst { it.colorName == "Negro" }
                        else -> selectedNumberIndex
                    }
                }
                3, 4, 5, 6 -> {
                    // Etapa 2: Casi-Acierto (sale el número pegado al lado en la rueda)
                    when (selectedBetMode) {
                        "ROJO" -> wheelPockets.indexOfFirst { it.colorName == "Negro" }
                        "NEGRO" -> wheelPockets.indexOfFirst { it.colorName == "Rojo" }
                        else -> (selectedNumberIndex - 1 + wheelPockets.size) % wheelPockets.size
                    }
                }
                else -> {
                    // Etapa 3: Sale el 0 Verde o el color opuesto (Ventaja de la Banca)
                    0 // 0 Verde
                }
            }

            val sliceAngle = 360f / wheelPockets.size
            // Alinear el casillero elegido arriba en la flecha dorada (-90 grados)
            val targetAngleMod = (360f - (targetPocketIndex * sliceAngle) - sliceAngle / 2f) % 360f
            val currentMod = wheelRotation.value % 360f
            val delta = ((targetAngleMod - currentMod) + 360f) % 360f
            val totalTargetRotation = wheelRotation.value + 1080f + delta

            wheelRotation.animateTo(
                targetValue = totalTargetRotation,
                animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing)
            )

            landedPocket = wheelPockets[targetPocketIndex]
            spinsUsed = nextSpinNumber
            bonusChips = (bonusChips - 1).coerceAtLeast(0)

            when (nextSpinNumber) {
                1, 2 -> {
                    fakeLockedWinnings += 4000
                    resultBannerText = "🎉 ¡Salió ${landedPocket.number} ${landedPocket.colorName}! +$4.000 (Bono trabado)"
                    algorithmStageTitle = "Etapa 1 (Tirada $nextSpinNumber/10): El Gancho Inicial"
                    algorithmExplanation = "Al principio te hacen acertar para entusiasmarse, pero el premio queda trabado como 'Saldo de Bono' y no se puede retirar sin apostar mucho más."
                }
                3, 4, 5, 6 -> {
                    val betLabel = when (selectedBetMode) {
                        "ROJO" -> "Rojo"
                        "NEGRO" -> "Negro"
                        else -> "#${selectedPocket.number} ${selectedPocket.colorName}"
                    }
                    resultBannerText = "⚡ ¡Casi! Apostaste a $betLabel y frenó al lado: ${landedPocket.number} ${landedPocket.colorName}"
                    algorithmStageTitle = "Etapa 2 (Tirada $nextSpinNumber/10): Casi-Acierto y Mito de la Racha"
                    algorithmExplanation = "La bolilla frenó justo al lado de tu ficha (${landedPocket.number} ${landedPocket.colorName}). Es un mito creer que 'ya está por salir': cada giro es independiente y busca tentarte a seguir."
                }
                else -> {
                    fakeLockedWinnings = (fakeLockedWinnings - 2000).coerceAtLeast(0)
                    resultBannerText = "🟢 Salió 0 Verde • Gana la Banca (Perdés tu ficha)"
                    algorithmStageTitle = "Etapa 3 (Tirada $nextSpinNumber/10): El Cero Verde y la Ventaja de la Casa"
                    algorithmExplanation = "Aunque Rojo o Negro parezcan 50/50, el 0 Verde inclina la probabilidad real a 47,3%. Cuantas más tiradas hacés, más seguro es que la banca se quede con todo."
                }
            }

            HapticFeedbackHelper.vibrateShort(context, 45L)
            isSpinning = false
            showResultPopup = true
        }
    }

    val sensorReading by rememberAccelerometerMonitor(
        enabled = !isSpinning && !showResultPopup && spinsUsed < maxSpins,
        shakeThresholdG = 1.42f,
        minIntervalMs = 600L
    ) {
        triggerRouletteSpin()
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
            // 1. CABECERA CON CONTADOR DE FICHAS EN UNA SOLA LÍNEA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161922))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Juego 4: La Ruleta del 29",
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
                        text = "🪙 Fichas: $bonusChips/$maxSpins",
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

            // 2. RUEDA DE RULETA VISUAL ANIMADA EN CANVAS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E142D), Color(0xFF111622))
                        )
                    )
                    .border(
                        width = 1.6.dp,
                        brush = Brush.horizontalGradient(listOf(BordeCyan, BordeMagenta)),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(152.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val sweepAngle = 360f / wheelPockets.size

                        // Aro exterior dorado/neón
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 6f)
                        )

                        // Rueda giratoria con los 12 sectores de colores
                        rotate(degrees = wheelRotation.value, pivot = center) {
                            wheelPockets.forEachIndexed { idx, pocket ->
                                val startAngle = -90f + idx * sweepAngle
                                drawArc(
                                    color = pocket.pocketColor,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - radius + 6f, center.y - radius + 6f),
                                    size = Size((radius - 6f) * 2f, (radius - 6f) * 2f)
                                )
                                drawArc(
                                    color = Color.White.copy(alpha = 0.35f),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - radius + 6f, center.y - radius + 6f),
                                    size = Size((radius - 6f) * 2f, (radius - 6f) * 2f),
                                    style = Stroke(width = 2f)
                                )

                                // Puntito de casillero cerca del borde para visualizar el giro
                                val midRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                                val dotDist = radius * 0.78f
                                val dotX = center.x + (cos(midRad) * dotDist).toFloat()
                                val dotY = center.y + (sin(midRad) * dotDist).toFloat()
                                drawCircle(
                                    color = if (pocket.number == selectedPocket.number && selectedBetMode == "PLENO") {
                                        BordeCyan
                                    } else {
                                        Color.White.copy(alpha = 0.85f)
                                    },
                                    radius = if (pocket.number == selectedPocket.number && selectedBetMode == "PLENO") 8f else 4.5f,
                                    center = Offset(dotX, dotY)
                                )
                            }
                        }

                        // Puntero / Bolilla blanca arriba al centro (-90°)
                        val pointerPath = Path().apply {
                            moveTo(center.x, 2f)
                            lineTo(center.x - 12f, -14f)
                            lineTo(center.x + 12f, -14f)
                            close()
                        }
                        drawPath(path = pointerPath, color = Color(0xFFFFD54F))
                        drawCircle(
                            color = Color.White,
                            radius = 9f,
                            center = Offset(center.x, 22f)
                        )
                    }

                    // Centro de la ruleta mostrando el número que salió
                    Column(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0E131D))
                            .border(2.2.dp, landedPocket.pocketColor, CircleShape),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isSpinning) "..." else "${landedPocket.number}",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isSpinning) "GIRANDO" else landedPocket.colorName.uppercase(),
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = when (landedPocket.colorName) {
                                "Rojo" -> RojoAlerta
                                "Verde" -> VerdeExito
                                else -> BordeCyan
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = resultBannerText,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = BordeCyan,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. PAÑO DE APUESTA INTERACTIVO: ELEGIR NÚMERO O COLOR CLARAMENTE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BurbujaChat.copy(alpha = 0.90f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Tocá para elegir tu apuesta:",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Bono retenido: $$fakeLockedWinnings 🔒",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BordeMagenta
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selector de modo: Pleno al Número vs Todo al Rojo vs Todo al Negro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        Triple("PLENO", "🎯 Pleno #${selectedPocket.number}", BordeCyan),
                        Triple("ROJO", "🔴 Rojo x2", RojoAlerta),
                        Triple("NEGRO", "⚫ Negro x2", Color(0xFF8A99AD))
                    )
                    modes.forEach { (modeKey, label, accent) ->
                        val isSelected = selectedBetMode == modeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(33.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.25f)
                                    else Color(0xFF121722)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accent else Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !isSpinning && !showResultPopup) {
                                    SoundManager.playClick()
                                    selectedBetMode = modeKey
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "2. Tocá cualquier casillero para cambiar el número a apostar:",
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.sp,
                    color = TextoSecundario,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Grilla de números de la ruleta (2 filas de 6 números cada una)
                val firstRow = wheelPockets.take(6)
                val secondRow = wheelPockets.drop(6)

                listOf(firstRow, secondRow).forEachIndexed { rowIdx, rowPockets ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowPockets.forEachIndexed { colIdx, pocket ->
                            val actualIndex = rowIdx * 6 + colIdx
                            val isNumberSelected = selectedBetMode == "PLENO" && selectedNumberIndex == actualIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(33.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pocket.pocketColor)
                                    .border(
                                        width = if (isNumberSelected) 2.2.dp else 1.dp,
                                        color = if (isNumberSelected) BordeCyan else Color.White.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isSpinning && !showResultPopup) {
                                        SoundManager.playClick()
                                        selectedNumberIndex = actualIndex
                                        selectedBetMode = "PLENO"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isNumberSelected) "🪙${pocket.number}" else "${pocket.number}",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón de acción por Acelerómetro o toque
                if (spinsUsed < maxSpins) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isSpinning || showResultPopup) listOf(Color.Gray, Color.DarkGray)
                                    else listOf(BordeMagenta, BordeCyan)
                                )
                            )
                            .clickable(enabled = !isSpinning && !showResultPopup) { triggerRouletteSpin() },
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
                                text = if (isSpinning) {
                                    "Girando la ruleta..."
                                } else {
                                    "¡AGITÁ EL CELULAR O TOCÁ PARA GIRAR! ($bonusChips fichas)"
                                },
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "¡Se agotaron las 10 fichas! El casino te pide cargar plata real:",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = RojoAlerta,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
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
                                text = "⚠️ Cargar $5.000 de mis ahorros para seguir buscando el pleno",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. BOTÓN PARA PLANTARSE A TIEMPO
            AnimatedVisibility(
                visible = spinsUsed >= 1,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
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
                                "Rechazar recarga y Cerrar Ruleta ($0 Deuda)"
                            },
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // OVERLAY MODAL DE EXPLICACIÓN POR CADA TIRADA (SIN SCROLL EN PANTALLA PRINCIPAL)
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
                    .background(Color.Black.copy(alpha = 0.74f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showResultPopup = false
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
                            brush = Brush.horizontalGradient(listOf(stageColor, BordeCyan)),
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
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = resultBannerText,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        lineHeight = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0E131D))
                            .border(1.dp, BordeCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "🔍 Cómo funciona la ventaja de la banca:",
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
                            fontSize = 14.sp,
                            color = stageColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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
                            .background(Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)))
                            .clickable {
                                SoundManager.playClick()
                                showResultPopup = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (spinsUsed < maxSpins) {
                                "Seguir tirando ($bonusChips fichas restantes)"
                            } else {
                                "Continuar (Se agotaron las fichas)"
                            },
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
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
                                    val elapsed = System.currentTimeMillis() - startTimeMillis
                                    onGameFinished(
                                        true,
                                        spinsUsed,
                                        bonusChips,
                                        realMoneyLost,
                                        elapsed
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🛡️ Plantarse ahora y no caer en la trampa",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = VerdeExito
                            )
                        }
                    }
                }
            }
        }
    }
}
