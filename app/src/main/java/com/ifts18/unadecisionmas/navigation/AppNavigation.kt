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
                onNavigateToInfo = {
                    navController.navigate(Screen.InfoResources.route)
                }
            )
        }

        composable(route = Screen.InfoResources.route) {
            InfoResourcesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
