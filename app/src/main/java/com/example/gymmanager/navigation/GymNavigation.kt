package com.example.gymmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymmanager.screen.AddMemberScreen
import com.example.gymmanager.screen.DashboardScreen
import com.example.gymmanager.screen.EditMemberScreen
import com.example.gymmanager.screen.ExpiringMembersScreen
import com.example.gymmanager.screen.LoginScreen
import com.example.gymmanager.screen.MemberListScreen
import com.example.gymmanager.screen.SubscriptionLockScreen // 👈 Ensure this is imported
import com.example.gymmanager.viewmodel.MemberViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun GymNavigation() {
    val navController = rememberNavController()
    val sharedViewModel: MemberViewModel = viewModel()
    val auth = FirebaseAuth.getInstance()

    val startingScreen = if (auth.currentUser != null) "dashboard" else "login"

    // 👇 THE SAAS BOUNCER: Observe the lock state
    val isLocked by sharedViewModel.isSubscriptionLocked

    // If locked, and they aren't already on the login or lock screen, kick them out!
    LaunchedEffect(isLocked) {
        val currentRoute = navController.currentDestination?.route
        if (isLocked && currentRoute != "login" && currentRoute != "subscription_locked") {
            navController.navigate("subscription_locked") {
                popUpTo(0) // Wipes the backstack so they can't press "Back" to cheat
            }
        }
    }

    // Every time navigation starts, verify the subscription
    LaunchedEffect(Unit) {
        if (auth.currentUser != null) {
            sharedViewModel.verifySubscription()
        }
    }

    NavHost(navController = navController, startDestination = startingScreen) {

        composable("login") {
            LoginScreen(navController = navController)
        }

        // 👇 THE NEW LOCK SCREEN COMPOSABLE
        composable("subscription_locked") {
            SubscriptionLockScreen(
                onLogoutClick = {
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(navController = navController, viewModel = sharedViewModel)
        }

        composable("add_member") {
            AddMemberScreen(navController = navController, viewModel = sharedViewModel)
        }

        composable("member_list") {
            MemberListScreen(navController = navController, viewModel = sharedViewModel)
        }

        composable("expiring_members") {
            ExpiringMembersScreen(navController = navController, viewModel = sharedViewModel)
        }

        composable("edit_member/{memberId}") { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId")
            if (memberId != null) {
                EditMemberScreen(navController = navController, memberId = memberId, viewModel = sharedViewModel)
            }
        }
    }
}