package com.example.kotonowa.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.kotonowa.presentation.chat.list.ChatListScreen
import com.example.kotonowa.presentation.chat.room.ChatRoomScreen
import com.example.kotonowa.presentation.common.MainBottomBar
import com.example.kotonowa.presentation.common.MainTab
import com.example.kotonowa.presentation.invites.InviteListScreen
import com.example.kotonowa.presentation.settings.SettingsScreen
import com.example.kotonowa.presentation.settings.notification.NotificationSettingsScreen
import com.example.kotonowa.presentation.splash.SplashScreen
import com.example.kotonowa.presentation.tasks.TaskListScreen


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

    // Step 36-F: 届いた招待の一覧画面の住所。id は要らない（自分宛ての招待は頭脳がログイン情報から探す）
    // TODO(36-F-1) CALENDAR_CREATE と同じ形。名前は INVITES、中身の文字は "invites"
    const val INVITES = "invites"

    // Step 45: 通知設定画面の住所。id は要らない（自分の設定は頭脳がログイン情報から探す）
    // TODO(45-C-1) INVITES と同じ形。名前は NOTIFICATION_SETTINGS、中身の文字は "notification_settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"

    // Step 47-E: タスク一覧画面の住所。id は要らない（全カレンダーのタスクは頭脳がログイン情報から集める）
    // TODO(47-E-4) NOTIFICATION_SETTINGS と同じ形。名前は TASKS、中身の文字は "tasks"
    const val TASKS = "tasks"

    // Step 49-F: トーク一覧の住所。id は要らない（入っているカレンダーは頭脳がログイン情報から集める）
    // TODO(49-F-2) TASKS と同じ形。名前は CHAT_LIST、中身の文字は "chat_list"
    const val CHAT_LIST="chat_list"

    // Step 49-F: トーク画面の住所。どのカレンダーのトークかを id で受け取る（CALENDAR_MEMBERS / calendarMembers と同じ形）
    //   { } の中の名前は、ChatRoomViewModel が savedStateHandle["calendarId"] で受け取る名前と同じ綴りにする
    const val CHAT_ROOM = "chat_room/{calendarId}"

    // TODO(49-F-3) 誰の：この関数の ／ 何をする：受け取った id を住所の文字に差し込む（calendarMembers と同じ形。§6-㉖）
    fun chatRoom(calendarId: String) = "chat_room/$calendarId"

    // Step 51-F: DM のトーク画面の住所。どの DM の部屋かを id で受け取る（CHAT_ROOM と同じ形）
    //   { } の中の名前は、ChatRoomViewModel が savedStateHandle["chatId"] で受け取る名前と同じ綴りにする
    // TODO(51-F-1) 名前は DM_ROOM、中身の文字は "dm_room/{chatId}"（すぐ上の CHAT_ROOM と同じ形）
    const val DM_ROOM = "dm_room/{chatId}"

    // TODO(51-F-2) 受け取った chatId を住所の文字に差し込む（すぐ上の chatRoom と同じ形。名前は dmRoom）
    fun dmRoom(chatId: String) = "dm_room/$chatId"
}

/**
 * 下のタブが押されたときの行き先（Step 47-E。grammar §3-(146)）。Claude が書いた。
 *
 * 履歴はいつも [HOME] か [HOME, TASKS] か [HOME, CHAT_LIST] にする（タブを押すたびに積み上げない）。
 *  ・カレンダー … HOME まで戻る（HOME は消さない）
 *  ・タスク／チャット … HOME より上を片付けてから行く。すでにそれが一番上なら作り直さない
 *    （トーク画面 [HOME, CHAT_LIST, CHAT_ROOM] からタスクへ行くときも、トーク画面ごと片付く）
 * 地図の係（NavHostController）に足した拡張関数なので、navController.navigateToTab(tab) と呼べる（§4-㊱）。
 */
private fun NavHostController.navigateToTab(tab: MainTab) {
    when (tab) {
        MainTab.CALENDAR -> popBackStack(Routes.HOME, inclusive = false)
        MainTab.TASKS -> navigate(Routes.TASKS) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
        }
        // Step 49-F（Claude が書いた）。タスクと同じ形で、行き先だけ CHAT_LIST
        MainTab.CHAT -> navigate(Routes.CHAT_LIST) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
        }
    }
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
    // Step 50：チャットの通知から開かれたときの、開きたいトークのカレンダー id（無ければ null）
    openChatCalendarId: String? = null,
    // Step 50：トークを開き終わったら鳴らす（MainActivity が null に戻す）
    onChatOpened: () -> Unit = {},
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
            // Step 50：チャットの通知から開かれたら、ホームに着いたところでトーク画面へ進む（Claude が書いた）。
            // ホームで待つのは、起動直後はスプラッシュ→ログイン確認→ホームと進むため（ログインしていなければホームに来ない）。
            // 履歴は [HOME, CHAT_LIST, CHAT_ROOM] にする（「戻る」でトーク一覧 → ホーム）
            LaunchedEffect(openChatCalendarId) {
                val id = openChatCalendarId ?: return@LaunchedEffect
                navController.navigateToTab(MainTab.CHAT)
                // TODO(50-F-4) 誰の：地図の係（navController）の ／ 何をする：id を差し込んだトーク画面の住所へ行く
                //   49-F-4 で書いた onRoomClick の中身と同じ 1 行
                navController.navigate(Routes.chatRoom(id))
                onChatOpened()
            }

            CalendarScreen(
                onAddClick = { navController.navigate((Routes.SCHEDULE_EDIT)) },
                onItemClick = { id -> navController.navigate(Routes.scheduleDetail(id)) },
                onSetting = { navController.navigate(Routes.SETTING) },
                onCreateCalendarClick = { navController.navigate(Routes.CALENDAR_CREATE) },
                onManageCalendarsClick = { navController.navigate(Routes.CALENDAR_LIST) },
                // TODO(36-F-3) 誰の：地図の係（navController）の ／ 何をする：招待一覧の住所へ行く
                //   すぐ上の onManageCalendarsClick と同じ形
                onInvitesClick = { navController.navigate(Routes.INVITES) },
                // Step 47-E: 画面の下のタブ。お手本はすぐ下の TASKS の bottomBar（選ばれているタブだけが違う）
                bottomBar = {
                    MainBottomBar(
                        // TODO(47-E-7) ここはカレンダー画面。選ばれているのはどのタブか（MainTab の 2 つのどちらか）
                        selected = MainTab.CALENDAR,
                        // TODO(47-E-8) 誰の：地図の係（navController）の ／ 何をする：押されたタブ（tab）の行き先へ行く（上の navigateToTab）
                        onTabSelected = { tab ->  navController.navigateToTab(tab) },
                    )
                },
            )
        }

        // Step 47-E: タスク一覧画面
        composable(Routes.TASKS) {
            TaskListScreen(
                // TODO(47-E-9) 行が押されたら、そのタスクの詳細画面へ（HOME の onItemClick とまったく同じ 1 行）
                onItemClick = { id -> navController.navigate(Routes.scheduleDetail(id)) },
                bottomBar = {
                    MainBottomBar(
                        selected = MainTab.TASKS,
                        onTabSelected = { tab -> navController.navigateToTab(tab) },
                    )
                },
            )
        }

        // Step 49-F: トーク一覧（下のタブの 3 つ目）。お手本はすぐ上の TASKS
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                // TODO(49-F-4) 誰の：地図の係（navController）の ／ 何をする：押された行の id を差し込んだトーク画面の住所へ行く
                //   住所は 49-F-3 の chatRoom で作る。お手本は TASKS の onItemClick（scheduleDetail が chatRoom に変わる）
                onRoomClick = { id -> navController.navigate(Routes.chatRoom(id)) },
                bottomBar = {
                    MainBottomBar(
                        // TODO(49-F-5) ここはチャット画面。選ばれているのはどのタブか（TASKS の bottomBar と比べる）
                        selected = MainTab.CHAT,
                        onTabSelected = { tab -> navController.navigateToTab(tab) },
                    )
                },
            )
        }

        // Step 49-F: トーク画面。お手本は下の CALENDAR_MEMBERS（id の穴がある住所）。下のタブは出さない（LINE と同じ）
        composable(
            route = Routes.CHAT_ROOM,
            arguments = listOf(
                // TODO(49-F-6) 穴の名前と、中身が文字であること（34-D-7 と同じ形。名前は CHAT_ROOM の { } の中と同じ綴り）
                navArgument("calendarId") { type = NavType.StringType },
            ),
        ) {
            ChatRoomScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // Step 51-F: DM のトーク画面。画面も頭脳も CHAT_ROOM と同じもの（頭脳が住所の穴の名前で「グループか DM か」を見分ける）
        composable(
            route = Routes.DM_ROOM,
            arguments = listOf(
                // TODO(51-F-3) 穴の名前と、中身が文字であること（すぐ上の CHAT_ROOM の navArgument と同じ形。名前は DM_ROOM の { } の中と同じ綴り）
                navArgument("chatId") { type = NavType.StringType },
            ),
        ) {
            ChatRoomScreen(
                onNavigateBack = { navController.popBackStack() },
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
                // TODO(45-C-3) 「通知設定」が押されたら、通知設定画面の住所へ行く（すぐ下の CALENDAR_LIST の onAddClick と同じ形）
                onNotificationSettingsClick = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        // Step 45：通知設定画面。戻るは popBackStack（設定画面へ戻る）
        composable(Routes.NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CALENDAR_LIST) {
            CalendarListScreen(
                onNavigateBack = { navController.popBackStack() },
                // TODO(32-D-2) 「＋」が押されたら作成画面の住所へ行く（§3-(117)）
                onAddClick = { navController.navigate(Routes.CALENDAR_CREATE) },
                // TODO(34-D-6) 誰の：案内係の ／ 何をする：押されたカレンダーの id を載せた住所へ行く
                //   住所は Routes の「id を差し込む関数」で作る（34-B-1）。お手本は HOME の onItemClick
                onCalendarClick = { calendarId ->
                    navController.navigate(
                        Routes.calendarMembers(
                            calendarId
                        )
                    )
                },
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
                // TODO(51-G-11) 誰の：地図の係の ／ 何をする：受け取った id を差し込んだ DM の住所へ行く
                //   お手本はチャット一覧の onRoomClick = { id -> navController.navigate(Routes.chatRoom(id)) }。住所の係を 51-F-2 の dmRoom に変える
                onOpenDm = { id -> navController.navigate(Routes.dmRoom(id)) },
            )
        }

        // Step 36-F: 招待一覧画面を地図に登録する。お手本は CALENDAR_CREATE（id の穴が無いので arguments も要らない）
        // TODO(36-F-2) 住所は 36-F-1 で足したもの。「戻る」でホームへ 1 枚戻す
        composable(Routes.INVITES) {
            InviteListScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
