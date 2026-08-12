package com.example.kotonowa.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kotonowa.presentation.auth.login.LoginScreen
import com.example.kotonowa.presentation.auth.passwordreset.PasswordResetScreen
import com.example.kotonowa.presentation.auth.signup.SignUpScreen
import com.example.kotonowa.presentation.calendar.CalendarScreen
import com.example.kotonowa.presentation.splash.SplashScreen


object Routes {
    const val LOGIN = "login"
    const val HOME = "home"

    const val SIGN_UP = "sign_up"

    const val PASSWORD_RESET = "password_reset"

    const val SPLASH = "splash"
}

/**
 * アプリ全体の画面遷移を定義する場所。
 *
 * どの画面からどこへ行けるかがこの1ファイルに集まるので、
 * 画面が増えても迷子になりにくい。
 */
@Composable
fun KotonowaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNotLoggedIn = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }

                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        // ログイン画面を履歴から消し、戻るボタンで戻れないようにする
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP)
                },

                onNavigateToPasswordReset = {
                    navController.navigate(Routes.PASSWORD_RESET)
                },
            )
        }

        composable(Routes.HOME) {
            CalendarScreen()
        }

        composable(Routes.PASSWORD_RESET) {
            PasswordResetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
            )
        }
    }
}
