package com.ifts18.unadecisionmas.data.model

import androidx.annotation.DrawableRes
import com.ifts18.unadecisionmas.R

/**
 * Perfil del jugador con nombre y avatar seleccionado.
 */
data class PlayerProfile(
    val name: String = "Mateo",
    @DrawableRes val avatarResId: Int = R.drawable.avatar_1
)

/**
 * Mensaje individual que aparece simulando chat de mensajería.
 */
data class ChatMessage(
    val sender: String,
    val message: String = "",
    val delayAfterMillis: Long = 2000L,
    @DrawableRes val imageResId: Int? = null,
    val isForwarded: Boolean = false
)

/**
 * Opción de decisión para el jugador.
 */
data class DecisionOption(
    val text: String,
    val isSafe: Boolean,
    val penaltyAmount: Int = 0,
    val educationalFeedback: String
)

/**
 * Escenario narrativo correspondiente a un día de la semana.
 */
data class GameScenario(
    val dayIndex: Int,
    val dayName: String,
    val title: String,
    val messages: List<ChatMessage>,
    val safeOption: DecisionOption,
    val riskyOption: DecisionOption,
    val timeoutPenalty: Int = 1000,
    val pedagogicalFocus: String
)

/**
 * Registro de una decisión tomada para las estadísticas finales.
 */
data class DecisionRecord(
    val dayName: String,
    val chosenOptionText: String,
    val isSafe: Boolean,
    val addedDebt: Int,
    val feedback: String
)

/**
 * Estado general inmutable del juego para la arquitectura MVVM.
 */
data class GameState(
    val profile: PlayerProfile = PlayerProfile(),
    val currentDayIndex: Int = 0,
    val currentDebt: Int = 0,
    val correctDecisionsCount: Int = 0,
    val incorrectDecisionsCount: Int = 0,
    val decisionHistory: List<DecisionRecord> = emptyList(),
    val isGameOver: Boolean = false,
    val isPlayingSound: Boolean = true
)
