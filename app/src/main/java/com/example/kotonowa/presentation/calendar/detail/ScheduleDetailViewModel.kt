package com.example.kotonowa.presentation.calendar.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 予定/タスクの詳細画面の頭脳。
 *
 * 一覧の行から navigate されるとき、行き先のルートに itemId が埋め込まれている
 * （"schedule_detail/abc123"）。その abc123 を SavedStateHandle 経由で受け取り、
 * getItem で 1 件読み込んで画面に見せる。
 *
 * 削除できたら isDeleted を立てるだけ。画面遷移（一覧へ戻る）は知らない
 * ＝ ScheduleEditViewModel が isSaved を立てるだけなのと同じ分担。
 */
@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository, // 予定/タスクを出し入れする窓口
    savedStateHandle: SavedStateHandle, // NavHost が渡してくる「宛名付きメモ」
) : ViewModel() {

    // ルート "schedule_detail/{itemId}" の {itemId} に入っていた値。
    // 詳細画面は必ず itemId 付きで開かれるので、無かったらプログラムのバグ → checkNotNull で即落とす。
    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val _uiState = MutableStateFlow(ScheduleDetailUiState())
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    // ViewModel が作られた直後に 1 回だけ走る。開いた瞬間に読み込みを始めたいのでここで呼ぶ。
    init {
        load()
    }

    /** itemId の 1 件を読み込んで uiState.item に入れる。 */
    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {

            scheduleRepository.getItem(itemId)
                .onSuccess { item -> _uiState.update { it.copy(item = item, isLoading = false) } }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "読み込めません"
                        )
                    }
                }
        }
    }

    /** itemId の 1 件を削除する。成功したら isDeleted を立てる（画面がそれを見て戻る）。 */
    fun delete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

        viewModelScope.launch {
            scheduleRepository.deleteItem(itemId)
                .onSuccess { _uiState.update { it.copy(isDeleting = false, isDeleted = true) } }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "削除できませんでした"
                        )
                    }
                }
        }
    }

}