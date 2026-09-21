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
import androidx.compose.ui.Modifier
import com.example.kotonowa.presentation.navigation.KotonowaNavHost
import com.example.kotonowa.ui.theme.KotonowaTheme
import dagger.hilt.android.AndroidEntryPoint

/** `@AndroidEntryPoint` を付けると、この Activity 配下で Hilt が材料を配れるようになる。 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KotonowaNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
