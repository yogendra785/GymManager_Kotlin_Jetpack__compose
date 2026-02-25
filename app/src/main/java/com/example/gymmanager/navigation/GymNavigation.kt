package com.example.gymmanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymmanager.screen.AddMemberScreen
import com.example.gymmanager.screen.LoginScreen
import com.example.gymmanager.screen.DashboardScreen
import com.example.gymmanager.screen.ExpiringMembersScreen
import com.example.gymmanager.screen.MemberListScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun GymNavigation() {
    // This remembers the navigation state across screen rotations
    val navController = rememberNavController()

    val auth = FirebaseAuth.getInstance()
    val startingScreen = if(auth.currentUser !=null) "dashboard" else "login"

    // NavHost controls which screen to show based on the "route" string
    NavHost(navController = navController, startDestination = startingScreen) {

        composable("login") {
            // We pass the navController so the LoginScreen can trigger navigation later
            LoginScreen(navController = navController)
        }

        composable("dashboard") {

             DashboardScreen(navController = navController)
        }
        composable("add_member") {
            AddMemberScreen(navController = navController)
        }

        composable("member_list") {
            MemberListScreen(navController = navController)
        }
        composable("expiring_members") {
            ExpiringMembersScreen(navController = navController)
        }
    }
}