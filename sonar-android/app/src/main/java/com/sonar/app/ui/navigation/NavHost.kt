package com.sonar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sonar.app.ui.screens.home.HomeScreen
import com.sonar.app.ui.screens.result.ResultScreen
import com.sonar.app.ui.screens.settings.SettingsScreen

@Composable
fun SonarNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onRecordingClick = { id ->
                    navController.navigate("result/${id}")
                },
                onAnalyzeClick = { id ->
                    navController.navigate("result/${id}")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "result/{recordingId}",
            arguments = listOf(
                navArgument("recordingId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getString("recordingId") ?: return@composable
            ResultScreen(
                recordingId = recordingId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}