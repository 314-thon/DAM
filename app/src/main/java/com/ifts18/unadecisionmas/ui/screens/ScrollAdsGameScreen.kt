package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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

private data class InvasiveAdSpec(
    val id: Int,
    val triggerProgress: Float,
    val badge: String,
    val title: String,
    val influencerOrSource: String,
    val bodyText: String,
    val trapButtonText: String,
    val accentColor: Color,
    val emoji: String
)

/**
 * JUEGO 2: "Scroll sin Caer"
 * El jugador debe leer/scrollear un apunte escolar vectorizado de principio a fin.
 * A mitad del scroll saltan anuncios invasivos de casinos, préstamos de $20.000 e influencers
 * que bloquean la lectura. Para destruirlos debe AGITAR EL TELÉFONO usando el Acelerómetro.
 */
@Composable
fun ScrollAdsGameScreen(
    playerName: String,
    onBackToHub: () -> Unit,
    onGameFinished: (adsShakenOff: Int, adTrapsClicked: Int, elapsedMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val adsCatalog = remember {
        listOf(
            InvasiveAdSpec(
                id = 1,
                triggerProgress = 0.16f,
                badge = "PUBLICIDAD INVASIVA • INFLUENCER VIP",
                title = "¡GANÁ CON $2.000 MIENTRAS ENTRENÁS!",
                influencerOrSource = "@TomiFitness_Millionaire",
                bodyText = "«Bro, dejá de estudiar. Yo generé mi riqueza desde el gimnasio apostando $2.000 en B-Play VIP. El primer tiro tiene ganancia 100% garantizada.»",
                trapButtonText = "Instalar App y Apostar $4.000",
                accentColor = BordeMagenta,
                emoji = "💪"
            ),
            InvasiveAdSpec(
                id = 2,
                triggerProgress = 0.42f,
                badge = "ALERTA DE BILLETERA • CRÉDITO AL TOQUE",
                title = "¡PRÉSTAMO INMEDIATO DE $20.000!",
                influencerOrSource = "Cajero Express 24/7",
                bodyText = "Te acreditamos $20.000 ahora mismo sin recibo ni firma de tus padres. Usalo en la ruleta en vivo y devolvelo cuando ganes.",
                trapButtonText = "Aceptar Préstamo de $20.000",
                accentColor = Color(0xFFFFB300),
                emoji = "💸"
            ),
            InvasiveAdSpec(
                id = 3,
                triggerProgress = 0.68f,
                badge = "SPAM DE WHATSAPP • SUPERCLÁSICO",
                title = "¡CUOTA X10 EN EL SUPERCLÁSICO!",
                influencerOrSource = "Grupo Fijas del Colegio",
                bodyText = "Todos tus compañeros ya cargaron fichas. Si no entrás en 5 segundos te quedás afuera del pozo acumulado.",
                trapButtonText = "Transferir $4.000 al Cajero",
                accentColor = RojoAlerta,
                emoji = "⚽"
            ),
            InvasiveAdSpec(
                id = 4,
                triggerProgress = 0.88f,
                badge = "POP-UP TRAMPA • CASINO CLANDESTINO",
                title = "¡NO CIERRES! GIRO GRATIS PENDIENTE",
                influencerOrSource = "Casino Nocturno .bet",
                bodyText = "El botón 'X' de arriba es falso. Sacudí fuerte el teléfono para romper este bloqueo publicitario sin entregar tus ahorros.",
                trapButtonText = "Reclamar Bono Trampa ($4.000)",
                accentColor = BordeCyan,
                emoji = "🎰"
            )
        )
    }

    val triggeredAdIds = remember { mutableStateListOf<Int>() }
    var activeAd by remember { mutableStateOf<InvasiveAdSpec?>(null) }
    var shakeProgress by remember { mutableFloatStateOf(0f) }
    var adsShakenCount by remember { mutableIntStateOf(0) }
    var trapsClickedCount by remember { mutableIntStateOf(0) }
    var startTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var remainingSeconds by remember { mutableIntStateOf(60) }
    val shakeWobble = remember { Animatable(0f) }

    val scrollFraction by remember {
        derivedStateOf {
            if (scrollState.maxValue <= 0) 0f
            else (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        }
    }

    // Monitoreo del Acelerómetro en tiempo real respetando el ciclo de vida
    val sensorReading by rememberAccelerometerMonitor(
        enabled = true,
        shakeThresholdG = 1.38f,
        minIntervalMs = 95L
    ) { gForce ->
        if (activeAd != null) {
            HapticFeedbackHelper.vibrateShort(context, 35L)
            SoundManager.playClick()
            val increment = ((gForce - 1.15f) * 0.24f).coerceIn(0.18f, 0.36f)
            val newProgress = (shakeProgress + increment).coerceAtMost(1f)
            shakeProgress = newProgress

            scope.launch {
                shakeWobble.snapTo(if (shakeWobble.value >= 0f) -14f else 14f)
                shakeWobble.animateTo(0f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
            }

            if (newProgress >= 1f) {
                adsShakenCount++
                activeAd = null
                shakeProgress = 0f
            }
        }
    }

    // Disparar anuncio invasivo según el progreso de scroll del PDF vectorizado
    LaunchedEffect(scrollFraction) {
        if (activeAd == null) {
            val nextAd = adsCatalog.firstOrNull { ad ->
                scrollFraction >= ad.triggerProgress && !triggeredAdIds.contains(ad.id)
            }
            if (nextAd != null) {
                triggeredAdIds.add(nextAd.id)
                shakeProgress = 0f
                activeAd = nextAd
                HapticFeedbackHelper.vibrateAlertPattern(context)
            }
        }
    }

    // Reloj de cuenta regresiva del juego
    LaunchedEffect(Unit) {
        startTimeMillis = System.currentTimeMillis()
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        val elapsed = System.currentTimeMillis() - startTimeMillis
        onGameFinished(adsShakenCount, trapsClickedCount, elapsed)
    }

    val animatedReadProgress by animateFloatAsState(
        targetValue = scrollFraction,
        animationSpec = tween(200),
        label = "readProgress"
    )

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
        ) {
            // 1. HEADER CON PROGRESO DEL APUNTE Y TELEMETRÍA DEL SENSOR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161922))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = "Juego 2: Scroll sin Caer",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Scrolleá el PDF • Agitá el celu si salta spam",
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.5.sp,
                                color = BordeCyan
                            )
                        }
                    }

                    // Reloj y Contador de Ads destruidos
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VerdeExito.copy(alpha = 0.18f))
                                .border(1.dp, VerdeExito.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📳 $adsShakenCount/4",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = VerdeExito
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (remainingSeconds <= 12) RojoAlerta.copy(alpha = 0.25f)
                                    else BordeMagenta.copy(alpha = 0.18f)
                                )
                                .border(
                                    1.dp,
                                    if (remainingSeconds <= 12) RojoAlerta else BordeMagenta,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${remainingSeconds}s",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barra de progreso de lectura del PDF y estado del Acelerómetro en vivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lectura del PDF: ${(animatedReadProgress * 100).toInt()}%",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Acelerómetro: ${"%.2f".format(sensorReading.gForce)} G",
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.sp,
                        color = if (sensorReading.gForce > 1.35f) VerdeExito else TextoSecundario
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                LinearProgressIndicator(
                    progress = { animatedReadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = BordeCyan,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. VISOR DE DOCUMENTO / PDF VECTORIZADO ESCOLAR (SCROLLEABLE)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(
                        state = scrollState,
                        enabled = activeAd == null // Se bloquea el scroll cuando salta una publicidad invasiva
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VectorPdfPageCard(
                    pageNumber = 1,
                    totalPages = 6,
                    tag = "APUNTE DE MATEMÁTICA • 5.° AÑO",
                    title = "1. ¿Por qué las casas de apuestas nunca pierden?",
                    bulletPoints = listOf(
                        "Toda plataforma de apuestas aplica un margen matemático llamado 'Ventaja de la Casa' (House Edge).",
                        "En una ruleta o en cuotas deportivas, la suma de probabilidades implícitas supera el 100%.",
                        "Cuanto más tiempo jugás, más se acerca tu saldo real a la pérdida estadística programada."
                    ),
                    diagramType = 1
                )

                VectorPdfPageCard(
                    pageNumber = 2,
                    totalPages = 6,
                    tag = "PSICOLOGÍA DEL CONSUMO • PUBLICIDAD",
                    title = "2. El truco de los Influencers y las 'Cuentas Demo'",
                    bulletPoints = listOf(
                        "Muchos creadores promocionan casinos ilegales mostrando supuestos retiros millonarios.",
                        "Esas capturas se graban con 'cuentas demo' cargadas con saldo ficticio por el propio casino.",
                        "El influencer cobra una comisión por cada adolescente que se registra y pierde sus ahorros."
                    ),
                    diagramType = 2
                )

                VectorPdfPageCard(
                    pageNumber = 3,
                    totalPages = 6,
                    tag = "ECONOMÍA COTIDIANA • BILLETERAS",
                    title = "3. Fricción Cero y Préstamos Instantáneos",
                    bulletPoints = listOf(
                        "Pagar con huella o QR anestesia la percepción de pérdida frente al billete físico.",
                        "Los 'cajeros de WhatsApp' y apps de crédito rápido ofrecen préstamos para que apuestes plata que no tenés.",
                        "Nunca uses el dinero de la SUBE, apuntes o transferencias familiares para intentar 'duplicar'."
                    ),
                    diagramType = 3
                )

                VectorPdfPageCard(
                    pageNumber = 4,
                    totalPages = 6,
                    tag = "NEUROCIENCIA • DOPAMINA",
                    title = "4. El Efecto 'Casi Ganás' (Near-Miss)",
                    bulletPoints = listOf(
                        "Cuando salen dos símbolos iguales y el tercero pasa rozando, tu cerebro libera dopamina como si hubieras ganado.",
                        "En los casinos digitales ese 'casi acierto' no es casualidad: está programado por software para que sigas tirando.",
                        "Reconocer este estímulo te permite cortar a tiempo."
                    ),
                    diagramType = 1
                )

                VectorPdfPageCard(
                    pageNumber = 5,
                    totalPages = 6,
                    tag = "PREVENCIÓN ESCOLAR • REDES",
                    title = "5. El sesgo de las capturas en el grupo del curso",
                    bulletPoints = listOf(
                        "En los grupos de WhatsApp solo se comparten los tickets ganadores (1 de cada 20).",
                        "Nadie sube las 19 recargas perdidas de la semana. Eso genera la falsa sensación de que 'todos ganan menos vos'.",
                        "Invertir en planes reales (fútbol, cine, salidas) construye bienestar duradero."
                    ),
                    diagramType = 2
                )

                VectorPdfPageCard(
                    pageNumber = 6,
                    totalPages = 6,
                    tag = "RESUMEN FINAL • CANALES DE APOYO",
                    title = "6. Conclusión del Apunte y Cierre de Lectura",
                    bulletPoints = listOf(
                        "¡Llegaste a la última página del documento esquivando las trampas publicitarias!",
                        "Si algún amigo o compañero está pasando por esto, la Línea 141 y Boti atienden gratis y de forma confidencial 24/7.",
                        "Tocá el botón de abajo para entregar la lectura y registrar tu puntaje en el Ranking."
                    ),
                    diagramType = 3
                )

                // Botón final al terminar de scrollear las 6 páginas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                        .clickable(enabled = activeAd == null) {
                            SoundManager.playClick()
                            val elapsed = System.currentTimeMillis() - startTimeMillis
                            onGameFinished(adsShakenCount, trapsClickedCount, elapsed)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entregar Apunte Leído y Ver Puntaje",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 3. OVERLAY DE ANUNCIO INVASIVO (SE QUITA AGITANDO EL TELÉFONO CON EL ACELERÓMETRO)
        AnimatedVisibility(
            visible = activeAd != null,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.88f),
            exit = fadeOut(tween(180))
        ) {
            val ad = activeAd ?: return@AnimatedVisibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.86f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = shakeWobble.value
                            rotationZ = shakeWobble.value * 0.25f
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1A1025))
                        .border(2.5.dp, ad.accentColor, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cabecera del Pop-up invasivo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ad.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ad.badge,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = ad.accentColor
                            )
                        }

                        // Falsa X diminuta que avisa que está bloqueado
                        Text(
                            text = "[X Bloqueado]",
                            fontFamily = SpaceGrotesk,
                            fontSize = 10.sp,
                            color = TextoSecundario
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = ad.emoji,
                        fontSize = 44.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = ad.title,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = ad.influencerOrSource,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = BordeCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = ad.bodyText,
                        fontFamily = SpaceGrotesk,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // INSTRUCCIÓN PRINCIPAL DE SENSOR (ACELERÓMETRO)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0E1624))
                            .border(1.5.dp, BordeCyan, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = BordeCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¡AGITÁ EL CELULAR PARA CERRAR EL SPAM!",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = BordeCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { shakeProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = VerdeExito,
                            trackColor = Color.White.copy(alpha = 0.14f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resistencia al Spam: ${(shakeProgress * 100).toInt()}%",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = Color.White
                            )

                            // Botón auxiliar por si se prueba desde un emulador sin acelerómetro físico
                            Text(
                                text = "[Simular sacudida]",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = VerdeExito,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        HapticFeedbackHelper.vibrateShort(context, 30L)
                                        SoundManager.playClick()
                                        shakeProgress = (shakeProgress + 0.34f).coerceAtMost(1f)
                                        if (shakeProgress >= 1f) {
                                            adsShakenCount++
                                            activeAd = null
                                            shakeProgress = 0f
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón Trampa del Anuncio (si el usuario cae y lo toca en vez de agitar, suma deuda)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ad.accentColor.copy(alpha = 0.22f))
                            .border(1.dp, ad.accentColor, RoundedCornerShape(12.dp))
                            .clickable {
                                SoundManager.playClick()
                                trapsClickedCount++
                                activeAd = null
                                shakeProgress = 0f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️ ${ad.trapButtonText} (Trampa)",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Página individual del PDF / Apunte escolar vectorizado con gráficos dibujados en Canvas.
 */
@Composable
private fun VectorPdfPageCard(
    pageNumber: Int,
    totalPages: Int,
    tag: String,
    title: String,
    bulletPoints: List<String>,
    diagramType: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF151924))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Cabecera estilo hoja de PDF académico
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                color = BordeCyan
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BurbujaChat)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PÁG. $pageNumber / $totalPages",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 17.5.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Ilustración vectorial en Canvas dentro del apunte PDF
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D1117))
        ) {
            val w = size.width
            val h = size.height

            when (diagramType) {
                1 -> {
                    // Gráfico vectorial: Curva de saldo del jugador cayendo frente a la ventaja de la casa
                    val path = Path().apply {
                        moveTo(24f, h * 0.25f)
                        lineTo(w * 0.25f, h * 0.18f)
                        lineTo(w * 0.48f, h * 0.52f)
                        lineTo(w * 0.72f, h * 0.68f)
                        lineTo(w - 24f, h * 0.85f)
                    }
                    drawPath(path = path, color = RojoAlerta, style = Stroke(width = 5f))
                    drawLine(
                        color = VerdeExito.copy(alpha = 0.6f),
                        start = Offset(24f, h * 0.35f),
                        end = Offset(w - 24f, h * 0.35f),
                        strokeWidth = 3f
                    )
                }
                2 -> {
                    // Gráfico vectorial: 1 ticket ganador visible vs 9 tickets perdedores ocultos
                    val barCount = 8
                    val barW = (w - 80f) / barCount
                    for (i in 0 until barCount) {
                        val isWinner = i == 2
                        val barH = if (isWinner) h * 0.70f else h * 0.28f
                        drawRoundRect(
                            color = if (isWinner) BordeCyan else RojoAlerta.copy(alpha = 0.55f),
                            topLeft = Offset(36f + i * barW, h - barH - 12f),
                            size = Size(barW * 0.65f, barH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                else -> {
                    // Gráfico vectorial: Escudo de ahorro y bienestar vs intereses de deuda
                    drawCircle(
                        color = VerdeExito.copy(alpha = 0.25f),
                        radius = h * 0.36f,
                        center = Offset(w * 0.30f, h * 0.5f)
                    )
                    drawCircle(
                        color = BordeMagenta.copy(alpha = 0.25f),
                        radius = h * 0.36f,
                        center = Offset(w * 0.70f, h * 0.5f)
                    )
                    drawLine(
                        color = BordeCyan,
                        start = Offset(w * 0.30f, h * 0.5f),
                        end = Offset(w * 0.70f, h * 0.5f),
                        strokeWidth = 4f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        bulletPoints.forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BordeCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = point,
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }
        }
    }
}
