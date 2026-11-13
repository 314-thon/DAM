package com.ifts18.unadecisionmas.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ifts18.unadecisionmas.ui.components.AchievementBadgeIcon
import com.ifts18.unadecisionmas.ui.screens.GameDecisionScreen
import com.ifts18.unadecisionmas.ui.screens.GameResultScreen
import com.ifts18.unadecisionmas.ui.screens.LeaderboardScreen
import com.ifts18.unadecisionmas.ui.screens.RouletteTrapGameScreen
import com.ifts18.unadecisionmas.ui.screens.ScrollAdsGameScreen
import com.ifts18.unadecisionmas.ui.screens.SlotsTrapGameScreen
import com.ifts18.unadecisionmas.ui.screens.WelcomeScreen
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import com.ifts18.unadecisionmas.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val slotsState by viewModel.slotsState.collectAsStateWithLifecycle()
    val rouletteState by viewModel.rouletteState.collectAsStateWithLifecycle()
    val rankingList by viewModel.rankingList.collectAsStateWithLifecycle()
    val isFirebaseOnline by viewModel.isFirebaseOnline.collectAsStateWithLifecycle()
    val firebaseStatusText by viewModel.firebaseStatusText.collectAsStateWithLifecycle()

    // Auto-ocultar notificación flotante de logro recién desbloqueado tras 4 segundos
    LaunchedEffect(gameState.newlyUnlockedAchievement) {
        if (gameState.newlyUnlockedAchievement != null) {
            delay(4000L)
            viewModel.dismissNewlyUnlockedAchievement()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route
        ) {
            composable(route = Screen.Welcome.route) {
                WelcomeScreen(
                    initialPlayerName = gameState.profile.name,
                    hasSavedGame = gameState.hasSavedGameInRoom,
                    savedGameDayName = gameState.savedGameDayName,
                    unlockedAchievementsCount = gameState.unlockedAchievementIds.size,
                    onStartWeekGame = { playerName, avatarResId ->
                        viewModel.startNewWeekGame(playerName, avatarResId)
                        navController.navigate(Screen.GameDecision.route)
                    },
                    onResumeSavedGame = {
                        viewModel.resumeSavedGameFromRoom {
                            navController.navigate(Screen.GameDecision.route)
                        }
                    },
                    onStartScrollAdsGame = { playerName, avatarResId ->
                        viewModel.setPlayerProfile(playerName, avatarResId)
                        navController.navigate(Screen.ScrollAdsGame.route)
                    },
                    onStartSlotsTrapGame = { playerName, avatarResId ->
                        viewModel.setPlayerProfile(playerName, avatarResId)
                        navController.navigate(Screen.SlotsTrapGame.route)
                    },
                    onStartRouletteTrapGame = { playerName, avatarResId ->
                        viewModel.setPlayerProfile(playerName, avatarResId)
                        navController.navigate(Screen.RouletteTrapGame.route)
                    },
                    onOpenLeaderboard = { playerName, avatarResId ->
                        viewModel.setPlayerProfile(playerName, avatarResId)
                        navController.navigate(Screen.Leaderboard.route)
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

            composable(route = Screen.ScrollAdsGame.route) {
                ScrollAdsGameScreen(
                    playerName = gameState.profile.name,
                    onBackToHub = {
                        navController.popBackStack()
                    },
                    onGameFinished = { adsShakenOff, adTrapsClicked, elapsedMillis ->
                        viewModel.completeScrollAdsGame(
                            adsShakenOff = adsShakenOff,
                            adTrapsClicked = adTrapsClicked,
                            elapsedMillis = elapsedMillis
                        )
                        navController.navigate(Screen.Result.route) {
                            popUpTo(Screen.ScrollAdsGame.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.SlotsTrapGame.route) {
                SlotsTrapGameScreen(
                    state = slotsState,
                    onStateChange = { updated -> viewModel.updateSlotsProgress(updated) },
                    onBackToHub = {
                        navController.popBackStack()
                    },
                    onGameFinished = { stoppedEarly, spinsUsed, remainingChips, realMoneyLost, elapsedMillis ->
                        viewModel.completeSlotsTrapGame(
                            stoppedEarly = stoppedEarly,
                            spinsUsed = spinsUsed,
                            remainingBonusChips = remainingChips,
                            realMoneyLost = realMoneyLost,
                            elapsedMillis = elapsedMillis
                        )
                        navController.navigate(Screen.Result.route) {
                            popUpTo(Screen.SlotsTrapGame.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.RouletteTrapGame.route) {
                RouletteTrapGameScreen(
                    state = rouletteState,
                    onStateChange = { updated -> viewModel.updateRouletteProgress(updated) },
                    onBackToHub = {
                        navController.popBackStack()
                    },
                    onGameFinished = { stoppedEarly, spinsUsed, remainingChips, realMoneyLost, elapsedMillis ->
                        viewModel.completeRouletteTrapGame(
                            stoppedEarly = stoppedEarly,
                            spinsUsed = spinsUsed,
                            remainingBonusChips = remainingChips,
                            realMoneyLost = realMoneyLost,
                            elapsedMillis = elapsedMillis
                        )
                        navController.navigate(Screen.Result.route) {
                            popUpTo(Screen.RouletteTrapGame.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Result.route) {
                GameResultScreen(
                    gameState = gameState,
                    onOpenLeaderboard = {
                        navController.navigate(Screen.Leaderboard.route)
                    },
                    onQrCodeScanned = { scanned ->
                        viewModel.onQrCodeScanned(scanned)
                    },
                    onRestartGame = {
                        viewModel.restartGame()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Leaderboard.route) {
                LeaderboardScreen(
                    gameState = gameState,
                    rankingList = rankingList,
                    isFirebaseOnline = isFirebaseOnline,
                    firebaseStatusText = firebaseStatusText,
                    onRefreshRanking = { viewModel.refreshOnlineRanking() },
                    onQrCodeScanned = { scanned -> viewModel.onQrCodeScanned(scanned) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Banner emergente superior cuando se desbloquea una medalla o el Easter Egg SIXSEVEN
        gameState.newlyUnlockedAchievement?.let { unlocked ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF18122B))
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(VerdeExito, BordeCyan, BordeMagenta)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { viewModel.dismissNewlyUnlockedAchievement() }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AchievementBadgeIcon(
                        achievementId = unlocked.id,
                        isUnlocked = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MEDALLA DESBLOQUEADA (${unlocked.codeNumber})",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = VerdeExito
                        )
                        Text(
                            text = "${unlocked.title} (+${unlocked.bonusPoints} pts)",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
