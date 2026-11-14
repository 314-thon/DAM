package com.ifts18.unadecisionmas.ui.screens

import android.view.SoundEffectConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.sensor.HapticFeedbackHelper
import com.ifts18.unadecisionmas.ui.components.SoundToggleButton
import com.ifts18.unadecisionmas.ui.components.TutorialCoachmarkOverlay
import com.ifts18.unadecisionmas.ui.components.TutorialStep
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.GlowCyan
import com.ifts18.unadecisionmas.ui.theme.GlowVioleta
import com.ifts18.unadecisionmas.ui.theme.JuegoLimpioTheme
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.delay

private enum class IntroLogoStage {
    SPLASH_LOADING,
    TRAVELING_TO_HEADER,
    SETTLED
}

private data class HubGameCardSpec(
    val modeKey: String,
    val title: String,
    val categoryTag: String,
    val defaultBadge: String,
    @DrawableRes val coverImageRes: Int,
    val borderAccent: Color,
    val dotColor: Color,
    val onClick: () -> Unit
)

// El archivo Lottie R.raw.juego_limpio_jackpot (245 frames) arma el logo completo entre 0.0f y 0.50f
// y entre 0.70f y 1.0f reproduce una salida donde se desvanece a 0.
// En 0.50f (frame 122) el logo completo, marco, moneda y letras quedan al 100% visibles y estáticos.
private const val LOTTIE_HERO_SETTLED_PROGRESS = 0.50f

@Composable
fun WelcomeScreen(
    initialPlayerName: String = "Mateo",
    initialAvatarResId: Int = R.drawable.avatar_1,
    hasSavedGame: Boolean = false,
    savedGameDayName: String = "",
    unlockedAchievementsCount: Int = 0,
    bestScoresByMode: Map<String, Int> = emptyMap(),
    isTutorialCompleted: Boolean = true,
    hasPlayedIntroSplash: Boolean = false,
    onProfileChanged: (String, Int) -> Unit = { _, _ -> },
    onIntroSplashCompleted: () -> Unit = {},
    onTutorialCompleted: () -> Unit = {},
    onRestartTutorial: () -> Unit = {},
    onStartWeekGame: (String, Int) -> Unit,
    onResumeSavedGame: () -> Unit,
    onStartScrollAdsGame: (String, Int) -> Unit,
    onStartSlotsTrapGame: (String, Int) -> Unit,
    onStartRouletteTrapGame: (String, Int) -> Unit = { _, _ -> },
    onOpenLeaderboard: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var playerName by remember(initialPlayerName) {
        mutableStateOf(initialPlayerName.ifBlank { "Mateo" })
    }
    var selectedAvatarResId by remember(initialAvatarResId) {
        mutableIntStateOf(if (initialAvatarResId != 0) initialAvatarResId else R.drawable.avatar_1)
    }

    // Control de la pantalla de carga inicial (Splash), viaje del logo y estado quieto en cabecera
    var introStage by remember {
        mutableStateOf(
            if (hasPlayedIntroSplash) IntroLogoStage.SETTLED else IntroLogoStage.SPLASH_LOADING
        )
    }
    var splashSoundTriggered by remember { mutableStateOf(hasPlayedIntroSplash) }
    val isSplashLoading = introStage == IntroLogoStage.SPLASH_LOADING
    val isLogoSettled = introStage == IntroLogoStage.SETTLED

    // Control de modales del Hub (Edición de Apodo, Selector de Avatares, Modal Partida Guardada)
    var showNicknameModal by remember { mutableStateOf(false) }
    var showAvatarModal by remember { mutableStateOf(false) }
    var showSavedWeekModal by remember { mutableStateOf(false) }

    // Control del Tutorial Guiado de 5 pasos (se activa cuando el logo ya terminó de viajar a su lugar)
    var activeTutorialStep by remember { mutableStateOf<TutorialStep?>(null) }
    var hasAutoStartedTutorial by remember { mutableStateOf(false) }
    var hasConfirmedAvatarInTutorial by remember { mutableStateOf(false) }

    // Coordenadas en Root de los elementos para el recorte iluminado del tutorial
    var nicknameBounds by remember { mutableStateOf<Rect?>(null) }
    var avatarBounds by remember { mutableStateOf<Rect?>(null) }
    var muteButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var gamesGridBounds by remember { mutableStateOf<Rect?>(null) }
    var rankingButtonBounds by remember { mutableStateOf<Rect?>(null) }

    val avatarResList = remember {
        listOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5
        )
    }

    val playClickFeedback: () -> Unit = {
        try {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}
        SoundManager.playClick()
    }

    val currentCleanName: () -> String = {
        playerName.trim().ifEmpty { "Mateo" }
    }

    // Composición Lottie del logo "Juego Limpio" limitada hasta su punto máximo (0.50f)
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.juego_limpio_jackpot)
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isSplashLoading,
        clipSpec = LottieClipSpec.Progress(min = 0f, max = LOTTIE_HERO_SETTLED_PROGRESS),
        iterations = 1,
        speed = 0.85f
    )

    // Cuando termina de armarse el logo en pantalla completa (0.50f):
    // 1) Suena el efecto de casino y vibra
    // 2) Pasa a TRAVELING_TO_HEADER para que el logo viaje a su lugar superior
    // 3) Al terminar el viaje, queda SETTLED y quieto en la pantalla principal
    val isSplashAnimDone = composition != null &&
        lottieProgress >= (LOTTIE_HERO_SETTLED_PROGRESS - 0.01f)

    LaunchedEffect(isSplashAnimDone) {
        if (isSplashAnimDone && introStage == IntroLogoStage.SPLASH_LOADING && !splashSoundTriggered) {
            splashSoundTriggered = true
            SoundManager.playCasinoSplashSound()
            HapticFeedbackHelper.vibrateShort(context, 65L)
            delay(260L)
            if (introStage == IntroLogoStage.SPLASH_LOADING) {
                introStage = IntroLogoStage.TRAVELING_TO_HEADER
            }
        }
    }

    // Fallback de seguridad por si Lottie tarda en inicializar en algún dispositivo
    LaunchedEffect(Unit) {
        delay(2500L)
        if (introStage == IntroLogoStage.SPLASH_LOADING) {
            if (!splashSoundTriggered) {
                splashSoundTriggered = true
                SoundManager.playCasinoSplashSound()
                HapticFeedbackHelper.vibrateShort(context, 65L)
            }
            introStage = IntroLogoStage.TRAVELING_TO_HEADER
        }
    }

    // Completa el viaje del logo hacia la cabecera sin cancelarse por el cambio a TRAVELING_TO_HEADER
    LaunchedEffect(introStage) {
        if (introStage == IntroLogoStage.TRAVELING_TO_HEADER) {
            delay(720L)
            introStage = IntroLogoStage.SETTLED
            onIntroSplashCompleted()
        }
    }

    // Inicia automáticamente el tutorial en el primer lanzamiento apenas el logo queda fijo en cabecera
    LaunchedEffect(isLogoSettled, isTutorialCompleted) {
        if (isLogoSettled && !isTutorialCompleted && !hasAutoStartedTutorial) {
            hasAutoStartedTutorial = true
            scrollState.scrollTo(0)
            activeTutorialStep = TutorialStep.NICKNAME
        }
    }

    // Si se reinicia o muestra el tutorial, garantiza que el scroll vuelva arriba para encuadrar todos los botones
    LaunchedEffect(activeTutorialStep != null) {
        if (activeTutorialStep != null && scrollState.value != 0) {
            scrollState.scrollTo(0)
        }
    }

    // Nivel y rango dinámico según medallas desbloqueadas (estilo Stitch "Nivel 4 • Aventurero")
    val playerRankLabel = remember(unlockedAchievementsCount) {
        when (unlockedAchievementsCount) {
            0 -> "Nivel 1 • Principiante"
            1, 2 -> "Nivel 2 • Explorador Consciente"
            3, 4 -> "Nivel 4 • Aventurero"
            5, 6 -> "Nivel 6 • Guardián del Saldo"
            else -> "Nivel 7 • Leyenda Intocable"
        }
    }

    val hubAlpha by animateFloatAsState(
        targetValue = if (isSplashLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "hubContentAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06181F),
                        Color(0xFF091122),
                        Color(0xFF040810)
                    )
                )
            )
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowCyan.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.50f, size.height * 0.14f),
                        radius = size.maxDimension * 0.65f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowVioleta.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.68f),
                        radius = size.maxDimension * 0.70f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val screenHeight = maxHeight
        val isCompactHeight = screenHeight < 680.dp
        val settledLogoHeight = if (isCompactHeight) 96.dp else 112.dp
        val cardImageHeight = if (isCompactHeight) 124.dp else 136.dp

        // Animación fluida del viaje del logo desde el centro de la pantalla hasta su cabecera fija
        val logoHeight by animateDpAsState(
            targetValue = if (isSplashLoading) 248.dp else settledLogoHeight,
            animationSpec = tween(
                durationMillis = 680,
                easing = FastOutSlowInEasing
            ),
            label = "logoHeight"
        )
        val logoExtraTopOffset by animateDpAsState(
            targetValue = if (isSplashLoading) (screenHeight * 0.22f) else 6.dp,
            animationSpec = tween(
                durationMillis = 680,
                easing = FastOutSlowInEasing
            ),
            label = "logoExtraTopOffset"
        )
        val lottieSplashAlpha by animateFloatAsState(
            targetValue = if (isSplashLoading) 1f else 0f,
            animationSpec = tween(
                durationMillis = 360,
                easing = FastOutSlowInEasing
            ),
            label = "lottieSplashAlpha"
        )
        val appIconAlpha by animateFloatAsState(
            targetValue = if (isSplashLoading) 0f else 1f,
            animationSpec = tween(
                durationMillis = 420,
                easing = FastOutSlowInEasing
            ),
            label = "appIconAlpha"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState, enabled = isLogoSettled && activeTutorialStep == null)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP HUD BAR: PERFIL DEL JUGADOR (AVATAR + APODO + NIVEL) A LA IZQUIERDA + '?' Y SONIDO A LA DERECHA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = hubAlpha },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Perfil del jugador en su ubicación natural superior izquierda (coherente con GameTopBar)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar circular con aro neón cyan-magenta y botón flotante de cámara
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                avatarBounds = coords.boundsInRoot()
                            }
                            .size(52.dp)
                            .clickable(enabled = !isSplashLoading) {
                                playClickFeedback()
                                HapticFeedbackHelper.vibrateShort(context, 35L)
                                showAvatarModal = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = CircleShape,
                                    ambientColor = BordeCyan,
                                    spotColor = BordeMagenta
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(BordeCyan, BordeMagenta)
                                    )
                                )
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = selectedAvatarResId),
                                contentDescription = "Avatar de ${currentCleanName()}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        // Badge flotante de cámara/editar avatar (esquina inferior derecha)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF06B6D4), Color(0xFF2DD4BF))
                                    )
                                )
                                .border(1.5.dp, Color(0xFF091122), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Cambiar avatar",
                                tint = Color(0xFF041019),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Apodo editable + Nivel y rango debajo
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    nicknameBounds = coords.boundsInRoot()
                                }
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable(enabled = !isSplashLoading) {
                                    playClickFeedback()
                                    showNicknameModal = true
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentCleanName(),
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar apodo",
                                tint = BordeCyan,
                                modifier = Modifier.size(12.5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = playerRankLabel.uppercase(),
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.6.sp,
                            color = Color(0xFF2DD4BF).copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Acciones derechas: Repetir Tutorial (?) + Botón de Mute
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B1E28).copy(alpha = 0.90f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable(enabled = !isSplashLoading) {
                                playClickFeedback()
                                HapticFeedbackHelper.vibrateShort(context, 35L)
                                hasAutoStartedTutorial = true
                                hasConfirmedAvatarInTutorial = false
                                showNicknameModal = false
                                showAvatarModal = false
                                onRestartTutorial()
                                activeTutorialStep = TutorialStep.NICKNAME
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BordeCyan
                        )
                    }

                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            muteButtonBounds = coords.boundsInRoot()
                        }
                    ) {
                        SoundToggleButton()
                    }
                }
            }

            // 2. HERO LOGO: Lottie en el Splash inicial -> viaja arriba y pasa al ícono oficial de la app
            Spacer(modifier = Modifier.height(logoExtraTopOffset))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(logoHeight)
                    .drawBehind {
                        if (!isSplashLoading) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GlowCyan.copy(alpha = 0.20f * appIconAlpha),
                                        Color(0xFFF59E0B).copy(alpha = 0.10f * appIconAlpha),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.height * 0.85f
                                ),
                                radius = size.height * 0.85f,
                                center = center
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isLogoSettled && composition != null) {
                    LottieAnimation(
                        composition = composition,
                        // En 0.50f el logo completo de Lottie está al 100% de escala y opacidad
                        progress = {
                            if (isSplashLoading) lottieProgress else LOTTIE_HERO_SETTLED_PROGRESS
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = lottieSplashAlpha }
                    )
                }

                if (!isSplashLoading) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Juego Limpio",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = appIconAlpha }
                    )
                }
            }

            // Indicador de carga sólo visible durante el Splash inicial a pantalla completa
            AnimatedVisibility(
                visible = isSplashLoading,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(200))
            ) {
                val normalizedSplashLoad = (lottieProgress / LOTTIE_HERO_SETTLED_PROGRESS).coerceIn(0.08f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, start = 42.dp, end = 42.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { normalizedSplashLoad },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = BordeCyan,
                        trackColor = Color.White.copy(alpha = 0.14f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "CARGANDO JUEGO LIMPIO...",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.4.sp,
                        color = BordeCyan
                    )
                }
            }

            // 3. CONTENIDO DEL HUB REDISEÑADO (Aparece mientras el logo viaja a su lugar fijo)
            if (!isSplashLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = hubAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 10.dp))

                    // 4. SECCIÓN DE JUEGOS: GRILLA 2x2 CON IMÁGENES TEMÁTICAS Y SHADER DE SOMBRA
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                gamesGridBounds = coords.boundsInRoot()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = BordeCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "SELECCIONÁ UN MODO DE JUEGO",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val weekBest = bestScoresByMode["La Semana"]
                        val scrollBest = bestScoresByMode["Scroll sin Caer"]
                        val slotsBest = bestScoresByMode["Trampa del Jackpot"]
                        val rouletteBest = bestScoresByMode["La Ruleta del 29"]

                        val cards = listOf(
                            HubGameCardSpec(
                                modeKey = "La Semana",
                                title = "LA SEMANA:\nUNA DECISIÓN MÁS",
                                categoryTag = "Historia • 7 Días",
                                defaultBadge = when {
                                    hasSavedGame -> "En curso ($savedGameDayName)"
                                    weekBest != null && weekBest > 0 -> "Récord: $weekBest pts"
                                    else -> "7 Días • Chat"
                                },
                                coverImageRes = R.drawable.ad_prestamo_express,
                                borderAccent = BordeCyan,
                                dotColor = if (hasSavedGame) VerdeExito else BordeCyan,
                                onClick = {
                                    playClickFeedback()
                                    keyboardController?.hide()
                                    val clean = currentCleanName()
                                    onProfileChanged(clean, selectedAvatarResId)
                                    if (hasSavedGame) {
                                        showSavedWeekModal = true
                                    } else {
                                        onStartWeekGame(clean, selectedAvatarResId)
                                    }
                                }
                            ),
                            HubGameCardSpec(
                                modeKey = "Scroll sin Caer",
                                title = "SCROLL\nSIN CAER",
                                categoryTag = "Feed • Acelerómetro",
                                defaultBadge = if (scrollBest != null && scrollBest > 0) {
                                    "Récord: $scrollBest pts"
                                } else {
                                    "Feed • Sensor"
                                },
                                coverImageRes = R.drawable.ad_fitness_casino,
                                borderAccent = BordeMagenta,
                                dotColor = VerdeExito,
                                onClick = {
                                    playClickFeedback()
                                    keyboardController?.hide()
                                    val clean = currentCleanName()
                                    onProfileChanged(clean, selectedAvatarResId)
                                    onStartScrollAdsGame(clean, selectedAvatarResId)
                                }
                            ),
                            HubGameCardSpec(
                                modeKey = "Trampa del Jackpot",
                                title = "TRAGAMONEDAS\nJACKPOT 777",
                                categoryTag = "Slots 3x3 • Sensor",
                                defaultBadge = if (slotsBest != null && slotsBest > 0) {
                                    "Récord: $slotsBest pts"
                                } else {
                                    "Slots 3x3"
                                },
                                coverImageRes = R.drawable.card_slots_jackpot,
                                borderAccent = Color(0xFFF59E0B),
                                dotColor = Color(0xFFFBBF24),
                                onClick = {
                                    playClickFeedback()
                                    keyboardController?.hide()
                                    val clean = currentCleanName()
                                    onProfileChanged(clean, selectedAvatarResId)
                                    onStartSlotsTrapGame(clean, selectedAvatarResId)
                                }
                            ),
                            HubGameCardSpec(
                                modeKey = "La Ruleta del 29",
                                title = "LA RULETA\nDEL 29",
                                categoryTag = "Ruleta • Sensor",
                                defaultBadge = if (rouletteBest != null && rouletteBest > 0) {
                                    "Récord: $rouletteBest pts"
                                } else {
                                    "Ruleta Europea"
                                },
                                coverImageRes = R.drawable.card_roulette_wheel,
                                borderAccent = Color(0xFFA855F7),
                                dotColor = BordeMagenta,
                                onClick = {
                                    playClickFeedback()
                                    keyboardController?.hide()
                                    val clean = currentCleanName()
                                    onProfileChanged(clean, selectedAvatarResId)
                                    onStartRouletteTrapGame(clean, selectedAvatarResId)
                                }
                            )
                        )

                        // Fila 1 de la grilla 2x2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VisualGameCard(
                                spec = cards[0],
                                cardHeight = cardImageHeight,
                                modifier = Modifier.weight(1f)
                            )
                            VisualGameCard(
                                spec = cards[1],
                                cardHeight = cardImageHeight,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fila 2 de la grilla 2x2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VisualGameCard(
                                spec = cards[2],
                                cardHeight = cardImageHeight,
                                modifier = Modifier.weight(1f)
                            )
                            VisualGameCard(
                                spec = cards[3],
                                cardHeight = cardImageHeight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

                    // 5. ACCESO DESTACADO A RANKING Y MEDALLAS (Zona inferior ergonómica)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                rankingButtonBounds = coords.boundsInRoot()
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A).copy(alpha = 0.92f),
                                        Color(0xFF0C2233).copy(alpha = 0.92f)
                                    )
                                )
                            )
                            .border(
                                width = 1.3.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        BordeCyan.copy(alpha = 0.80f),
                                        BordeMagenta.copy(alpha = 0.55f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !isSplashLoading) {
                                playClickFeedback()
                                keyboardController?.hide()
                                val clean = currentCleanName()
                                onProfileChanged(clean, selectedAvatarResId)
                                onOpenLeaderboard(clean, selectedAvatarResId)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(BordeCyan.copy(alpha = 0.16f))
                                        .border(1.dp, BordeCyan.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Ranking y Medallas",
                                        tint = BordeCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Ranking y medallas",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Tabla global y vitrina de logros",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = TextoSecundario
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(BordeCyan.copy(alpha = 0.16f))
                                        .border(1.dp, BordeCyan.copy(alpha = 0.55f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$unlockedAchievementsCount/7",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFA5F3FC)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = BordeCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. TUTORIAL GUIADO INTERACTIVO (Sombreado + Mano apuntando a cada sección)
        val tutorialStep = activeTutorialStep
        if (isLogoSettled && tutorialStep != null && !showAvatarModal && !showNicknameModal) {
            val currentTargetBounds = when (tutorialStep) {
                TutorialStep.NICKNAME -> nicknameBounds
                TutorialStep.AVATAR -> avatarBounds
                TutorialStep.MUTE_BUTTON -> muteButtonBounds
                TutorialStep.GAMES_SECTION -> gamesGridBounds
                TutorialStep.RANKING_SECTION -> rankingButtonBounds
            }

            val canAdvance = when (tutorialStep) {
                TutorialStep.NICKNAME -> playerName.trim().isNotEmpty()
                TutorialStep.AVATAR -> hasConfirmedAvatarInTutorial
                else -> true
            }

            TutorialCoachmarkOverlay(
                currentStep = tutorialStep,
                targetBoundsInRoot = currentTargetBounds,
                canAdvanceCurrentStep = canAdvance,
                onTargetClicked = {
                    playClickFeedback()
                    HapticFeedbackHelper.vibrateShort(context, 40L)
                    when (tutorialStep) {
                        TutorialStep.NICKNAME -> {
                            showNicknameModal = true
                        }
                        TutorialStep.AVATAR -> {
                            showAvatarModal = true
                        }
                        TutorialStep.MUTE_BUTTON -> {
                            SoundManager.toggleMute()
                            activeTutorialStep = TutorialStep.GAMES_SECTION
                        }
                        TutorialStep.GAMES_SECTION -> {
                            activeTutorialStep = TutorialStep.RANKING_SECTION
                        }
                        TutorialStep.RANKING_SECTION -> {
                            activeTutorialStep = null
                            onTutorialCompleted()
                        }
                    }
                },
                onNextStep = {
                    playClickFeedback()
                    HapticFeedbackHelper.vibrateShort(context, 40L)
                    when (tutorialStep) {
                        TutorialStep.NICKNAME -> {
                            val clean = currentCleanName()
                            playerName = clean
                            onProfileChanged(clean, selectedAvatarResId)
                            hasConfirmedAvatarInTutorial = false
                            activeTutorialStep = TutorialStep.AVATAR
                        }
                        TutorialStep.AVATAR -> {
                            if (hasConfirmedAvatarInTutorial) {
                                activeTutorialStep = TutorialStep.MUTE_BUTTON
                            } else {
                                showAvatarModal = true
                            }
                        }
                        TutorialStep.MUTE_BUTTON -> {
                            activeTutorialStep = TutorialStep.GAMES_SECTION
                        }
                        TutorialStep.GAMES_SECTION -> {
                            activeTutorialStep = TutorialStep.RANKING_SECTION
                        }
                        TutorialStep.RANKING_SECTION -> {
                            activeTutorialStep = null
                            onTutorialCompleted()
                        }
                    }
                }
            )
        }

        // 6. MODAL PARA EDITAR NOMBRE / APODO
        AnimatedVisibility(
            visible = showNicknameModal,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f)
        ) {
            NicknameEditModal(
                initialName = playerName,
                isTutorialMode = activeTutorialStep == TutorialStep.NICKNAME,
                onDismiss = {
                    showNicknameModal = false
                },
                onConfirmName = { updatedName ->
                    playClickFeedback()
                    HapticFeedbackHelper.vibrateShort(context, 40L)
                    val clean = updatedName.trim().ifEmpty { "Mateo" }
                    playerName = clean
                    onProfileChanged(clean, selectedAvatarResId)
                    showNicknameModal = false
                    if (activeTutorialStep == TutorialStep.NICKNAME) {
                        hasConfirmedAvatarInTutorial = false
                        activeTutorialStep = TutorialStep.AVATAR
                    }
                }
            )
        }

        // 7. SELECTOR DE AVATAR: ARRIBA EN GRANDE EL ELEGIDO + DEBAJO EN TAMAÑO MEDIO LOS ICONOS (SIN NOMBRES)
        AnimatedVisibility(
            visible = showAvatarModal,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.92f)
        ) {
            LargeAvatarPickerModal(
                avatarResIds = avatarResList,
                currentAvatarResId = selectedAvatarResId,
                onSelectAndConfirm = { chosenAvatarRes ->
                    playClickFeedback()
                    HapticFeedbackHelper.vibrateShort(context, 45L)
                    selectedAvatarResId = chosenAvatarRes
                    val clean = currentCleanName()
                    onProfileChanged(clean, chosenAvatarRes)
                    showAvatarModal = false
                    if (activeTutorialStep == TutorialStep.AVATAR) {
                        hasConfirmedAvatarInTutorial = true
                        activeTutorialStep = TutorialStep.MUTE_BUTTON
                    }
                },
                onDismiss = {
                    playClickFeedback()
                    showAvatarModal = false
                    if (activeTutorialStep == TutorialStep.AVATAR) {
                        hasConfirmedAvatarInTutorial = true
                        activeTutorialStep = TutorialStep.MUTE_BUTTON
                    }
                }
            )
        }

        // 8. MODAL DE PARTIDA GUARDADA (Estilo Stitch: Continuar vs Empezar de nuevo)
        AnimatedVisibility(
            visible = showSavedWeekModal,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.94f)
        ) {
            SavedGameStitchModal(
                savedGameDayName = savedGameDayName.ifBlank { "En progreso" },
                onContinueSaved = {
                    playClickFeedback()
                    showSavedWeekModal = false
                    onResumeSavedGame()
                },
                onRestartFromDayOne = {
                    playClickFeedback()
                    showSavedWeekModal = false
                    val clean = currentCleanName()
                    onProfileChanged(clean, selectedAvatarResId)
                    onStartWeekGame(clean, selectedAvatarResId)
                },
                onDismiss = {
                    showSavedWeekModal = false
                }
            )
        }
    }
}

/**
 * Tarjeta visual para la grilla 2x2 inspirada en la referencia de categorías (Twitch/Kick)
 * y el diseño "Hub de Juego - Inicio Rediseñado" de Stitch:
 * - Imagen temática a sangre ocupando la tarjeta
 * - Badge superior con indicador luminoso y récord/estado
 * - Shader/sombra oscura profunda en la base para lectura nítida del título en mayúsculas
 * - Chip de categoría debajo
 */
@Composable
private fun VisualGameCard(
    spec: HubGameCardSpec,
    cardHeight: Dp = 136.dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = spec.onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            spec.borderAccent.copy(alpha = 0.75f),
                            spec.borderAccent.copy(alpha = 0.28f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // 1. Imagen relacionada al juego a sangre
            Image(
                painter = painterResource(id = spec.coverImageRes),
                contentDescription = spec.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Shader / Sombra oscura de fondo en la parte inferior para lectura del título
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.18f),
                                0.38f to Color.Transparent,
                                0.65f to Color(0xCC050811),
                                1.0f to Color(0xF503060C)
                            )
                        )
                    )
            )

            // 3. Pill superior izquierdo (como el "• 44,7 mil" de la referencia)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xD9090D16))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(spec.dotColor)
                    )
                    Text(
                        text = spec.defaultBadge,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Nombre del juego en grande sobre el shader inferior con sombra tipográfica
            Text(
                text = spec.title,
                style = TextStyle(
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    lineHeight = 17.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(0f, 3f),
                        blurRadius = 10f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Chip de categoría inferior estilo referencia
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF151E2E))
                    .border(
                        width = 1.dp,
                        color = spec.borderAccent.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 9.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = spec.categoryTag,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Modal de selección de Avatar:
 * - Arriba en grande el avatar elegido (138.dp)
 * - Debajo en tamaño medio (58-64.dp) los iconos para elegir, sin nombres
 * - Permite elegir/confirmar el mismo avatar actual y siempre permite cerrar la ventana
 */
@Composable
private fun LargeAvatarPickerModal(
    avatarResIds: List<Int>,
    currentAvatarResId: Int,
    onSelectAndConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tempSelectedResId by remember(currentAvatarResId) {
        mutableIntStateOf(currentAvatarResId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
            .padding(horizontal = 18.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0C1626))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(BordeCyan, BordeMagenta)),
                    shape = RoundedCornerShape(26.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera con título y botón 'X' siempre habilitado para cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Elegí tu Avatar",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Seleccioná un icono debajo para usarlo en tu perfil",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.5.sp,
                        color = TextoSecundario
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 1. ARRIBA EN GRANDE EL AVATAR ELEGIDO
            Box(
                modifier = Modifier.size(144.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(138.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = CircleShape,
                            ambientColor = BordeCyan,
                            spotColor = BordeMagenta
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BordeCyan, BordeMagenta)
                            )
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = tempSelectedResId),
                        contentDescription = "Avatar seleccionado",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // Check verde indicando selección actual
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(VerdeExito)
                        .border(2.5.dp, Color(0xFF0C1626), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2. DEBAJO EN TAMAÑO MEDIO LOS ICONOS PARA ELEGIR (SIN NOMBRES)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                avatarResIds.forEach { resId ->
                    MediumAvatarOptionItem(
                        resId = resId,
                        isSelected = tempSelectedResId == resId,
                        onClick = {
                            SoundManager.playClick()
                            HapticFeedbackHelper.vibrateShort(context, 30L)
                            tempSelectedResId = resId
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. BOTÓN CONFIRMAR (Siempre habilitado, incluso con el mismo avatar que ya tenías)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                    .clickable {
                        onSelectAndConfirm(tempSelectedResId)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Confirmar Avatar",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF041019)
                )
            }
        }
    }
}

@Composable
private fun MediumAvatarOptionItem(
    @DrawableRes resId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val itemSize by animateDpAsState(
        targetValue = if (isSelected) 62.dp else 54.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mediumAvatarSize"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 3.dp else 1.5.dp,
                    brush = if (isSelected) {
                        Brush.linearGradient(listOf(BordeCyan, BordeMagenta))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color(0xFF161F30)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * Modal para ingresar o editar el nombre/apodo del jugador.
 */
@Composable
private fun NicknameEditModal(
    initialName: String,
    isTutorialMode: Boolean,
    onDismiss: () -> Unit,
    onConfirmName: (String) -> Unit
) {
    var draftName by remember(initialName) {
        mutableStateOf(if (isTutorialMode && initialName == "Mateo") "" else initialName)
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isTutorialMode) onDismiss()
            }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C1626))
                .border(
                    width = 1.8.dp,
                    brush = Brush.horizontalGradient(listOf(BordeCyan, BordeMagenta)),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tu Nombre o Apodo",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Con este nombre vas a aparecer en los chats y en el ranking con tus amigos.",
                fontFamily = SpaceGrotesk,
                fontSize = 12.5.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            TextField(
                value = draftName,
                onValueChange = { input ->
                    if (input.length <= 22 && !input.contains("\n")) {
                        draftName = input
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                ),
                placeholder = {
                    Text(
                        text = "Escribí tu apodo (Ej: Mateo)",
                        style = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontSize = 15.sp,
                            color = TextoSecundario.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF142033),
                    unfocusedContainerColor = Color(0xFF142033),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = BordeCyan,
                    focusedIndicatorColor = BordeCyan,
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.25f)
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        val finalName = draftName.trim().ifEmpty { "Mateo" }
                        onConfirmName(finalName)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                    .clickable {
                        keyboardController?.hide()
                        val finalName = draftName.trim().ifEmpty { "Mateo" }
                        onConfirmName(finalName)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Guardar apodo",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Color(0xFF041019)
                )
            }
        }
    }
}

/**
 * Modal de confirmación de partida guardada en Room ("Hub de Juego - Inicio Rediseñado" de Stitch).
 */
@Composable
private fun SavedGameStitchModal(
    savedGameDayName: String,
    onContinueSaved: () -> Unit,
    onRestartFromDayOne: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBF020617))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0C1626))
                .border(
                    width = 1.5.dp,
                    color = BordeCyan.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(26.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF083344))
                        .border(1.dp, BordeCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BordeCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "La Semana: Una Decisión Más",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                text = "Tenés una partida en progreso",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = BordeCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Podés continuar tu progreso guardado en Room o reiniciar la historia desde el primer día.",
                fontFamily = SpaceGrotesk,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Botón Principal Verde Neón: Continuar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF2DD4BF))
                        )
                    )
                    .clickable { onContinueSaved() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF041019),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Continuar (Día $savedGameDayName)",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF041019)
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            // Botón Secundario: Empezar de nuevo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF131D31))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(13.dp))
                    .clickable { onRestartFromDayOne() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TextoSecundario,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Empezar de nuevo",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B14)
@Composable
fun WelcomeScreenPreview() {
    JuegoLimpioTheme {
        WelcomeScreen(
            hasPlayedIntroSplash = true,
            isTutorialCompleted = true,
            onStartWeekGame = { _, _ -> },
            onResumeSavedGame = {},
            onStartScrollAdsGame = { _, _ -> },
            onStartSlotsTrapGame = { _, _ -> },
            onStartRouletteTrapGame = { _, _ -> },
            onOpenLeaderboard = { _, _ -> }
        )
    }
}
