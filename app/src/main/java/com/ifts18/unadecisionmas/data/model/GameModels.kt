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
    val delayAfterMillis: Long = 1600L,
    @DrawableRes val imageResId: Int? = null,
    val isForwarded: Boolean = false
)

/**
 * Opción de decisión para el jugador.
 * Incluye impacto en deuda, ahorro, puntos de bienestar y logros desbloqueables.
 */
data class DecisionOption(
    val text: String,
    val isSafe: Boolean,
    val penaltyAmount: Int = 0,
    val savingsDelta: Int = 0,
    val wellbeingDelta: Int = 0,
    val achievementId: String? = null,
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
    val alternativeOption: DecisionOption? = null,
    val riskyOption: DecisionOption,
    val timeoutPenalty: Int = 2000,
    val decisionTimeSeconds: Int = 25,
    val triggersSpamShakeAlert: Boolean = false,
    val pedagogicalFocus: String
) {
    fun getAllOptions(): List<DecisionOption> {
        return listOfNotNull(safeOption, alternativeOption, riskyOption)
    }
}

/**
 * Registro de una decisión tomada para las estadísticas finales y Room.
 */
data class DecisionRecord(
    val dayName: String,
    val chosenOptionText: String,
    val isSafe: Boolean,
    val addedDebt: Int,
    val savingsDelta: Int = 0,
    val wellbeingDelta: Int = 0,
    val responseTimeMillis: Long = 0L,
    val feedback: String
)

/**
 * Definición de un logro o medalla del sistema.
 */
data class AchievementDef(
    val id: String,
    val codeNumber: String,
    val title: String,
    val description: String,
    val bonusPoints: Int,
    val isSecret: Boolean = false
)

object AchievementsCatalog {
    const val ID_INTOCABLE = "INTOCABLE"
    const val ID_HONRADO_100 = "HONRADO_100"
    const val ID_EVITANDO_TENTACION = "EVITANDO_TENTACION"
    const val ID_OJO_ABIERTO = "OJO_ABIERTO"
    const val ID_VIDA_REAL = "VIDA_REAL"
    const val ID_TODO_AL_ROJO = "TODO_AL_ROJO"
    const val ID_SIXSEVEN = "SIXSEVEN"

    const val QR_EASTER_EGG_PAYLOAD = "JUEGOLIMPIO://ACHIEVEMENT/SIXSEVEN_67"

    val all: List<AchievementDef> = listOf(
        AchievementDef(
            id = ID_INTOCABLE,
            codeNumber = "#01",
            title = "Intocable ($0 deuda)",
            description = "Terminaste la semana sin contraer deuda en apuestas.",
            bonusPoints = 1500
        ),
        AchievementDef(
            id = ID_HONRADO_100,
            codeNumber = "#02",
            title = "100% Honrado",
            description = "Devolviste los $80.000 de mamá sin tocar el casino.",
            bonusPoints = 1200
        ),
        AchievementDef(
            id = ID_EVITANDO_TENTACION,
            codeNumber = "#03",
            title = "Sin caer en el scroll",
            description = "Sacudiste el teléfono para frenar el bombardeo de anuncios.",
            bonusPoints = 1300
        ),
        AchievementDef(
            id = ID_OJO_ABIERTO,
            codeNumber = "#04",
            title = "Ojo abierto",
            description = "Desarmaste el truco matemático antes de que la casa se quede con todo.",
            bonusPoints = 1200
        ),
        AchievementDef(
            id = ID_VIDA_REAL,
            codeNumber = "#05",
            title = "Canchita y amigos",
            description = "Elegiste planes reales con amigos por encima de billeteras de apuestas.",
            bonusPoints = 1000
        ),
        AchievementDef(
            id = ID_TODO_AL_ROJO,
            codeNumber = "#06",
            title = "Sin celu hasta el lunes",
            description = "Saltó la ficha o cerraste con deuda y te quedaste sin teléfono el finde.",
            bonusPoints = 400,
            isSecret = false
        ),
        AchievementDef(
            id = ID_SIXSEVEN,
            codeNumber = "#67",
            title = "SixSeven",
            description = "Medalla secreta desbloqueada al escanear el QR oculto de otro jugador.",
            bonusPoints = 2500,
            isSecret = true
        )
    )

    fun findById(id: String): AchievementDef? = all.find { it.id == id }
}

/**
 * Símbolos vectoriales de los rodillos del tragamonedas (sin emojis).
 */
enum class SlotSymbol(val label: String) {
    SEVEN("777"),
    COIN("Moneda"),
    BAR("BAR"),
    DIAMOND("Diamante"),
    BELL("Campana"),
    CHERRY("Cereza"),
    GRAPES("Uvas"),
    LEMON("Limón")
}

/**
 * Estado persistente del minijuego Tragamonedas (Juego 3).
 */
data class SlotsGameState(
    val reel1: SlotSymbol = SlotSymbol.SEVEN,
    val reel2: SlotSymbol = SlotSymbol.DIAMOND,
    val reel3: SlotSymbol = SlotSymbol.CHERRY,
    val spinsUsed: Int = 0,
    val maxSpins: Int = 10,
    val bonusChips: Int = 10,
    val fakeLockedWinnings: Int = 0,
    val realMoneyLost: Int = 0,
    val elapsedMillis: Long = 0L,
    val algorithmStageTitle: String = "Tenés 10 fichas de bono",
    val algorithmExplanation: String = "Agitá el celular o tocá Girar para probar la máquina.",
    val spinStatusBanner: String = "3 iguales suman saldo de bono"
)

/**
 * Estado persistente del minijuego La Ruleta del 29 (Juego 4).
 */
data class RouletteGameState(
    val selectedNumberIndex: Int = 30,
    val selectedBetMode: String = "PLENO",
    val landedPocketIndex: Int = 30,
    val spinsUsed: Int = 0,
    val maxSpins: Int = 10,
    val bonusChips: Int = 10,
    val fakeLockedWinnings: Int = 0,
    val realMoneyLost: Int = 0,
    val elapsedMillis: Long = 0L,
    val resultBannerText: String = "Elegí número o color y agitá para girar",
    val algorithmStageTitle: String = "10 fichas de bono activas",
    val algorithmExplanation: String = "Probá el paño para ver qué pasa en cada tirada."
)

/**
 * Entrada del Ranking Global (Firebase + caché local Room).
 */
data class RankingEntry(
    val id: String = "",
    val playerName: String = "Mateo",
    val avatarIndex: Int = 1,
    val score: Int = 0,
    val debt: Int = 0,
    val savings: Int = 0,
    val wellbeingPoints: Int = 0,
    val totalTimeMillis: Long = 0L,
    val achievementsCount: Int = 0,
    val hasSixSeven: Boolean = false,
    val gameMode: String = "La Semana",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)

/**
 * Estado general inmutable del juego para la arquitectura MVVM.
 */
data class GameState(
    val profile: PlayerProfile = PlayerProfile(),
    val currentDayIndex: Int = 0,
    val currentDebt: Int = 0,
    val currentSavings: Int = 15000,
    val wellbeingPoints: Int = 20,
    val correctDecisionsCount: Int = 0,
    val incorrectDecisionsCount: Int = 0,
    val totalDecisionTimeMillis: Long = 0L,
    val decisionHistory: List<DecisionRecord> = emptyList(),
    val unlockedAchievementIds: Set<String> = emptySet(),
    val sessionAchievementIds: Set<String> = emptySet(),
    val newlyUnlockedAchievement: AchievementDef? = null,
    val hasSavedGameInRoom: Boolean = false,
    val savedGameDayName: String = "",
    val lastCompletedMode: String = "La Semana",
    val lastModeSummary: String = "",
    val lastEarnedScore: Int = 0,
    val betMomMoneyOnTuesday: Boolean = false,
    val isPunishedNoPhone: Boolean = false,
    val punishmentReasonTitle: String = "",
    val punishmentReasonBody: String = "",
    val isGameOver: Boolean = false,
    val isPlayingSound: Boolean = true,
    val bestScoresByMode: Map<String, Int> = emptyMap(),
    val isTutorialCompleted: Boolean = true,
    val hasPlayedIntroSplash: Boolean = false
) {
    /**
     * Calcula el puntaje competitivo combinando ahorro conservado, ausencia de deuda,
     * puntos de bienestar, logros y bonus por velocidad de respuesta (evita empates).
     */
    fun calculateCompetitiveScore(): Int {
        val netMoneyScore = ((currentSavings - currentDebt) / 5).coerceAtLeast(-5000)
        val wellbeingBonus = wellbeingPoints * 120
        val accuracyBonus = correctDecisionsCount * 600
        val achievementsBonus = sessionAchievementIds.sumOf { id ->
            AchievementsCatalog.findById(id)?.bonusPoints ?: 400
        }
        // Bonus de velocidad: máximo 175 segundos (7 días * 25s). Cada segundo ahorrado suma 18 pts.
        val maxTimeMillis = 175_000L
        val savedMillis = (maxTimeMillis - totalDecisionTimeMillis).coerceAtLeast(0L)
        val speedBonus = (savedMillis / 100L).toInt() * 2

        return (netMoneyScore + wellbeingBonus + accuracyBonus + achievementsBonus + speedBonus).coerceAtLeast(100)
    }
}
