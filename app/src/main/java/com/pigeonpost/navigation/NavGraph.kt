package com.pigeonpost.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pigeonpost.data.repository.AuthRepository
import com.pigeonpost.ui.screens.auth.LoginScreen
import com.pigeonpost.ui.screens.chat.ChatScreen
import com.pigeonpost.ui.screens.conversations.ConversationsScreen
import com.pigeonpost.ui.screens.map.PigeonMapScreen
import com.pigeonpost.ui.screens.newchat.NewChatScreen
import com.pigeonpost.ui.screens.splash.SplashScreen

/**
 * Main navigation graph for the PigeonPost app.
 * Routes: Splash -> (Login | Conversations) -> (NewChat) -> Chat -> PigeonMap
 *
 * The splash screen checks authentication state and navigates directly
 * to Conversations if the user already has a valid session.
 */
@Composable
fun PigeonPostNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route
    ) {
        composable(Routes.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    val destination = if (authRepository.isAuthenticated()) {
                        Routes.Conversations.route
                    } else {
                        Routes.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Conversations.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Conversations.route) {
            ConversationsScreen(
                onConversationClick = { userId ->
                    navController.navigate(Routes.Chat.createRoute(userId))
                },
                onNewConversation = {
                    navController.navigate(Routes.NewChat.route)
                },
                onSignOut = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Conversations.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.NewChat.route) {
            NewChatScreen(
                onUserClick = { userId ->
                    // Replace the picker in the back stack so returning from the
                    // chat lands back on The Aviary, which then refreshes.
                    navController.navigate(Routes.Chat.createRoute(userId)) {
                        popUpTo(Routes.NewChat.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.Chat.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ChatScreen(
                otherUserId = userId,
                onNavigateToMap = { messageId ->
                    navController.navigate(Routes.PigeonMap.createRoute(messageId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.PigeonMap.route,
            arguments = listOf(navArgument("messageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
            PigeonMapScreen(
                messageId = messageId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
