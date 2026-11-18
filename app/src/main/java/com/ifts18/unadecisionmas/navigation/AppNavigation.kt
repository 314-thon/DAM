package com.ifts18.unadecisionmas.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.ifts18.unadecisionmas.ui.theme.FondoGeneral
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import com.ifts18.unadecisionmas.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onGoogleSignInResult(result.data)
    }
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val slotsState by viewModel.slotsState.collectAsStateWithLifecycle()
    val rouletteState by viewModel.rouletteState.collectAsStateWithLifecycle()
    val rankingList by viewModel.rankingList.collectAsStateWithLifecycle()
    val currentUserRanking by viewModel.currentUserRanking.collectAsStateWithLifecycle()
    val isFirebaseOnline by viewModel.isFirebaseOnline.collectAsStateWithLifecycle()
    val firebaseStatusText by viewModel.firebaseStatusText.collectAsStateWithLifecycle()

    val currentNavEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavEntry?.destination?.route

    fun navigateFrom(
        expectedCurrentRoute: String,
        targetRoute: String,
        beforeNavigate: () -> Unit = {},
        builder: (NavOptionsBuilder.() -> Unit)? = null
    ) {
        if (navController.currentBackStackEntry?.destination?.route != expectedCurrentRoute) return
        beforeNavigate()
        navController.navigate(targetRoute) {
            launchSingleTop = true
            builder?.invoke(this)
        }
    }

    fun popSafely(expectedCurrentRoute: String) {
        val activeRoute = navController.currentBackStackEntry?.destination?.route
        if (activeRoute == expectedCurrentRoute && navController.previousBackStackEntry != null) {
            navController.popBackStack(expectedCurrentRoute, inclusive = true)
        }
    }

    fun completeMinigameToResult(expectedRoute: String, complete: () -> Unit) {
        navigateFrom(expectedRoute, Screen.Result.route, beforeNavigate = complete) {
            popUpTo(expectedRoute) { inclusive = true }
        }
    }

    // Auto-ocultar notificación flotante de logro recién desbloqueado tras 4 segundos
    LaunchedEffect(gameState.newlyUnlockedAchievement) {
        if (gameState.newlyUnlockedAchievement != null) {
            delay(4000L)
            viewModel.dismissNewlyUnlockedAchievement()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoGeneral)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            enterTransition = { fadeIn(animationSpec = tween(180)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { fadeIn(animationSpec = tween(180)) },
            popExitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            composable(route = Screen.Welcome.route) {
                WelcomeScreen(
                    initialPlayerName = gameState.profile.name,
                    initialAvatarResId = gameState.profile.avatarResId,
                    authState = authState,
                    hasSavedGame = gameState.hasSavedGameInRoom,
                    savedGameDayName = gameState.savedGameDayName,
                    unlockedAchievementsCount = gameState.unlockedAchievementIds.size,
                    bestScoresByMode = gameState.bestScoresByMode,
                    isTutorialCompleted = gameState.isTutorialCompleted,
                    hasPlayedIntroSplash = gameState.hasPlayedIntroSplash,
                    onProfileChanged = { playerName, avatarResId ->
                        viewModel.setPlayerProfile(playerName, avatarResId)
                    },
                    onSignInAnonymous = { nickname, avatarResId ->
                        viewModel.signInAnonymously(nickname, avatarResId)
                    },
                    onSignInEmail = { email, password, isRegister ->
                        viewModel.signInWithEmail(email, password, isRegister)
                    },
                    onSignInGoogle = {
                        val intent = viewModel.buildGoogleSignInIntent(context)
                        if (intent != null) {
                            googleSignInLauncher.launch(intent)
                        }
                    },
                    onCompleteNickname = { nickname, avatarResId ->
                        viewModel.completeNicknameSetup(nickname, avatarResId)
                    },
                    onSignOut = {
                        viewModel.signOut()
                    },
                    onClearAuthError = {
                        viewModel.clearAuthError()
                    },
                    onIntroSplashCompleted = {
                        viewModel.markIntroSplashCompleted()
                    },
                    onTutorialCompleted = {
                        viewModel.markTutorialCompleted()
                    },
                    onRestartTutorial = {
                        viewModel.restartTutorial()
                    },
                    onStartWeekGame = { playerName, avatarResId ->
                        navigateFrom(
                            expectedCurrentRoute = Screen.Welcome.route,
                            targetRoute = Screen.GameDecision.route,
                            beforeNavigate = { viewModel.startNewWeekGame(playerName, avatarResId) }
                        )
                    },
                    onResumeSavedGame = {
                        if (navController.currentBackStackEntry?.destination?.route == Screen.Welcome.route) {
                            viewModel.resumeSavedGameFromRoom {
                                navigateFrom(Screen.Welcome.route, Screen.GameDecision.route)
                            }
                        }
                    },
                    onStartScrollAdsGame = { playerName, avatarResId ->
                        navigateFrom(
                            expectedCurrentRoute = Screen.Welcome.route,
                            targetRoute = Screen.ScrollAdsGame.route,
                            beforeNavigate = { viewModel.setPlayerProfile(playerName, avatarResId) }
                        )
                    },
                    onStartSlotsTrapGame = { playerName, avatarResId ->
                        navigateFrom(
                            expectedCurrentRoute = Screen.Welcome.route,
                            targetRoute = Screen.SlotsTrapGame.route,
                            beforeNavigate = { viewModel.setPlayerProfile(playerName, avatarResId) }
                        )
                    },
                    onStartRouletteTrapGame = { playerName, avatarResId ->
                        navigateFrom(
                            expectedCurrentRoute = Screen.Welcome.route,
                            targetRoute = Screen.RouletteTrapGame.route,
                            beforeNavigate = { viewModel.setPlayerProfile(playerName, avatarResId) }
                        )
                    },
                    onOpenLeaderboard = { playerName, avatarResId ->
                        navigateFrom(
                            expectedCurrentRoute = Screen.Welcome.route,
                            targetRoute = Screen.Leaderboard.route,
                            beforeNavigate = { viewModel.setPlayerProfile(playerName, avatarResId) }
                        )
                    },
                    onQrCodeScanned = { scanned ->
                        viewModel.onQrCodeScanned(scanned)
                    }
                )
            }

            composable(route = Screen.GameDecision.route) {
                BackHandler(enabled = currentRoute == Screen.GameDecision.route) {
                    popSafely(Screen.GameDecision.route)
                }
                GameDecisionScreen(
                    viewModel = viewModel,
                    gameState = gameState,
                    onNavigateToResult = {
                        navigateFrom(Screen.GameDecision.route, Screen.Result.route) {
                            popUpTo(Screen.GameDecision.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.ScrollAdsGame.route) {
                BackHandler(enabled = currentRoute == Screen.ScrollAdsGame.route) {
                    popSafely(Screen.ScrollAdsGame.route)
                }
                ScrollAdsGameScreen(
                    playerName = gameState.profile.name,
                    onBackToHub = {
                        popSafely(Screen.ScrollAdsGame.route)
                    },
                    onGameFinished = { adsShakenOff, adTrapsClicked, elapsedMillis ->
                        completeMinigameToResult(Screen.ScrollAdsGame.route) {
                            viewModel.completeScrollAdsGame(
                                adsShakenOff = adsShakenOff,
                                adTrapsClicked = adTrapsClicked,
                                elapsedMillis = elapsedMillis
                            )
                        }
                    }
                )
            }

            composable(route = Screen.SlotsTrapGame.route) {
                BackHandler(enabled = currentRoute == Screen.SlotsTrapGame.route) {
                    popSafely(Screen.SlotsTrapGame.route)
                }
                SlotsTrapGameScreen(
                    state = slotsState,
                    onStateChange = { updated -> viewModel.updateSlotsProgress(updated) },
                    onBackToHub = {
                        popSafely(Screen.SlotsTrapGame.route)
                    },
                    onGameFinished = { stoppedEarly, spinsUsed, remainingChips, realMoneyLost, elapsedMillis ->
                        completeMinigameToResult(Screen.SlotsTrapGame.route) {
                            viewModel.completeSlotsTrapGame(
                                stoppedEarly = stoppedEarly,
                                spinsUsed = spinsUsed,
                                remainingBonusChips = remainingChips,
                                realMoneyLost = realMoneyLost,
                                elapsedMillis = elapsedMillis
                            )
                        }
                    }
                )
            }

            composable(route = Screen.RouletteTrapGame.route) {
                BackHandler(enabled = currentRoute == Screen.RouletteTrapGame.route) {
                    popSafely(Screen.RouletteTrapGame.route)
                }
                RouletteTrapGameScreen(
                    state = rouletteState,
                    onStateChange = { updated -> viewModel.updateRouletteProgress(updated) },
                    onBackToHub = {
                        popSafely(Screen.RouletteTrapGame.route)
                    },
                    onGameFinished = { stoppedEarly, spinsUsed, remainingChips, realMoneyLost, elapsedMillis ->
                        completeMinigameToResult(Screen.RouletteTrapGame.route) {
                            viewModel.completeRouletteTrapGame(
                                stoppedEarly = stoppedEarly,
                                spinsUsed = spinsUsed,
                                remainingBonusChips = remainingChips,
                                realMoneyLost = realMoneyLost,
                                elapsedMillis = elapsedMillis
                            )
                        }
                    }
                )
            }

            composable(route = Screen.Result.route) {
                BackHandler(enabled = currentRoute == Screen.Result.route) {
                    popSafely(Screen.Result.route)
                }
                GameResultScreen(
                    gameState = gameState,
                    currentUserRanking = currentUserRanking,
                    onOpenLeaderboard = {
                        navigateFrom(Screen.Result.route, Screen.Leaderboard.route)
                    },
                    onRestartGame = {
                        navigateFrom(
                            expectedCurrentRoute = Screen.Result.route,
                            targetRoute = Screen.Welcome.route,
                            beforeNavigate = { viewModel.restartGame() }
                        ) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Leaderboard.route) {
                BackHandler(enabled = currentRoute == Screen.Leaderboard.route) {
                    popSafely(Screen.Leaderboard.route)
                }
                LeaderboardScreen(
                    gameState = gameState,
                    rankingList = rankingList,
                    currentUserRanking = currentUserRanking,
                    currentAuthUid = authState.uid,
                    isFirebaseOnline = isFirebaseOnline,
                    firebaseStatusText = firebaseStatusText,
                    onRefreshRanking = { viewModel.refreshOnlineRanking() },
                    onBack = { popSafely(Screen.Leaderboard.route) }
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
