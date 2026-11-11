package com.ifts18.unadecisionmas.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object GameDecision : Screen("game_decision")
    data object ScrollAdsGame : Screen("scroll_ads_game")
    data object SlotsTrapGame : Screen("slots_trap_game")
    data object RouletteTrapGame : Screen("roulette_trap_game")
    data object Result : Screen("result")
    data object Leaderboard : Screen("leaderboard")
}
