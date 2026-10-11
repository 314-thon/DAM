package com.ifts18.unadecisionmas.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.ui.components.ConfettiEffect
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.GlowCyan
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
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
        delay(200L)
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
                VerdeExito.copy(alpha = 0.24f),
                BordeCyan.copy(alpha = 0.12f),
                FondoGeneral
            ),
            center = Offset(x = 160f, y = 200f),
            radius = 800f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                RojoAlerta.copy(alpha = 0.24f),
                BordeMagenta.copy(alpha = 0.12f),
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (isWon) {
            ConfettiEffect(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =========================================================================
            // BLOQUE 1: AVISO / RESULTADO (Título de impacto, estado pedagógico y mensaje)
            // =========================================================================
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 2 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF161922).copy(alpha = 0.85f))
                        .border(
                            width = 1.dp,
                            color = if (isWon) VerdeExito.copy(alpha = 0.4f) else RojoAlerta.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isWon) "¡Felicitaciones! 🎉" else "¡Atención! ⚠️",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = if (isWon) VerdeExito else RojoAlerta,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isWon) {
                            "¡Completaste la semana con $0 de deuda!\nDemostraste autocontrol ante las ofertas y presiones sociales."
                        } else {
                            "Terminaste la semana con una deuda contraída.\nLas apuestas digitales están algorítmicamente diseñadas para generar pérdidas continuas."
                        },
                        fontFamily = SpaceGrotesk,
                        fontSize = 15.5.sp,
                        lineHeight = 22.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // BLOQUE 2: CANTIDAD DE ACIERTOS / DESACIERTOS CON SALDO FINAL
            // =========================================================================
            AnimatedVisibility(
                visible = isContentVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BurbujaChat.copy(alpha = 0.85f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Balance de Decisiones",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Métricas: Aciertos, Desaciertos y Saldo Final
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Tarjeta Aciertos
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141F18))
                                .border(1.dp, VerdeExito.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aciertos", fontFamily = SpaceGrotesk, fontSize = 12.sp, color = VerdeExito)
                            }
                            Text(
                                text = "${gameState.correctDecisionsCount} / 7",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tarjeta Desaciertos
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF261418))
                                .border(1.dp, RojoAlerta.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = RojoAlerta, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Riesgo", fontFamily = SpaceGrotesk, fontSize = 12.sp, color = RojoAlerta)
                            }
                            Text(
                                text = "${gameState.incorrectDecisionsCount} / 7",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tarjeta Saldo / Deuda Final
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isWon) Color(0xFF141F18) else Color(0xFF261418))
                                .border(1.dp, if (isWon) VerdeExito else RojoAlerta, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Deuda Final",
                                fontFamily = SpaceGrotesk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isWon) VerdeExito else RojoAlerta
                            )
                            Text(
                                text = "$${gameState.currentDebt}",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Desglose sintético del historial
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gameState.decisionHistory.forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33000000))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (record.isSafe) VerdeExito else RojoAlerta)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = record.dayName,
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = if (record.isSafe) "+$0" else "+$${record.addedDebt}",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (record.isSafe) VerdeExito else RojoAlerta
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // BLOQUE 3: BOTONES DE AYUDA Y VOLVER A EMPEZAR
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fila horizontal compacta de los 3 canales de ayuda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón 1: Orientación al Jugador
                    HelpButton(
                        title = "Orientación\nal Jugador",
                        icon = Icons.Default.Info,
                        onClick = { dialPhone("08006666006") },
                        modifier = Modifier.weight(1f)
                    )

                    // Botón 2: Chat Boti con animación Lottie (aporte de Evelyn)
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
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
            .height(98.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = shape
            )
            .clip(shape)
            .background(Color(0xFF1A1D26).copy(alpha = 0.90f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
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
                    modifier = Modifier.size(36.dp)
                )
            } else if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.size(32.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}


