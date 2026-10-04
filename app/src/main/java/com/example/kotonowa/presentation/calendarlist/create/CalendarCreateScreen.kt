package com.example.kotonowa.presentation.calendarlist.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.ui.theme.KotonowaTheme

/**
 * カレンダー作成画面。ViewModel 係。
 *
 * お手本は `presentation/calendar/edit/ScheduleEditScreen`（Screen ＋ Content の 2 段構え）。
 * 画面は `navController` を受け取らず、呼び鈴（`() -> Unit`）だけを持つ。
 *
 * @param onSaved 保存が終わったときに鳴らす呼び鈴。NavHost が一覧へ戻す。
 * @param onNavigateBack 「戻る」を押したときに鳴らす呼び鈴。
 */
@Composable
fun CalendarCreateScreen(
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // TODO(32-C-1) 保存が終わった旗を見張り、立ったら呼び鈴を鳴らす（§3-⑬）
    LaunchedEffect(uiState.isSaved) {
        if (____) onSaved()
    }

    // TODO(32-C-1) ViewModel の関数を、Content の呼び鈴に繋ぐ（§4-⑲）
    CalendarCreateContent(
        uiState = uiState,
        onNameChange = ____,
        onColorChange = ____,
        onSaveClick = ____,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

/**
 * 作成画面の見た目。ViewModel を受け取らないので `@Preview` で確認できる。
 */
@Composable
private fun CalendarCreateContent(
    uiState: CalendarCreateUiState,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("カレンダーを作成", style = MaterialTheme.typography.titleLarge)

        // TODO(32-C-2) 名前の入力欄。部品は状態を持たない（§3-(76)）
        OutlinedTextField(
            value = ____,
            onValueChange = ____,
            label = { Text("名前") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("色", style = MaterialTheme.typography.titleMedium)

        // TODO(32-C-3) 色の候補を 1 つずつ丸にして横に並べる（§4-(114)）
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CALENDAR_COLORS.forEach { hex ->
                val isSelected = ____

                Surface(
                    onClick = { ____ },                    // §3-(116)
                    shape = CircleShape,
                    color = Color(hex.toColorInt()),       // §3-(115)
                    border = if (isSelected) {
                        BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                    } else {
                        null
                    },
                    modifier = Modifier.size(36.dp),
                ) { }
            }
        }

        val message = uiState.errorMessage
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        // TODO(32-C-4) 通信中は押せなくする
        Button(
            onClick = onSaveClick,
            enabled = ____,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存")
        }

        TextButton(
            onClick = onNavigateBack,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("戻る")
        }
    }
}

@Preview(showBackground = true, name = "開いた直後")
@Composable
private fun CalendarCreateContentPreview() {
    KotonowaTheme {
        CalendarCreateContent(
            uiState = CalendarCreateUiState(),
            onNameChange = {},
            onColorChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "入力済み・青を選択・エラー")
@Composable
private fun CalendarCreateContentFilledPreview() {
    KotonowaTheme {
        CalendarCreateContent(
            uiState = CalendarCreateUiState(
                name = "家族",
                color = CALENDAR_COLORS[1],
                errorMessage = "保存できませんでした",
            ),
            onNameChange = {},
            onColorChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}
