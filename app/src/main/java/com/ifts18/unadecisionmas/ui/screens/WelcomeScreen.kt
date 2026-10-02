package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.BurbujaChat
import com.ifts18.unadecisionmas.ui.theme.FondoOscuroInicio
import com.ifts18.unadecisionmas.ui.theme.GlowCyan
import com.ifts18.unadecisionmas.ui.theme.GlowVioleta
import com.ifts18.unadecisionmas.ui.theme.GradienteJuegoFin
import com.ifts18.unadecisionmas.ui.theme.GradienteJuegoInicio
import com.ifts18.unadecisionmas.ui.theme.GradienteJuegoMedio
import com.ifts18.unadecisionmas.ui.theme.GradienteLimpioFin
import com.ifts18.unadecisionmas.ui.theme.GradienteLimpioInicio
import com.ifts18.unadecisionmas.ui.theme.GradienteLimpioMedio
import com.ifts18.unadecisionmas.ui.theme.UnaDecisionMasTheme

@Composable
fun WelcomeScreen(
    onNavigateToDecision: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var playerName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val submitName: () -> Unit = {
        keyboardController?.hide()
        val finalName = playerName.trim().ifEmpty { "Mateo" }
        onNavigateToDecision(finalName)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FondoOscuroInicio)
            .drawBehind {
                // Brillo ambiental superior derecho (Cian / Esmeralda)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowCyan.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.95f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    )
                )

                // Brillo ambiental inferior izquierdo (Violeta / Magenta)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowVioleta.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.05f, size.height * 0.65f),
                        radius = size.width * 0.95f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Espaciado superior previo al titulo "Juego Limpio" (~22% de la pantalla)
            Spacer(modifier = Modifier.weight(0.9f))

            // Titulo principal "Juego Limpio" alineado a la izquierda
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 24.dp)
            ) {
                Text(
                    text = "Juego",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 70.sp,
                        lineHeight = 72.sp,
                        letterSpacing = (-1.5).sp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                GradienteJuegoInicio,
                                GradienteJuegoMedio,
                                GradienteJuegoFin
                            )
                        )
                    )
                )

                Text(
                    text = "Limpio",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 70.sp,
                        lineHeight = 72.sp,
                        letterSpacing = (-1.5).sp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                GradienteLimpioInicio,
                                GradienteLimpioMedio,
                                GradienteLimpioFin
                            )
                        )
                    )
                )
            }

            // Espaciado central entre el titulo y el ingreso de nombre
            Spacer(modifier = Modifier.weight(1.1f))

            // Seccion interactiva "Ingresa tu nombre"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ingresá tu\nnombre",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Campo de texto sobre las 10 lineas punteadas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Texto ingresado (aparece centrado sobre los guiones)
                        BasicTextField(
                            value = playerName,
                            onValueChange = { input ->
                                if (input.length <= 10) {
                                    playerName = input
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = 3.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitName() }
                            ),
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 10 guiones blancos continuos del diseno
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(10) {
                                Box(
                                    modifier = Modifier
                                        .width(15.dp)
                                        .height(3.5.dp)
                                        .background(
                                            color = Color.White,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Boton de accion (pildora neon del Design System)
            // Se muestra al escribir el nombre, manteniendo la vista inicial identica al mock
            AnimatedVisibility(
                visible = playerName.isNotBlank(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(52.dp)
                            .background(
                                color = BurbujaChat.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(9999.dp)
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(BordeMagenta, BordeCyan)
                                ),
                                shape = RoundedCornerShape(9999.dp)
                            )
                            .clickable { submitName() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Siguiente",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            // Espaciado inferior para zona ergonomica / thumb zone (~20% restante)
            Spacer(modifier = Modifier.weight(0.8f))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121118)
@Composable
fun WelcomeScreenPreview() {
    UnaDecisionMasTheme {
        WelcomeScreen(onNavigateToDecision = {})
    }
}