package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.UnaDecisionMasTheme
import com.ifts18.unadecisionmas.ui.theme.VerdeExito

@Composable
fun FirstDecisionScreen(
    playerName: String = "Mateo",
    onApostarClick: () -> Unit = {},
    onNoApostarClick: () -> Unit = {},
    onNavigateToInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Gradiente difuminado verde/azulado en la esquina superior izquierda
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF00E676).copy(alpha = 0.28f),
            Color(0xFF00E5FF).copy(alpha = 0.15f),
            FondoGeneral
        ),
        center = Offset(x = 100f, y = 350f),
        radius = 900f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BARRA SUPERIOR: Titulo "Lunes" y Foto de perfil circular
            HeaderSection(
                title = "Lunes"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 2. TARJETA CENTRAL ESTILO CHAT (Speech Bubble)
            ChatMessageCard(
                playerName = playerName,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. BARRA DE PROGRESO (Dividida en Verde y Rojo)
            SplitProgressBar(
                greenRatio = 0.78f,
                redRatio = 0.22f,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4. BOTONES DE ACCIÓN INFERIORES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GradientPillButton(
                    text = "Apostar $1.000",
                    onClick = onApostarClick,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                GradientPillButton(
                    text = "No apostar",
                    onClick = {
                        onNoApostarClick()
                        onNavigateToInfo()
                    },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

/**
 * Encabezado con el titulo centrado "Lunes" y foto de perfil alineada a la derecha.
 */
@Composable
private fun HeaderSection(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        // Texto Centrado "Lunes"
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        // Imagen de perfil circular a la derecha
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(50.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(BordeCyan, BordeMagenta)
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(Color(0xFF2A2038)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Foto de perfil",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Tarjeta de mensaje estilo chat con bordes de gradiente (magenta a verde),
 * fondo translúcido, icono tilde verde y texto exacto.
 */
@Composable
private fun ChatMessageCard(
    playerName: String,
    modifier: Modifier = Modifier
) {
    val borderGradient = Brush.horizontalGradient(
        colors = listOf(BordeMagenta, VerdeExito)
    )

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .border(width = 1.5.dp, brush = borderGradient, shape = shape)
            .clip(shape)
            .background(BurbujaChat.copy(alpha = 0.85f))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icono pequeño de tilde/check verde en la esquina superior derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Mensaje entregado",
                    tint = VerdeExito,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Texto exacto según requerimiento
            Text(
                text = "Hola $playerName! Querés ganar hoy mismo $2.000.- ? Solo tenés que pagar $ 1.000.- ahora, en menos de 10 segudos!!! Te espero !!!",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * Barra de progreso lineal redondeada dividida en dos partes: Verde a la izquierda y Roja a la derecha.
 */
@Composable
private fun SplitProgressBar(
    modifier: Modifier = Modifier,
    greenRatio: Float = 0.78f,
    redRatio: Float = 0.22f
) {
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .height(14.dp)
            .clip(shape)
            .background(Color(0xFF1E1E1E))
    ) {
        // Seccion Verde (Izquierda)
        Box(
            modifier = Modifier
                .weight(greenRatio)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(VerdeExito)
        )

        Spacer(modifier = Modifier.width(3.dp))

        // Seccion Roja (Derecha)
        Box(
            modifier = Modifier
                .weight(redRatio)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(RojoAlerta)
        )
    }
}

/**
 * Boton redondeado (pill-shaped) con borde de gradiente (magenta a cyan/verde) y fondo translucido.
 */
@Composable
private fun GradientPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    val borderBrush = Brush.horizontalGradient(
        colors = listOf(BordeMagenta, BordeCyan)
    )

    Box(
        modifier = modifier
            .height(54.dp)
            .border(width = 1.5.dp, brush = borderBrush, shape = shape)
            .clip(shape)
            .background(Color(0x33121212))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun FirstDecisionScreenPreview() {
    UnaDecisionMasTheme {
        FirstDecisionScreen(
            playerName = "Mateo"
        )
    }
}
