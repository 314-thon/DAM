package com.ifts18.unadecisionmas.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object FirstDecision : Screen("first_decision/{playerName}") {
        fun createRoute(playerName: String): String = "first_decision/$playerName"
    }
    data object InfoResources : Screen("info_resources/{didBet}") {
        fun createRoute(didBet: Boolean): String = "info_resources/$didBet"
    }
}
