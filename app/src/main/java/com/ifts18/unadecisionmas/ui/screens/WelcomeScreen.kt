package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
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
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.UnaDecisionMasTheme
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onNavigateToDecision: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var playerName by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val maxCharacters = 10

    // Animacion sutil de parpadeo del cursor en el slot activo
    val infiniteTransition = rememberInfiniteTransition(label = "cursorAnimation")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val submitName: () -> Unit = {
        keyboardController?.hide()
        val finalName = playerName.trim().ifEmpty { "Mateo" }
        onNavigateToDecision(finalName)
    }

    // Asegurar que al abrir el teclado el input y boton se mantengan visibles
    LaunchedEffect(isFocused, playerName) {
        if (isFocused) {
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FondoOscuroInicio)
            .drawBehind {
                // Brillo ambiental superior derecho (Cian / Esmeralda) sin bordes cortados
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowCyan.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.90f, size.height * 0.06f),
                        radius = size.maxDimension * 0.75f
                    )
                )

                // Brillo ambiental inferior izquierdo (Violeta / Magenta) sin bordes cortados
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlowVioleta.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.08f, size.height * 0.65f),
                        radius = size.maxDimension * 0.80f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val contentMinHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = contentMinHeight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Titulo principal "Juego Limpio" centrado con tipografia Space Grotesk
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Juego",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 68.sp,
                            lineHeight = 70.sp,
                            letterSpacing = (-1.5).sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    GradienteJuegoInicio,
                                    GradienteJuegoMedio,
                                    GradienteJuegoFin
                                )
                            )
                        ),
                        modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = "Limpio",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 68.sp,
                            lineHeight = 70.sp,
                            letterSpacing = (-1.5).sp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    GradienteLimpioInicio,
                                    GradienteLimpioMedio,
                                    GradienteLimpioFin
                                )
                            )
                        ),
                        modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }

                // 2. Seccion central "Ingresa tu nombre" con guiones referenciales
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ingresá tu\nnombre",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Input con 10 slots individuales y guiones bajos referenciales
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                    ) {
                        BasicTextField(
                            value = playerName,
                            onValueChange = { input ->
                                if (input.length <= maxCharacters && !input.contains("\n")) {
                                    playerName = input
                                }
                            },
                            singleLine = true,
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
                                .onFocusChanged { isFocused = it.isFocused },
                            decorationBox = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until maxCharacters) {
                                        val hasChar = i < playerName.length
                                        val char = if (hasChar) playerName[i].toString() else ""
                                        val isCurrentCursor = i == playerName.length && isFocused

                                        Column(
                                            modifier = Modifier.width(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            // Caracter o cursor parpadeante
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(34.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (hasChar) {
                                                    Text(
                                                        text = char,
                                                        style = TextStyle(
                                                            fontFamily = SpaceGrotesk,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 22.sp,
                                                            color = Color.White,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    )
                                                } else if (isCurrentCursor) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height(22.dp)
                                                            .background(
                                                                Color.White.copy(alpha = cursorAlpha),
                                                                RoundedCornerShape(1.dp)
                                                            )
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Guion bajo referencial 100% nitido y visible
                                            Box(
                                                modifier = Modifier
                                                    .width(17.dp)
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
                        )
                    }
                }

                // 3. Boton "Siguiente" estilo Design System, ubicado por encima del teclado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(230.dp)
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
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
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