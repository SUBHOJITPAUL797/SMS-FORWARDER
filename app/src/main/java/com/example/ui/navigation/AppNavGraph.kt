package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.SmsBridgeApp
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.client.ClientHomeScreen
import com.example.ui.client.ClientViewModel
import com.example.ui.developer.DeveloperProfileScreen
import com.example.ui.host.HostHomeScreen
import com.example.ui.host.HostViewModel
import com.example.ui.pairing.ClientPairingScreen
import com.example.ui.pairing.HostPairingScreen
import com.example.ui.pairing.PairingViewModel
import com.example.ui.roleselect.RoleSelectScreen
import com.example.ui.roleselect.RoleSelectViewModel
import com.example.ui.splash.SplashScreen
import com.example.ui.splash.SplashViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    app: SmsBridgeApp,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    // Shared or scoped ViewModels initialized from app dependencies
    val authRepository = app.authRepository
    val pairingRepository = app.pairingRepository
    val smsRepository = app.smsRepository

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            val viewModel = remember { SplashViewModel(authRepository) }
            SplashScreen(
                viewModel = viewModel,
                onNavigate = { target ->
                    navController.navigate(target) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            val viewModel = remember { AuthViewModel(authRepository) }
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Register Screen
        composable(Screen.Register.route) {
            val viewModel = remember { AuthViewModel(authRepository) }
            RegisterScreen(
                viewModel = viewModel,
                onNavigateBackToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Role Select Screen
        composable(Screen.RoleSelect.route) {
            val viewModel = remember { RoleSelectViewModel(authRepository) }
            RoleSelectScreen(
                viewModel = viewModel,
                onSelectHost = {
                    navController.navigate(Screen.HostHome.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onSelectClient = {
                    navController.navigate(Screen.ClientHome.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onOpenDeveloperProfile = {
                    navController.navigate(Screen.DeveloperProfile.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.RoleSelect.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Client Pairing Screen
        composable(Screen.ClientPairing.route) {
            val viewModel = remember { PairingViewModel(pairingRepository) }
            ClientPairingScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPairingComplete = {
                    navController.navigate(Screen.ClientHome.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                }
            )
        }

        // Host Pairing Screen
        composable(Screen.HostPairing.route) {
            val viewModel = remember { PairingViewModel(pairingRepository) }
            HostPairingScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPairingSuccess = {
                    navController.navigate(Screen.HostHome.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                }
            )
        }

        // Host Home Screen
        composable(Screen.HostHome.route) {
            val viewModel = remember { HostViewModel(authRepository, smsRepository) }
            HostHomeScreen(
                viewModel = viewModel,
                onChangeRole = {
                    navController.navigate(Screen.RoleSelect.route)
                },
                onOpenDeveloperProfile = {
                    navController.navigate(Screen.DeveloperProfile.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Client Home Screen
        composable(Screen.ClientHome.route) {
            val viewModel = remember { ClientViewModel(authRepository, smsRepository) }
            ClientHomeScreen(
                viewModel = viewModel,
                onChangeRole = {
                    navController.navigate(Screen.RoleSelect.route)
                },
                onOpenDeveloperProfile = {
                    navController.navigate(Screen.DeveloperProfile.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Developer Profile Screen
        composable(Screen.DeveloperProfile.route) {
            DeveloperProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
