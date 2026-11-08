package com.ifts18.unadecisionmas.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.ui.components.SoundToggleButton
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoOscuroInicio
import com.ifts18.unadecisionmas.ui.theme.GlowCyan
import com.ifts18.unadecisionmas.ui.theme.GlowVioleta
import com.ifts18.unadecisionmas.ui.theme.JuegoLimpioTheme
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    initialPlayerName: String = "",
    hasSavedGame: Boolean = false,
    savedGameDayName: String = "",
    unlockedAchievementsCount: Int = 0,
    onStartWeekGame: (String, Int) -> Unit,
    onResumeSavedGame: () -> Unit,
    onStartScrollAdsGame: (String, Int) -> Unit,
    onStartSlotsTrapGame: (String, Int) -> Unit,
    onOpenLeaderboard: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var playerName by remember { mutableStateOf(initialPlayerName) }
    var selectedAvatarResId by remember { mutableIntStateOf(R.drawable.avatar_1) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val avatarsList = remember {
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

    LaunchedEffect(isFocused) {
        if (isFocused) {
            keyboardController?.show()
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FondoOscuroInicio)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowCyan.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.90f, size.height * 0.06f),
                        radius = size.maxDimension * 0.75f
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowVioleta.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.08f, size.height * 0.65f),
                        radius = size.maxDimension * 0.80f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. BARRA SUPERIOR: ACCESO A RANKING / MEDALLAS QR + BOTÓN DE SONIDO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A2030))
                        .border(1.dp, BordeCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            playClickFeedback()
                            keyboardController?.hide()
                            onOpenLeaderboard(currentCleanName(), selectedAvatarResId)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Ranking y Medallas",
                            tint = BordeCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ranking & QR ($unlockedAchievementsCount/7)",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }

                SoundToggleButton()
            }

            // 2. TÍTULO ANIMADO LOTTIE "JUEGO LIMPIO"
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.juego_limpio_jackpot)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = 0.5f
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            // 3. SELECTOR DE AVATAR Y APODO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF171824).copy(alpha = 0.90f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Elegí tu avatar e ingresá tu apodo",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    avatarsList.forEach { avatarRes ->
                        val isSelected = selectedAvatarResId == avatarRes
                        val avatarSize by animateDpAsState(
                            targetValue = if (isSelected) 56.dp else 48.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "avatarSize"
                        )
                        val imageSize by animateDpAsState(
                            targetValue = if (isSelected) 50.dp else 42.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "imageSize"
                        )

                        Box(
                            modifier = Modifier
                                .size(avatarSize)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.8.dp else 1.3.dp,
                                    brush = if (isSelected) {
                                        Brush.linearGradient(listOf(BordeCyan, BordeMagenta))
                                    } else {
                                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.15f)))
                                    },
                                    shape = CircleShape
                                )
                                .background(if (isSelected) BordeCyan.copy(alpha = 0.2f) else Color(0xFF1E1B26))
                                .clickable {
                                    playClickFeedback()
                                    selectedAvatarResId = avatarRes
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(imageSize)
                                    .clip(CircleShape)
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(VerdeExito),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextField(
                    value = playerName,
                    onValueChange = { input ->
                        if (input.length <= 25 && !input.contains("\n")) {
                            playerName = input
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    placeholder = {
                        Text(
                            text = "Tu apodo (Ej: Mateo)",
                            style = TextStyle(
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Normal,
                                fontSize = 17.sp,
                                color = TextoSecundario.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = BordeCyan,
                        unfocusedIndicatorColor = Color(0x66FFFFFF),
                        focusedPlaceholderColor = TextoSecundario.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = TextoSecundario.copy(alpha = 0.6f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .width(250.dp)
                )
            }

            // 4. SI HAY PARTIDA GUARDADA EN ROOM/SQLITE: BOTÓN DE CONTINUAR PARTIDA
            if (hasSavedGame) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF13261D))
                        .border(1.5.dp, VerdeExito, RoundedCornerShape(14.dp))
                        .clickable {
                            playClickFeedback()
                            keyboardController?.hide()
                            onResumeSavedGame()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💾 Continuar Partida Guardada (Room)",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = VerdeExito
                            )
                            Text(
                                text = "Retomar desde el día $savedGameDayName",
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Continuar",
                            tint = VerdeExito
                        )
                    }
                }
            }

            // 5. LOS 3 JUEGOS INTEGRADOS EN LA APLICACIÓN
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Seleccioná un modo de juego",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextoSecundario,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // JUEGO 1: LA SEMANA (MODO HISTORIA PRINCIPAL)
                GameLaunchCard(
                    badge = "JUEGO 1 • MODO HISTORIA (7 DÍAS)",
                    title = "La Semana: Una Decisión Más",
                    description = "Novela interactiva contrarreloj (25s), ahorros, amigos y desafíos cotidianos.",
                    emoji = "💬",
                    isPrimary = true,
                    onClick = {
                        playClickFeedback()
                        keyboardController?.hide()
                        onStartWeekGame(currentCleanName(), selectedAvatarResId)
                    }
                )

                // JUEGO 2: SCROLL SIN CAER (PDF VECTORIZADO + ADS + ACELERÓMETRO)
                GameLaunchCard(
                    badge = "JUEGO 2 • SENSOR ACELERÓMETRO",
                    title = "Scroll sin Caer: Spam Attack",
                    description = "Leé y scrolleá el apunte escolar y agitá el teléfono para destruir los anuncios de casinos e influencers.",
                    emoji = "📳",
                    isPrimary = false,
                    onClick = {
                        playClickFeedback()
                        keyboardController?.hide()
                        onStartScrollAdsGame(currentCleanName(), selectedAvatarResId)
                    }
                )

                // JUEGO 3: LA TRAMPA DEL JACKPOT (TRAGAMONEDAS + RULETA 10 TIROS CON SENSOR)
                GameLaunchCard(
                    badge = "JUEGO 3 • SIMULADOR ALGORÍTMICO + SENSOR",
                    title = "La Trampa del Jackpot (10 Tiros)",
                    description = "Girás el tragamonedas y la ruleta agitando el celu para descubrir por dentro cómo la casa siempre gana.",
                    emoji = "🎰",
                    isPrimary = false,
                    onClick = {
                        playClickFeedback()
                        keyboardController?.hide()
                        onStartSlotsTrapGame(currentCleanName(), selectedAvatarResId)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GameLaunchCard(
    badge: String,
    title: String,
    description: String,
    emoji: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isPrimary) BurbujaChat.copy(alpha = 0.92f)
                else Color(0xFF161A26).copy(alpha = 0.92f)
            )
            .border(
                width = if (isPrimary) 2.dp else 1.2.dp,
                brush = if (isPrimary) {
                    Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan))
                } else {
                    Brush.horizontalGradient(listOf(BordeCyan.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f)))
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isPrimary) BordeCyan else VerdeExito
                )
                Text(
                    text = title,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = TextoSecundario
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121118)
@Composable
fun WelcomeScreenPreview() {
    JuegoLimpioTheme {
        WelcomeScreen(
            onStartWeekGame = { _, _ -> },
            onResumeSavedGame = {},
            onStartScrollAdsGame = { _, _ -> },
            onStartSlotsTrapGame = { _, _ -> },
            onOpenLeaderboard = { _, _ -> }
        )
    }
}
