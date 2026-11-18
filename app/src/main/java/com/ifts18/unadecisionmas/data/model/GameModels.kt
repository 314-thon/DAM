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
            title = "Ad evadido",
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

    val MAX_TOTAL_BONUS: Int = all.sumOf { it.bonusPoints }

    fun findById(id: String): AchievementDef? = all.find { it.id == id }

    /**
     * Suma el puntaje único de cada medalla válida desbloqueada (sin duplicados).
     */
    fun calculateTotalBonus(achievementIds: Collection<String>): Int {
        return achievementIds
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sumOf { id -> findById(id)?.bonusPoints ?: 0 }
    }

    /**
     * Parsea una lista CSV de IDs de medallas filtrando únicamente los existentes en el catálogo.
     */
    fun parseValidIds(rawCsv: String): Set<String> {
        if (rawCsv.isBlank()) return emptySet()
        return rawCsv.split(",")
            .asSequence()
            .map { it.trim() }
            .filter { findById(it) != null }
            .toSet()
    }
}

/**
 * Modos de juego canónicos cuyas mejores marcas se suman en el Ranking Global.
 */
object GameModes {
    const val LA_SEMANA = "La Semana"
    const val SCROLL_SIN_CAER = "Scroll sin Caer"
    const val TRAMPA_JACKPOT = "Trampa del Jackpot"
    const val LA_RULETA_29 = "La Ruleta del 29"

    val allModes: List<String> = listOf(
        LA_SEMANA,
        SCROLL_SIN_CAER,
        TRAMPA_JACKPOT,
        LA_RULETA_29
    )

    fun canonicalize(rawMode: String): String {
        val cleaned = rawMode.trim()
        return allModes.firstOrNull { it.equals(cleaned, ignoreCase = true) } ?: cleaned.ifBlank { LA_SEMANA }
    }

    fun slug(modeName: String): String {
        return canonicalize(modeName)
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "general" }
    }

    fun shortLabel(modeName: String): String = when (canonicalize(modeName)) {
        LA_SEMANA -> "Semana"
        SCROLL_SIN_CAER -> "Scroll"
        TRAMPA_JACKPOT -> "Slots"
        LA_RULETA_29 -> "Ruleta"
        else -> modeName
    }
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
    val algorithmStageTitle: String = "Bono de bienvenida activo",
    val algorithmExplanation: String = "Elegí tu apuesta y girá los rodillos.",
    val spinStatusBanner: String = "3 iguales suman saldo a tu cuenta"
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
    val algorithmStageTitle: String = "Bono de bienvenida activo",
    val algorithmExplanation: String = "Elegí tu ficha y probá tu suerte en el paño."
)

/**
 * Proveedores de autenticación soportados por Firebase Auth.
 */
enum class AuthProviderType {
    NONE,
    ANONYMOUS,
    EMAIL,
    GOOGLE
}

/**
 * Estado observable de autenticación y configuración obligatoria de apodo.
 */
data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val uid: String = "",
    val providerType: AuthProviderType = AuthProviderType.NONE,
    val needsNicknameSetup: Boolean = false,
    val loadingProvider: AuthProviderType? = null,
    val errorMessage: String? = null
) {
    val isLoading: Boolean get() = loadingProvider != null
}

/**
 * Entrada individual por modo del Ranking (Firebase `ranking_juego_limpio` + caché local Room).
 */
data class RankingEntry(
    val id: String = "",
    val uid: String = "",
    val playerName: String = "Mateo",
    val avatarIndex: Int = 1,
    val score: Int = 0,
    val debt: Int = 0,
    val savings: Int = 0,
    val wellbeingPoints: Int = 0,
    val totalTimeMillis: Long = 0L,
    val achievementsCount: Int = 0,
    val hasSixSeven: Boolean = false,
    val gameMode: String = GameModes.LA_SEMANA,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val scoreVersion: Int = CURRENT_SCORE_VERSION
) {
    companion object {
        const val CURRENT_SCORE_VERSION = 2
    }
}

/**
 * Medallas desbloqueadas por un jugador (Firebase `ranking_medallas/{uid}`),
 * sumadas por separado a la suma de sus mejores puntajes por modo.
 */
data class PlayerMedalsRecord(
    val uid: String = "",
    val playerName: String = "Jugador",
    val avatarIndex: Int = 1,
    val unlockedIds: Set<String> = emptySet(),
    val medalsBonus: Int = 0,
    val achievementsCount: Int = 0,
    val hasSixSeven: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Fila consolidada del Ranking Global:
 * totalScore = suma del mejor puntaje de cada modo + suma de bonusPoints de medallas únicas.
 */
data class PlayerRankingSummary(
    val uid: String = "",
    val playerName: String = "Jugador",
    val avatarIndex: Int = 1,
    val totalScore: Int = 0,
    val modesTotalScore: Int = 0,
    val medalsBonusScore: Int = 0,
    val bestScoreByMode: Map<String, Int> = emptyMap(),
    val modesPlayedCount: Int = 0,
    val unlockedMedalIds: Set<String> = emptySet(),
    val achievementsCount: Int = 0,
    val hasSixSeven: Boolean = false,
    val totalTimeMillis: Long = 0L,
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
    val lastCompletedMode: String = GameModes.LA_SEMANA,
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
     * Indica si el jugador contrajo deuda con algún cajero o app de préstamos durante la semana
     * (Jueves: préstamo exprés; Viernes: plata fiada del cajero), excluyendo vencimientos por tiempo.
     */
    fun hasCajeroDebt(): Boolean {
        return decisionHistory.any { record ->
            !record.isSafe &&
                !record.chosenOptionText.startsWith("Tiempo agotado") &&
                (record.dayName == "Jueves" || record.dayName == "Viernes")
        }
    }

    /**
     * Calcula el puntaje puro del modo "La Semana" (sin medallas, ya que las medallas
     * se suman por separado en el Ranking Global junto a los mejores puntajes de cada modo).
     */
    fun calculateCompetitiveScore(): Int {
        val netMoneyScore = ((currentSavings - currentDebt) / 5).coerceAtLeast(-5000)
        val wellbeingBonus = wellbeingPoints * 120
        val accuracyBonus = correctDecisionsCount * 600
        // Bonus de velocidad: máximo 175 segundos (7 días * 25s). Cada segundo ahorrado suma 20 pts.
        val maxTimeMillis = 175_000L
        val savedMillis = (maxTimeMillis - totalDecisionTimeMillis).coerceAtLeast(0L)
        val speedBonus = (savedMillis / 100L).toInt() * 2

        return (netMoneyScore + wellbeingBonus + accuracyBonus + speedBonus).coerceAtLeast(100)
    }
}
