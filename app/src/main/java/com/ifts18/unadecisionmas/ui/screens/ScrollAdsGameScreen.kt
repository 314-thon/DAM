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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.R
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
    onBackToHub: () -> Unit,
    onGameFinished: (adsShakenOff: Int, adTrapsClicked: Int, elapsedMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val igStories = remember {
        listOf(
            IgStoryItem(handle = "Tu historia", avatarResId = R.drawable.avatar_1, isOwnStory = true),
            IgStoryItem(handle = "valen.musica", avatarResId = R.drawable.avatar_2),
            IgStoryItem(handle = "lauti_bici", avatarResId = R.drawable.avatar_3),
            IgStoryItem(handle = "camila.art", avatarResId = R.drawable.avatar_4),
            IgStoryItem(handle = "tomas.voley", avatarResId = R.drawable.avatar_5),
            IgStoryItem(handle = "mora.sketch", avatarResId = R.drawable.avatar_4),
            IgStoryItem(handle = "fede_ok", avatarResId = R.drawable.avatar_1)
        )
    }

    val igSuggestions = remember {
        listOf(
            IgSuggestionItem("Julieta Paz", "julipaz.ok", R.drawable.avatar_4),
            IgSuggestionItem("Mate & Código", "mateycodigo", R.drawable.avatar_3),
            IgSuggestionItem("Bici Urbana BA", "biciurbanaba", R.drawable.avatar_5),
            IgSuggestionItem("Cine Club 5B", "cineclub.5b", R.drawable.avatar_1)
        )
    }

    val igPosts = remember {
        listOf(
            IgFeedPostItem(
                id = 1,
                handle = "club.barrio_sur",
                subLabel = "Audio original • Torneo 5.° Año",
                isVerified = true,
                avatarResId = R.drawable.avatar_3,
                audioBadge = "REEL • 0:24",
                likesBase = 1284,
                commentsCount = 43,
                sharesCount = 19,
                likedByHandle = "lauti_bici",
                likedOthersCount = "1.283",
                caption = "Hoy se juega la semi en la cancha del club con toda la banda del curso, cero excusas ⚽🔥",
                hashtags = "#FutsalBarrioSur #PlanReal #SinTimba",
                timeAgo = "Hace 2 horas",
                visualTheme = 1
            ),
            IgFeedPostItem(
                id = 2,
                handle = "valen.musica",
                subLabel = "Sugerencia para ti",
                isVerified = false,
                avatarResId = R.drawable.avatar_2,
                audioBadge = "Lo-Fi Study Mix",
                likesBase = 892,
                commentsCount = 27,
                sharesCount = 64,
                likedByHandle = "camila.art",
                likedOthersCount = "891",
                caption = "Armé playlist nueva para meterle al TP sin distraerse ni caer en notificaciones truchas 📚🎧",
                hashtags = "#MusicaParaEstudiar #Playlist5B #ModoFocus",
                timeAgo = "Hace 4 horas",
                visualTheme = 2
            ),
            IgFeedPostItem(
                id = 3,
                handle = "dato.claro",
                subLabel = "Educación Digital • Carrusel",
                isVerified = true,
                avatarResId = R.drawable.avatar_1,
                audioBadge = "1 / 4",
                likesBase = 3410,
                commentsCount = 118,
                sharesCount = 405,
                likedByHandle = "fede_ok",
                likedOthersCount = "3.409",
                caption = "Cuando un influencer muestra un retiro de $400k casi siempre usa 'cuenta demo' con saldo ficticio. Nadie sube las 20 pérdidas de la semana.",
                hashtags = "#OjoConElDemo #VentajaDeLaCasa #DatoReal",
                timeAgo = "Hace 6 horas",
                visualTheme = 3
            ),
            IgFeedPostItem(
                id = 4,
                handle = "camila.art",
                subLabel = "Palermo, Buenos Aires",
                isVerified = false,
                avatarResId = R.drawable.avatar_4,
                audioBadge = "Acústico • Café y Trazos",
                likesBase = 645,
                commentsCount = 19,
                sharesCount = 12,
                likedByHandle = "valen.musica",
                likedOthersCount = "644",
                caption = "Lo mejor de cuidar tus ahorros es poder disfrutar salidas reales, merienda con amigas y cero deuda en el bolsillo ☕✏️",
                hashtags = "#SalidasReales #Sketchbook #Cuidate",
                timeAgo = "Hace 9 horas",
                visualTheme = 4
            ),
            IgFeedPostItem(
                id = 5,
                handle = "espacio.joven141",
                subLabel = "Información Pública • CABA",
                isVerified = true,
                avatarResId = R.drawable.ic_boti,
                audioBadge = "Recurso Gratuito 24/7",
                likesBase = 4920,
                commentsCount = 95,
                sharesCount = 812,
                likedByHandle = "tomas.voley",
                likedOthersCount = "4.919",
                caption = "Si algún compañero la está pasando mal con apuestas o préstamos de cajeros, hablar a tiempo cambia todo. Línea 141 y Boti atienden gratis 24/7 🛡️",
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
                igSponsoredLabel = "Publicidad • B-Play VIP Casino",
                avatarResId = R.drawable.avatar_5,
                postImageResId = R.drawable.avatar_5,
                imageBannerTag = "📲 RETIRO EN VIVO (CUENTA DEMO)",
                imageOverlayHeadline = "+$450.000 EN 10 MINUTOS 💪",
                imageOverlaySubtext = "«Empecé con $2.000 entre serie y serie en el gym»",
                likesCountText = "Les gusta a lucas_09 y 18.420 personas más",
                bodyText = "Bro, dejá de scrollear gratis. Yo generé mi riqueza desde el gimnasio apostando $2.000. El primer tiro tiene ganancia asegurada.",
                hashtags = "#FitnessMillionaire #CasinoVIP #GanarFacil #BonoGratis",
                trapButtonText = "Instalar App y Apostar $4.000",
                accentColor = BordeMagenta
            ),
            InvasiveAdSpec(
                id = 2,
                triggerProgress = 0.42f,
                igUsername = "cajero.express24",
                igSponsoredLabel = "Publicidad • Crédito al Toque",
                avatarResId = R.drawable.avatar_2,
                postImageResId = R.drawable.avatar_2,
                imageBannerTag = "⚡ BILLETERA SIN RECIBO NI FIRMA",
                imageOverlayHeadline = "¡PRÉSTAMO DE $20.000 AL INSTANTE! 💸",
                imageOverlaySubtext = "Acreditación en 1 clic • Devolvelo cuando ganes",
                likesCountText = "Les gusta a nico.apuestas y 9.150 personas más",
                bodyText = "Te acreditamos $20.000 ahora mismo sin recibo ni firma de tus padres. Cargá fichas ya en la ruleta nocturna.",
                hashtags = "#PrestamoAlToque #Cajero24hs #FichasFiadas #SinLimite",
                trapButtonText = "Aceptar Préstamo de $20.000",
                accentColor = Color(0xFFFFB300)
            ),
            InvasiveAdSpec(
                id = 3,
                triggerProgress = 0.68f,
                igUsername = "fijas.superclasico",
                igSponsoredLabel = "Publicidad • Apuestas Deportivas",
                avatarResId = R.drawable.avatar_4,
                postImageResId = R.drawable.captura,
                imageBannerTag = "⚽ COMBINADA VIP • CUOTA X10.5",
                imageOverlayHeadline = "¡TODOS TUS AMIGOS YA ENTRARON!",
                imageOverlaySubtext = "Pozo acumulado del Superclásico • Cierra en 5s",
                likesCountText = "Les gusta a mati_futbol y 24.300 personas más",
                bodyText = "Mirá el ticket ganador que acaban de pasar al grupo. Si no transferís ahora te quedás afuera de la combinada.",
                hashtags = "#Superclasico #FijaDelDia #CuotaX10 #TimbaDeportiva",
                trapButtonText = "Transferir $4.000 al Cajero",
                accentColor = RojoAlerta
            ),
            InvasiveAdSpec(
                id = 4,
                triggerProgress = 0.88f,
                igUsername = "casino.nocturno.bet",
                igSponsoredLabel = "Publicidad • Bono Bienvenida",
                avatarResId = R.drawable.avatar_1,
                postImageResId = R.drawable.juego_limpio_logo_transparent,
                imageBannerTag = "🎰 GIRO GRATIS PENDIENTE • NO CIERRES",
                imageOverlayHeadline = "¡RECLAMÁ $50.000 EN FICHAS DE BONO!",
                imageOverlaySubtext = "El botón 'X' está bloqueado • Sacudí para salir",
                likesCountText = "Les gusta a cajero_nocturno y 31.900 personas más",
                bodyText = "Tenés un Jackpot pre-asignado esperando tu depósito mínimo de $4.000. ¡No pierdas tu racha!",
                hashtags = "#JackpotVIP #SlotsOnline #RuletaEnVivo #BonoCebo",
                trapButtonText = "Reclamar Bono Trampa ($4.000)",
                accentColor = BordeCyan
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

    // Disparar anuncio invasivo según el progreso de scroll del feed de Instagram
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
            // 1. HEADER CON PROGRESO DEL FEED Y TELEMETRÍA DEL SENSOR
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
                                text = "Scrolleá el feed • Agitá el celu si salta spam",
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

                // Barra de progreso de recorrido del feed y estado del Acelerómetro en vivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recorrido del Feed: ${(animatedReadProgress * 100).toInt()}%",
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

            // 2. REPLICA DE FEED DE INSTAGRAM EN MODO OSCURO (SCROLLEABLE)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF060709))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .verticalScroll(
                        state = scrollState,
                        enabled = activeAd == null // Se bloquea el scroll cuando salta una publicidad invasiva
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
                        enabled = activeAd == null,
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
                            enabled = activeAd == null,
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
                            text = "Terminar Feed sin Caer y Ver Puntaje",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
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
                    // A. CABECERA DE POST DE INSTAGRAM (Avatar con aro Story + Usuario Verificado + Publicidad)
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
                                Image(
                                    painter = painterResource(id = ad.avatarResId),
                                    contentDescription = ad.igUsername,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
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
                                    Text(
                                        text = "✓",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF3897F0)
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "[X Bloqueado]",
                                fontFamily = SpaceGrotesk,
                                fontSize = 10.sp,
                                color = TextoSecundario
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
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

                    // C. BARRA CTA DE INSTAGRAM ADS (TRAMPA: SI EL JUGADOR LA TOCA, SUMA DEUDA)
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
                            text = "⚠️ ${ad.trapButtonText} (Trampa)",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = "➔",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ad.accentColor
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
                                    text = "¡AGITÁ EL CELULAR PARA CERRAR EL POST!",
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
                                    text = "Resistencia al Spam: ${(shakeProgress * 100).toInt()}%",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )

                                Text(
                                    text = "[Simular sacudida]",
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
                        Image(
                            painter = painterResource(id = story.avatarResId),
                            contentDescription = story.handle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
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
                    Image(
                        painter = painterResource(id = post.avatarResId),
                        contentDescription = post.handle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
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
 * Pieza gráfica estilo Reel/Post de Instagram con ilustraciones vectoriales y botón de silencio.
 */
@Composable
private fun IgPostVisualMedia(
    theme: Int,
    audioBadge: String
) {
    val bgColors = when (theme) {
        1 -> listOf(Color(0xFF0A2E2A), Color(0xFF0D1B2A))
        2 -> listOf(Color(0xFF28123D), Color(0xFF121629))
        3 -> listOf(Color(0xFF141E30), Color(0xFF0B0F17))
        4 -> listOf(Color(0xFF3A1C32), Color(0xFF1A1726))
        else -> listOf(Color(0xFF0D2836), Color(0xFF111827))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Brush.linearGradient(bgColors))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (theme) {
                1 -> {
                    // Cancha de futsal estilizada
                    drawRect(
                        color = VerdeExito.copy(alpha = 0.35f),
                        topLeft = Offset(32f, 24f),
                        size = Size(w - 64f, h - 48f),
                        style = Stroke(width = 4f)
                    )
                    drawLine(
                        color = VerdeExito.copy(alpha = 0.35f),
                        start = Offset(w / 2f, 24f),
                        end = Offset(w / 2f, h - 24f),
                        strokeWidth = 3f
                    )
                    drawCircle(
                        color = VerdeExito.copy(alpha = 0.35f),
                        radius = h * 0.22f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 3f)
                    )
                }
                2 -> {
                    // Barras de ecualizador Lo-Fi
                    val barCount = 14
                    val step = (w - 80f) / barCount
                    for (i in 0 until barCount) {
                        val factor = ((i * 3 + 2) % 5 + 2) / 7f
                        val barH = h * 0.52f * factor
                        drawRoundRect(
                            color = if (i % 2 == 0) BordeMagenta.copy(alpha = 0.5f) else BordeCyan.copy(alpha = 0.45f),
                            topLeft = Offset(44f + i * step, (h - barH) / 2f),
                            size = Size(step * 0.56f, barH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
                3 -> {
                    // Gráfico comparativo: ventaja matemática de la casa vs ahorro real
                    val pathDown = Path().apply {
                        moveTo(40f, h * 0.30f)
                        lineTo(w * 0.35f, h * 0.24f)
                        lineTo(w * 0.65f, h * 0.62f)
                        lineTo(w - 40f, h * 0.80f)
                    }
                    drawPath(path = pathDown, color = RojoAlerta.copy(alpha = 0.78f), style = Stroke(width = 6f))
                    val pathSteady = Path().apply {
                        moveTo(40f, h * 0.55f)
                        lineTo(w * 0.45f, h * 0.48f)
                        lineTo(w - 40f, h * 0.32f)
                    }
                    drawPath(path = pathSteady, color = VerdeExito.copy(alpha = 0.8f), style = Stroke(width = 5f))
                }
                4 -> {
                    // Círculos cálidos de boceto urbano
                    drawCircle(
                        color = Color(0xFFFF8A65).copy(alpha = 0.22f),
                        radius = h * 0.34f,
                        center = Offset(w * 0.72f, h * 0.36f)
                    )
                    drawCircle(
                        color = BordeMagenta.copy(alpha = 0.18f),
                        radius = h * 0.25f,
                        center = Offset(w * 0.28f, h * 0.64f)
                    )
                }
                else -> {
                    // Ondas de resguardo y acompañamiento
                    drawCircle(
                        color = BordeCyan.copy(alpha = 0.16f),
                        radius = h * 0.44f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 3f)
                    )
                    drawCircle(
                        color = VerdeExito.copy(alpha = 0.24f),
                        radius = h * 0.28f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        // Centro visual del post según temática
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (theme) {
                1 -> {
                    Text(
                        text = "⚽ SEMIFINAL 5.° AÑO",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cancha Barrio Sur • 18:30 hs",
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        color = VerdeExito
                    )
                }
                2 -> {
                    Text(
                        text = "🎧 LO-FI PARA ESTUDIAR",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "42 min sin publicidad • Modo foco",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.5.sp,
                        color = BordeCyan
                    )
                }
                3 -> {
                    Text(
                        text = "📊 ¿CUENTA REAL O CUENTA DEMO?",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1 ticket ganador en el grupo oculta las 19 recargas perdidas.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center
                    )
                }
                4 -> {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_4),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "☕ Tarde de merienda y bocetos",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_boti),
                        contentDescription = "Boti",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(2.dp, BordeCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🛡️ LÍNEA 141 Y BOTI 24/7",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Escucha gratuita y confidencial si necesitás hablar",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        color = BordeCyan,
                        textAlign = TextAlign.Center
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
                        Image(
                            painter = painterResource(id = item.avatarResId),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
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
