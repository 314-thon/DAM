package com.ifts18.unadecisionmas.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.data.model.AuthProviderType
import com.ifts18.unadecisionmas.data.model.AuthUiState
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito

/**
 * Modal de autenticación con privacidad garantizada y apodo obligatorio.
 * Soporta: Invitado con Apodo (Anonymous), Email/Contraseña y Google Sign-In.
 */
@Composable
fun AuthOnboardingModal(
    authState: AuthUiState,
    avatarResIds: List<Int>,
    onSignInAnonymous: (nickname: String, avatarResId: Int) -> Unit,
    onSignInEmail: (email: String, password: String, isRegister: Boolean) -> Unit,
    onSignInGoogle: () -> Unit,
    onCompleteNickname: (nickname: String, avatarResId: Int) -> Unit,
    onSignOut: () -> Unit,
    onClearError: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Invitado, 1 = Email
    var nicknameDraft by remember { mutableStateOf("") }
    var selectedAvatarResId by remember {
        mutableIntStateOf(avatarResIds.firstOrNull() ?: R.drawable.avatar_1)
    }
    var emailDraft by remember { mutableStateOf("") }
    var passwordDraft by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9020617))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C1626))
                .border(
                    width = 1.8.dp,
                    brush = Brush.horizontalGradient(listOf(BordeCyan, BordeMagenta)),
                    shape = RoundedCornerShape(24.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BordeCyan.copy(alpha = 0.14f))
                    .border(1.2.dp, BordeCyan.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = BordeCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (authState.isAuthenticated && authState.needsNicknameSetup) {
                // Paso obligatorio de Apodo + Avatar (nunca prellenado con datos reales de Google)
                Text(
                    text = "Elegí tu Apodo de Jugador",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Por tu privacidad, nunca mostramos tu nombre real ni tu correo. Elegí un apodo y avatar para jugar y entrar al ranking.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                NicknameAndAvatarInputs(
                    nickname = nicknameDraft,
                    onNicknameChange = {
                        onClearError()
                        nicknameDraft = it
                    },
                    avatarResIds = avatarResIds,
                    selectedAvatarResId = selectedAvatarResId,
                    onAvatarSelected = { selectedAvatarResId = it }
                )

                ErrorBannerIfAny(authState.errorMessage)

                Spacer(modifier = Modifier.height(14.dp))

                GradientActionButton(
                    text = "Confirmar apodo y jugar",
                    isLoading = authState.isLoading,
                    enabled = !authState.isLoading,
                    onClick = { onCompleteNickname(nicknameDraft, selectedAvatarResId) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Cambiar de cuenta",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = TextoSecundario,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !authState.isLoading) { onSignOut() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            } else {
                // Pantalla de Login con 3 vías (Invitado con Apodo, Google, Email/Contraseña)
                Text(
                    text = "Ingresá a Juego Limpio",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Elegí cómo acceder. Tus puntajes usan únicamente el apodo que elijas.",
                    fontFamily = SpaceGrotesk,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de pestaña: Rápido con Apodo vs Email
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131D31))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AuthModeTab(
                        label = "Rápido (Apodo)",
                        selected = selectedTab == 0,
                        onClick = {
                            onClearError()
                            selectedTab = 0
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AuthModeTab(
                        label = "Email",
                        selected = selectedTab == 1,
                        onClick = {
                            onClearError()
                            selectedTab = 1
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    NicknameAndAvatarInputs(
                        nickname = nicknameDraft,
                        onNicknameChange = {
                            onClearError()
                            nicknameDraft = it
                        },
                        avatarResIds = avatarResIds,
                        selectedAvatarResId = selectedAvatarResId,
                        onAvatarSelected = { selectedAvatarResId = it }
                    )

                    ErrorBannerIfAny(authState.errorMessage)

                    Spacer(modifier = Modifier.height(14.dp))

                    GradientActionButton(
                        text = "Entrar rápido con Apodo",
                        isLoading = authState.loadingProvider == AuthProviderType.ANONYMOUS,
                        enabled = !authState.isLoading,
                        onClick = { onSignInAnonymous(nicknameDraft, selectedAvatarResId) }
                    )
                } else {
                    StyledDarkInputField(
                        value = emailDraft,
                        onValueChange = {
                            onClearError()
                            emailDraft = it
                        },
                        placeholder = "Correo electrónico",
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    StyledDarkInputField(
                        value = passwordDraft,
                        onValueChange = {
                            onClearError()
                            passwordDraft = it
                        },
                        placeholder = "Contraseña (mín. 6 caracteres)",
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRegisterMode) "¿Ya tenés cuenta? Iniciá sesión"
                            else "¿Primera vez con email? Crear cuenta",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = BordeCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onClearError()
                                    isRegisterMode = !isRegisterMode
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    ErrorBannerIfAny(authState.errorMessage)

                    Spacer(modifier = Modifier.height(10.dp))

                    GradientActionButton(
                        text = if (isRegisterMode) "Registrarme con Email" else "Ingresar con Email",
                        isLoading = authState.loadingProvider == AuthProviderType.EMAIL,
                        enabled = !authState.isLoading,
                        onClick = { onSignInEmail(emailDraft, passwordDraft, isRegisterMode) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Text(
                        text = "  o  ",
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.5.sp,
                        color = TextoSecundario
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opción Google Sign-In
                val isGoogleLoading = authState.loadingProvider == AuthProviderType.GOOGLE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF152238))
                        .border(1.2.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(13.dp))
                        .clickable(enabled = !authState.isLoading) {
                            onClearError()
                            onSignInGoogle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            color = BordeCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continuar con Google",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal de gestión de cuenta desde el HUD superior de WelcomeScreen.
 */
@Composable
fun AccountStatusModal(
    playerName: String,
    authState: AuthUiState,
    onEditNickname: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val providerLabel = when (authState.providerType) {
        AuthProviderType.ANONYMOUS -> "Invitado rápido (sesión anónima)"
        AuthProviderType.EMAIL -> "Email y contraseña (apodo privado)"
        AuthProviderType.GOOGLE -> "Google Sign-In (identidad real protegida)"
        AuthProviderType.NONE -> "Sin sesión activa"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBF020617))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0C1626))
                .border(1.5.dp, BordeCyan.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cuenta y Privacidad",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (authState.providerType) {
                        AuthProviderType.EMAIL -> Icons.Default.Email
                        AuthProviderType.GOOGLE -> Icons.Default.AccountCircle
                        else -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = BordeCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Apodo público: $playerName",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = providerLabel,
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.5.sp,
                        color = TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131D31))
                    .border(1.dp, BordeCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onEditNickname()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Editar apodo",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = BordeCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RojoAlerta.copy(alpha = 0.14f))
                    .border(1.dp, RojoAlerta.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onSignOut()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = RojoAlerta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cerrar sesión / Cambiar cuenta",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RojoAlerta
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(BordeCyan, BordeMagenta))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = if (selected) Color(0xFF041019) else TextoSecundario
        )
    }
}

@Composable
private fun NicknameAndAvatarInputs(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    avatarResIds: List<Int>,
    selectedAvatarResId: Int,
    onAvatarSelected: (Int) -> Unit
) {
    TextField(
        value = nickname,
        onValueChange = { input ->
            if (input.length <= 22 && !input.contains("\n")) {
                onNicknameChange(input)
            }
        },
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        ),
        placeholder = {
            Text(
                text = "Tu apodo (ej: RayoBA)",
                style = TextStyle(
                    fontFamily = SpaceGrotesk,
                    fontSize = 14.sp,
                    color = TextoSecundario.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF142033),
            unfocusedContainerColor = Color(0xFF142033),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = BordeCyan,
            focusedIndicatorColor = BordeCyan,
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.22f)
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Elegí tu avatar",
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = TextoSecundario
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        avatarResIds.forEach { resId ->
            val isSelected = resId == selectedAvatarResId
            Box(
                modifier = Modifier
                    .size(if (isSelected) 52.dp else 44.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.2.dp,
                        color = if (isSelected) BordeCyan else Color.White.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarSelected(resId) }
            ) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun StyledDarkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = TextStyle(
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 14.5.sp,
            color = Color.White
        ),
        placeholder = {
            Text(
                text = placeholder,
                fontFamily = SpaceGrotesk,
                fontSize = 13.5.sp,
                color = TextoSecundario.copy(alpha = 0.65f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (isPassword) Icons.Default.Lock else Icons.Default.Email,
                contentDescription = null,
                tint = BordeCyan.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF142033),
            unfocusedContainerColor = Color(0xFF142033),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = BordeCyan,
            focusedIndicatorColor = BordeCyan,
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.22f)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun ErrorBannerIfAny(errorMessage: String?) {
    if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RojoAlerta.copy(alpha = 0.14f))
                .border(1.dp, RojoAlerta.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = errorMessage,
                fontFamily = SpaceGrotesk,
                fontSize = 12.sp,
                color = Color(0xFFFCA5A5),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.horizontalGradient(listOf(VerdeExito, BordeCyan)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color(0xFF041019),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = text,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF041019)
            )
        }
    }
}
