package com.ifts18.unadecisionmas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ifts18.unadecisionmas.ui.screens.GameDecisionScreen
import com.ifts18.unadecisionmas.ui.screens.GameResultScreen
import com.ifts18.unadecisionmas.ui.screens.WelcomeScreen
import com.ifts18.unadecisionmas.ui.viewmodel.GameViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToDecision = { playerName, avatarResId ->
                    viewModel.setPlayerProfile(playerName, avatarResId)
                    navController.navigate(Screen.GameDecision.route)
                }
            )
        }

        composable(route = Screen.GameDecision.route) {
            GameDecisionScreen(
                viewModel = viewModel,
                gameState = gameState,
                onNavigateToResult = {
                    navController.navigate(Screen.Result.route) {
                        popUpTo(Screen.GameDecision.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Result.route) {
            GameResultScreen(
                gameState = gameState,
                onRestartGame = {
                    viewModel.restartGame()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
