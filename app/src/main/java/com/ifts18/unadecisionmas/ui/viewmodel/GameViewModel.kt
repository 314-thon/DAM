package com.ifts18.unadecisionmas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.local.AchievementEntity
import com.ifts18.unadecisionmas.data.local.AppDatabase
import com.ifts18.unadecisionmas.data.local.MinigameProgressEntity
import com.ifts18.unadecisionmas.data.local.SavedGameEntity
import com.ifts18.unadecisionmas.data.model.AchievementDef
import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.DecisionOption
import com.ifts18.unadecisionmas.data.model.DecisionRecord
import com.ifts18.unadecisionmas.data.model.GameScenario
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.data.model.PlayerProfile
import com.ifts18.unadecisionmas.data.model.RankingEntry
import com.ifts18.unadecisionmas.data.model.RouletteGameState
import com.ifts18.unadecisionmas.data.model.SlotSymbol
import com.ifts18.unadecisionmas.data.model.SlotsGameState
import com.ifts18.unadecisionmas.data.remote.FirebaseRankingRepository
import com.ifts18.unadecisionmas.data.repository.GameScenariosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val gameDao = database.gameDao()
    private val rankingRepository = FirebaseRankingRepository(application, gameDao)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _slotsState = MutableStateFlow(SlotsGameState())
    val slotsState: StateFlow<SlotsGameState> = _slotsState.asStateFlow()

    private val _rouletteState = MutableStateFlow(RouletteGameState())
    val rouletteState: StateFlow<RouletteGameState> = _rouletteState.asStateFlow()

    val rankingList: StateFlow<List<RankingEntry>> = rankingRepository.rankingList
    val isFirebaseOnline: StateFlow<Boolean> = rankingRepository.isOnlineSynced
    val firebaseStatusText: StateFlow<String> = rankingRepository.connectionStatusMessage

    private var scenarios: List<GameScenario> = GameScenariosRepository.getScenarios("Mateo")

    init {
        observeRoomState()
    }

    private fun observeRoomState() {
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.getMinigameProgress(KEY_SLOTS)?.let { saved ->
                _slotsState.value = SlotsGameState(
                    reel1 = runCatching { SlotSymbol.valueOf(saved.reel1Name) }.getOrDefault(SlotSymbol.SEVEN),
                    reel2 = runCatching { SlotSymbol.valueOf(saved.reel2Name) }.getOrDefault(SlotSymbol.DIAMOND),
                    reel3 = runCatching { SlotSymbol.valueOf(saved.reel3Name) }.getOrDefault(SlotSymbol.CHERRY),
                    spinsUsed = saved.spinsUsed,
                    bonusChips = saved.bonusChips,
                    fakeLockedWinnings = saved.fakeLockedWinnings,
                    realMoneyLost = saved.realMoneyLost,
                    elapsedMillis = saved.elapsedMillis,
                    algorithmStageTitle = saved.stageTitle.ifBlank { "Tenés ${saved.bonusChips} fichas de bono" },
                    algorithmExplanation = saved.stageExplanation.ifBlank { "Agitá o tocá Girar para seguir." },
                    spinStatusBanner = saved.statusBanner.ifBlank { "3 iguales suman saldo de bono" }
                )
            }
            gameDao.getMinigameProgress(KEY_ROULETTE)?.let { saved ->
                _rouletteState.value = RouletteGameState(
                    selectedNumberIndex = saved.selectedNumberIndex.coerceIn(0, 11),
                    selectedBetMode = saved.selectedBetMode.ifBlank { "PLENO" },
                    landedPocketIndex = saved.landedPocketIndex.coerceIn(0, 11),
                    spinsUsed = saved.spinsUsed,
                    bonusChips = saved.bonusChips,
                    fakeLockedWinnings = saved.fakeLockedWinnings,
                    realMoneyLost = saved.realMoneyLost,
                    elapsedMillis = saved.elapsedMillis,
                    resultBannerText = saved.statusBanner.ifBlank { "Elegí tu apuesta y agitá para girar" },
                    algorithmStageTitle = saved.stageTitle.ifBlank { "${saved.bonusChips} fichas activas" },
                    algorithmExplanation = saved.stageExplanation.ifBlank { "Probá el paño y mirá qué pasa." }
                )
            }
        }

        viewModelScope.launch {
            gameDao.observeSavedGame().collect { saved ->
                val dayName = if (saved != null) {
                    val list = GameScenariosRepository.getScenarios(saved.playerName)
                    list.getOrNull(saved.currentDayIndex)?.dayName ?: "En curso"
                } else ""

                _gameState.update { current ->
                    current.copy(
                        hasSavedGameInRoom = saved != null,
                        savedGameDayName = dayName
                    )
                }
            }
        }

        viewModelScope.launch {
            gameDao.observeAchievements().collect { list ->
                val ids = list.map { it.achievementId }.toSet()
                _gameState.update { current ->
                    current.copy(unlockedAchievementIds = ids)
                }
            }
        }
    }

    fun setPlayerProfile(name: String, avatarResId: Int) {
        val cleanName = name.trim().ifEmpty { "Mateo" }
        scenarios = GameScenariosRepository.getScenarios(cleanName)
        _gameState.update { current ->
            current.copy(
                profile = PlayerProfile(
                    name = cleanName,
                    avatarResId = if (avatarResId != 0) avatarResId else R.drawable.avatar_1
                )
            )
        }
    }

    /**
     * Inicia una nueva partida desde el Lunes, limpiando el checkpoint previo.
     */
    fun startNewWeekGame(name: String, avatarResId: Int) {
        setPlayerProfile(name, avatarResId)
        SoundManager.stopGameOverSequence()
        SoundManager.stopHeartbeat()
        SoundManager.resetMusicSpeed()
        SoundManager.startBgMusic()

        _gameState.update { current ->
            current.copy(
                currentDayIndex = 0,
                currentDebt = 0,
                currentSavings = 15000,
                wellbeingPoints = 20,
                correctDecisionsCount = 0,
                incorrectDecisionsCount = 0,
                totalDecisionTimeMillis = 0L,
                decisionHistory = emptyList(),
                lastCompletedMode = "La Semana",
                lastModeSummary = "",
                sessionAchievementIds = emptySet(),
                betMomMoneyOnTuesday = false,
                isPunishedNoPhone = false,
                punishmentReasonTitle = "",
                punishmentReasonBody = "",
                isGameOver = false
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            gameDao.clearSavedGame()
        }
    }

    /**
     * Recupera desde Room/SQLite la partida que quedó a mitad de semana.
     */
    fun resumeSavedGameFromRoom(onLoaded: () -> Unit) {
        viewModelScope.launch {
            val saved = gameDao.getSavedGame() ?: return@launch
            scenarios = GameScenariosRepository.getScenarios(saved.playerName)
            val history = SavedGameEntity.deserializeHistory(saved.serializedHistory)
            val restoredSessionAchievements = saved.serializedSessionAchievements
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            SoundManager.stopGameOverSequence()
            SoundManager.startBgMusic()

            _gameState.update { current ->
                current.copy(
                    profile = PlayerProfile(saved.playerName, saved.avatarResId),
                    currentDayIndex = saved.currentDayIndex.coerceIn(0, scenarios.lastIndex),
                    currentDebt = saved.currentDebt,
                    currentSavings = saved.currentSavings,
                    wellbeingPoints = saved.wellbeingPoints,
                    correctDecisionsCount = saved.correctDecisionsCount,
                    incorrectDecisionsCount = saved.incorrectDecisionsCount,
                    totalDecisionTimeMillis = saved.totalDecisionTimeMillis,
                    decisionHistory = history,
                    sessionAchievementIds = restoredSessionAchievements,
                    betMomMoneyOnTuesday = saved.betMomMoneyOnTuesday,
                    lastCompletedMode = "La Semana",
                    isGameOver = false
                )
            }
            onLoaded()
        }
    }

    fun getCurrentScenario(): GameScenario {
        val index = _gameState.value.currentDayIndex.coerceIn(0, scenarios.lastIndex)
        return scenarios[index]
    }

    fun selectOption(option: DecisionOption, responseTimeMillis: Long = 4000L) {
        val current = _gameState.value
        val scenario = getCurrentScenario()

        SoundManager.playClick()
        SoundManager.resetMusicSpeed()
        SoundManager.stopHeartbeat()

        val addedDebt = if (!option.isSafe) option.penaltyAmount else 0
        val newDebt = current.currentDebt + addedDebt
        val newSavings = (current.currentSavings + option.savingsDelta).coerceAtLeast(0)
        val newWellbeing = (current.wellbeingPoints + option.wellbeingDelta).coerceIn(0, 150)
        val newTimeMillis = current.totalDecisionTimeMillis + responseTimeMillis.coerceAtLeast(300L)

        val record = DecisionRecord(
            dayName = scenario.dayName,
            chosenOptionText = option.text,
            isSafe = option.isSafe,
            addedDebt = addedDebt,
            savingsDelta = option.savingsDelta,
            wellbeingDelta = option.wellbeingDelta,
            responseTimeMillis = responseTimeMillis,
            feedback = option.educationalFeedback
        )

        option.achievementId?.let { unlockAchievement(it) }

        val newHistory = current.decisionHistory + record
        val newCorrect = if (option.isSafe) current.correctDecisionsCount + 1 else current.correctDecisionsCount
        val newIncorrect = if (!option.isSafe) current.incorrectDecisionsCount + 1 else current.incorrectDecisionsCount

        val betMomOnTuesday = current.betMomMoneyOnTuesday || (current.currentDayIndex == 1 && !option.isSafe)
        val nextDayIndex = current.currentDayIndex + 1
        val isLastDay = current.currentDayIndex >= scenarios.lastIndex

        applyStepOutcome(
            nextDayIndex = nextDayIndex,
            isLastDay = isLastDay,
            newDebt = newDebt,
            newSavings = newSavings,
            newWellbeing = newWellbeing,
            newTimeMillis = newTimeMillis,
            newHistory = newHistory,
            newCorrect = newCorrect,
            newIncorrect = newIncorrect,
            betMomOnTuesday = betMomOnTuesday,
            lastModeSummary = if (newDebt == 0) {
                "Superaste la semana con $$newSavings en ahorros y $0 de deuda."
            } else {
                "Terminaste la semana con $$newDebt de deuda y $$newSavings en ahorros."
            }
        )
    }

    fun onTimeout() {
        val current = _gameState.value
        val scenario = getCurrentScenario()

        SoundManager.resetMusicSpeed()
        SoundManager.stopHeartbeat()

        val timeoutMillis = scenario.decisionTimeSeconds * 1000L
        val newDebt = current.currentDebt + scenario.timeoutPenalty
        val newSavings = (current.currentSavings - (scenario.timeoutPenalty / 2)).coerceAtLeast(0)
        val newWellbeing = (current.wellbeingPoints - 8).coerceAtLeast(0)
        val newTimeMillis = current.totalDecisionTimeMillis + timeoutMillis

        val record = DecisionRecord(
            dayName = scenario.dayName,
            chosenOptionText = "Tiempo agotado (${scenario.decisionTimeSeconds}s)",
            isSafe = false,
            addedDebt = scenario.timeoutPenalty,
            savingsDelta = -(scenario.timeoutPenalty / 2),
            wellbeingDelta = -8,
            responseTimeMillis = timeoutMillis,
            feedback = "El tiempo límite venció. En ciberapuestas, la urgencia forzada empuja a perder el control."
        )

        val newHistory = current.decisionHistory + record
        val newIncorrect = current.incorrectDecisionsCount + 1
        val betMomOnTuesday = current.betMomMoneyOnTuesday
        val nextDayIndex = current.currentDayIndex + 1
        val isLastDay = current.currentDayIndex >= scenarios.lastIndex

        applyStepOutcome(
            nextDayIndex = nextDayIndex,
            isLastDay = isLastDay,
            newDebt = newDebt,
            newSavings = newSavings,
            newWellbeing = newWellbeing,
            newTimeMillis = newTimeMillis,
            newHistory = newHistory,
            newCorrect = current.correctDecisionsCount,
            newIncorrect = newIncorrect,
            betMomOnTuesday = betMomOnTuesday,
            lastModeSummary = "Terminaste la semana con $$newDebt de deuda acumulada."
        )
    }

    private fun applyStepOutcome(
        nextDayIndex: Int,
        isLastDay: Boolean,
        newDebt: Int,
        newSavings: Int,
        newWellbeing: Int,
        newTimeMillis: Long,
        newHistory: List<DecisionRecord>,
        newCorrect: Int,
        newIncorrect: Int,
        betMomOnTuesday: Boolean,
        lastModeSummary: String
    ) {
        // Evaluar disparadores del "Emulador de Castigo" (Sin Celu hasta el próximo Lunes)
        val caughtMomTransferOnFriday = !isLastDay && nextDayIndex == 4 && betMomOnTuesday
        val brokeAndInDebt = !isLastDay && newSavings == 0 && newDebt >= 10000
        val shouldTriggerPunishment = caughtMomTransferOnFriday || brokeAndInDebt

        val punishmentTitle = when {
            caughtMomTransferOnFriday -> "Saltó la ficha: sin celu"
            brokeAndInDebt -> "Te quedaste en cero"
            else -> ""
        }
        val punishmentBody = when {
            caughtMomTransferOnFriday -> "Tu mamá pidió el comprobante de las copias y vio la transferencia de $72.000 al cajero. Te sacaron el celular todo el finde."
            brokeAndInDebt -> "Gastaste tus ahorros ($0) hasta quedarte sin SUBE y con $$newDebt de deuda. En tu casa te sacaron el celular hasta el lunes."
            else -> ""
        }

        if (shouldTriggerPunishment) {
            unlockAchievement(AchievementsCatalog.ID_TODO_AL_ROJO)
            SoundManager.playGameOverSequence(isWon = false)
        }

        _gameState.update { state ->
            val updated = state.copy(
                currentDebt = newDebt,
                currentSavings = newSavings,
                wellbeingPoints = newWellbeing,
                correctDecisionsCount = newCorrect,
                incorrectDecisionsCount = newIncorrect,
                totalDecisionTimeMillis = newTimeMillis,
                decisionHistory = newHistory,
                currentDayIndex = if (isLastDay) state.currentDayIndex else nextDayIndex,
                betMomMoneyOnTuesday = betMomOnTuesday,
                isPunishedNoPhone = shouldTriggerPunishment,
                punishmentReasonTitle = punishmentTitle,
                punishmentReasonBody = punishmentBody,
                lastCompletedMode = "La Semana",
                lastModeSummary = lastModeSummary,
                isGameOver = isLastDay && !shouldTriggerPunishment
            )
            updated.copy(lastEarnedScore = updated.calculateCompetitiveScore())
        }

        if (shouldTriggerPunishment) {
            viewModelScope.launch(Dispatchers.IO) {
                gameDao.clearSavedGame()
            }
        } else if (isLastDay) {
            finalizeWeekGame(newDebt, newCorrect, newIncorrect)
        } else {
            persistCheckpointToRoom(
                nextDayIndex = nextDayIndex,
                debt = newDebt,
                savings = newSavings,
                wellbeing = newWellbeing,
                correct = newCorrect,
                incorrect = newIncorrect,
                timeMillis = newTimeMillis,
                history = newHistory,
                betMomMoneyOnTuesday = betMomOnTuesday,
                sessionAchievements = _gameState.value.sessionAchievementIds
            )
        }
    }

    /**
     * Teletransporta al jugador al "Lunes siguiente" tras cumplir su fin de semana sin celular.
     */
    fun restartToNextMondayAfterPunishment() {
        SoundManager.stopGameOverSequence()
        SoundManager.stopHeartbeat()
        SoundManager.resetMusicSpeed()
        SoundManager.startBgMusic()

        _gameState.update { current ->
            current.copy(
                currentDayIndex = 0,
                currentDebt = 0,
                currentSavings = 15000,
                wellbeingPoints = 20,
                correctDecisionsCount = 0,
                incorrectDecisionsCount = 0,
                totalDecisionTimeMillis = 0L,
                decisionHistory = emptyList(),
                sessionAchievementIds = emptySet(),
                betMomMoneyOnTuesday = false,
                isPunishedNoPhone = false,
                punishmentReasonTitle = "",
                punishmentReasonBody = "",
                isGameOver = false
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            gameDao.clearSavedGame()
        }
    }

    /**
     * Permite cerrar la partida desde la pantalla de penitencia para ver el Balance Final y Ranking.
     */
    fun finishPunishedWeekToResults() {
        val current = _gameState.value
        val finalScore = current.calculateCompetitiveScore()
        _gameState.update { state ->
            state.copy(
                isPunishedNoPhone = false,
                lastCompletedMode = "La Semana",
                lastModeSummary = "Saltó la ficha antes del domingo: te sacaron el celu con $${current.currentDebt} de deuda y $${current.currentSavings} de ahorro.",
                lastEarnedScore = finalScore,
                isGameOver = true
            )
        }
        submitCurrentStateToRanking(modeName = "La Semana", customScore = finalScore)
    }

    private fun persistCheckpointToRoom(
        nextDayIndex: Int,
        debt: Int,
        savings: Int,
        wellbeing: Int,
        correct: Int,
        incorrect: Int,
        timeMillis: Long,
        history: List<DecisionRecord>,
        betMomMoneyOnTuesday: Boolean,
        sessionAchievements: Set<String>
    ) {
        val profile = _gameState.value.profile
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.saveGameProgress(
                SavedGameEntity(
                    id = 1,
                    playerName = profile.name,
                    avatarResId = profile.avatarResId,
                    currentDayIndex = nextDayIndex,
                    currentDebt = debt,
                    currentSavings = savings,
                    wellbeingPoints = wellbeing,
                    correctDecisionsCount = correct,
                    incorrectDecisionsCount = incorrect,
                    totalDecisionTimeMillis = timeMillis,
                    serializedHistory = SavedGameEntity.serializeHistory(history),
                    betMomMoneyOnTuesday = betMomMoneyOnTuesday,
                    serializedSessionAchievements = sessionAchievements.joinToString(",")
                )
            )
        }
    }

    private fun finalizeWeekGame(finalDebt: Int, correctCount: Int, incorrectCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.clearSavedGame()
        }

        if (finalDebt == 0) {
            unlockAchievement(AchievementsCatalog.ID_INTOCABLE)
        } else if (incorrectCount >= 1) {
            unlockAchievement(AchievementsCatalog.ID_TODO_AL_ROJO)
        }

        val updatedState = _gameState.value
        val finalScore = updatedState.calculateCompetitiveScore()
        if (finalScore == 6767 || finalScore % 100 == 67) {
            unlockAchievement(AchievementsCatalog.ID_SIXSEVEN)
        }

        handleGameOverSound(isWon = finalDebt == 0)
        submitCurrentStateToRanking(modeName = "La Semana", customScore = finalScore)
    }

    /**
     * Registra la finalización del Juego 2: "Scroll sin Caer" (Feed Instagram + Ads + Acelerómetro).
     */
    fun completeScrollAdsGame(
        adsShakenOff: Int,
        adTrapsClicked: Int,
        elapsedMillis: Long
    ) {
        val sessionEarned = if (adsShakenOff >= 1) {
            unlockAchievement(
                AchievementsCatalog.ID_EVITANDO_TENTACION,
                resetSessionTo = setOf(AchievementsCatalog.ID_EVITANDO_TENTACION)
            )
            setOf(AchievementsCatalog.ID_EVITANDO_TENTACION)
        } else {
            _gameState.update { it.copy(sessionAchievementIds = emptySet()) }
            emptySet()
        }

        val addedDebt = adTrapsClicked * 4000
        val savings = (15000 + (adsShakenOff * 1500) - addedDebt).coerceAtLeast(0)
        val wellbeing = (40 + adsShakenOff * 12 - adTrapsClicked * 15).coerceAtLeast(10)
        val speedBonus = ((45_000L - elapsedMillis).coerceAtLeast(0L) / 25L).toInt()
        val score = (savings / 2) + (adsShakenOff * 1400) - (adTrapsClicked * 1800) + speedBonus +
                (sessionEarned.size * 600)

        val summary = if (adTrapsClicked == 0) {
            "Terminaste el feed y frenaste $adsShakenOff anuncios sin caer en trampas."
        } else {
            "Sacudiste $adsShakenOff anuncios, pero abriste $adTrapsClicked trampas ($$addedDebt de deuda)."
        }

        _gameState.update { state ->
            state.copy(
                currentDebt = addedDebt,
                currentSavings = savings,
                wellbeingPoints = wellbeing,
                correctDecisionsCount = adsShakenOff,
                incorrectDecisionsCount = adTrapsClicked,
                totalDecisionTimeMillis = elapsedMillis,
                decisionHistory = emptyList(),
                lastCompletedMode = "Scroll sin Caer",
                lastModeSummary = summary,
                lastEarnedScore = score.coerceAtLeast(250),
                isGameOver = true
            )
        }

        handleGameOverSound(isWon = adTrapsClicked == 0)
        submitCurrentStateToRanking(modeName = "Scroll sin Caer", customScore = score.coerceAtLeast(250))
    }

    fun updateSlotsProgress(state: SlotsGameState) {
        _slotsState.value = state
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.saveMinigameProgress(
                MinigameProgressEntity(
                    gameKey = KEY_SLOTS,
                    spinsUsed = state.spinsUsed,
                    bonusChips = state.bonusChips,
                    fakeLockedWinnings = state.fakeLockedWinnings,
                    realMoneyLost = state.realMoneyLost,
                    elapsedMillis = state.elapsedMillis,
                    reel1Name = state.reel1.name,
                    reel2Name = state.reel2.name,
                    reel3Name = state.reel3.name,
                    statusBanner = state.spinStatusBanner,
                    stageTitle = state.algorithmStageTitle,
                    stageExplanation = state.algorithmExplanation
                )
            )
        }
    }

    fun updateRouletteProgress(state: RouletteGameState) {
        _rouletteState.value = state
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.saveMinigameProgress(
                MinigameProgressEntity(
                    gameKey = KEY_ROULETTE,
                    spinsUsed = state.spinsUsed,
                    bonusChips = state.bonusChips,
                    fakeLockedWinnings = state.fakeLockedWinnings,
                    realMoneyLost = state.realMoneyLost,
                    elapsedMillis = state.elapsedMillis,
                    selectedNumberIndex = state.selectedNumberIndex,
                    selectedBetMode = state.selectedBetMode,
                    landedPocketIndex = state.landedPocketIndex,
                    statusBanner = state.resultBannerText,
                    stageTitle = state.algorithmStageTitle,
                    stageExplanation = state.algorithmExplanation
                )
            )
        }
    }

    /**
     * Registra la finalización del Juego 3: "La Trampa del Jackpot" (Tragamonedas con sensor).
     */
    fun completeSlotsTrapGame(
        stoppedEarly: Boolean,
        spinsUsed: Int,
        remainingBonusChips: Int,
        realMoneyLost: Int,
        elapsedMillis: Long
    ) {
        val sessionEarned = setOf(AchievementsCatalog.ID_OJO_ABIERTO)
        unlockAchievement(AchievementsCatalog.ID_OJO_ABIERTO, resetSessionTo = sessionEarned)

        val isWon = stoppedEarly || realMoneyLost == 0
        val savings = (15000 - realMoneyLost + (if (stoppedEarly) remainingBonusChips * 500 else 0)).coerceAtLeast(0)
        val wellbeing = if (stoppedEarly) 85 else 55
        val score = (if (stoppedEarly) 11500 else 7800) +
                (remainingBonusChips * 450) -
                (realMoneyLost / 2) +
                (sessionEarned.size * 600)

        val summary = if (stoppedEarly) {
            "Te plantaste en el tiro $spinsUsed/10 y frenaste antes de que la casa vaciara tus fichas."
        } else {
            "Completaste los $spinsUsed tiros del tragamonedas: viste el gancho inicial, los casi-aciertos y la pérdida del final."
        }

        _slotsState.value = SlotsGameState()
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.clearMinigameProgress(KEY_SLOTS)
        }

        _gameState.update { state ->
            state.copy(
                currentDebt = realMoneyLost,
                currentSavings = savings,
                wellbeingPoints = wellbeing,
                correctDecisionsCount = if (stoppedEarly) 7 else 5,
                incorrectDecisionsCount = if (stoppedEarly) 0 else 2,
                totalDecisionTimeMillis = elapsedMillis,
                decisionHistory = emptyList(),
                lastCompletedMode = "Trampa del Jackpot",
                lastModeSummary = summary,
                lastEarnedScore = score.coerceAtLeast(400),
                isGameOver = true
            )
        }

        handleGameOverSound(isWon = isWon)
        submitCurrentStateToRanking(modeName = "Trampa del Jackpot", customScore = score.coerceAtLeast(400))
    }

    /**
     * Registra la finalización del Juego 4: "La Ruleta del 29" (Ruleta con sensor acelerómetro).
     */
    fun completeRouletteTrapGame(
        stoppedEarly: Boolean,
        spinsUsed: Int,
        remainingBonusChips: Int,
        realMoneyLost: Int,
        elapsedMillis: Long
    ) {
        val sessionEarned = setOf(AchievementsCatalog.ID_OJO_ABIERTO)
        unlockAchievement(AchievementsCatalog.ID_OJO_ABIERTO, resetSessionTo = sessionEarned)

        val isWon = stoppedEarly || realMoneyLost == 0
        val savings = (15000 - realMoneyLost + (if (stoppedEarly) remainingBonusChips * 550 else 0)).coerceAtLeast(0)
        val wellbeing = if (stoppedEarly) 88 else 52
        val score = (if (stoppedEarly) 11800 else 7600) +
                (remainingBonusChips * 480) -
                (realMoneyLost / 2) +
                (sessionEarned.size * 600)

        val summary = if (stoppedEarly) {
            "Te plantaste en la tirada $spinsUsed/10 antes de que la banca vaciara tus fichas."
        } else {
            "Completaste las $spinsUsed tiradas de ruleta y comprobaste cómo el 0 Verde inclina la ventaja."
        }

        _rouletteState.value = RouletteGameState()
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.clearMinigameProgress(KEY_ROULETTE)
        }

        _gameState.update { state ->
            state.copy(
                currentDebt = realMoneyLost,
                currentSavings = savings,
                wellbeingPoints = wellbeing,
                correctDecisionsCount = if (stoppedEarly) 7 else 5,
                incorrectDecisionsCount = if (stoppedEarly) 0 else 2,
                totalDecisionTimeMillis = elapsedMillis,
                decisionHistory = emptyList(),
                lastCompletedMode = "La Ruleta del 29",
                lastModeSummary = summary,
                lastEarnedScore = score.coerceAtLeast(400),
                isGameOver = true
            )
        }

        handleGameOverSound(isWon = isWon)
        submitCurrentStateToRanking(modeName = "La Ruleta del 29", customScore = score.coerceAtLeast(400))
    }

    private fun submitCurrentStateToRanking(modeName: String, customScore: Int) {
        val current = _gameState.value
        val entry = RankingEntry(
            playerName = current.profile.name,
            avatarIndex = FirebaseRankingRepository.avatarResToIndex(current.profile.avatarResId),
            score = customScore,
            debt = current.currentDebt,
            savings = current.currentSavings,
            wellbeingPoints = current.wellbeingPoints,
            totalTimeMillis = current.totalDecisionTimeMillis.coerceAtLeast(1000L),
            achievementsCount = current.sessionAchievementIds.size,
            hasSixSeven = current.unlockedAchievementIds.contains(AchievementsCatalog.ID_SIXSEVEN),
            gameMode = modeName,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            rankingRepository.submitScore(entry)
        }
    }

    fun unlockAchievement(
        achievementId: String,
        resetSessionTo: Set<String>? = null
    ) {
        val def = AchievementsCatalog.findById(achievementId) ?: return
        val alreadyUnlocked = _gameState.value.unlockedAchievementIds.contains(achievementId)

        _gameState.update { state ->
            val updatedSession = resetSessionTo ?: (state.sessionAchievementIds + achievementId)
            state.copy(
                sessionAchievementIds = updatedSession,
                unlockedAchievementIds = state.unlockedAchievementIds + achievementId,
                newlyUnlockedAchievement = if (!alreadyUnlocked) def else state.newlyUnlockedAchievement
            )
        }

        if (!alreadyUnlocked) {
            viewModelScope.launch(Dispatchers.IO) {
                gameDao.unlockAchievement(AchievementEntity(achievementId = achievementId))
            }
        }
    }

    fun dismissNewlyUnlockedAchievement() {
        _gameState.update { it.copy(newlyUnlockedAchievement = null) }
    }

    /**
     * Procesa el resultado del escáner de cámara QR para desbloquear el Easter Egg SIXSEVEN.
     */
    fun onQrCodeScanned(scannedText: String): Boolean {
        val normalized = scannedText.trim().uppercase()
        return if (normalized.contains("SIXSEVEN") || normalized.contains("67") || normalized.contains("JUEGOLIMPIO")) {
            unlockAchievement(AchievementsCatalog.ID_SIXSEVEN)
            SoundManager.playWin()
            true
        } else {
            false
        }
    }

    fun refreshOnlineRanking() {
        viewModelScope.launch {
            rankingRepository.refreshRanking()
        }
    }

    private fun handleGameOverSound(isWon: Boolean) {
        SoundManager.resetMusicSpeed()
        SoundManager.stopHeartbeat()
        SoundManager.playGameOverSequence(isWon)
    }

    fun restartGame() {
        SoundManager.stopGameOverSequence()
        SoundManager.stopHeartbeat()
        SoundManager.resetMusicSpeed()
        SoundManager.startBgMusic()
        _gameState.update { current ->
            current.copy(
                currentDayIndex = 0,
                currentDebt = 0,
                currentSavings = 15000,
                wellbeingPoints = 20,
                correctDecisionsCount = 0,
                incorrectDecisionsCount = 0,
                totalDecisionTimeMillis = 0L,
                decisionHistory = emptyList(),
                sessionAchievementIds = emptySet(),
                betMomMoneyOnTuesday = false,
                isPunishedNoPhone = false,
                punishmentReasonTitle = "",
                punishmentReasonBody = "",
                isGameOver = false
            )
        }
    }

    companion object {
        private const val KEY_SLOTS = "SLOTS"
        private const val KEY_ROULETTE = "ROULETTE"
    }
}
