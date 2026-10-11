package com.ifts18.unadecisionmas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.RojoAlerta

@Composable
fun SoundToggleButton(
    modifier: Modifier = Modifier
) {
    val isMuted by SoundManager.isMuted

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF1B1E28).copy(alpha = 0.90f))
            .border(
                width = 1.dp,
                color = if (isMuted) RojoAlerta.copy(alpha = 0.40f) else BordeCyan.copy(alpha = 0.55f),
                shape = CircleShape
            )
            .clickable {
                SoundManager.toggleMute()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                id = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            ),
            contentDescription = if (isMuted) "Activar sonido" else "Silenciar sonido",
            tint = if (isMuted) RojoAlerta else BordeCyan,
            modifier = Modifier.size(20.dp)
        )
    }
}
