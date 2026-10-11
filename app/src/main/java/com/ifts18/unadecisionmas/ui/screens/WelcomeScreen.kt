package com.ifts18.unadecisionmas.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import com.ifts18.unadecisionmas.audio.SoundManager
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
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onNavigateToDecision: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var playerName by remember { mutableStateOf("") }
    var selectedAvatarResId by remember { mutableIntStateOf(R.drawable.avatar_1) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val avatarsList = remember {
        listOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4
        )
    }

    val playClickFeedback: () -> Unit = {
        try {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}
        SoundManager.playClick()
    }

    val submitName: () -> Unit = {
        playClickFeedback()
        keyboardController?.hide()
        val finalName = playerName.trim().ifEmpty { "Mateo" }
        onNavigateToDecision(finalName, selectedAvatarResId)
    }

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
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Título principal "Juego Limpio"
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
                            fontSize = 64.sp,
                            lineHeight = 66.sp,
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
                            fontSize = 64.sp,
                            lineHeight = 66.sp,
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

                // 2. Selector de Avatar (Las 4 fotos de perfil)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Elegí tu avatar",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatarsList.forEach { avatarRes ->
                            val isSelected = selectedAvatarResId == avatarRes
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        brush = if (isSelected) {
                                            Brush.linearGradient(listOf(BordeCyan, BordeMagenta))
                                        } else {
                                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.15f)))
                                        },
                                        shape = CircleShape
                                    )
                                    .background(if (isSelected) BordeCyan.copy(alpha = 0.2f) else Color(0xFF1E1B26))
                                    .clickable {
                                        playClickFeedback()
                                        selectedAvatarResId = avatarRes
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = avatarRes),
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(if (isSelected) 58.dp else 52.dp).clip(CircleShape)
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(VerdeExito),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Campo de texto para ingresar nombre
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ingresá tu apodo",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                            fontSize = 22.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        placeholder = {
                            Text(
                                text = "Ej: Mateo",
                                style = TextStyle(
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 20.sp,
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

                // 4. Botón "Comenzar Semana"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
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
                            text = "Comenzar Semana",
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
        WelcomeScreen(onNavigateToDecision = { _, _ -> })
    }
}