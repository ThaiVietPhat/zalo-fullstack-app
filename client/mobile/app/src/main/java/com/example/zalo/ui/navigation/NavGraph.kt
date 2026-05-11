package com.example.zalo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.zalo.data.local.TokenManager
import com.example.zalo.ui.auth.LoginScreen
import com.example.zalo.ui.auth.RegisterScreen
import com.example.zalo.ui.chat.ChatScreen
import com.example.zalo.ui.chat.GroupChatScreen
import com.example.zalo.ui.home.HomeScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CHAT = "chat/{chatId}"
    const val GROUP_CHAT = "group/{groupId}"
    const val PROFILE = "profile"
    const val CREATE_GROUP = "create_group"
    
    fun chatRoute(chatId: String) = "chat/$chatId"
    fun groupChatRoute(groupId: String) = "group/$groupId"
}

@Composable
fun ZaloNavGraph(
    navController: NavHostController,
    tokenManager: TokenManager
) {
    val startDestination = if (tokenManager.getToken() != null) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }
        
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onChatClick = { chatId ->
                    navController.navigate(Routes.chatRoute(chatId))
                },
                onGroupClick = { groupId ->
                    navController.navigate(Routes.groupChatRoute(groupId))
                },
                onSearchClick = { /* TODO */ },
                onProfileClick = {
                    navController.navigate(Routes.PROFILE)
                },
                onCreateGroupClick = {
                    navController.navigate(Routes.CREATE_GROUP)
                }
            )
        }

        composable(Routes.PROFILE) {
            com.example.zalo.ui.user.ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATE_GROUP) {
            com.example.zalo.ui.group.CreateGroupScreen(
                onBack = { navController.popBackStack() },
                onGroupCreated = { groupId ->
                    navController.popBackStack()
                    navController.navigate(Routes.groupChatRoute(groupId))
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() },
                tokenManager = tokenManager
            )
        }

        composable(
            route = Routes.GROUP_CHAT,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupChatScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                tokenManager = tokenManager
            )
        }
    }
}
