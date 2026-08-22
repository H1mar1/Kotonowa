package com.example.kotonowa.presentation.calendar.edit

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.Instant

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
        onStartDateClick = { viewModel.onPickerOpen(PickerTarget.START_DATE) },
        onStartTimeClick = { viewModel.onPickerOpen(PickerTarget.START_TIME) },
        onEndDateClick = { viewModel.onPickerOpen(PickerTarget.END_DATE) },
        onEndTimeClick = { viewModel.onPickerOpen(PickerTarget.END_TIME) },
        onStartDateChange = viewModel::onStartDateChange,
        onStartTimeChange = viewModel::onStartTimeChange,
        onEndDateChange = viewModel::onEndDateChange,
        onEndTimeChange = viewModel::onEndTimeChange,
        onPickerDismiss = viewModel::onPickerDismiss,
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
    onStartDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onStartTimeChange: (LocalTime) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onEndTimeChange: (LocalTime) -> Unit,
    onPickerDismiss: () -> Unit,
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
            )
            {
                Text("終日")
                Switch(
                    checked = uiState.allDay,
                    onCheckedChange = onAllDayChange,
                )
            }
        }

        when (uiState.itemType) {
            ScheduleItemType.EVENT -> {
                DateTimeField(
                    label = "開始",
                    date = uiState.startDate,
                    time = uiState.startTime,
                    showTime = !uiState.allDay,
                    onDateClick = onStartDateClick,
                    onTimeClick = onStartTimeClick,
                )
                DateTimeField(
                    label = "終了",
                    date = uiState.endDate,
                    time = uiState.endTime,
                    showTime = !uiState.allDay,
                    onDateClick = onEndDateClick,
                    onTimeClick = onEndTimeClick,
                )
            }

            ScheduleItemType.TASK -> {
                // TODO: 期限の DateTimeField を出す
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

    // 開いている旗を見て、対応するダイアログを出す。
    // ダイアログは画面の場所を取らない（別の窓に出る）ので Column の外に置く。
    when (uiState.pickerTarget) {
        PickerTarget.START_DATE -> DateSelectDialog(
            initialDate = uiState.startDate,
            onConfirm = onStartDateChange,
            onDismiss = onPickerDismiss,
        )

        PickerTarget.END_DATE -> DateSelectDialog(
            initialDate = uiState.endDate,
            onConfirm = onEndDateChange,
            onDismiss = onPickerDismiss,
        )

        PickerTarget.START_TIME -> TimeSelectDialog(
            initialTime = uiState.startTime,
            onConfirm = onStartTimeChange,
            onDismiss = onPickerDismiss,
        )

        PickerTarget.END_TIME -> TimeSelectDialog(
            initialTime = uiState.endTime,
            onConfirm = onEndTimeChange,
            onDismiss = onPickerDismiss,
        )

        null -> {}
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

/**
 * 日付を選ぶダイアログ。
 *
 * ぐるぐる選んでいる途中の値はこのダイアログ自身が持ち（§3-(82)）、
 * OK が押されたときだけ [onConfirm] で外に渡す。キャンセルなら途中の値は捨てる。
 *
 * @param initialDate ダイアログを開いたときに最初に選ばれている日
 * @param onConfirm OK が押されたときに、選ばれた日を渡して呼ぶ呼び鈴
 * @param onDismiss ダイアログを閉じたいときに鳴らす呼び鈴
 */
@Composable
private fun DateSelectDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onConfirm(date)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },

        ) {
        DatePicker(state = pickerState)

    }
}

/**
 * 時刻を選ぶダイアログ。
 *
 * [DateSelectDialog] と役割は同じで、扱うものが日付ではなく時刻というだけ。
 * 選んでいる途中の値はこのダイアログ自身が持ち（§3-(82)）、
 * OK が押されたときだけ [onConfirm] で外に渡す。
 *
 * 日付のときのようなエポックミリ秒の変換は要らない。
 * [TimePickerState] は「何時」「何分」をそのままの数で持っているため。
 *
 * @param initialTime ダイアログを開いたときに最初に選ばれている時刻
 * @param onConfirm OK が押されたときに、選ばれた時刻を渡して呼ぶ呼び鈴
 * @param onDismiss ダイアログを閉じたいときに鳴らす呼び鈴
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )


    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text("時刻を選択") },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(pickerState.hour, pickerState.minute))
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
    ) {
        TimePicker(state = pickerState)
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
            onStartDateClick = {},
            onStartTimeClick = {},
            onEndDateClick = {},
            onEndTimeClick = {},
            onStartDateChange = {},
            onStartTimeChange = {},
            onEndDateChange = {},
            onEndTimeChange = {},
            onPickerDismiss = {},
            onSaveClick = {},
            onNavigateBack = {},
        )
    }
}
