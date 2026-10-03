package com.ifts18.unadecisionmas.ui.screens

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.delay

/**
 * Pantalla final con diseño UI/UX estilo mockup Dribbble / Jetpack Compose:
 * - Encabezado minimalista: 'Lunes' + resultado grande '¡Fallaste!' o '¡Felicitaciones!'.
 * - Cero etiquetas en forma de píldora ni textos que digan 'Decisión'.
 * - Todo el texto del cuerpo, burbujas y botones en blanco puro (#FFFFFF) con tipografía agrandada.
 * - Flujo secuencial de mensajes que van apareciendo cada 2 segundos.
 * - Fila horizontal de botones de ayuda más grandes con el logo oficial de Boti sin fondo.
 * - Botones y controles fijos en la zona caliente del pulgar.
 * - Efectos de sonido win/lose en 48 kHz.
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

    // Estados para la aparición secuencial de mensajes cada 2 segundos
    var isMessage1Loaded by remember { mutableStateOf(false) }
    var isMessage2Loaded by remember { mutableStateOf(false) }
    var isConclusionLoaded by remember { mutableStateOf(false) }

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

    // 2. SoundPool para confirmacion sonora leve en botones
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

    // Reproducir sonido al ingresar y desplegar mensajes secuencialmente cada 2 seg
    LaunchedEffect(Unit) {
        delay(120L)
        try {
            resultSoundPlayer?.start()
        } catch (_: Exception) {}

        // Mensaje 1 aparece a los 350 ms
        delay(350L)
        isMessage1Loaded = true

        // Mensaje 2 aparece 2 segundos después
        delay(2000L)
        isMessage2Loaded = true

        // Conclusión / advertencia aparece 2 segundos después
        delay(2000L)
        isConclusionLoaded = true
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

    // Acciones de contacto directo
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

    // Gradiente ambiental oscuro de fondo
    val backgroundBrush = if (didBet) {
        Brush.radialGradient(
            colors = listOf(
                RojoAlerta.copy(alpha = 0.22f),
                BordeMagenta.copy(alpha = 0.10f),
                FondoGeneral
            ),
            center = Offset(x = 160f, y = 200f),
            radius = 800f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                VerdeExito.copy(alpha = 0.24f),
                BordeCyan.copy(alpha = 0.12f),
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
        // Confeti animado si no aposto (victoria)
        if (!didBet) {
            ConfettiEffect(
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ENCABEZADO SIMPLE: 'Lunes' + flecha atras (SIN etiquetas ni pills de 'Decisión')
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
                        .background(Color(0x2AFFFFFF))
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

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Lunes",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. CONTENIDO PRINCIPAL (Ocupa el espacio restante, scrollable si la pantalla es reducida)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TITULO DE RESULTADO GRANDE (Único elemento con color de acento)
                Text(
                    text = if (didBet) "¡Fallaste!" else "¡Felicitaciones! 🎉",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = if (didBet) RojoAlerta else VerdeExito,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FLUJO SECUENCIAL DE MENSAJES HACIA ABAJO (Aparecen cada 2 segundos)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mensaje 1
                    AnimatedVisibility(
                        visible = isMessage1Loaded,
                        enter = fadeIn(animationSpec = tween(350)) +
                                slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 380f
                                    )
                                ) { -it / 2 } +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 380f
                                    )
                                )
                    ) {
                        SequentialMessageBubble(
                            text = if (didBet) {
                                "Apostaste los $1.000 creyendo en la promesa del dinero fácil."
                            } else {
                                "Elegiste no apostar y cuidaste tu dinero."
                            }
                        )
                    }

                    // Mensaje 2 (cae 2 seg después)
                    AnimatedVisibility(
                        visible = isMessage2Loaded,
                        enter = fadeIn(animationSpec = tween(350)) +
                                slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 380f
                                    )
                                ) { -it / 2 } +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 380f
                                    )
                                )
                    ) {
                        SequentialMessageBubble(
                            text = if (didBet) {
                                "La urgencia del mensaje te hizo reaccionar por impulso sin tiempo para pensar."
                            } else {
                                "Tuviste la cabeza fría para frenar a tiempo y no dejarte presionar por el grupo."
                            }
                        )
                    }

                    // Advertencia / conclusión concisa (cae 2 seg después)
                    AnimatedVisibility(
                        visible = isConclusionLoaded,
                        enter = fadeIn(animationSpec = tween(350)) +
                                slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 380f
                                    )
                                ) { -it / 2 } +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 380f
                                    )
                                )
                    ) {
                        ConciseWarningBox(
                            text = if (didBet) {
                                "En las apuestas digitales la casa siempre gana: el 95% de los apostadores pierde a largo plazo."
                            } else {
                                "Poder decir 'no' ante la insistencia social es la verdadera victoria."
                            },
                            accentColor = if (didBet) RojoAlerta else VerdeExito
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. ZONA CALIENTE DEL PULGAR (FIJA EN LA BASE DE LA PANTALLA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fila horizontal compacta de los 3 canales de ayuda (más grandes, con logo oficial Boti sin fondo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Boton 1: Orientacion al Jugador
                    CompactHelpButton(
                        title = "Orientación\nal Jugador",
                        icon = Icons.Default.Info,
                        onClick = { dialPhone("08006666006") },
                        modifier = Modifier.weight(1f)
                    )

                    // Boton 2: Chat Boti con logo oficial de Boti sin fondo
                    CompactHelpButton(
                        title = "Chat\nBoti",
                        imageRes = R.drawable.ic_boti,
                        onClick = { openWhatsApp("1150500147") },
                        modifier = Modifier.weight(1f)
                    )

                    // Boton 3: Linea 141
                    CompactHelpButton(
                        title = "Línea\n141",
                        icon = Icons.Default.Phone,
                        onClick = { dialPhone("141") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Boton secundario en la base para 'Volver a intentar'
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x26FFFFFF))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(onClick = {
                            playClickFeedback()
                            onRestartGame()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Volver a intentar",
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

/**
 * Burbuja de mensaje en cascada con texto en blanco puro y fuente agrandada.
 */
@Composable
private fun SequentialMessageBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = shape
            )
            .clip(shape)
            .background(BurbujaChat.copy(alpha = 0.85f))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            fontSize = 17.5.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 25.sp,
            color = Color.White
        )
    }
}

/**
 * Caja de advertencia concisa con texto en blanco puro y fuente agrandada.
 */
@Composable
private fun ConciseWarningBox(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.45f),
                shape = shape
            )
            .clip(shape)
            .background(Color(0xFF1E1629).copy(alpha = 0.80f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Aviso",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Boton compacto de canal de ayuda más grande, con soporte para icono o imagen oficial (Boti) y texto blanco puro.
 */
@Composable
private fun CompactHelpButton(
    title: String,
    icon: ImageVector? = null,
    imageRes: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(102.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = shape
            )
            .clip(shape)
            .background(Color(0xFF1A1D26).copy(alpha = 0.90f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.size(36.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
