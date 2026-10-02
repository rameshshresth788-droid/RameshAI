package com.rameshai.ui

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Memory : Screen("memory")
    object Reminders : Screen("reminders")
    object Routine : Screen("routine")
}
