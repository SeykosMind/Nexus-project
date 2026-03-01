package com.nexus.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.ui.screens.DashboardScreen
import com.nexus.ui.screens.SearchScreen
import com.nexus.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Search    : Screen("search")
    object Settings  : Screen("settings")
}

@Composable
fun NexusNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) { DashboardScreen(navController) }
        composable(Screen.Search.route)    { SearchScreen(navController) }
        composable(Screen.Settings.route)  { SettingsScreen(navController) }
    }
}
