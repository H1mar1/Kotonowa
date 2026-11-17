package com.example.kotonowa.presentation.calendar

import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.ScheduleItem
import java.time.LocalDate
import java.time.YearMonth

/**
 * カレンダー画面の「今の状態」をまとめて表したもの。
 *
 * 画面（Composable）はこの箱だけを見て描く。
 * 中身を書き換えるのは CalendarViewModel の仕事。
 */
data class CalendarUiState(

    val items: List<ScheduleItem> = emptyList(),

    /** 予定/タスクがある日。升目に点を打つのに使う。 */
    val datesWithItems: Set<LocalDate> = emptySet(),

    val currentMonth: YearMonth=YearMonth.now(),

    val selectedDate: LocalDate= LocalDate.now(),

    /** 今表示しているカレンダーの名前。上のバーに出す。掲示板が白紙のときは個人（Step 33-C）。 */
    val calendarName: String = "マイカレンダー",

    /**
     * 今表示しているカレンダーの id。マイカレンダー（掲示板が白紙）なら null。
     * ▼ のメニューで、どの行に ✓ を付けるかの判定に使う（Step 33-H）。
     */
    val selectedCalendarId: String? = null,

    /** 自分が入っている共有カレンダー。▼ のメニューに並べる（Step 33-H）。 */
    val calendars: List<Calendar> = emptyList(),

    /** ▼ のメニューが開いているか。部品は自分で開かないので、ここに持つ（grammar §3-(107)）。 */
    val isCalendarMenuOpen: Boolean = false,

    /**
     * 自分宛て・返事待ちの招待状の数（Step 36-D）。
     * 1 以上なら ▼ の角に赤い点、メニューに「届いた招待（n）」を出す。
     * 点と行が**同じ 1 つの数字**を見るので、「点はあるのにメニューに無い」という食い違いが起きない。
     */
    // TODO(36-D-1) 型は整数（Int）、最初は 0 件
    val pendingInviteCount: Int = 0,

    /**
     * 今表示しているカレンダーの予定・タスクを書き換えてよいか（Step 38）。
     * マイカレンダー・オーナー・編集者なら「はい」、閲覧者なら「いいえ」。
     * 「いいえ」のとき、画面は「＋」を出さず、完了チェックを灰色にし、名前の下に「閲覧のみ」と出す。
     */
    // TODO(38-A-1) 型は「はい／いいえ」、最初は「はい」（アプリはマイカレンダーから始まるため）
    val canEdit: Boolean = true,

    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
