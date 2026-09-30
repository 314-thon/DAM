package com.ifts18.unadecisionmas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral

@Composable
fun FirstDecisionScreen(
    playerName: String,
    onNavigateToInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FondoGeneral),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "DEUDA: $0",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Text(
                text = "Turno de: $playerName",
                style = MaterialTheme.typography.titleLarge,
                color = BordeCyan
            )

            Button(
                onClick = onNavigateToInfo
            ) {
                Text("Ver canales de asistencia")
            }
        }
    }
}