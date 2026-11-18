package com.ifts18.unadecisionmas.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.GameModes
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import kotlin.math.abs

private val StoryBg = Color(0xFF0B0F17)
private val StoryTextPrimary = Color(0xFFF2EFE8)
private val StoryTextMuted = Color(0xFFA9AEBB)
private val StoryTrack = Color(0xFF262B36)
private val StoryAccent = Color(0xFF2FE0F2)
private val StoryCoral = Color(0xFFFF7A6B)
private val StoryButtonText = Color(0xFF04161B)

internal const val STORY_SLIDE_DURATION_MS = 10_000L

internal enum class StoryGraphicType {
    NONE,
    ROULETTE_BALANCE_CURVE,
    SLOTS_RISING_BARS,
    ADS_FEED_PATTERN
}

internal data class StoryParagraph(
    val text: String,
    val isBold: Boolean = false,
    val isAccent: Boolean = false
)

internal data class DidacticStorySlide(
    val headline: String,
    val headlineFontSizeSp: Int = 44,
    val graphic: StoryGraphicType = StoryGraphicType.NONE,
    val graphicStartLabel: String = "$5.000",
    val graphicEndLabel: String = "$0",
    val paragraphs: List<StoryParagraph> = emptyList(),
    val listItems: List<String> = emptyList(),
    val footnote: String? = null,
    val isFinalSlide: Boolean = false
)

internal data class DidacticStoryDeck(
    val headerTitle: String,
    val isPositiveOutcome: Boolean,
    val slides: List<DidacticStorySlide>
)

private fun formatStoryPesos(amount: Int): String {
    val formatted = "%,d".format(abs(amount)).replace(',', '.')
    return "$$formatted"
}

internal fun buildReflectionDeck(gameState: GameState): DidacticStoryDeck {
    val playerName = gameState.profile.name.ifBlank { "Mateo" }
    val mode = GameModes.canonicalize(gameState.lastCompletedMode)

    return when (mode) {
        GameModes.LA_RULETA_29 -> {
            val noDeposit = gameState.currentDebt == 0
            val startAmountLabel = if (noDeposit) "$8.000" else formatStoryPesos(gameState.currentDebt)
            DidacticStoryDeck(
                headerTitle = "Ruleta: la trampa",
                isPositiveOutcome = noDeposit,
                slides = listOf(
                    DidacticStorySlide(
                        headline = if (noDeposit) "Frenaste a tiempo." else "No fue tu mala suerte.",
                        headlineFontSizeSp = 48,
                        graphic = StoryGraphicType.ROULETTE_BALANCE_CURVE,
                        graphicStartLabel = startAmountLabel,
                        graphicEndLabel = if (noDeposit) "Límite $30.000" else "$0",
                        paragraphs = listOf(
                            StoryParagraph(
                                if (noDeposit) {
                                    "Paraste antes de cargar tu plata: te dejó acertar al principio para entusiasmarte, pero siempre te frena antes del retiro ($30.000)."
                                } else {
                                    "Cargaste $startAmountLabel, te dejó acertar un par de tiros para entusiasmarte y después te vació."
                                }
                            )
                        )
                    ),
                    DidacticStorySlide(
                        headline = "La ruleta online no tiene suerte. Tiene un programa.",
                        headlineFontSizeSp = 42,
                        paragraphs = listOf(
                            StoryParagraph("Y ese programa ya sabe que, a la larga, la casa gana.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "A veces te deja ganar algo.",
                        headlineFontSizeSp = 44,
                        paragraphs = listOf(
                            StoryParagraph("No porque tengas suerte, sino para que vuelvas y deposites más.")
                        ),
                        footnote = "En las casas legales ese programa está controlado; en las ilegales nadie sabe qué hay detrás."
                    ),
                    DidacticStorySlide(
                        headline = "No existe el 50/50.",
                        headlineFontSizeSp = 44,
                        paragraphs = listOf(
                            StoryParagraph("Hay casinos legales para mayores de 18 años, pero al entrar la comisión por carga, baja muchísimo la probabilidad de salir ganando.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "La casa siempre gana.",
                        headlineFontSizeSp = 48,
                        paragraphs = listOf(
                            StoryParagraph("Decir que no es cuidarte.", isAccent = true)
                        ),
                        isFinalSlide = true
                    )
                )
            )
        }

        GameModes.TRAMPA_JACKPOT -> {
            val noDeposit = gameState.currentDebt == 0
            val depositedLabel = formatStoryPesos(gameState.currentDebt)
            DidacticStoryDeck(
                headerTitle = "Tragamonedas: la trampa",
                isPositiveOutcome = noDeposit,
                slides = listOf(
                    DidacticStorySlide(
                        headline = if (noDeposit) "Frenaste a tiempo." else "No fue tu mala suerte.",
                        headlineFontSizeSp = 48,
                        paragraphs = listOf(
                            StoryParagraph(
                                if (noDeposit) {
                                    "No pusiste plata real. Te dio premios chicos y varios “casi ganás” para que sigas sin llegar nunca al retiro ($30.000)."
                                } else {
                                    "Cargaste $depositedLabel. Te dio premios chicos y varios “casi ganás” para que sigas hasta quedar en cero."
                                }
                            ),
                            StoryParagraph("Está armado así para que pierdas.", isBold = true)
                        )
                    ),
                    DidacticStorySlide(
                        headline = "Casi ganás, casi, casi… pero nunca.",
                        headlineFontSizeSp = 40,
                        graphic = StoryGraphicType.SLOTS_RISING_BARS,
                        paragraphs = listOf(
                            StoryParagraph("Metiste $100, después $200, después $500.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "Las luces y los sonidos te hacen sentir que estás cerca.",
                        headlineFontSizeSp = 36,
                        paragraphs = listOf(
                            StoryParagraph("Pero la máquina está programada para quedarse con parte de lo que entra y darte premios chicos para que gastes más.")
                        ),
                        footnote = "En las casas legales al menos ese porcentaje está controlado; en las que no son legales no hay control."
                    ),
                    DidacticStorySlide(
                        headline = "No existe el 50/50.",
                        headlineFontSizeSp = 44,
                        paragraphs = listOf(
                            StoryParagraph("Hay casinos legales para mayores de 18 años, pero al entrar la comisión por carga, baja muchísimo la probabilidad de salir ganando.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "La casa siempre gana.",
                        headlineFontSizeSp = 48,
                        paragraphs = listOf(
                            StoryParagraph("Decir que no es cuidarte.", isAccent = true)
                        ),
                        isFinalSlide = true
                    )
                )
            )
        }

        GameModes.SCROLL_SIN_CAER -> {
            val avoidedAllTraps = gameState.currentDebt == 0
            val trapsCount = gameState.incorrectDecisionsCount
            val trapsText = if (trapsCount == 1) "Tocaste 1 anuncio trampa." else "Tocaste $trapsCount anuncios trampa."
            DidacticStoryDeck(
                headerTitle = "Anuncios: la trampa",
                isPositiveOutcome = avoidedAllTraps,
                slides = listOf(
                    DidacticStorySlide(
                        headline = if (avoidedAllTraps) "Esquivaste la trampa." else "No es casualidad que caigas.",
                        headlineFontSizeSp = 46,
                        paragraphs = listOf(
                            StoryParagraph(
                                if (avoidedAllTraps) {
                                    "Frenaste ${gameState.correctDecisionsCount} publicidades invasivas sin tocar ningún enlace: están armados para tentarte con plata fácil."
                                } else {
                                    "$trapsText No fue tu culpa: están armados para tentarte con plata fácil."
                                }
                            )
                        )
                    ),
                    DidacticStorySlide(
                        headline = "Te aparece un streamer ganando una fortuna.",
                        headlineFontSizeSp = 38,
                        graphic = StoryGraphicType.ADS_FEED_PATTERN,
                        paragraphs = listOf(
                            StoryParagraph("Un bono de “te regalamos plata”. Un anuncio cada 2 videos.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "No es casualidad que te aparezca tanto.",
                        headlineFontSizeSp = 40,
                        paragraphs = listOf(
                            StoryParagraph("Te muestran solo al que gana, nunca a los miles que pierden."),
                            StoryParagraph("Y te regalan saldo al principio para que después pongas la tuya.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "Si te tienen que convencer tanto para que juegues, es porque el que gana no sos vos.",
                        headlineFontSizeSp = 34
                    ),
                    DidacticStorySlide(
                        headline = "No existe el 50/50.",
                        headlineFontSizeSp = 44,
                        paragraphs = listOf(
                            StoryParagraph("Hay casinos legales para mayores de 18 años, pero al entrar la comisión por carga, baja muchísimo la probabilidad de salir ganando.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "La casa siempre gana.",
                        headlineFontSizeSp = 48,
                        paragraphs = listOf(
                            StoryParagraph("Decir que no es cuidarte.", isAccent = true)
                        ),
                        isFinalSlide = true
                    )
                )
            )
        }

        else -> {
            val cleanWeek = gameState.currentDebt == 0 && gameState.incorrectDecisionsCount == 0
            DidacticStoryDeck(
                headerTitle = "Una decisión más",
                isPositiveOutcome = cleanWeek,
                slides = listOf(
                    DidacticStorySlide(
                        headline = if (cleanWeek) "¡Felicitaciones, $playerName!" else "No fue tu culpa.",
                        headlineFontSizeSp = if (cleanWeek) 44 else 52,
                        paragraphs = listOf(
                            StoryParagraph(
                                if (cleanWeek) {
                                    "No apostaste ni pediste plata prestada en toda la semana. Cuidaste tus ahorros y tu tranquilidad."
                                } else {
                                    "Si apostaste o pediste plata prestada, no es tu culpa: estos juegos están arreglados para engancharte y hacerte perder."
                                }
                            )
                        )
                    ),
                    DidacticStorySlide(
                        headline = "Las apuestas no solo te sacan dinero.",
                        headlineFontSizeSp = 40,
                        paragraphs = listOf(
                            StoryParagraph("Te sacan la tranquilidad,\nte dejan con:")
                        ),
                        listItems = listOf(
                            "Ansiedad",
                            "Culpa",
                            "Pendiente del celular",
                            "Alejado de tus amigos",
                            "Sin ganas de nada"
                        )
                    ),
                    DidacticStorySlide(
                        headline = "Y te hacen creer que el problema sos vos.",
                        headlineFontSizeSp = 42,
                        paragraphs = listOf(
                            StoryParagraph("Pero no es tu suerte. Está hecho para que pierdas.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "No existe el 50/50.",
                        headlineFontSizeSp = 44,
                        paragraphs = listOf(
                            StoryParagraph("Hay casinos legales para mayores de 18 años, pero al entrar la comisión por carga, baja muchísimo la probabilidad de salir ganando.")
                        )
                    ),
                    DidacticStorySlide(
                        headline = "La casa siempre gana.",
                        headlineFontSizeSp = 48,
                        paragraphs = listOf(
                            StoryParagraph("Decir que no es cuidarte.", isAccent = true)
                        ),
                        isFinalSlide = true
                    )
                )
            )
        }
    }
}

/**
 * Carrusel estilo Historias de Instagram (5s por historia, tap derecha/izquierda y pausa al mantener presionado)
 * basado en el diseño de "Tarjeta La trampa - ruleta online.html".
 */
@Composable
fun DidacticReflectionOverlay(
    gameState: GameState,
    onDismissToSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val deck = remember(
        gameState.lastCompletedMode,
        gameState.currentDebt,
        gameState.correctDecisionsCount,
        gameState.incorrectDecisionsCount,
        gameState.profile.name
    ) {
        buildReflectionDeck(gameState)
    }

    val slides = deck.slides
    var currentSlideIndex by remember(deck) { mutableIntStateOf(0) }
    var slideResetCount by remember(deck) { mutableIntStateOf(0) }
    var slideProgress by remember(deck) { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var isLifecycleResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isLifecycleResumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun goNextSlide() {
        if (currentSlideIndex < slides.lastIndex) {
            currentSlideIndex += 1
        } else {
            onDismissToSummary()
        }
    }

    fun goPreviousSlide() {
        if (currentSlideIndex > 0) {
            currentSlideIndex -= 1
        } else {
            slideResetCount += 1
        }
    }

    // Temporizador de 5 segundos por historia; se congela mientras el usuario mantiene presionada la pantalla
    LaunchedEffect(currentSlideIndex, slideResetCount, slides.size) {
        slideProgress = 0f
        var elapsedMs = 0f
        var lastFrameNanos = 0L

        while (elapsedMs < STORY_SLIDE_DURATION_MS) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L && !isHolding && isLifecycleResumed) {
                    val deltaMs = ((frameNanos - lastFrameNanos) / 1_000_000f).coerceIn(0f, 100f)
                    elapsedMs = (elapsedMs + deltaMs).coerceAtMost(STORY_SLIDE_DURATION_MS.toFloat())
                    slideProgress = (elapsedMs / STORY_SLIDE_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                }
                lastFrameNanos = frameNanos
            }
        }

        if (currentSlideIndex < slides.lastIndex) {
            currentSlideIndex += 1
        } else {
            onDismissToSummary()
        }
    }

    BackHandler(enabled = true) {
        SoundManager.playClick()
        if (currentSlideIndex > 0) {
            currentSlideIndex -= 1
        } else {
            onDismissToSummary()
        }
    }

    val activeSlide = slides[currentSlideIndex.coerceIn(0, slides.lastIndex)]

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StoryBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(slides.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val startX = down.position.x
                    isHolding = true

                    val up = waitForUpOrCancellation()
                    isHolding = false

                    if (up != null && !up.isConsumed) {
                        val pressDuration = System.currentTimeMillis() - downTime
                        val deltaX = up.position.x - startX
                        when {
                            pressDuration < 350L && abs(deltaX) > 64f -> {
                                SoundManager.playClick()
                                if (deltaX < 0f) goNextSlide() else goPreviousSlide()
                            }
                            pressDuration < 250L -> {
                                SoundManager.playClick()
                                if (up.position.x < size.width / 2f) {
                                    goPreviousSlide()
                                } else {
                                    goNextSlide()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val isCompactScreen = maxHeight < 700.dp
        val fontScale = if (isCompactScreen) 0.84f else 1f

        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera idéntica al HTML: título gris a la izquierda + botón circular X a la derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 20.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deck.headerTitle,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = StoryTextMuted
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, StoryTrack, CircleShape)
                        .clickable {
                            SoundManager.playClick()
                            onDismissToSummary()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CloseCrossIcon(modifier = Modifier.size(20.dp))
                }
            }

            // Cuerpo de la diapositiva con barras de progreso tipo Instagram Stories
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = 28.dp,
                        end = 28.dp,
                        top = if (isCompactScreen) 16.dp else 24.dp,
                        bottom = if (isCompactScreen) 20.dp else 32.dp
                    )
            ) {
                // Barras de progreso de historias (5s cada una)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slides.forEachIndexed { idx, _ ->
                        val fillFraction = when {
                            idx < currentSlideIndex -> 1f
                            idx == currentSlideIndex -> slideProgress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(StoryTrack)
                        ) {
                            if (fillFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fillFraction)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(StoryAccent)
                                )
                            }
                        }
                    }
                }

                // Contenido central animado entre historias
                AnimatedContent(
                    targetState = activeSlide,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "storySlideContent"
                ) { slide ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = if (isCompactScreen) 14.dp else 24.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            if (isCompactScreen) 16.dp else 24.dp,
                            Alignment.CenterVertically
                        )
                    ) {
                        val scaledHeadlineSp = (slide.headlineFontSizeSp * fontScale).sp
                        Text(
                            text = slide.headline,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = scaledHeadlineSp,
                            lineHeight = scaledHeadlineSp * 1.07f,
                            letterSpacing = (-0.8).sp,
                            color = StoryTextPrimary
                        )

                        when (slide.graphic) {
                            StoryGraphicType.ROULETTE_BALANCE_CURVE -> {
                                RouletteBalanceCurveGraphic(
                                    startLabel = slide.graphicStartLabel,
                                    endLabel = slide.graphicEndLabel,
                                    compact = isCompactScreen
                                )
                            }
                            StoryGraphicType.SLOTS_RISING_BARS -> {
                                SlotsRisingBarsGraphic(compact = isCompactScreen)
                            }
                            StoryGraphicType.ADS_FEED_PATTERN -> {
                                AdsFeedPatternGraphic()
                            }
                            StoryGraphicType.NONE -> {}
                        }

                        slide.paragraphs.forEach { paragraph ->
                            val bodySp = (25f * fontScale).sp
                            Text(
                                text = paragraph.text,
                                fontFamily = SpaceGrotesk,
                                fontWeight = when {
                                    paragraph.isBold -> FontWeight.Bold
                                    paragraph.isAccent -> FontWeight.Medium
                                    else -> FontWeight.Normal
                                },
                                fontSize = bodySp,
                                lineHeight = bodySp * 1.32f,
                                color = if (paragraph.isAccent) StoryAccent else StoryTextPrimary
                            )
                        }

                        if (slide.listItems.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val itemSp = (24f * fontScale).sp
                                slide.listItems.forEachIndexed { idx, item ->
                                    HorizontalDivider(thickness = 1.dp, color = StoryTrack)
                                    Text(
                                        text = item,
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = itemSp,
                                        lineHeight = itemSp * 1.2f,
                                        color = StoryTextPrimary,
                                        modifier = Modifier.padding(
                                            vertical = if (isCompactScreen) 9.dp else 13.dp
                                        )
                                    )
                                    if (idx == slide.listItems.lastIndex) {
                                        HorizontalDivider(thickness = 1.dp, color = StoryTrack)
                                    }
                                }
                            }
                        }

                        slide.footnote?.let { subText ->
                            val subSp = (19f * fontScale).sp
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(thickness = 1.dp, color = StoryTrack)
                                Text(
                                    text = subText,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = subSp,
                                    lineHeight = subSp * 1.4f,
                                    color = StoryTextMuted,
                                    modifier = Modifier.padding(top = if (isCompactScreen) 14.dp else 20.dp)
                                )
                            }
                        }
                    }
                }

                // Pie fijo en la última historia: botón "Ver resumen de la partida"
                if (activeSlide.isFinalSlide) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StoryAccent)
                            .clickable {
                                SoundManager.playClick()
                                onDismissToSummary()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ver resumen de la partida",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = StoryButtonText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouletteBalanceCurveGraphic(
    startLabel: String,
    endLabel: String,
    compact: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 76.dp else 96.dp)
        ) {
            val w = size.width
            val h = size.height
            fun pt(x: Float, y: Float) = Offset(x / 334f * w, y / 104f * h)

            val upPath = Path().apply {
                val p0 = pt(2f, 28f)
                val p1 = pt(52f, 22f)
                val p2 = pt(90f, 12f)
                val p3 = pt(124f, 26f)
                val p4 = pt(160f, 10f)
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
            }
            val downPath = Path().apply {
                val p4 = pt(160f, 10f)
                val p5 = pt(196f, 30f)
                val p6 = pt(240f, 62f)
                val p7 = pt(284f, 88f)
                val p8 = pt(326f, 98f)
                moveTo(p4.x, p4.y)
                lineTo(p5.x, p5.y)
                lineTo(p6.x, p6.y)
                lineTo(p7.x, p7.y)
                lineTo(p8.x, p8.y)
            }

            val stroke = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            drawPath(path = upPath, color = StoryTextPrimary, style = stroke)
            drawPath(path = downPath, color = StoryCoral, style = stroke)
            drawCircle(
                color = StoryCoral,
                radius = 4.5.dp.toPx(),
                center = pt(326f, 98f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = startLabel,
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                color = StoryTextMuted
            )
            Text(
                text = endLabel,
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                color = StoryCoral
            )
        }
    }
}

@Composable
private fun SlotsRisingBarsGraphic(compact: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 84.dp else 104.dp)
        ) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

            fun drawScaledBar(x: Float, y: Float, bw: Float, bh: Float, color: Color) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x / 334f * w, y / 110f * h),
                    size = Size(bw / 334f * w, bh / 110f * h),
                    cornerRadius = corner
                )
            }

            drawScaledBar(10f, 90f, 88f, 20f, StoryTextMuted)
            drawScaledBar(123f, 70f, 88f, 40f, StoryTextMuted)
            drawScaledBar(236f, 10f, 88f, 100f, StoryCoral)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Text(
                text = "$100",
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                color = StoryTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$200",
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                color = StoryTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$500",
                fontFamily = SpaceGrotesk,
                fontSize = 15.sp,
                color = StoryCoral,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdsFeedPatternGraphic() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            val xs = floatArrayOf(0f, 37.8f, 75.5f, 113.3f, 151f, 188.8f, 226.5f, 264.3f, 302f)
            xs.forEachIndexed { index, x ->
                val isAd = index % 3 == 2
                drawRoundRect(
                    color = if (isAd) StoryCoral else StoryTrack,
                    topLeft = Offset(x / 334f * w, 0f),
                    size = Size(32f / 334f * w, h),
                    cornerRadius = corner
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Text(
                text = "Video",
                fontFamily = SpaceGrotesk,
                fontSize = 14.sp,
                color = StoryTextMuted,
                modifier = Modifier.weight(0.226f)
            )
            Text(
                text = "Anuncio",
                fontFamily = SpaceGrotesk,
                fontSize = 14.sp,
                color = StoryCoral,
                modifier = Modifier.weight(0.774f)
            )
        }
    }
}

@Composable
private fun CloseCrossIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val start = size.width * 0.25f
        val end = size.width * 0.75f
        drawLine(
            color = StoryTextPrimary,
            start = Offset(start, start),
            end = Offset(end, end),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = StoryTextPrimary,
            start = Offset(end, start),
            end = Offset(start, end),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

