package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.sensor.HapticFeedbackHelper
import com.ifts18.unadecisionmas.sensor.rememberAccelerometerMonitor
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class InvasiveAdSpec(
    val id: Int,
    val triggerProgress: Float,
    val igUsername: String,
    val igSponsoredLabel: String,
    val avatarResId: Int,
    val postImageResId: Int,
    val imageBannerTag: String,
    val imageOverlayHeadline: String,
    val imageOverlaySubtext: String,
    val likesCountText: String,
    val bodyText: String,
    val hashtags: String,
    val trapButtonText: String,
    val accentColor: Color
)

private data class IgStoryItem(
    val handle: String,
    val avatarResId: Int,
    val isOwnStory: Boolean = false
)

private data class IgSuggestionItem(
    val displayName: String,
    val handle: String,
    val avatarResId: Int
)

private data class IgFeedPostItem(
    val id: Int,
    val handle: String,
    val subLabel: String,
    val isVerified: Boolean,
    val avatarResId: Int,
    val audioBadge: String,
    val likesBase: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val likedByHandle: String,
    val likedOthersCount: String,
    val caption: String,
    val hashtags: String,
    val timeAgo: String,
    val visualTheme: Int
)

/**
 * JUEGO 2: "Scroll sin Caer"
 * El jugador scrollea su feed de Instagram en modo oscuro de principio a fin.
 * A mitad del scroll saltan posts patrocinados de influencers, préstamos de $20.000
 * y casinos que bloquean la pantalla. Para destruirlos debe AGITAR EL TELÉFONO.
 */
@Composable
fun ScrollAdsGameScreen(
    playerName: String,
    @Suppress("UNUSED_PARAMETER") onBackToHub: () -> Unit,
    onGameFinished: (adsShakenOff: Int, adTrapsClicked: Int, elapsedMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val igStories = remember {
        listOf(
            IgStoryItem(handle = "Tu historia", avatarResId = 0, isOwnStory = true),
            IgStoryItem(handle = "valen.musica", avatarResId = 0),
            IgStoryItem(handle = "lauti_bici", avatarResId = 0),
            IgStoryItem(handle = "camila.art", avatarResId = 0),
            IgStoryItem(handle = "tomas.voley", avatarResId = 0),
            IgStoryItem(handle = "mora.sketch", avatarResId = 0),
            IgStoryItem(handle = "fede_ok", avatarResId = 0)
        )
    }

    val igSuggestions = remember {
        listOf(
            IgSuggestionItem("Julieta Paz", "julipaz.ok", 0),
            IgSuggestionItem("Mate & Código", "mateycodigo", 0),
            IgSuggestionItem("Bici Urbana BA", "biciurbanaba", 0),
            IgSuggestionItem("Cine Club 5B", "cineclub.5b", 0)
        )
    }

    val igPosts = remember {
        listOf(
            IgFeedPostItem(
                id = 1,
                handle = "club.barrio_sur",
                subLabel = "Audio original • Torneo 5.° Año",
                isVerified = true,
                avatarResId = 0,
                audioBadge = "REEL • 0:24",
                likesBase = 1284,
                commentsCount = 43,
                sharesCount = 19,
                likedByHandle = "lauti_bici",
                likedOthersCount = "1.283",
                caption = "Hoy se juega la semi en la cancha del club con toda la banda del curso.",
                hashtags = "#FutsalBarrioSur #PlanReal #SinTimba",
                timeAgo = "Hace 2 horas",
                visualTheme = 1
            ),
            IgFeedPostItem(
                id = 2,
                handle = "valen.musica",
                subLabel = "Sugerencia para ti",
                isVerified = false,
                avatarResId = 0,
                audioBadge = "Lo-Fi Study Mix",
                likesBase = 892,
                commentsCount = 27,
                sharesCount = 64,
                likedByHandle = "camila.art",
                likedOthersCount = "891",
                caption = "Armé playlist nueva para meterle al TP sin distraerse ni caer en notificaciones truchas.",
                hashtags = "#MusicaParaEstudiar #Playlist5B #ModoFocus",
                timeAgo = "Hace 4 horas",
                visualTheme = 2
            ),
            IgFeedPostItem(
                id = 3,
                handle = "dato.claro",
                subLabel = "Educación Digital • Carrusel",
                isVerified = true,
                avatarResId = 0,
                audioBadge = "1 / 4",
                likesBase = 3410,
                commentsCount = 118,
                sharesCount = 405,
                likedByHandle = "fede_ok",
                likedOthersCount = "3.409",
                caption = "Cuando un influencer muestra un retiro gigante casi siempre usa saldo ficticio. Nadie sube las 20 pérdidas de la semana.",
                hashtags = "#OjoConElDemo #VentajaDeLaCasa #DatoReal",
                timeAgo = "Hace 6 horas",
                visualTheme = 3
            ),
            IgFeedPostItem(
                id = 4,
                handle = "camila.art",
                subLabel = "Palermo, Buenos Aires",
                isVerified = false,
                avatarResId = 0,
                audioBadge = "Acústico • Café y Trazos",
                likesBase = 645,
                commentsCount = 19,
                sharesCount = 12,
                likedByHandle = "valen.musica",
                likedOthersCount = "644",
                caption = "Lo mejor de cuidar tus ahorros es disfrutar salidas reales, merienda con amigas y cero deuda.",
                hashtags = "#SalidasReales #Sketchbook #Cuidate",
                timeAgo = "Hace 9 horas",
                visualTheme = 4
            ),
            IgFeedPostItem(
                id = 5,
                handle = "espacio.joven141",
                subLabel = "Información Pública • CABA",
                isVerified = true,
                avatarResId = 0,
                audioBadge = "Recurso Gratuito 24/7",
                likesBase = 4920,
                commentsCount = 95,
                sharesCount = 812,
                likedByHandle = "tomas.voley",
                likedOthersCount = "4.919",
                caption = "Si algún compañero la está pasando mal con apuestas o préstamos de cajeros, hablar a tiempo cambia todo. Línea 141 y Boti atienden gratis 24/7.",
                hashtags = "#Linea141 #BotiTeAyuda #JuegoLimpio",
                timeAgo = "Hace 1 día",
                visualTheme = 5
            )
        )
    }

    val likedPostIds = remember { mutableStateMapOf<Int, Boolean>() }
    val savedPostIds = remember { mutableStateMapOf<Int, Boolean>() }
    val followedHandles = remember { mutableStateMapOf<String, Boolean>() }
    val dismissedSuggestionHandles = remember { mutableStateMapOf<String, Boolean>() }

    val adsCatalog = remember {
        listOf(
            InvasiveAdSpec(
                id = 1,
                triggerProgress = 0.16f,
                igUsername = "tomifitness.vip",
                igSponsoredLabel = "Publicidad • B-Play VIP",
                avatarResId = 0,
                postImageResId = R.drawable.ad_fitness_casino,
                imageBannerTag = "RETIRO EN VIVO",
                imageOverlayHeadline = "Saqué 450K en 3 tiradas",
                imageOverlaySubtext = "Empecé con $2.000 entre serie y serie",
                likesCountText = "Les gusta a lucas_09 y 18.420 personas más",
                bodyText = "Dejá de scrollear gratis. Yo levanté esto entre serie y serie en el gym. El primer tiro viene con premio.",
                hashtags = "#BPlayVIP #RetiroEnVivo #CasinoOnline",
                trapButtonText = "Instalar App",
                accentColor = BordeMagenta
            ),
            InvasiveAdSpec(
                id = 2,
                triggerProgress = 0.42f,
                igUsername = "cajero.express24",
                igSponsoredLabel = "Publicidad • Crédito al Toque",
                avatarResId = 0,
                postImageResId = R.drawable.ad_prestamo_express,
                imageBannerTag = "SIN RECIBO NI FIRMA",
                imageOverlayHeadline = "$20.000 en tu cuenta en 1 minuto",
                imageOverlaySubtext = "Cargás fichas ahora y pagás cuando ganás",
                likesCountText = "Les gusta a nico.apuestas y 9.150 personas más",
                bodyText = "Te acreditamos $20.000 ahora mismo sin recibo ni garantes. Entrá a la mesa VIP hoy.",
                hashtags = "#PrestamoAlToque #Cajero24hs #SinLimite",
                trapButtonText = "Pedir crédito",
                accentColor = Color(0xFFFFB300)
            ),
            InvasiveAdSpec(
                id = 3,
                triggerProgress = 0.68f,
                igUsername = "fijas.superclasico",
                igSponsoredLabel = "Publicidad • Apuestas en Vivo",
                avatarResId = 0,
                postImageResId = R.drawable.ad_superclasico_bet,
                imageBannerTag = "CUOTA X10.5 HOY",
                imageOverlayHeadline = "La combinada fija del Superclásico",
                imageOverlaySubtext = "Últimos cupos antes del pitazo inicial",
                likesCountText = "Les gusta a mati_futbol y 24.300 personas más",
                bodyText = "Mirá la cuota que se está por cerrar antes del partido. Si entrás ahora multiplicás tu entrada por diez.",
                hashtags = "#Superclasico #FijaDelDia #CuotaX10",
                trapButtonText = "Apostar $4.000",
                accentColor = RojoAlerta
            ),
            InvasiveAdSpec(
                id = 4,
                triggerProgress = 0.88f,
                igUsername = "casino.nocturno.bet",
                igSponsoredLabel = "Publicidad • Sala Nocturna",
                avatarResId = 0,
                postImageResId = R.drawable.ad_casino_nocturno,
                imageBannerTag = "BONO BIENVENIDA",
                imageOverlayHeadline = "Reclamá $50.000 en fichas gratis",
                imageOverlaySubtext = "Giro VIP habilitado solo por esta noche",
                likesCountText = "Les gusta a cajero_nocturno y 31.900 personas más",
                bodyText = "Tenés un tiro VIP esperando con recarga mínima de $4.000. Aprovechá antes que expire.",
                hashtags = "#JackpotVIP #SlotsOnline #RuletaEnVivo",
                trapButtonText = "Reclamar bono",
                accentColor = BordeCyan
            )
        )
    }

    val triggeredAdIds = remember { mutableStateListOf<Int>() }
    var activeAd by remember { mutableStateOf<InvasiveAdSpec?>(null) }
    var shakeProgress by remember { mutableFloatStateOf(0f) }
    var adsShakenCount by remember { mutableIntStateOf(0) }
    var trapsClickedCount by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(60) }
    var showIntroPopup by remember { mutableStateOf(true) }
    val shakeWobble = remember { Animatable(0f) }

    var isLifecycleResumed by remember { mutableStateOf(true) }
    var sessionOpenedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var accumulatedElapsedMillis by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleResumed = true
                    sessionOpenedAt = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    if (isLifecycleResumed && !showIntroPopup) {
                        accumulatedElapsedMillis += (System.currentTimeMillis() - sessionOpenedAt).coerceAtLeast(0L)
                    }
                    isLifecycleResumed = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun currentForegroundElapsed(): Long {
        if (showIntroPopup) return accumulatedElapsedMillis
        val extra = if (isLifecycleResumed) (System.currentTimeMillis() - sessionOpenedAt).coerceAtLeast(0L) else 0L
        return accumulatedElapsedMillis + extra
    }

    fun dismissIntroPopup() {
        if (showIntroPopup) {
            SoundManager.playClick()
            sessionOpenedAt = System.currentTimeMillis()
            showIntroPopup = false
        }
    }

    val scrollFraction by remember {
        derivedStateOf {
            if (scrollState.maxValue <= 0) 0f
            else (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        }
    }

    // Monitoreo del Acelerómetro en tiempo real respetando el ciclo de vida
    rememberAccelerometerMonitor(
        enabled = isLifecycleResumed && !showIntroPopup,
        shakeThresholdG = 1.38f,
        minIntervalMs = 95L
    ) { gForce ->
        if (activeAd != null && isLifecycleResumed && !showIntroPopup) {
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

    // Disparar anuncio invasivo según el progreso de scroll del feed de Instagram
    LaunchedEffect(scrollFraction) {
        if (activeAd == null && !showIntroPopup) {
            val nextAd = adsCatalog.firstOrNull { ad ->
                scrollFraction >= ad.triggerProgress && !triggeredAdIds.contains(ad.id)
            }
            if (nextAd != null) {
                triggeredAdIds.add(nextAd.id)
                shakeProgress = 0f
                activeAd = nextAd
                if (isLifecycleResumed) {
                    HapticFeedbackHelper.vibrateAlertPattern(context)
                }
            }
        }
    }

    // Reloj de cuenta regresiva del juego (se congela en segundo plano y mientras está el pop-up inicial)
    LaunchedEffect(Unit) {
        while (showIntroPopup) {
            delay(100L)
        }
        sessionOpenedAt = System.currentTimeMillis()
        while (remainingSeconds > 0) {
            var waitRemaining = 1000L
            while (waitRemaining > 0L) {
                while (!isLifecycleResumed || showIntroPopup) {
                    delay(100L)
                }
                val chunk = minOf(100L, waitRemaining)
                delay(chunk)
                if (isLifecycleResumed && !showIntroPopup) {
                    waitRemaining -= chunk
                }
            }
            if (isLifecycleResumed && !showIntroPopup) {
                remainingSeconds--
            }
        }
        onGameFinished(adsShakenCount, trapsClickedCount, currentForegroundElapsed())
    }

    val isFeedInteractive = activeAd == null && !showIntroPopup

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060709))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // REPLICA DE FEED DE INSTAGRAM EN MODO OSCURO (SCROLLEABLE)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060709))
                .verticalScroll(
                    state = scrollState,
                    enabled = isFeedInteractive // Se bloquea el scroll cuando salta una publicidad invasiva o el pop-up inicial
                )
        ) {
            IgFeedTopBar(playerName = playerName)

            IgStoriesStrip(stories = igStories)

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            igPosts.forEachIndexed { index, post ->
                IgFeedPostCard(
                    post = post,
                    isLiked = likedPostIds[post.id] == true,
                    isSaved = savedPostIds[post.id] == true,
                    isFollowed = followedHandles[post.handle] == true,
                    enabled = isFeedInteractive,
                    onToggleLike = {
                        SoundManager.playClick()
                        likedPostIds[post.id] = !(likedPostIds[post.id] == true)
                    },
                    onToggleSave = {
                        SoundManager.playClick()
                        savedPostIds[post.id] = !(savedPostIds[post.id] == true)
                    },
                    onToggleFollow = {
                        SoundManager.playClick()
                        followedHandles[post.handle] = !(followedHandles[post.handle] == true)
                    }
                )

                if (index == 1) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    IgSuggestionsCarousel(
                        suggestions = igSuggestions,
                        followedHandles = followedHandles,
                        dismissedHandles = dismissedSuggestionHandles,
                        enabled = isFeedInteractive,
                        onToggleFollow = { handle ->
                            SoundManager.playClick()
                            followedHandles[handle] = !(followedHandles[handle] == true)
                        },
                        onDismiss = { handle ->
                            SoundManager.playClick()
                            dismissedSuggestionHandles[handle] = true
                        }
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }

            // Botón final al llegar al final del feed esquivando los anuncios
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                    .clickable(enabled = isFeedInteractive) {
                        SoundManager.playClick()
                        onGameFinished(adsShakenCount, trapsClickedCount, currentForegroundElapsed())
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
                        text = "Terminar feed",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. OVERLAY DE ANUNCIO ESTILO POST DE INSTAGRAM PATROCINADO (SE QUITA AGITANDO EL CELULAR)
        AnimatedVisibility(
            visible = activeAd != null,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.88f),
            exit = fadeOut(tween(180))
        ) {
            val ad = activeAd ?: return@AnimatedVisibility
            val igRingGradient = Brush.linearGradient(
                listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = shakeWobble.value
                            rotationZ = shakeWobble.value * 0.25f
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF121318))
                        .border(2.dp, ad.accentColor, RoundedCornerShape(20.dp))
                ) {
                    // A. CABECERA DE POST DE INSTAGRAM
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, igRingGradient, CircleShape)
                                    .padding(2.dp)
                            ) {
                                IgProfileAvatar(
                                    handle = ad.igUsername,
                                    size = 34.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ad.igUsername,
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verificado",
                                        tint = Color(0xFF3897F0),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = ad.igSponsoredLabel,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 11.sp,
                                    color = BordeCyan
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // B. IMAGEN VISUAL DEL POST DE INSTAGRAM CON STICKERS Y OVERLAY PUBLICITARIO
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(Color(0xFF0A0D14))
                    ) {
                        Image(
                            painter = painterResource(id = ad.postImageResId),
                            contentDescription = ad.imageOverlayHeadline,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Degradado oscuro superior/inferior para resaltar los textos del post
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.55f),
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Black.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )

                        // Etiqueta superior tipo Sticker de Instagram
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ad.accentColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ad.imageBannerTag,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = Color.Black
                            )
                        }

                        // Titular llamativo sobre la imagen del post
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = ad.imageOverlayHeadline,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.5.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ad.imageOverlaySubtext,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = VerdeExito
                            )
                        }
                    }

                    // C. BARRA CTA DE INSTAGRAM ADS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ad.accentColor.copy(alpha = 0.22f))
                            .clickable {
                                SoundManager.playClick()
                                trapsClickedCount++
                                activeAd = null
                                shakeProgress = 0f
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ad.trapButtonText,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = ad.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // D. ÍCONOS DE INTERACCIÓN DE INSTAGRAM + LIKES + DESCRIPCIÓN
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Like",
                                    tint = Color(0xFFFF3040),
                                    modifier = Modifier.size(21.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Comentar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Compartir",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Guardar",
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = ad.likesCountText,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "${ad.igUsername}  ${ad.bodyText}",
                            fontFamily = SpaceGrotesk,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color.White.copy(alpha = 0.92f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = ad.hashtags,
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.sp,
                            color = Color(0xFF70B5F9)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // E. INSTRUCCIÓN PRINCIPAL DE SENSOR (ACELERÓMETRO)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0E1624))
                                .border(1.5.dp, BordeCyan, RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = BordeCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Agitá el celular para cerrar el anuncio",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = BordeCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { shakeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = VerdeExito,
                                trackColor = Color.White.copy(alpha = 0.14f)
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bloqueo: ${(shakeProgress * 100).toInt()}%",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )

                                Text(
                                    text = "Probar sacudida",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
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
                    }
                }
            }
        }
    }

    if (showIntroPopup) {
        Dialog(onDismissRequest = { dismissIntroPopup() }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF141824))
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(BordeCyan, BordeMagenta)),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BordeCyan.copy(alpha = 0.16f))
                        .border(1.5.dp, BordeCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = BordeCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Esquivá los anuncios",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Scrolleá el feed hasta el final. Cuando aparezca publicidad invasiva, agitá el celular para esquivarla sin caer en la trampa.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(BordeCyan, VerdeExito)))
                        .clickable { dismissIntroPopup() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Entendido",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

private val IgStoryRingGradient = Brush.linearGradient(
    listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF), Color(0xFF515BD4))
)

/**
 * Barra superior estilo Instagram en modo oscuro (+, Instagram v, corazón).
 */
@Composable
private fun IgFeedTopBar(playerName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.AddBox,
            contentDescription = "Crear",
            tint = Color.White,
            modifier = Modifier.size(23.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Instagram",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Notificaciones de $playerName",
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3040))
            )
        }
    }
}

/**
 * Tira horizontal de Historias estilo Instagram con aro degradado y "Tu historia".
 */
@Composable
private fun IgStoriesStrip(stories: List<IgStoryItem>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .then(
                                if (story.isOwnStory) {
                                    Modifier.border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                } else {
                                    Modifier.border(2.2.dp, IgStoryRingGradient, CircleShape)
                                }
                            )
                            .padding(3.dp)
                    ) {
                        IgProfileAvatar(
                            handle = story.handle,
                            size = 58.dp
                        )
                    }

                    if (story.isOwnStory) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0095F6))
                                .border(2.dp, Color(0xFF060709), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = story.handle,
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.sp,
                    color = if (story.isOwnStory) TextoSecundario else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Publicación individual del feed de Instagram con cabecera, multimedia, barra social y caption.
 */
@Composable
private fun IgFeedPostCard(
    post: IgFeedPostItem,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowed: Boolean,
    enabled: Boolean,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF060709))
    ) {
        // Cabecera del post
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, IgStoryRingGradient, CircleShape)
                        .padding(2.dp)
                ) {
                    IgProfileAvatar(
                        handle = post.handle,
                        size = 32.dp
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.handle,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verificado",
                                tint = Color(0xFF3897F0),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextoSecundario,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = post.subLabel,
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.sp,
                            color = TextoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFollowed) Color.White.copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFollowed) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = enabled) { onToggleFollow() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isFollowed) "Siguiendo" else "Seguir",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Más opciones",
                    tint = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Contenido visual del Post / Reel
        IgPostVisualMedia(
            theme = post.visualTheme,
            audioBadge = post.audioBadge
        )

        // Barra de acciones + contadores
        val displayLikes = if (isLiked) post.likesBase + 1 else post.likesBase
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = enabled) { onToggleLike() }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (isLiked) Color(0xFFFF3040) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$displayLikes",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comentarios",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.commentsCount}",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Compartir",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.sharesCount}",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            Icon(
                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Guardar",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(enabled = enabled) { onToggleSave() }
            )
        }

        // Likes + Caption + Fecha
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Les gusta a ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(post.likedByHandle)
                    }
                    append(" y ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("${post.likedOthersCount} personas más")
                    }
                },
                fontFamily = SpaceGrotesk,
                fontSize = 11.5.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append("${post.handle}  ")
                    }
                    append(post.caption)
                },
                fontFamily = SpaceGrotesk,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = Color.White.copy(alpha = 0.92f)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = post.hashtags,
                fontFamily = SpaceGrotesk,
                fontSize = 11.5.sp,
                color = Color(0xFF70B5F9)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = post.timeAgo,
                fontFamily = SpaceGrotesk,
                fontSize = 10.5.sp,
                color = TextoSecundario
            )
        }
    }
}

/**
 * Pieza fotográfica dedicada para cada publicación del feed de Instagram.
 */
@Composable
private fun IgPostVisualMedia(
    theme: Int,
    audioBadge: String
) {
    val imageResId = when (theme) {
        1 -> R.drawable.ig_post_futsal
        2 -> R.drawable.ig_post_lofi_study
        3 -> R.drawable.ig_post_dato_claro
        4 -> R.drawable.ig_post_merienda_arte
        else -> R.drawable.ig_post_ayuda_141
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
            .background(Color(0xFF0A0D14))
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = audioBadge,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            when (theme) {
                1 -> {
                    Text(
                        text = "SEMIFINAL 5.° AÑO",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Cancha Barrio Sur • 18:30 hs",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.5.sp,
                        color = VerdeExito
                    )
                }
                2 -> {
                    Text(
                        text = "LO-FI PARA ESTUDIAR",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "42 min sin publicidad • Modo foco",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.5.sp,
                        color = BordeCyan
                    )
                }
                3 -> {
                    Text(
                        text = "¿CUENTA REAL O CUENTA DEMO?",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Un ticket ganado oculta las 19 recargas perdidas.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }
                4 -> {
                    Text(
                        text = "Tarde de merienda y bocetos",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                else -> {
                    Text(
                        text = "LÍNEA 141 Y BOTI 24/7",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Escucha gratuita y confidencial si necesitás hablar",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        color = BordeCyan
                    )
                }
            }
        }

        // Badge superior izquierdo tipo carrusel / reel
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.60f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = audioBadge,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )
        }

        // Botón redondo de audio silenciado abajo a la derecha (como en IG Reels)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = "Audio silenciado",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Carrusel intercalado "Sugerencias para ti" con perfiles ficticios y botón Seguir.
 */
@Composable
private fun IgSuggestionsCarousel(
    suggestions: List<IgSuggestionItem>,
    followedHandles: Map<String, Boolean>,
    dismissedHandles: Map<String, Boolean>,
    enabled: Boolean,
    onToggleFollow: (String) -> Unit,
    onDismiss: (String) -> Unit
) {
    val visibleItems = suggestions.filter { dismissedHandles[it.handle] != true }
    if (visibleItems.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF060709))
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sugerencias para ti",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = Color.White
            )
            Text(
                text = "Ver todo",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Color(0xFF3897F0)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visibleItems) { item ->
                val isFollowed = followedHandles[item.handle] == true
                Box(
                    modifier = Modifier
                        .width(148.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF11141B))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ocultar sugerencia",
                        tint = TextoSecundario,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clickable(enabled = enabled) { onDismiss(item.handle) }
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        IgProfileAvatar(
                            handle = item.handle,
                            size = 60.dp,
                            modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.displayName,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${item.handle}",
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.sp,
                            color = TextoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sugerencia para ti",
                            fontFamily = SpaceGrotesk,
                            fontSize = 10.sp,
                            color = TextoSecundario.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFollowed) Color.White.copy(alpha = 0.14f)
                                    else Color(0xFF0095F6)
                                )
                                .clickable(enabled = enabled) { onToggleFollow(item.handle) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFollowed) "Siguiendo" else "Seguir",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Avatar estilizado con iniciales y gradiente propio para cuentas de Instagram,
 * evitando reutilizar los avatares de personajes de La Semana.
 */
@Composable
private fun IgProfileAvatar(
    handle: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        listOf(Color(0xFFE1306C), Color(0xFF833AB4)),
        listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
        listOf(Color(0xFF00B09B), Color(0xFF96C93D)),
        listOf(Color(0xFFFF512F), Color(0xFFF09819)),
        listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
    )
    val colors = palette[(handle.hashCode().let { if (it < 0) -it else it }) % palette.size]
    val initials = handle
        .split(".", "_", " ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "IG" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
            color = Color.White
        )
    }
}
