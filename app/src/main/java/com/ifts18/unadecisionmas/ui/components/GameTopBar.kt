package com.ifts18.unadecisionmas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito

@Composable
fun GameTopBar(
    gameState: GameState,
    currentDayName: String,
    modifier: Modifier = Modifier
) {
    val totalDays = 7
    val isDebtZero = gameState.currentDebt == 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161922).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Fila 1: Avatar + Nombre | Saldo / Deuda
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Perfil del jugador
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(BordeCyan, BordeMagenta)),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(Color(0xFF24142D)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = gameState.profile.avatarResId),
                        contentDescription = "Avatar de ${gameState.profile.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = gameState.profile.name,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$currentDayName (${gameState.currentDayIndex + 1}/$totalDays)",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = BordeCyan
                    )
                }
            }

            // Indicador de Deuda en tiempo real
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDebtZero) VerdeExito.copy(alpha = 0.18f)
                        else RojoAlerta.copy(alpha = 0.22f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDebtZero) VerdeExito else RojoAlerta,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DEUDA",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = if (isDebtZero) VerdeExito else RojoAlerta
                    )
                    Text(
                        text = "$${gameState.currentDebt}",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fila 2: Barra visual de registro de decisiones tomadas (Lunes a Domingo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val daysLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
            daysLabels.forEachIndexed { index, label ->
                val decision = gameState.decisionHistory.getOrNull(index)
                val isCurrent = index == gameState.currentDayIndex

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    val dotColor = when {
                        decision != null && decision.isSafe -> VerdeExito
                        decision != null && !decision.isSafe -> RojoAlerta
                        isCurrent -> BordeCyan
                        else -> Color.White.copy(alpha = 0.20f)
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .then(
                                if (isCurrent) {
                                    Modifier.border(1.5.dp, Color.White, CircleShape)
                                } else Modifier
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        fontFamily = SpaceGrotesk,
                        fontSize = 10.5.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) Color.White else TextoSecundario.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
