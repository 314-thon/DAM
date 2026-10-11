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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
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
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.delay

@Composable
fun GameResultScreen(
    gameState: GameState,
    onRestartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    val isWon = gameState.currentDebt == 0
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150L)
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

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // =========================================================================
            // ÁREA SUPERIOR Y MEDIA (Scrollable si la pantalla es reducida)
            // =========================================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // BLOQUE 1: AVISO / RESULTADO (Compacto y equilibrado)
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { -it / 2 }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161922).copy(alpha = 0.90f))
                            .border(
                                width = 1.dp,
                                color = if (isWon) VerdeExito.copy(alpha = 0.5f) else RojoAlerta.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isWon) "🎉" else "⚠️",
                            fontSize = 30.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isWon) "¡Felicitaciones! Deuda $0" else "¡Atención! Deuda contraída",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isWon) VerdeExito else RojoAlerta
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isWon) {
                                    "Superaste la semana sin deudas. Demostraste autocontrol ante las ofertas."
                                } else {
                                    "En ciberapuestas la casa siempre gana: el 95% de los jugadores pierde dinero a largo plazo."
                                },
                                fontFamily = SpaceGrotesk,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        SoundToggleButton()
                    }
                }

                // BLOQUE 2: BALANCE DE DECISIONES, MÉTRICAS Y DESGLOSE EN 2 COLUMNAS
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }
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
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Balance de Decisiones",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3 Tarjetas de métricas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Aciertos
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF141F18))
                                    .border(1.dp, VerdeExito.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Aciertos", fontFamily = SpaceGrotesk, fontSize = 11.sp, color = VerdeExito)
                                }
                                Text(
                                    text = "${gameState.correctDecisionsCount} / 7",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }

                            // Desaciertos / Riesgo
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF261418))
                                    .border(1.dp, RojoAlerta.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = RojoAlerta, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Riesgo", fontFamily = SpaceGrotesk, fontSize = 11.sp, color = RojoAlerta)
                                }
                                Text(
                                    text = "${gameState.incorrectDecisionsCount} / 7",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }

                            // Deuda Final
                            Column(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isWon) Color(0xFF141F18) else Color(0xFF261418))
                                    .border(1.dp, if (isWon) VerdeExito else RojoAlerta, RoundedCornerShape(10.dp))
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Deuda Final",
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isWon) VerdeExito else RojoAlerta
                                )
                                Text(
                                    text = "$${gameState.currentDebt}",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Desglose optimizado en 2 columnas
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
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // BLOQUE 3: ZONA DEL PULGAR (FIJA EN LA BASE, NUNCA SE CORTA)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fila horizontal de los 3 canales de ayuda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón 1: Orientación al Jugador
                    HelpButton(
                        title = "Orientación\nal Jugador",
                        icon = Icons.Default.Info,
                        onClick = { dialPhone("08006666006") },
                        modifier = Modifier.weight(1f)
                    )

                    // Botón 2: Chat Boti con animación Lottie
                    HelpButton(
                        title = "Chat\nBoti",
                        lottieRawRes = R.raw.boti_animacion,
                        onClick = { openWhatsApp("1150500147") },
                        modifier = Modifier.weight(1f)
                    )

                    // Botón 3: Línea 141
                    HelpButton(
                        title = "Línea\n141",
                        icon = Icons.Default.Phone,
                        onClick = { dialPhone("141") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Botón destacado: Volver a empezar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.horizontalGradient(listOf(BordeMagenta, BordeCyan))
                        )
                        .clickable(onClick = {
                            playClickFeedback()
                            onRestartGame()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Volver a empezar",
                        color = Color.White,
                        fontFamily = SpaceGrotesk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
                fontSize = 11.5.sp,
                color = Color.White
            )
        }

        Text(
            text = if (record.isSafe) "+$0" else "+$${record.addedDebt}",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
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
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(102.dp)
            .border(
                width = 1.2.dp,
                color = if (lottieRawRes != null) BordeCyan.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.22f),
                shape = shape
            )
            .clip(shape)
            .background(Color(0xFF1D212C).copy(alpha = 0.95f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
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
                    modifier = Modifier.size(56.dp)
                )
            } else if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.size(38.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}
