package com.ifts18.unadecisionmas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import kotlin.math.abs

private fun formatModalPesos(amount: Int): String {
    val formatted = "%,d".format(abs(amount)).replace(',', '.')
    return if (amount < 0) "-$$formatted" else "$$formatted"
}

/**
 * Modal de cajero / pasarela de carga con descuento de comisiones sobre el monto transferido.
 */
@Composable
fun CashierDepositModal(
    visible: Boolean,
    currentBalance: Int,
    grossDepositAmount: Int = 5_000,
    cashierCommission: Int = 750,
    processingFee: Int = 250,
    onConfirmDeposit: (totalPaid: Int, creditedAmount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val creditedAmount = (grossDepositAmount - cashierCommission - processingFee).coerceAtLeast(0)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.94f),
        exit = fadeOut(tween(160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    SoundManager.playClick()
                    onDismiss()
                }
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141824))
                    .border(
                        width = 1.8.dp,
                        brush = Brush.horizontalGradient(listOf(Color(0xFFFFB300), RojoAlerta)),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cajero • Cargar Saldo",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tu saldo actual (${formatModalPesos(currentBalance)}) es insuficiente para seguir jugando.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C1018))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    DepositBreakdownRow(
                        label = "Monto a transferir:",
                        amount = formatModalPesos(grossDepositAmount),
                        amountColor = Color.White
                    )
                    DepositBreakdownRow(
                        label = "Comisión de cajero (15%):",
                        amount = "-${formatModalPesos(cashierCommission)}",
                        amountColor = Color(0xFFFFB74D)
                    )
                    DepositBreakdownRow(
                        label = "Cargo operativo (5%):",
                        amount = "-${formatModalPesos(processingFee)}",
                        amountColor = Color(0xFFFFB74D)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

                    DepositBreakdownRow(
                        label = "Saldo acreditado en sala:",
                        amount = formatModalPesos(creditedAmount),
                        amountColor = Color(0xFFFFD54F),
                        bold = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BordeCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Las comisiones se descuentan automáticamente del saldo acreditado.",
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFB45309), RojoAlerta)))
                        .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .clickable {
                            SoundManager.playClick()
                            onConfirmDeposit(grossDepositAmount, creditedAmount)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Confirmar carga (${formatModalPesos(grossDepositAmount)})",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable {
                            SoundManager.playClick()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancelar",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DepositBreakdownRow(
    label: String,
    amount: String,
    amountColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = SpaceGrotesk,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (bold) 13.sp else 11.5.sp,
            color = if (bold) Color.White else Color.White.copy(alpha = 0.82f)
        )
        Text(
            text = amount,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = if (bold) 14.sp else 12.sp,
            color = amountColor
        )
    }
}
