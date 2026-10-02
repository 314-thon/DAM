package com.ifts18.unadecisionmas.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ifts18.unadecisionmas.ui.screens.FirstDecisionScreen
import com.ifts18.unadecisionmas.ui.screens.InfoResourcesScreen
import com.ifts18.unadecisionmas.ui.screens.WelcomeScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToDecision = { playerName ->
                    navController.navigate(Screen.FirstDecision.createRoute(playerName))
                }
            )
        }

        composable(
            route = Screen.FirstDecision.route,
            arguments = listOf(
                navArgument("playerName") {
                    type = NavType.StringType
                    defaultValue = "Mateo"
                }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Mateo"
            FirstDecisionScreen(
                playerName = playerName,
                onApostarClick = {
                    navController.navigate(Screen.InfoResources.createRoute(didBet = true))
                },
                onNoApostarClick = {
                    navController.navigate(Screen.InfoResources.createRoute(didBet = false))
                },
                onNavigateToInfo = {
                    navController.navigate(Screen.InfoResources.createRoute(didBet = false))
                },
                onTimeout = {
                    navController.navigate(Screen.InfoResources.createRoute(didBet = false))
                }
            )
        }

        composable(
            route = Screen.InfoResources.route,
            arguments = listOf(
                navArgument("didBet") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val didBet = backStackEntry.arguments?.getBoolean("didBet") ?: false
            InfoResourcesScreen(
                didBet = didBet,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRestartGame = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
