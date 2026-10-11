package com.ifts18.unadecisionmas.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object GameDecision : Screen("game_decision")
    data object Result : Screen("result")
}
