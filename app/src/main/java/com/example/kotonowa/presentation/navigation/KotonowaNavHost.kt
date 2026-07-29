package com.example.kotonowa.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kotonowa.presentation.auth.login.LoginScreen
import com.example.kotonowa.presentation.home.HomeScreen
import  com.example.kotonowa.presentation.auth.signup.SignUpScreen

/** 画面を表す行き先の名前。文字列を直接書くとタイプミスに気づけないため定数にする。 */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"

    const val SIGN_UP = "sign_up"
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
    // TODO(Step 11): スプラッシュ画面を作り、すでにログイン済みなら HOME から始める
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = modifier,
    ) {
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
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
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
