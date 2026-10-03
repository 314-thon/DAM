package com.ifts18.unadecisionmas.ui.screens

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.view.SoundEffectConstants
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.GlowCyan
import com.ifts18.unadecisionmas.ui.theme.JuegoLimpioTheme
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.delay

/**
 * Pantalla final MVP con el resultado de la decision:
 * - Apostar -> Sonido de perder + Hero "¡Fallaste!" + Canales directos de ayuda CABA.
 * - No apostar -> Sonido de felicitacion + Confeti + Hero "¡Felicitaciones!" + Canales CABA para compartir.
 */
@Composable
fun InfoResourcesScreen(
    didBet: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onRestartGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    val scrollState = rememberScrollState()

    // 1. Reproductor de sonido de resultado: Felicitaciones o Perder (48 kHz, baja latencia)
    val resultSoundPlayer = remember {
        if (!isPreview) {
            try {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    val soundResId = if (didBet) R.raw.lose_sound else R.raw.win_sound
                    val afd = context.resources.openRawResourceFd(soundResId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    setVolume(0.70f, 0.70f)
                }
            } catch (_: Exception) {
                try {
                    val soundResId = if (didBet) R.raw.lose_sound else R.raw.win_sound
                    MediaPlayer.create(context, soundResId)?.apply {
                        setVolume(0.70f, 0.70f)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    // 2. SoundPool para confirmacion sonora leve en toques de botones
    val soundPool = remember {
        if (!isPreview) {
            try {
                SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val clickSoundId = remember(soundPool) {
        if (!isPreview && soundPool != null) {
            try {
                soundPool.load(context, R.raw.button_click, 1)
            } catch (_: Exception) {
                0
            }
        } else 0
    }

    val playClickFeedback: () -> Unit = {
        try {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}

        try {
            if (!isPreview && soundPool != null && clickSoundId != 0) {
                soundPool.play(clickSoundId, 0.40f, 0.40f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    // Reproducir sonido al ingresar a la pantalla
    LaunchedEffect(Unit) {
        delay(120L)
        try {
            resultSoundPlayer?.start()
        } catch (_: Exception) {}
    }

    // Liberar recursos al salir
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (resultSoundPlayer?.isPlaying == true) {
                    resultSoundPlayer.stop()
                }
                resultSoundPlayer?.release()
            } catch (_: Exception) {}

            try {
                soundPool?.release()
            } catch (_: Exception) {}
        }
    }

    // Acciones directas
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

    // Gradiente de fondo condicional
    val backgroundBrush = if (didBet) {
        Brush.radialGradient(
            colors = listOf(
                RojoAlerta.copy(alpha = 0.22f),
                BordeMagenta.copy(alpha = 0.10f),
                FondoGeneral
            ),
            center = Offset(x = 200f, y = 200f),
            radius = 800f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                VerdeExito.copy(alpha = 0.25f),
                BordeCyan.copy(alpha = 0.12f),
                FondoGeneral
            ),
            center = Offset(x = 200f, y = 200f),
            radius = 800f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
    ) {
        // Confeti animado si no aposto (celebracion)
        if (!didBet) {
            ConfettiEffect(
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BARRA SUPERIOR MVP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable(onClick = {
                            playClickFeedback()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (didBet) RojoAlerta.copy(alpha = 0.7f) else VerdeExito.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            if (didBet) RojoAlerta.copy(alpha = 0.12f)
                            else VerdeExito.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (didBet) "DECISIÓN: APOSTASTE" else "DECISIÓN: NO APOSTASTE",
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (didBet) RojoAlerta else VerdeExito
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. HERO PRINCIPAL: Icono + Titulo + Subtitulo directo
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        if (didBet) RojoAlerta.copy(alpha = 0.18f)
                        else VerdeExito.copy(alpha = 0.20f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (didBet) RojoAlerta else VerdeExito,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (didBet) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = if (didBet) "Fallaste" else "Felicitaciones",
                    tint = if (didBet) RojoAlerta else VerdeExito,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (didBet) "¡Fallaste!" else "¡Felicitaciones!",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = if (didBet) RojoAlerta else VerdeExito,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (didBet) {
                    "Apostaste los $1.000. La presión del grupo y la promesa de ganar rápido te hicieron caer en la trampa."
                } else {
                    "Elegiste no apostar. Tuviste la cabeza fría para frenar a tiempo y no dejarte manipular por la urgencia."
                },
                fontSize = 14.5.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. TARJETA DE REFLEXION MVP (1 sola idea clave y directa)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (didBet) RojoAlerta.copy(alpha = 0.4f) else VerdeExito.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(BurbujaChat.copy(alpha = 0.75f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (didBet) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = "Aviso",
                        tint = if (didBet) BordeMagenta else BordeCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (didBet) {
                            "Las plataformas están programadas para que la casa siempre gane: el 95% de los apostadores pierde a largo plazo."
                        } else {
                            "Decir 'no' frente a la insistencia y cuidar tu plata es una verdadera victoria personal."
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. CANALES OFICIALES CABA (Formato MVP directo y accionable)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (didBet) "Canales oficiales de ayuda · CABA" else "Canales para compartir · CABA",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Text(
                    text = "Asistencia gratuita, confidencial y disponible 24 hs en la Ciudad:",
                    fontSize = 12.5.sp,
                    color = TextoSecundario,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CANAL 1: LOTBA - Linea Gratuita
            MvpChannelCard(
                title = "Línea de Orientación (LOTBA)",
                contactNumber = "0800-666-6006",
                actionLabel = "Llamar gratis",
                badgeText = "24/7",
                themeColor = if (didBet) RojoAlerta else VerdeExito,
                onActionClick = { dialPhone("08006666006") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CANAL 2: WhatsApp BOTI CABA
            MvpChannelCard(
                title = "WhatsApp BOTI · Salud Mental",
                contactNumber = "11 5050-0147",
                actionLabel = "Abrir chat",
                badgeText = "Chat CABA",
                themeColor = BordeCyan,
                onActionClick = { openWhatsApp("1150500147") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. BOTONES DE ACCION PRINCIPALES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Boton Primario: Reiniciar / Volver a intentar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(52.dp)
                        .border(
                            width = 1.8.dp,
                            brush = Brush.horizontalGradient(
                                if (didBet) listOf(RojoAlerta, BordeMagenta)
                                else listOf(BordeMagenta, BordeCyan)
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(Color(0x33121212))
                        .clickable(onClick = {
                            playClickFeedback()
                            onRestartGame()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (didBet) "Volver a intentar" else "Jugar otra partida",
                        color = Color.White,
                        fontFamily = SpaceGrotesk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Boton Secundario: Volver al inicio
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = {
                            playClickFeedback()
                            onRestartGame()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Volver al inicio",
                        color = TextoSecundario,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta compacta MVP para canales oficiales de CABA con boton directo de accion.
 */
@Composable
private fun MvpChannelCard(
    title: String,
    contactNumber: String,
    actionLabel: String,
    badgeText: String,
    themeColor: Color,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(BurbujaChat.copy(alpha = 0.60f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = contactNumber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextoSecundario
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Boton de accion directa
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor.copy(alpha = 0.18f))
                    .border(
                        width = 1.dp,
                        color = themeColor.copy(alpha = 0.50f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
            }
        }
    }
}

/**
 * Modelo de particula de confeti para animacion suave y fluida.
 */
private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var speedY: Float,
    var speedX: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val isCircle: Boolean
)

/**
 * Efecto de confeti animado de alto rendimiento con Jetpack Compose Canvas.
 */
@Composable
private fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 60
) {
    val colors = remember {
        listOf(
            VerdeExito,
            BordeCyan,
            BordeMagenta,
            GlowCyan,
            Color(0xFFFFD54F),
            Color(0xFFFF4081),
            Color.White
        )
    }

    val particles = remember {
        val random = kotlin.random.Random(1337)
        List(particleCount) {
            ConfettiParticle(
                x = random.nextFloat(),
                y = -random.nextFloat() * 0.9f,
                speedY = 0.0022f + random.nextFloat() * 0.0045f,
                speedX = (random.nextFloat() - 0.5f) * 0.002f,
                rotation = random.nextFloat() * 360f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 6f,
                width = 8f + random.nextFloat() * 8f,
                height = 5f + random.nextFloat() * 7f,
                color = colors[random.nextInt(colors.size)],
                isCircle = random.nextFloat() > 0.65f
            )
        }
    }

    var frameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                frameTime = time
                for (i in particles.indices) {
                    val p = particles[i]
                    p.y += p.speedY
                    p.x += p.speedX
                    p.rotation += p.rotationSpeed
                    if (p.y > 1.05f) {
                        p.y = -0.05f
                        p.x = kotlin.random.Random.nextFloat()
                    }
                    if (p.x < 0f) p.x = 1f
                    else if (p.x > 1f) p.x = 0f
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTime
        val w = size.width
        val h = size.height

        for (i in particles.indices) {
            val p = particles[i]
            val px = p.x * w
            val py = p.y * h

            if (py >= -20 && py <= h + 20) {
                rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.width / 2f,
                            center = Offset(px, py)
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.width / 2f, py - p.height / 2f),
                            size = Size(p.width, p.height)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun InfoResourcesScreenBetPreview() {
    JuegoLimpioTheme {
        InfoResourcesScreen(
            didBet = true
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun InfoResourcesScreenNoBetPreview() {
    JuegoLimpioTheme {
        InfoResourcesScreen(
            didBet = false
        )
    }
}
