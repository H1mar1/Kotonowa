package com.example.kotonowa.data.local

import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.repository.SelectedCalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SelectedCalendarRepository] の約束を、端末のメモリだけで果たすクラス。
 *
 * Firestore にも端末の保存領域にも書かない。アプリを閉じると白紙（個人）に戻る。
 * `@Singleton` なので、アプリの中にこの掲示板は 1 枚しか無い（grammar §4-(119)）。
 * 2 枚あると、ホームと作成画面が別々の掲示板を見てしまい、意味が無くなる。
 */
@Singleton
class SelectedCalendarRepositoryImpl @Inject constructor() : SelectedCalendarRepository {

    // TODO(33-A-2) 書き換えられる版。最初は白紙（grammar §4-(58)、§4-(119) の <型> の明示）
    private val _selected = MutableStateFlow<Calendar?>(null)

    // TODO(33-A-2) 外へは「読むだけ」の姿で見せる（§4-(58)）
    override val selected: StateFlow<Calendar?> = _selected.asStateFlow()

    override fun select(calendar: Calendar?) {
        // TODO(33-A-2) 掲示板を、渡されたカレンダーに貼り替える（§4-(119)）
        _selected.value = calendar
    }

    override fun clear() {
        // TODO(33-A-2) 掲示板を白紙に戻す
        _selected.value = null
    }
}
