package com.example.kotonowa.presentation.chat.list

import com.example.kotonowa.domain.model.Calendar

/**
 * トーク一覧の状態（Step 49-E）。画面はこれを見て描くだけ。
 *
 * ① ではグループのトークだけなので、行＝自分が入っている共有カレンダー 1 つ。
 * ③ で DM が入ったら、行の型を「グループか DM か」を持てる形に作り替える。
 *
 * @property rooms トークの行（自分が入っている共有カレンダー）
 * @property isLoading 最初の読み込み中か（開いた瞬間に見張りが始まるので true 始まり。Step 31 と同じ）
 * @property errorMessage 読めなかったときの一言
 */
data class ChatListUiState(
    // TODO(49-E-1) トークの行。型はカレンダー（Calendar）の並び、最初は空の並び（49-D-1 と同じ形。中身の型だけ違う）
    val rooms: List<Calendar> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
