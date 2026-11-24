package com.example.kotonowa

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.kotonowa.data.remote.EXTRA_CHAT_CALENDAR_ID
import com.example.kotonowa.presentation.navigation.KotonowaNavHost
import com.example.kotonowa.ui.theme.KotonowaTheme
import dagger.hilt.android.AndroidEntryPoint

/** `@AndroidEntryPoint` を付けると、この Activity 配下で Hilt が材料を配れるようになる。 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Step 50：チャットの通知から開かれたなら、依頼書に書き添えられたカレンダーの id（grammar §8-(150)）。
        // savedInstanceState が null ＝「初めて作られた」。画面を回したときの作り直しでは読まない（同じトークへ何度も飛ばないため）
        val chatCalendarIdFromNotification: String? =
            if (savedInstanceState == null) {
                // TODO(50-F-3) 誰の：この Activity を開いた依頼書（intent）の ／ 何をする：名札 EXTRA_CHAT_CALENDAR_ID の文字を読む（getStringExtra(名札)）
                intent.getStringExtra(EXTRA_CHAT_CALENDAR_ID)
            } else {
                null
            }

        setContent {
            KotonowaTheme {

                // Android 13 以降は通知に許可が要る（grammar §8-(100)）。
                // いまは起動時に尋ねる。将来はリマインダーを設定した瞬間に尋ねる形へ寄せる。
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    // 拒否されても画面は使える。通知だけが出なくなる
                }

                // LaunchedEffect(Unit) で包まないと、画面が描き直されるたびにダイアログが出る（§8-(101)）
                LaunchedEffect(Unit) {
                    // POST_NOTIFICATIONS は Android 13 で追加された権限。
                    // minSdk 26 なので、古い端末で呼ばないよう世代で分ける（§8-(102)）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                // これから開くトーク（Step 50。Claude が書いた）。開いたら null に戻す。
                // rememberSaveable は画面を回しても覚えている remember（開き終わった null のまま保つ。grammar §8-(150)）
                var chatToOpen by rememberSaveable { mutableStateOf(chatCalendarIdFromNotification) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KotonowaNavHost(
                        modifier = Modifier.padding(innerPadding),
                        openChatCalendarId = chatToOpen,
                        onChatOpened = { chatToOpen = null },
                    )
                }
            }
        }
    }
}
