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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlin.math.abs

private val GoldAccentLight = Color(0xFFFFE58F)
private val GoldAccent = Color(0xFFE5B338)

private fun formatWithdrawPesos(amount: Int): String {
    val formatted = "%,d".format(abs(amount)).replace(',', '.')
    return if (amount < 0) "-$$formatted" else "$$formatted"
}

/**
 * Píldora superior de saldo que abre el modal de retiro al tocarla.
 */
@Composable
fun BalanceWithdrawPill(
    currentBalance: Int,
    withdrawalMinimum: Int,
    isLowBalance: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BordeMagenta.copy(alpha = 0.22f))
            .border(1.dp, BordeMagenta, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) {
                SoundManager.playClick()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = "Ver saldo y retiro",
            tint = if (isLowBalance) RojoAlerta else GoldAccentLight,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = formatWithdrawPesos(currentBalance),
            style = TextStyle(
                fontFamily = SpaceGrotesk,
                fontSize = 12.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            color = if (isLowBalance) RojoAlerta else Color.White,
            maxLines = 1,
            softWrap = false
        )
        if (currentBalance < withdrawalMinimum) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Retiro bloqueado",
                tint = BordeMagenta,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Modal de retiro que revela el mínimo requerido ($30.000) solo cuando el usuario toca su saldo.
 */
@Composable
fun WithdrawBonusModal(
    visible: Boolean,
    currentBalance: Int,
    withdrawalMinimum: Int,
    isBonusBalance: Boolean,
    onOpenDeposit: () -> Unit,
    onDismiss: () -> Unit
) {
    val canWithdraw = currentBalance >= withdrawalMinimum
    val missingAmount = (withdrawalMinimum - currentBalance).coerceAtLeast(0)
    val progress = (currentBalance.toFloat() / withdrawalMinimum.toFloat()).coerceIn(0f, 1f)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.94f),
        exit = fadeOut(tween(160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
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
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF141824))
                    .border(
                        width = 1.8.dp,
                        brush = Brush.horizontalGradient(listOf(BordeMagenta, GoldAccent)),
                        shape = RoundedCornerShape(22.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = GoldAccentLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Retirar Saldo",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isBonusBalance) BordeMagenta.copy(alpha = 0.18f)
                            else RojoAlerta.copy(alpha = 0.18f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isBonusBalance) BordeMagenta else RojoAlerta,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isBonusBalance) "SALDO DE BONO" else "SALDO REAL",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = if (isBonusBalance) BordeMagenta else RojoAlerta
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatWithdrawPesos(currentBalance),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = GoldAccentLight
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C1018))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso para retirar",
                            fontFamily = SpaceGrotesk,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Text(
                            text = "${formatWithdrawPesos(currentBalance)} / ${formatWithdrawPesos(withdrawalMinimum)}",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = BordeMagenta
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BordeMagenta,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (canWithdraw) VerdeExito else RojoAlerta,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (canWithdraw) {
                                "Alcanzaste el saldo mínimo para retirar."
                            } else {
                                "Saldo mínimo para retirar ${formatWithdrawPesos(withdrawalMinimum)}. Te faltan ${formatWithdrawPesos(missingAmount)}."
                            },
                            fontFamily = SpaceGrotesk,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (canWithdraw) VerdeExito.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (canWithdraw) VerdeExito else Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (canWithdraw) VerdeExito else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (canWithdraw) {
                                "Retirar ${formatWithdrawPesos(currentBalance)}"
                            } else {
                                "Retiro bloqueado (mín. ${formatWithdrawPesos(withdrawalMinimum)})"
                            },
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (canWithdraw) VerdeExito else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isBonusBalance) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(GoldAccent.copy(alpha = 0.18f))
                                .border(1.dp, GoldAccent, RoundedCornerShape(11.dp))
                                .clickable {
                                    SoundManager.playClick()
                                    onOpenDeposit()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cargar saldo",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = GoldAccentLight
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(11.dp))
                            .clickable {
                                SoundManager.playClick()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Volver",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
