package com.ifts18.unadecisionmas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BordeMagenta,
    secondary = BordeCyan,
    tertiary = AcentoVioleta,
    background = FondoGeneral,
    surface = BurbujaChat,
    onPrimary = TextoPrincipal,
    onSecondary = FondoGeneral,
    onTertiary = TextoPrincipal,
    onBackground = TextoPrincipal,
    onSurface = TextoPrincipal,
    error = RojoAlerta,
    onError = TextoPrincipal
)

@Composable
fun UnaDecisionMasTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
