package com.example.kotonowa.presentation.calendar

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * カレンダー画面。
 *
 * Phase2 前半は「今月の予定/タスクを縦に並べるだけ」の最小構成。
 * 月の升目（カレンダーらしい見た目）は後のステップで足す。
 *
 * 画面は [CalendarUiState] を見て描くだけで、
 * 取得やエラー判断は [CalendarViewModel] が行う。
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    // TODO 10: ViewModel の状態を受け取る（手本: LoginScreen.kt:65）
    //   val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    //   by は §3-⑫。import が 2 つ要る（getValue と collectAsStateWithLifecycle）

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            // TODO 11: FloatingActionButton を置く
            //   onClick で viewModel.addDummyItem() を呼ぶ
            //   中身は Icon(Icons.Default.Add, contentDescription = "予定を追加")
        },
    ) { innerPadding ->
        // TODO 12: uiState の中身によって表示を出し分ける（§5-(64) の「主語なし when」）
        //   isLoading が true      → CircularProgressIndicator（ぐるぐる）
        //   errorMessage が null 以外 → Text でエラー文言
        //   items が空             → Text("今月の予定はありません")
        //   それ以外               → 16-D-3 で一覧（LazyColumn）を書く
        //
        //   どの枝でも Modifier に .padding(innerPadding) を付けること（理由は解説）
    }
}
