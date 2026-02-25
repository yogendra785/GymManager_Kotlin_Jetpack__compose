package com.example.gymmanager.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel // 👈 Make sure this is imported!
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymmanager.screen.AddMemberScreen
import com.example.gymmanager.screen.DashboardScreen
import com.example.gymmanager.screen.EditMemberScreen
import com.example.gymmanager.screen.ExpiringMembersScreen
import com.example.gymmanager.screen.LoginScreen
import com.example.gymmanager.screen.MemberListScreen
import com.example.gymmanager.viewmodel.MemberViewModel // 👈 And this!
import com.google.firebase.auth.FirebaseAuth

@Composable
fun GymNavigation() {
    val navController = rememberNavController()

    // 👇 THIS IS THE MAGIC! We create ONE master ViewModel here for the whole app.
    val sharedViewModel: MemberViewModel = viewModel()

    val auth = FirebaseAuth.getInstance()
    val startingScreen = if(auth.currentUser !=null) "dashboard" else "login"

    NavHost(navController = navController, startDestination = startingScreen) {

        composable("login") {
            LoginScreen(navController = navController)
        }

        composable("dashboard") {
            // 👇 Pass the shared ViewModel into the screen!
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
                // 👇 Because it uses the Shared ViewModel, the list is already loaded and it finds the member instantly!
                EditMemberScreen(navController = navController, memberId = memberId, viewModel = sharedViewModel)
            }
        }
    }
}