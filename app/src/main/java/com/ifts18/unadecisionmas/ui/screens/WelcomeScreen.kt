package com.ifts18.unadecisionmas.ui.screens

import android.media.AudioAttributes
import android.media.SoundPool
import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.R
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
import com.ifts18.unadecisionmas.ui.theme.JuegoLimpioTheme
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
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

    val context = LocalContext.current
    val view = LocalView.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current

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
                soundPool?.release()
            } catch (_: Exception) {}
        }
    }

    val submitName: () -> Unit = {
        playClickFeedback()
        keyboardController?.hide()
        val finalName = playerName.trim().ifEmpty { "Mateo" }
        onNavigateToDecision(finalName)
    }

    // Al abrir el teclado o enfocar el input, asegurar que todo el bloque inferior sea visible
    LaunchedEffect(isFocused) {
        if (isFocused) {
            keyboardController?.show()
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
                // Brillo ambiental superior derecho (Cian / Esmeralda) suave sin bordes duros
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

                // Brillo ambiental inferior izquierdo (Violeta / Magenta) suave sin bordes duros
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
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

                // 2. Seccion "Ingresa tu nombre" con TextField Material minimalista
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

                    // TextField de Material Design minimalista con linea inferior continua y fondo transparente
                    TextField(
                        value = playerName,
                        onValueChange = { input ->
                            if (input.length <= 25 && !input.contains("\n")) {
                                playerName = input
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        placeholder = {
                            Text(
                                text = "Ej: Marcos",
                                style = TextStyle(
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 22.sp,
                                    color = TextoSecundario.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color(0x66FFFFFF),
                            focusedPlaceholderColor = TextoSecundario.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = TextoSecundario.copy(alpha = 0.6f)
                        ),
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
                            .onFocusChanged { isFocused = it.isFocused }
                            .width(260.dp)
                    )
                }

                // 3. Boton "Siguiente" centrado y ubicado por encima del teclado
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
    JuegoLimpioTheme {
        WelcomeScreen(onNavigateToDecision = {})
    }
}