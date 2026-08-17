package com.example.kotonowa.presentation.calendar.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 予定/タスクの作成画面。
 *
 * ViewModel から状態を受け取り、操作を ViewModel に伝えるだけの役割。
 * 実際の見た目は [ScheduleEditContent] が描く（ViewModel を持たないので Preview で確認できる）。
 *
 * @param onSaved 保存が終わったときに呼ばれる呼び鈴。前の画面に戻るのに使う。
 * @param onNavigateBack 「戻る」を押したときに呼ばれる呼び鈴。
 */

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d(E)")
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ScheduleEditScreen(
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    ScheduleEditContent(
        uiState = uiState,
        onItemTypeChange = viewModel::onItemTypeChange,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAllDayChange = viewModel::onAllDayChange,
        onSaveClick = viewModel::save,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

/**
 * 作成画面の見た目。
 *
 * ViewModel を受け取らず、表示に必要なものを引数でもらうだけにしてある。
 * こうしておくと [ScheduleEditContentPreview] で見た目だけ確認できる。
 */
@Composable
private fun ScheduleEditContent(
    uiState: ScheduleEditUiState,
    onItemTypeChange: (ScheduleItemType) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAllDayChange: (Boolean) -> Unit,
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

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = uiState.itemType == ScheduleItemType.EVENT,
                onClick = { onItemTypeChange(ScheduleItemType.EVENT) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("予定")
            }
            SegmentedButton(
                selected = uiState.itemType == ScheduleItemType.TASK,
                onClick = { onItemTypeChange(ScheduleItemType.TASK) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("タスク")
            }
        }

        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChange,
            label = { Text("タイトル") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("メモ") },
            modifier = Modifier.fillMaxWidth(),
        )

        // 終日は「予定」だけの考え方。タスクの期限は一点の時刻なので出さない
        if (uiState.itemType == ScheduleItemType.EVENT) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("終日")
                Switch(
                    checked = uiState.allDay,
                    onCheckedChange = onAllDayChange,
                )
            }
        }

        val message = uiState.errorMessage
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onSaveClick,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }

        TextButton(
            onClick = onNavigateBack,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("戻る")
        }
    }
}

/**
 * 「ラベル ｜ 日付 ｜ 時刻」の 1 行。
 *
 * 予定の開始/終了とタスクの期限で使い回す。
 * 押されたことを伝えるだけで、ダイアログを出すのは呼ぶ側の仕事。
 */
@Composable
private fun DateTimeField(
    label: String,
    date: LocalDate,
    time: LocalTime,
    showTime: Boolean,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDateClick) {
                Text(date.format(DATE_FORMATTER))
            }
            if (showTime) {
                TextButton(onClick = onTimeClick) {
                    Text(time.format(TIME_FORMATTER))
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleEditContentPreview() {
    KotonowaTheme {
        ScheduleEditContent(
            uiState = ScheduleEditUiState(title = "打ち合わせ"),
            onItemTypeChange = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onAllDayChange = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}
