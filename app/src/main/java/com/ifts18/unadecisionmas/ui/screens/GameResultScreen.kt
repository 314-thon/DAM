package com.ifts18.unadecisionmas.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import com.ifts18.unadecisionmas.data.model.AchievementDef
import com.ifts18.unadecisionmas.ui.components.AchievementBadgeIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.DecisionRecord
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.ui.components.ConfettiEffect
import com.ifts18.unadecisionmas.ui.components.SoundToggleButton
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import com.ifts18.unadecisionmas.util.QrCodeHelper
import kotlinx.coroutines.delay

@Composable
fun GameResultScreen(
    gameState: GameState,
    onOpenLeaderboard: () -> Unit,
    onRestartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    val isWon = gameState.currentDebt == 0
    var isContentVisible by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }
    var showMedalsModal by remember { mutableStateOf(false) }

    val sessionUnlockedDefs = remember(gameState.sessionAchievementIds) {
        AchievementsCatalog.all.filter { gameState.sessionAchievementIds.contains(it.id) }
    }

    val userQrBitmap = remember(gameState.profile.name) {
        QrCodeHelper.generateQrImageBitmap(
            content = "${AchievementsCatalog.QR_EASTER_EGG_PAYLOAD}|FROM:${gameState.profile.name}",
            sizePx = 520
        )
    }

    LaunchedEffect(Unit) {
        delay(120L)
        isContentVisible = true
    }

    val playClickFeedback: () -> Unit = {
        try {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}
        SoundManager.playClick()
    }

    val dialPhone: (String) -> Unit = { phoneNumber ->
        playClickFeedback()
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    val openWhatsApp: (String) -> Unit = { phone ->
        playClickFeedback()
        try {
            val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
            val message = Uri.encode("Quiero más info sobre la ludopatía infantil")
            val url = "https://wa.me/549$cleanPhone?text=$message"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    val backgroundBrush = if (isWon) {
        Brush.radialGradient(
            colors = listOf(
                VerdeExito.copy(alpha = 0.20f),
                BordeCyan.copy(alpha = 0.10f),
                FondoGeneral
            ),
            center = Offset(x = 160f, y = 200f),
            radius = 800f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                RojoAlerta.copy(alpha = 0.20f),
                BordeMagenta.copy(alpha = 0.10f),
                FondoGeneral
            ),
            center = Offset(x = 160f, y = 200f),
            radius = 800f
        )
    }

    val displayedScore = if (gameState.lastEarnedScore > 0) {
        gameState.lastEarnedScore
    } else {
        gameState.calculateCompetitiveScore()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val isCompactHeight = maxHeight < 680.dp
        val hasDecisionHistory = gameState.decisionHistory.isNotEmpty()
        val sectionGap = if (isCompactHeight) 5.dp else 7.dp
        val qrSize = when {
            isCompactHeight && hasDecisionHistory -> 60.dp
            isCompactHeight -> 72.dp
            else -> 82.dp
        }

        if (isWon) {
            ConfettiEffect(modifier = Modifier.fillMaxSize())
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ÁREA SUPERIOR Y MEDIA (Fija en pantalla, sin scroll vertical)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(sectionGap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // BLOQUE 1: AVISO / RESULTADO Y PUNTAJE RANKING
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFF161922).copy(alpha = 0.92f))
                            .border(
                                width = 1.dp,
                                color = if (isWon) VerdeExito.copy(alpha = 0.6f) else RojoAlerta.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(15.dp)
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = if (isCompactHeight) 7.dp else 9.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isWon) Icons.Default.EmojiEvents else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isWon) VerdeExito else RojoAlerta,
                            modifier = Modifier
                                .padding(end = 9.dp)
                                .size(if (isCompactHeight) 24.dp else 26.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${gameState.lastCompletedMode}: $displayedScore pts",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompactHeight) 15.sp else 16.sp,
                                lineHeight = if (isCompactHeight) 18.sp else 19.5.sp,
                                color = if (isWon) VerdeExito else RojoAlerta
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = gameState.lastModeSummary.ifBlank {
                                    if (isWon) "Superaste la experiencia sin deudas y cuidaste tus ahorros."
                                    else "En ciberapuestas la casa siempre gana: el 95% pierde dinero a largo plazo."
                                },
                                fontFamily = SpaceGrotesk,
                                fontSize = if (isCompactHeight) 10.5.sp else 11.5.sp,
                                lineHeight = if (isCompactHeight) 13.5.sp else 14.5.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        SoundToggleButton()
                    }
                }

                // BLOQUE 2: MÉTRICAS, DÍAS Y MEDALLAS
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(BurbujaChat.copy(alpha = 0.85f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(15.dp)
                            )
                            .padding(
                                horizontal = 11.dp,
                                vertical = if (isCompactHeight) 7.dp else 9.dp
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Balance de ${gameState.profile.name}",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                lineHeight = 18.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${"%.1f".format(gameState.totalDecisionTimeMillis / 1000f)}s • ${gameState.sessionAchievementIds.size} med.",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = BordeCyan,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(if (isCompactHeight) 5.dp else 7.dp))

                        // 3 Tarjetas de métricas: Ahorros, Bienestar y Deuda Final
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            MetricMiniCard(
                                label = "Ahorro Final",
                                value = "$${gameState.currentSavings}",
                                accentColor = BordeCyan,
                                compact = isCompactHeight,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "Bienestar",
                                value = "${gameState.wellbeingPoints}",
                                accentColor = VerdeExito,
                                compact = isCompactHeight,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "Deuda Final",
                                value = "$${gameState.currentDebt}",
                                accentColor = if (isWon) VerdeExito else RojoAlerta,
                                compact = isCompactHeight,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (gameState.decisionHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 5.dp else 7.dp))
                            val historyRows = gameState.decisionHistory.chunked(4)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                historyRows.forEachIndexed { rowIdx, rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        rowItems.forEach { record ->
                                            CompactDayGridChip(
                                                record = record,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowIdx > 0 && rowItems.size < 4) {
                                            repeat(4 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // VITRINA COMPACTA DE MEDALLAS (ALTO FIJO; TOCAR PARA VER DETALLE)
                        val totalMedalBonus = sessionUnlockedDefs.sumOf { it.bonusPoints }
                        val hasSessionMedals = sessionUnlockedDefs.isNotEmpty()
                        Spacer(modifier = Modifier.height(if (isCompactHeight) 5.dp else 7.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF121622))
                                .border(
                                    width = 1.dp,
                                    color = if (hasSessionMedals) VerdeExito.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(11.dp)
                                )
                                .clickable(enabled = hasSessionMedals) {
                                    playClickFeedback()
                                    showMedalsModal = true
                                }
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = if (isCompactHeight) 7.dp else 9.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (hasSessionMedals) {
                                    AchievementBadgeIcon(
                                        achievementId = sessionUnlockedDefs.first().id,
                                        isUnlocked = true,
                                        containerSize = 28.dp,
                                        iconSize = 15.dp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2434))
                                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = TextoSecundario,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (sessionUnlockedDefs.size) {
                                            0 -> "0 medallas desbloqueadas"
                                            1 -> "1 medalla desbloqueada"
                                            else -> "${sessionUnlockedDefs.size} medallas desbloqueadas"
                                        },
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        color = if (hasSessionMedals) VerdeExito else Color.White
                                    )
                                    Text(
                                        text = if (hasSessionMedals) {
                                            "Tocá para ver cuáles • Álbum: ${gameState.unlockedAchievementIds.size}/7"
                                        } else {
                                            "Probá devolver plata, planes reales o sacudir ante el spam"
                                        },
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        color = TextoSecundario
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            if (hasSessionMedals) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "+$totalMedalBonus pts",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = VerdeExito
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Ver medallas desbloqueadas",
                                        tint = VerdeExito,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "${gameState.unlockedAchievementIds.size}/7",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                    color = TextoSecundario
                                )
                            }
                        }
                    }
                }

                // EASTER EGG: QR DEL USUARIO EN PANTALLA FINAL (SIN DETALLES)
                if (userQrBitmap != null) {
                    AnimatedVisibility(
                        visible = isContentVisible,
                        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFF171326))
                                .border(
                                    width = 1.3.dp,
                                    brush = Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)),
                                    shape = RoundedCornerShape(15.dp)
                                )
                                .clickable {
                                    playClickFeedback()
                                    showQrModal = true
                                }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = if (isCompactHeight) 6.dp else 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(qrSize)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFF0D1117))
                                    .border(1.5.dp, BordeCyan, RoundedCornerShape(11.dp))
                                    .padding(5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = userQrBitmap,
                                    contentDescription = "Código QR de ${gameState.profile.name}",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QR de ${gameState.profile.name}",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isCompactHeight) 13.5.sp else 14.5.sp,
                                    lineHeight = if (isCompactHeight) 16.5.sp else 18.sp,
                                    color = BordeCyan
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tocá para ampliar",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 11.sp,
                                    color = TextoSecundario
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // BLOQUE 3: ZONA DEL PULGAR FIJA EN LA BASE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(sectionGap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fila horizontal de los 3 canales de ayuda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HelpButton(
                        title = "Orientación\nal Jugador",
                        icon = Icons.Default.Info,
                        onClick = { dialPhone("08006666006") },
                        compact = isCompactHeight,
                        modifier = Modifier.weight(1f)
                    )

                    HelpButton(
                        title = "Chat\nBoti",
                        lottieRawRes = R.raw.boti_animacion,
                        onClick = { openWhatsApp("1150500147") },
                        compact = isCompactHeight,
                        modifier = Modifier.weight(1f)
                    )

                    HelpButton(
                        title = "Línea\n141",
                        icon = Icons.Default.Phone,
                        onClick = { dialPhone("141") },
                        compact = isCompactHeight,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fila de 2 botones principales: Ver Ranking en Vivo + Volver al Menú
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isCompactHeight) 42.dp else 46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A2234))
                            .border(1.5.dp, BordeCyan, RoundedCornerShape(14.dp))
                            .clickable {
                                playClickFeedback()
                                onOpenLeaderboard()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = BordeCyan,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ranking en Vivo",
                                color = Color.White,
                                fontFamily = SpaceGrotesk,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isCompactHeight) 42.dp else 46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan))
                            )
                            .clickable {
                                playClickFeedback()
                                onRestartGame()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Menú principal",
                            color = Color.White,
                            fontFamily = SpaceGrotesk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showQrModal && userQrBitmap != null) {
        PlayerQrDialog(
            playerName = gameState.profile.name,
            qrBitmap = userQrBitmap,
            onDismiss = { showQrModal = false }
        )
    }

    if (showMedalsModal && sessionUnlockedDefs.isNotEmpty()) {
        SessionMedalsDialog(
            medals = sessionUnlockedDefs,
            totalAlbumUnlocked = gameState.unlockedAchievementIds.size,
            onDismiss = { showMedalsModal = false }
        )
    }
}

@Composable
private fun SessionMedalsDialog(
    medals: List<AchievementDef>,
    totalAlbumUnlocked: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141824))
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (medals.size == 1) "1 medalla desbloqueada" else "${medals.size} medallas desbloqueadas",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = VerdeExito,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Álbum total: $totalAlbumUnlocked/7",
                fontFamily = SpaceGrotesk,
                fontSize = 11.5.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                medals.forEach { trophy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A2232))
                            .border(1.dp, VerdeExito.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AchievementBadgeIcon(
                            achievementId = trophy.id,
                            isUnlocked = true,
                            containerSize = 36.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${trophy.codeNumber} • ${trophy.title}",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    lineHeight = 15.5.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+${trophy.bonusPoints} pts",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = VerdeExito
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = trophy.description,
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.sp,
                                lineHeight = 14.5.sp,
                                color = TextoSecundario
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cerrar",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun PlayerQrDialog(
    playerName: String,
    qrBitmap: ImageBitmap,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141824))
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "QR de $playerName",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BordeCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D1117))
                    .border(2.dp, BordeCyan, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Código QR de $playerName",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cerrar",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    label: String,
    value: String,
    accentColor: Color,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141923))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(vertical = if (compact) 4.dp else 5.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = SpaceGrotesk,
            fontSize = 10.sp,
            color = accentColor,
            maxLines = 1
        )
        Text(
            text = value,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 13.5.sp else 14.5.sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactDayGridChip(
    record: DecisionRecord,
    modifier: Modifier = Modifier
) {
    val shortDay = record.dayName.take(3)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (record.isSafe) VerdeExito else RojoAlerta)
            )
            Text(
                text = shortDay,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Color.White,
                maxLines = 1
            )
        }
        Text(
            text = if (record.isSafe) "+$0" else "+$${record.addedDebt}",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            color = if (record.isSafe) VerdeExito else RojoAlerta,
            maxLines = 1
        )
    }
}

@Composable
private fun HelpButton(
    title: String,
    icon: ImageVector? = null,
    imageRes: Int? = null,
    lottieRawRes: Int? = null,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(if (compact) 56.dp else 64.dp)
            .border(
                width = 1.2.dp,
                color = if (lottieRawRes != null) BordeCyan.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.22f),
                shape = shape
            )
            .clip(shape)
            .background(Color(0xFF1D212C).copy(alpha = 0.95f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (lottieRawRes != null) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRawRes))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(if (compact) 28.dp else 32.dp)
                )
            } else if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) 20.dp else 22.dp)
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontSize = if (compact) 10.sp else 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = if (compact) 11.5.sp else 12.5.sp
            )
        }
    }
}
