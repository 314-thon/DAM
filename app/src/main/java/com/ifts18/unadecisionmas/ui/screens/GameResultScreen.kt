package com.ifts18.unadecisionmas.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay

@Composable
fun GameResultScreen(
    gameState: GameState,
    onOpenLeaderboard: () -> Unit,
    onQrCodeScanned: (String) -> Boolean,
    onRestartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    val isWon = gameState.currentDebt == 0
    var isContentVisible by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }

    val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            val unlocked = onQrCodeScanned(contents)
            if (unlocked) {
                Toast.makeText(
                    context,
                    "👾 ¡MEDALLA SECRETA 'SIXSEVEN (#67)' DESBLOQUEADA!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "QR escaneado: $contents", Toast.LENGTH_SHORT).show()
            }
        }
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
            val url = "https://wa.me/549$cleanPhone?text=Hola,%20necesito%20orientacion%20sobre%20juego%20problematico"
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
    val hasSixSeven = gameState.unlockedAchievementIds.contains(AchievementsCatalog.ID_SIXSEVEN)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (isWon) {
            ConfettiEffect(modifier = Modifier.fillMaxSize())
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ÁREA SUPERIOR Y MEDIA (Scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(9.dp),
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161922).copy(alpha = 0.92f))
                            .border(
                                width = 1.dp,
                                color = if (isWon) VerdeExito.copy(alpha = 0.6f) else RojoAlerta.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isWon) "🎉" else "⚠️",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${gameState.lastCompletedMode}: $displayedScore pts",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isWon) VerdeExito else RojoAlerta
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = gameState.lastModeSummary.ifBlank {
                                    if (isWon) "Superaste la experiencia sin deudas y cuidaste tus ahorros."
                                    else "En ciberapuestas la casa siempre gana: el 95% pierde dinero a largo plazo."
                                },
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        SoundToggleButton()
                    }
                }

                // BLOQUE 2: MÉTRICAS Y DESGLOSE
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BurbujaChat.copy(alpha = 0.85f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "⏱ ${"%.1f".format(gameState.totalDecisionTimeMillis / 1000f)}s • 🏅 ${gameState.unlockedAchievementIds.size}/7",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = BordeCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3 Tarjetas de métricas: Ahorros, Bienestar y Deuda Final
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricMiniCard(
                                label = "Ahorro Final",
                                value = "$${gameState.currentSavings}",
                                accentColor = BordeCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "Bienestar",
                                value = "★ ${gameState.wellbeingPoints}",
                                accentColor = VerdeExito,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "Deuda Final",
                                value = "$${gameState.currentDebt}",
                                accentColor = if (isWon) VerdeExito else RojoAlerta,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (gameState.decisionHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val history = gameState.decisionHistory
                            val firstCol = history.take(4)
                            val secondCol = history.drop(4)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    firstCol.forEach { record ->
                                        CompactDayRecordPill(record)
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    secondCol.forEach { record ->
                                        CompactDayRecordPill(record)
                                    }
                                }
                            }
                        }

                        // VITRINA DE TROFEOS Y MEDALLAS DESBLOQUEADOS
                        val unlockedDefs = AchievementsCatalog.all.filter {
                            gameState.unlockedAchievementIds.contains(it.id)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF121622))
                                .border(1.dp, VerdeExito.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "🏅 Trofeos Desbloqueados (${unlockedDefs.size}/7)",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = VerdeExito
                            )

                            if (unlockedDefs.isEmpty()) {
                                Text(
                                    text = "Aún no destrabaste trofeos. ¡Devolvé transferencias, elegí planes reales o agitá el celu ante el spam!",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 11.sp,
                                    color = TextoSecundario
                                )
                            } else {
                                unlockedDefs.forEach { trophy ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1A2232))
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = trophy.iconEmoji, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${trophy.codeNumber} • ${trophy.title}",
                                                fontFamily = SpaceGrotesk,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = "+${trophy.bonusPoints} pts",
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = VerdeExito
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // BLOQUE QR EASTER EGG "SIXSEVEN" (#67) ENTRE DISPOSITIVOS
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF171326))
                            .border(
                                width = 1.4.dp,
                                brush = Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasSixSeven) "👾 Medalla SIXSEVEN (#67) Activa" else "👾 Easter Egg QR: Medalla SIXSEVEN",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = BordeCyan
                                )
                                Text(
                                    text = "Mostrá tu QR o escaneá el de otro compañero para destrabar 'SIXSEVEN'.",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 11.sp,
                                    color = TextoSecundario
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF251A3A))
                                    .border(1.dp, BordeCyan, RoundedCornerShape(10.dp))
                                    .clickable {
                                        playClickFeedback()
                                        showQrModal = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = BordeCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Mostrar QR #67",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)))
                                    .clickable {
                                        playClickFeedback()
                                        val options = ScanOptions().apply {
                                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                            setPrompt("Escaneá el QR de otro compañero para desbloquear SIXSEVEN (#67)")
                                            setBeepEnabled(true)
                                            setOrientationLocked(false)
                                        }
                                        qrScannerLauncher.launch(options)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Escanear QR",
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

            Spacer(modifier = Modifier.height(8.dp))

            // BLOQUE 3: ZONA DEL PULGAR FIJA EN LA BASE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
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
                        modifier = Modifier.weight(1f)
                    )

                    HelpButton(
                        title = "Chat\nBoti",
                        lottieRawRes = R.raw.boti_animacion,
                        onClick = { openWhatsApp("1150500147") },
                        modifier = Modifier.weight(1f)
                    )

                    HelpButton(
                        title = "Línea\n141",
                        icon = Icons.Default.Phone,
                        onClick = { dialPhone("141") },
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
                            .height(48.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ranking en Vivo",
                                color = Color.White,
                                fontFamily = SpaceGrotesk,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
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
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showQrModal) {
        SixSevenQrDialog(
            playerName = gameState.profile.name,
            onDismiss = { showQrModal = false }
        )
    }
}

@Composable
private fun MetricMiniCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141923))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = SpaceGrotesk,
            fontSize = 10.5.sp,
            color = accentColor
        )
        Text(
            text = value,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}

@Composable
private fun CompactDayRecordPill(record: DecisionRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (record.isSafe) VerdeExito else RojoAlerta)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = record.dayName,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Text(
            text = if (record.isSafe) "+$0" else "+$${record.addedDebt}",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (record.isSafe) VerdeExito else RojoAlerta
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
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(84.dp)
            .border(
                width = 1.2.dp,
                color = if (lottieRawRes != null) BordeCyan.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.22f),
                shape = shape
            )
            .clip(shape)
            .background(Color(0xFF1D212C).copy(alpha = 0.95f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                    modifier = Modifier.size(44.dp)
                )
            } else if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.size(30.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 13.5.sp
            )
        }
    }
}
