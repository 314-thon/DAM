package com.ifts18.unadecisionmas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito

@Composable
fun AchievementBadgeIcon(
    achievementId: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val (vector, accent) = if (!isUnlocked) {
        Icons.Default.Lock to TextoSecundario.copy(alpha = 0.7f)
    } else {
        when (achievementId) {
            AchievementsCatalog.ID_INTOCABLE -> Icons.Default.Shield to VerdeExito
            AchievementsCatalog.ID_HONRADO_100 -> Icons.Default.VerifiedUser to VerdeExito
            AchievementsCatalog.ID_EVITANDO_TENTACION -> Icons.Default.Vibration to BordeCyan
            AchievementsCatalog.ID_OJO_ABIERTO -> Icons.Default.Visibility to BordeCyan
            AchievementsCatalog.ID_VIDA_REAL -> Icons.Default.SportsSoccer to VerdeExito
            AchievementsCatalog.ID_TODO_AL_ROJO -> Icons.Default.PhonelinkErase to BordeMagenta
            AchievementsCatalog.ID_SIXSEVEN -> Icons.Default.QrCode2 to BordeMagenta
            else -> Icons.Default.MilitaryTech to VerdeExito
        }
    }

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(accent.copy(alpha = if (isUnlocked) 0.16f else 0.08f))
            .border(1.dp, accent.copy(alpha = if (isUnlocked) 0.65f else 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(iconSize)
        )
    }
}
