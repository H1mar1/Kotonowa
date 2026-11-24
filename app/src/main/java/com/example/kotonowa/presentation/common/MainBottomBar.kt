package com.example.kotonowa.presentation.common

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.kotonowa.ui.theme.KotonowaTheme

/**
 * 画面の下のタブの種類（Step 47-E）。2 択でも enum にすると、名前で読める（47-B の TaskFilter と同じ）。
 * Step 49-F で CHAT（チャット）を足した。足すと `when (tab)` の枝が足りない所がビルドで止まるので、直し忘れない。
 */
enum class MainTab { CALENDAR, TASKS, CHAT }

/**
 * 画面の下の「カレンダー／タスク／チャット」のタブ（Step 47-E。grammar §3-(145)）。
 *
 * カレンダー画面とタスク画面の両方の Scaffold の bottomBar に置く。
 * どちらが選ばれているかは外から受け取り、押されたら呼び鈴を鳴らすだけ。行き先は知らない（NavHost が決める）。
 *
 * @param selected 今いる画面のタブ
 * @param onTabSelected タブが押されたら、そのタブで鳴らす呼び鈴
 */
@Composable
fun MainBottomBar(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            // TODO(47-E-1) 選ばれているか：引数の selected が「カレンダー」と同じか（47-D-3 と同じ形。比べる相手は MainTab）
            selected = selected== MainTab.CALENDAR,
            // TODO(47-E-2) 押されたら：誰の：呼び鈴 onTabSelected を ／ 何をする：「カレンダー」を渡して鳴らす（47-D-4 と同じ形）
            onClick = { onTabSelected(MainTab.CALENDAR) },
            icon = { Text("📅") },
            label = { Text("カレンダー") },
        )
        NavigationBarItem(
            // TODO(47-E-3) 47-E-1 / 47-E-2 の「タスク」版
            selected = selected== MainTab.TASKS,
            onClick = { onTabSelected(MainTab.TASKS) },
            icon = { Text("✅") },
            label = { Text("タスク") },
        )
        NavigationBarItem(
            // TODO(49-F-1) 47-E-3 の「チャット」版。選ばれているか：selected が MainTab.CHAT と同じか ／
            //   押されたら：誰の：呼び鈴 onTabSelected を ／ 何をする：「チャット」を渡して鳴らす
            selected = selected== MainTab.CHAT,
            onClick = { onTabSelected(MainTab.CHAT) },
            icon = { Text("💬") },
            label = { Text("チャット") },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainBottomBarPreview() {
    KotonowaTheme {
        MainBottomBar(selected = MainTab.TASKS, onTabSelected = {})
    }
}
