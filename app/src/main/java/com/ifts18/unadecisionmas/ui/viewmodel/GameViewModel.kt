package com.ifts18.unadecisionmas.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.data.model.DecisionOption
import com.ifts18.unadecisionmas.data.model.DecisionRecord
import com.ifts18.unadecisionmas.data.model.GameScenario
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.data.model.PlayerProfile
import com.ifts18.unadecisionmas.data.repository.GameScenariosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var scenarios: List<GameScenario> = GameScenariosRepository.getScenarios("Mateo")

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

    fun getCurrentScenario(): GameScenario {
        val index = _gameState.value.currentDayIndex.coerceIn(0, scenarios.lastIndex)
        return scenarios[index]
    }

    fun selectOption(option: DecisionOption) {
        val current = _gameState.value
        val scenario = getCurrentScenario()

        SoundManager.playClick()
        SoundManager.resetMusicSpeed()
        SoundManager.stopHeartbeat()

        val addedDebt = if (!option.isSafe) option.penaltyAmount else 0
        val record = DecisionRecord(
            dayName = scenario.dayName,
            chosenOptionText = option.text,
            isSafe = option.isSafe,
            addedDebt = addedDebt,
            feedback = option.educationalFeedback
        )

        val nextDayIndex = current.currentDayIndex + 1
        val isLastDay = current.currentDayIndex >= scenarios.lastIndex

        _gameState.update { state ->
            state.copy(
                currentDebt = state.currentDebt + addedDebt,
                correctDecisionsCount = if (option.isSafe) state.correctDecisionsCount + 1 else state.correctDecisionsCount,
                incorrectDecisionsCount = if (!option.isSafe) state.incorrectDecisionsCount + 1 else state.incorrectDecisionsCount,
                decisionHistory = state.decisionHistory + record,
                currentDayIndex = if (isLastDay) state.currentDayIndex else nextDayIndex,
                isGameOver = isLastDay
            )
        }

        if (isLastDay) {
            handleGameOverSound()
        }
    }

    fun onTimeout() {
        val current = _gameState.value
        val scenario = getCurrentScenario()

        SoundManager.resetMusicSpeed()
        SoundManager.stopHeartbeat()

        val record = DecisionRecord(
            dayName = scenario.dayName,
            chosenOptionText = "Tiempo agotado (10s)",
            isSafe = false,
            addedDebt = scenario.timeoutPenalty,
            feedback = "El tiempo límite venció. En ciberapuestas, la urgencia forzada empuja a perder el control."
        )

        val nextDayIndex = current.currentDayIndex + 1
        val isLastDay = current.currentDayIndex >= scenarios.lastIndex

        _gameState.update { state ->
            state.copy(
                currentDebt = state.currentDebt + scenario.timeoutPenalty,
                incorrectDecisionsCount = state.incorrectDecisionsCount + 1,
                decisionHistory = state.decisionHistory + record,
                currentDayIndex = if (isLastDay) state.currentDayIndex else nextDayIndex,
                isGameOver = isLastDay
            )
        }

        if (isLastDay) {
            handleGameOverSound()
        }
    }

    private fun handleGameOverSound() {
        SoundManager.resetMusicSpeed()
        SoundManager.stopHeartbeat()
        val isWon = _gameState.value.currentDebt == 0
        SoundManager.playGameOverSequence(isWon)
    }

    fun restartGame() {
        SoundManager.stopGameOverSequence()
        SoundManager.stopHeartbeat()
        SoundManager.resetMusicSpeed()
        SoundManager.startBgMusic()
        _gameState.update { current ->
            GameState(
                profile = current.profile,
                currentDayIndex = 0,
                currentDebt = 0,
                correctDecisionsCount = 0,
                incorrectDecisionsCount = 0,
                decisionHistory = emptyList(),
                isGameOver = false
            )
        }
    }
}
