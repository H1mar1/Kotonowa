package com.example.kotonowa.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kotonowa.presentation.auth.login.LoginScreen
import com.example.kotonowa.presentation.auth.passwordreset.PasswordResetScreen
import com.example.kotonowa.presentation.auth.signup.SignUpScreen
import com.example.kotonowa.presentation.calendar.CalendarScreen
import com.example.kotonowa.presentation.calendar.detail.ScheduleDetailScreen
import com.example.kotonowa.presentation.calendar.edit.ScheduleEditScreen
import com.example.kotonowa.presentation.calendarlist.CalendarListScreen
import com.example.kotonowa.presentation.calendarlist.create.CalendarCreateScreen
import com.example.kotonowa.presentation.calendarlist.members.MemberListScreen
import com.example.kotonowa.presentation.settings.SettingsScreen
import com.example.kotonowa.presentation.splash.SplashScreen


object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SCHEDULE_EDIT = "schedule_edit"

    // Step 19-A: 編集用ルート（SCHEDULE_DETAIL / scheduleDetail と同じ形）
    const val SCHEDULE_EDIT_ITEM = "schedule_edit/{itemId}"

    fun scheduleEditItem(itemId: String) = "schedule_edit/$itemId"

    const val SCHEDULE_DETAIL = "schedule_detail/{itemId}"

    fun scheduleDetail(itemId: String) = "schedule_detail/$itemId"

    const val SIGN_UP = "sign_up"

    const val PASSWORD_RESET = "password_reset"

    const val SPLASH = "splash"

    const val SETTING = "setting"

    const val CALENDAR_LIST = "calendar_list"

    // TODO(32-D-1) カレンダー作成画面の住所（grammar §3-(117)）
    const val CALENDAR_CREATE = "calendar_create"

    // Step 34-B: メンバー画面の住所。SCHEDULE_DETAIL / scheduleDetail と同じ形（grammar §3-(124)）
    // TODO(34-B-1) { } の中は「ここに id が入る」という穴の名前。34-C の頭脳がこの名前で受け取る
    const val CALENDAR_MEMBERS = "calendar_members/{calendarId}"

    // TODO(34-B-1) 誰の：この関数の ／ 何をする：受け取った id を住所の文字に差し込む（§6-㉖）
    fun calendarMembers(calendarId: String) = "calendar_members/$calendarId"
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
            CalendarScreen(
                onAddClick = { navController.navigate((Routes.SCHEDULE_EDIT)) },
                onItemClick = { id -> navController.navigate(Routes.scheduleDetail(id)) },
                onSetting = { navController.navigate(Routes.SETTING) },
                onCreateCalendarClick = { navController.navigate(Routes.CALENDAR_CREATE) },
                onManageCalendarsClick = { navController.navigate(Routes.CALENDAR_LIST) },
            )
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

        composable(Routes.SCHEDULE_EDIT) {
            ScheduleEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaved = {
                    navController.popBackStack()
                }

            )
        }

        composable(
            route = Routes.SCHEDULE_DETAIL,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
            ),
        ) {
            ScheduleDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onEditClick = { editId -> navController.navigate(Routes.scheduleEditItem(editId)) },
            )
        }

        composable(
            route = Routes.SCHEDULE_EDIT_ITEM,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
            ),
        ) {
            ScheduleEditScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTING) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CALENDAR_LIST) {
            CalendarListScreen(
                onNavigateBack = { navController.popBackStack() },
                // TODO(32-D-2) 「＋」が押されたら作成画面の住所へ行く（§3-(117)）
                onAddClick = { navController.navigate(Routes.CALENDAR_CREATE) },
                // TODO(34-D-6) 誰の：案内係の ／ 何をする：押されたカレンダーの id を載せた住所へ行く
                //   住所は Routes の「id を差し込む関数」で作る（34-B-1）。お手本は HOME の onItemClick
                onCalendarClick = { calendarId -> navController.navigate(Routes.calendarMembers(calendarId)) },
            )
        }

        // TODO(32-D-3) 作成画面を地図に登録する。保存しても戻っても一覧へ 1 枚戻す（§3-(117)）
        composable(Routes.CALENDAR_CREATE) {
            CalendarCreateScreen(
                onSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // Step 34-D: メンバー画面を地図に登録する。お手本は SCHEDULE_DETAIL
        composable(
            // TODO(34-D-7) 受ける住所は「型」の方（{calendarId} の穴が開いている const val）
            route = Routes.CALENDAR_MEMBERS,
            arguments = listOf(
                // TODO(34-D-7) 穴の名前と、中身が文字であること。名前は 34-B-1 の { } の中と同じ綴り
                navArgument("calendarId") { type = NavType.StringType },
            ),
        ) {
            MemberListScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
