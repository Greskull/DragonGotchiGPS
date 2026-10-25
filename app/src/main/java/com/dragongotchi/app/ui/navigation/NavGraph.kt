package com.dragongotchi.app.ui.navigation

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dragongotchi.app.AppContainer
import com.dragongotchi.app.data.local.PetStateEntity
import com.dragongotchi.app.domain.RewardState
import com.dragongotchi.app.location.TrackingStatus
import com.dragongotchi.app.ui.screens.extras.EvolutionScreen
import com.dragongotchi.app.ui.screens.extras.RewardsScreen
import com.dragongotchi.app.ui.screens.extras.SettingsScreen
import com.dragongotchi.app.ui.screens.history.HistoryScreen
import com.dragongotchi.app.ui.screens.history.SessionDetailScreen
import com.dragongotchi.app.ui.screens.home.HomeScreen
import com.dragongotchi.app.ui.screens.onboarding.PermissionsOnboardingScreen
import com.dragongotchi.app.ui.screens.tracking.LiveTrackingScreen
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRACKING = "tracking"
    const val HISTORY = "history"
    const val SESSION_DETAIL = "session/{sessionId}"
    const val EVOLUTIONS = "evolutions"
    const val REWARDS = "rewards"
    const val SETTINGS = "settings"

    fun sessionDetail(sessionId: Long) = "session/$sessionId"
}

@Composable
fun DragonGotchiNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val onboardingCompleted by container.userPreferences.onboardingCompleted
        .collectAsState(initial = null)
    val pet by container.petRepository.pet.collectAsState(initial = PetStateEntity())
    val rewardState by container.rewardsRepository.state.collectAsState(initial = RewardState())
    val trackingState by container.trackingManager.state.collectAsState()
    val weightKg by container.userPreferences.weightKg.collectAsState(initial = 70f)
    val appLang by container.userPreferences.appLanguage
        .collectAsState(initial = com.dragongotchi.app.data.preferences.AppLanguage.ES)
    val scope = rememberCoroutineScope()

    // Esperamos a saber si es la primera instalacion
    if (onboardingCompleted == null) {
        Text(stringResource(R.string.loading))
        return
    }
    val completed = onboardingCompleted == true

    val startDestination = if (completed) Routes.HOME else Routes.ONBOARDING
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Routes.HOME, Routes.TRACKING, Routes.HISTORY, Routes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = { navController.navigate(Routes.HOME) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_my_dragon)) },
                        label = { Text(stringResource(R.string.nav_my_dragon)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.TRACKING,
                        onClick = { navController.navigate(Routes.TRACKING) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.nav_train)) },
                        label = {
                            Text(
                                if (trackingState.status == TrackingStatus.IDLE) stringResource(R.string.nav_train)
                                else stringResource(R.string.nav_training_ongoing)
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = { navController.navigate(Routes.HISTORY) { launchSingleTop = true } },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_history)) },
                        label = { Text(stringResource(R.string.nav_history)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                        label = { Text(stringResource(R.string.nav_settings)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.ONBOARDING) {
                PermissionsOnboardingScreen(
                    onFinished = {
                        scope.launch { container.userPreferences.setOnboardingCompleted() }
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    pet = pet,
                    rewardState = rewardState,
                    isTracking = trackingState.status != TrackingStatus.IDLE,
                    onStartClicked = { navController.navigate(Routes.TRACKING) },
                    onRewardsClicked = { navController.navigate(Routes.REWARDS) },
                    onEvolutionClicked = { navController.navigate(Routes.EVOLUTIONS) }
                )
            }
            composable(Routes.TRACKING) {
                LiveTrackingScreen(
                    trackingStateFlow = container.trackingManager.state,
                    petLevel = pet.level,
                    rewardState = rewardState,
                    onFinishedSession = { sessionId ->
                        if (sessionId != null) {
                            navController.navigate(Routes.sessionDetail(sessionId)) {
                                popUpTo(Routes.TRACKING) { inclusive = false }
                            }
                            container.trackingManager.reset()
                        } else {
                            navController.navigate(Routes.HOME) { launchSingleTop = true }
                            container.trackingManager.reset()
                        }
                    }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    sessionsFlow = container.sessionRepository.observeSessions(),
                    onSessionClicked = { id -> navController.navigate(Routes.sessionDetail(id)) }
                )
            }
            composable(
                route = Routes.SESSION_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
                SessionDetailScreen(
                    sessionFlow = container.sessionRepository.observeSession(sessionId),
                    pointsFlow = container.sessionRepository.observePoints(sessionId),
                    onDelete = { id -> container.sessionRepository.deleteSession(id) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.EVOLUTIONS) {
                EvolutionScreen(
                    currentLevel = pet.level,
                    onBack = { navController.popBackStack() },
                    selectedPetId = rewardState.selectedPetId,
                    shinyEquipped = rewardState.shinyEquipped
                )
            }
            composable(Routes.REWARDS) {
                RewardsScreen(
                    rewardState = rewardState,
                    onBack = { navController.popBackStack() },
                    onRewardsSeen = { scope.launch { container.rewardsRepository.markRewardsSeen() } },
                    onToggleAura = { enabled ->
                        scope.launch { container.rewardsRepository.setAuraEquipped(enabled) }
                    },
                    onToggleShiny = { enabled ->
                        scope.launch { container.rewardsRepository.setShinyEquipped(enabled) }
                    },
                    onSelectPet = { petId ->
                        scope.launch { container.rewardsRepository.setSelectedPet(petId) }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    initialWeightKg = weightKg,
                    initialPetName = pet.name,
                    selectedLanguage = appLang,
                    onLanguageSelected = { lang ->
                        scope.launch { container.userPreferences.setAppLanguage(lang) }
                    },
                    onSave = { weight, name ->
                        container.userPreferences.setWeightKg(weight)
                        container.userPreferences.setPetName(name)
                        container.petRepository.renamePet(name)
                    }
                )
            }
        }
    }
}
