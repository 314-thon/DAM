package com.ifts18.unadecisionmas.ui.screens

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.delay
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

/**
 * Pantalla final con el resultado de la decision (Apostar -> Fallaste, No apostar -> Felicitaciones con Confeti)
 * y los canales oficiales de asistencia y orientacion para la Ciudad Autonoma de Buenos Aires (CABA).
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

    // Reproductor de sonido de resultado: Felicitaciones o Perder (48 kHz, baja latencia)
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

    LaunchedEffect(Unit) {
        delay(120L)
        try {
            resultSoundPlayer?.start()
        } catch (_: Exception) {}
    }

    // SoundPool para confirmacion sonora leve en botones
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

    // Accion para discar telefono en el marcador del sistema
    val dialPhone: (String) -> Unit = { phoneNumber ->
        playClickFeedback()
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    // Accion para abrir WhatsApp
    val openWhatsApp: (String) -> Unit = { phone ->
        playClickFeedback()
        try {
            val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
            val url = "https://wa.me/549$cleanPhone?text=Hola,%20necesito%20orientacion%20sobre%20juego%20problematico"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    // Accion para abrir enlace web informativo
    val openWeb: (String) -> Unit = { url ->
        playClickFeedback()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    // Gradiente de fondo condicional segun resultado
    val backgroundBrush = if (didBet) {
        Brush.radialGradient(
            colors = listOf(
                RojoAlerta.copy(alpha = 0.22f),
                BordeMagenta.copy(alpha = 0.12f),
                FondoGeneral
            ),
            center = Offset(x = 200f, y = 200f),
            radius = 800f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                VerdeExito.copy(alpha = 0.25f),
                BordeCyan.copy(alpha = 0.14f),
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
        // Si no aposto, se despliega animacion de confeti celebratoria
        if (!didBet) {
            ConfettiEffect(
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BARRA SUPERIOR CON BOTON VOLVER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Badge de estado
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (didBet) RojoAlerta else VerdeExito,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            if (didBet) RojoAlerta.copy(alpha = 0.15f)
                            else VerdeExito.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (didBet) "DECISIÓN: APOSTASTE" else "DECISIÓN: NO APOSTASTE",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (didBet) RojoAlerta else VerdeExito
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. HERO PRINCIPAL: Titulo e impacto visual
            Box(
                modifier = Modifier
                    .size(72.dp)
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
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (didBet) "¡Fallaste!" else "¡Felicitaciones!",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = if (didBet) RojoAlerta else VerdeExito,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (didBet) {
                    "Perdiste los $1.000.- y caíste en la trampa del dinero fácil. En las apuestas digitales la casa siempre está calculada para que el usuario pierda."
                } else {
                    "¡Excelente decisión! Supiste frenar a tiempo y no te dejaste manipular por la urgencia. No arriesgar tu dinero es la verdadera victoria."
                },
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. TARJETA EDUCATIVA / REFLEXION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            if (didBet) listOf(RojoAlerta.copy(alpha = 0.6f), BordeMagenta.copy(alpha = 0.6f))
                            else listOf(BordeCyan.copy(alpha = 0.6f), VerdeExito.copy(alpha = 0.6f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(BurbujaChat.copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informacion",
                        tint = if (didBet) BordeMagenta else BordeCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (didBet) {
                            "La ludopatía digital en adolescentes suele comenzar con microapuestas bajo presión de tiempo o pares. Reconocer el impulso es el primer paso para no quedar atrapado."
                        } else {
                            "Apostar no es un juego inofensivo ni un método para ganar ingresos. Tener el criterio para decir 'No' ante mensajes persuasivos te protege a vos y a tu entorno."
                        },
                        fontSize = 13.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. SECCION DE CANALES OFICIALES DE CABA
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (didBet) "Canales oficiales de ayuda · CABA" else "Canales por si conocés a alguien que esté apostando",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (didBet) {
                        "Si sentís que las apuestas te generan angustia, deudas o no podés parar, comunicate con estos canales gratuitos y confidenciales de la Ciudad:"
                    } else {
                        "Muchos jóvenes caen en las apuestas online en silencio. Si un amigo, compañero o familiar está apostando, compartile estos contactos oficiales de CABA:"
                    },
                    fontSize = 13.sp,
                    color = TextoSecundario,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CANAL 1: LOTBA - Saber Jugar (CABA)
            OfficialChannelCard(
                title = "Línea de Orientación al Jugador Problemático",
                organization = "Lotería de la Ciudad (LOTBA) · Saber Jugar",
                badge = "Gratuito · CABA",
                badgeColor = VerdeExito,
                description = "Asesoramiento profesional, contención psicológica y derivación especializada para personas y familiares de CABA.",
                contactInfo = "0800-666-6006",
                actionLabel = "Llamar al 0800-666-6006",
                onActionClick = { dialPhone("08006666006") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CANAL 2: WhatsApp BOTI CABA
            OfficialChannelCard(
                title = "WhatsApp BOTI · Salud Mental CABA",
                organization = "Gobierno de la Ciudad de Buenos Aires",
                badge = "Chat 24/7 · CABA",
                badgeColor = BordeCyan,
                description = "Chateá con BOTI escribiendo 'Llamada Salud Mental' o 'Juego Responsable' para recibir orientación inmediata.",
                contactInfo = "11 5050-0147",
                actionLabel = "Abrir WhatsApp con BOTI",
                onActionClick = { openWhatsApp("1150500147") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CANAL 3: Linea 141 SEDRONAR
            OfficialChannelCard(
                title = "Línea 141 · Asistencia en Adicciones",
                organization = "SEDRONAR · Cobertura CABA y Nacional",
                badge = "24 hs · 365 días",
                badgeColor = GlowCyan,
                description = "Servicio telefónico confidencial de escucha, contención y acompañamiento profesional para la persona afectada o allegados.",
                contactInfo = "Línea 141",
                actionLabel = "Llamar al 141",
                onActionClick = { dialPhone("141") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // CANAL 4: Red CeSACs y Hospitales CABA
            OfficialChannelCard(
                title = "Red de CeSACs y Hospitales de la Ciudad",
                organization = "Ministerio de Salud CABA",
                badge = "Presencial · CABA",
                badgeColor = BordeMagenta,
                description = "Centros de Salud y Acción Comunitaria en todos los barrios porteños con equipos especializados en salud mental.",
                contactInfo = "Centros de Salud CABA",
                actionLabel = "Ver centros de salud en CABA",
                onActionClick = { openWeb("https://buenosaires.gob.ar/salud/centros-de-salud-y-accion-comunitaria-cesac") }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 5. BOTONES DE ACCION INFERIORES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Boton principal de reinicio / volver a jugar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(54.dp)
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Boton secundario para revisar la decision anterior
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = {
                            playClickFeedback()
                            onNavigateBack()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Revisar decisión anterior",
                        color = TextoSecundario,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar un canal oficial de asistencia con boton directo de accion.
 */
@Composable
private fun OfficialChannelCard(
    title: String,
    organization: String,
    badge: String,
    badgeColor: Color,
    description: String,
    contactInfo: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BurbujaChat.copy(alpha = 0.65f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = organization,
                    fontSize = 12.sp,
                    color = TextoSecundario,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .border(
                            width = 0.8.dp,
                            color = badgeColor.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Contacto: $contactInfo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = badgeColor.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Boton de accion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2038))
                    .border(
                        width = 1.dp,
                        color = badgeColor.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
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
    particleCount: Int = 65
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
