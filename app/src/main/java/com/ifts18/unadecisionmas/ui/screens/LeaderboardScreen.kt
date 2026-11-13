package com.ifts18.unadecisionmas.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.data.model.RankingEntry
import com.ifts18.unadecisionmas.data.remote.FirebaseRankingRepository
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import com.ifts18.unadecisionmas.util.QrCodeHelper
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Pantalla de Ranking Compartido (Firebase + Room) y Galería de Medallas con Escáner QR SIXSEVEN.
 */
@Composable
fun LeaderboardScreen(
    gameState: GameState,
    rankingList: List<RankingEntry>,
    isFirebaseOnline: Boolean,
    firebaseStatusText: String,
    onRefreshRanking: () -> Unit,
    onQrCodeScanned: (String) -> Boolean,
    onBack: () -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showQrGeneratorDialog by remember { mutableStateOf(false) }

    val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            val unlocked = onQrCodeScanned(contents)
            if (unlocked) {
                Toast.makeText(
                    context,
                    "Medalla secreta desbloqueada: SixSeven (#67)",
                    Toast.LENGTH_LONG
                ).show()
                selectedTab = 1
            } else {
                Toast.makeText(
                    context,
                    "QR leído: $contents",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val launchQrCamera: () -> Unit = {
        SoundManager.playClick()
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Apuntá al código QR de otro jugador")
            setBeepEnabled(true)
            setOrientationLocked(false)
        }
        qrScannerLauncher.launch(options)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FondoGeneral)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP BAR
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            SoundManager.playClick()
                            onBack()
                        },
                        modifier = Modifier.size(34.dp)
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
                            text = "Ranking & Medallas",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFirebaseOnline) Icons.Default.CloudDone else Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (isFirebaseOnline) VerdeExito else BordeCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = firebaseStatusText,
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.sp,
                                color = if (isFirebaseOnline) VerdeExito else BordeCyan
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        SoundManager.playClick()
                        onRefreshRanking()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar Firebase",
                        tint = BordeCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. SELECTOR DE PESTAÑAS: RANKING EN VIVO vs MEDALLAS & QR SIXSEVEN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tabs = listOf("Ranking Global", "Medallas")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan))
                                else Brush.horizontalGradient(listOf(Color(0xFF1A1E2B), Color(0xFF1A1E2B)))
                            )
                            .clickable {
                                SoundManager.playClick()
                                selectedTab = index
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                // PESTAÑA 1: TABLA DE RANKING EN TIEMPO REAL
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Criterio: 1.° Mayor Puntaje • 2.° Menor Tiempo (Desempate) • 3.° Medallas",
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.5.sp,
                        color = TextoSecundario,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(rankingList) { index, item ->
                            RankingRowItem(
                                position = index + 1,
                                entry = item,
                                isCurrentPlayer = item.playerName.equals(gameState.profile.name, ignoreCase = true)
                            )
                        }
                    }
                }
            } else {
                // PESTAÑA 2: MEDALLAS EN ROOM + ESCÁNER QR EASTER EGG SIXSEVEN
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Banner de acción QR entre dispositivos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A1429))
                            .border(1.5.dp, BordeCyan, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan)))
                                .clickable { launchQrCamera() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Escanear QR",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF121824))
                                .border(1.dp, BordeCyan, RoundedCornerShape(10.dp))
                                .clickable {
                                    SoundManager.playClick()
                                    showQrGeneratorDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = BordeCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mostrar mi QR",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BordeCyan
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(AchievementsCatalog.all) { _, achievement ->
                            val isUnlocked = gameState.unlockedAchievementIds.contains(achievement.id)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isUnlocked) BurbujaChat.copy(alpha = 0.95f)
                                        else Color(0xFF141720).copy(alpha = 0.7f)
                                    )
                                    .border(
                                        width = if (isUnlocked) 1.5.dp else 1.dp,
                                        color = if (isUnlocked) VerdeExito else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isHiddenSecret = achievement.isSecret && !isUnlocked
                                    com.ifts18.unadecisionmas.ui.components.AchievementBadgeIcon(
                                        achievementId = achievement.id,
                                        isUnlocked = isUnlocked,
                                        containerSize = 46.dp,
                                        iconSize = 22.dp
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isHiddenSecret) {
                                                    "Medalla oculta"
                                                } else {
                                                    "${achievement.codeNumber} • ${achievement.title}"
                                                },
                                                fontFamily = SpaceGrotesk,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = if (isUnlocked) Color.White else TextoSecundario
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isHiddenSecret) {
                                                "Seguí jugando y explorando la app para descubrirla."
                                            } else {
                                                achievement.description
                                            },
                                            fontFamily = SpaceGrotesk,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = if (isUnlocked) Color.White.copy(alpha = 0.85f) else TextoSecundario.copy(alpha = 0.65f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = if (isHiddenSecret) "?? pts" else "+${achievement.bonusPoints} pts",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isUnlocked) VerdeExito else TextoSecundario
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrGeneratorDialog) {
        SixSevenQrDialog(
            playerName = gameState.profile.name,
            onDismiss = { showQrGeneratorDialog = false }
        )
    }
}

@Composable
private fun RankingRowItem(
    position: Int,
    entry: RankingEntry,
    isCurrentPlayer: Boolean
) {
    val medalEmoji = "#$position"
    val secondsFormatted = "%.1fs".format(entry.totalTimeMillis / 1000f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrentPlayer) Color(0xFF28193D)
                else Color(0xFF161A25)
            )
            .border(
                width = if (isCurrentPlayer) 1.6.dp else 1.dp,
                color = when {
                    isCurrentPlayer -> BordeCyan
                    position == 1 -> Color(0xFFFFD700).copy(alpha = 0.65f)
                    else -> Color.White.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = medalEmoji,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = if (position <= 3) 20.sp else 14.sp,
                color = Color.White,
                modifier = Modifier.width(34.dp)
            )

            Image(
                painter = painterResource(
                    id = FirebaseRankingRepository.indexToAvatarRes(entry.avatarIndex)
                ),
                contentDescription = entry.playerName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, BordeMagenta, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.playerName,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    if (entry.hasSixSeven) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BordeMagenta.copy(alpha = 0.3f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "67",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = BordeCyan
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.gameMode} • $secondsFormatted • Deuda $${entry.debt}",
                    fontFamily = SpaceGrotesk,
                    fontSize = 11.5.sp,
                    color = TextoSecundario
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.score} pts",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (entry.debt == 0) VerdeExito else RojoAlerta
            )
            Text(
                text = "Ahorro $${entry.savings}",
                fontFamily = SpaceGrotesk,
                fontSize = 10.5.sp,
                color = BordeCyan
            )
        }
    }
}

@Composable
fun SixSevenQrDialog(
    playerName: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember {
        QrCodeHelper.generateQrImageBitmap(
            content = "${AchievementsCatalog.QR_EASTER_EGG_PAYLOAD}|FROM:$playerName",
            sizePx = 520
        )
    }

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
                text = "Código QR de jugador",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BordeCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Mostrale este código a otra persona que tenga la app o escaneá el suyo.",
                fontFamily = SpaceGrotesk,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (qrBitmap != null) {
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
                        contentDescription = "Código QR de jugador",
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                    text = "Cerrar QR",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}
