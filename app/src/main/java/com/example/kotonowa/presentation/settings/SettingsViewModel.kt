package com.example.kotonowa.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.User
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.PushTokenRepository
import com.example.kotonowa.domain.repository.SelectedCalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 設定画面の頭脳。
 *
 * 表示するのはログイン中のユーザー情報だけで、画面が開いている間は変わらない。
 * だから StateFlow（§2-(58)）は使わず、作られたときに 1 回読むだけにしている。
 * 「変わるか変わらないか」で道具を選ぶ。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val selectedCalendarRepository: SelectedCalendarRepository, // 選択中のカレンダーの掲示板（Step 33-A）
    private val pushTokenRepository: PushTokenRepository, // プッシュ通知の宛先の登録係（Step 41）
) : ViewModel() {

    /** ログイン中のユーザー。ログインしていなければ null。 */

    val user: User? = authRepository.currentUser

    /** ログアウトの途中か（二度押し防止。画面に出さないので UiState ではなくただの var）。 */
    private var isLoggingOut = false

    /**
     * ログアウトする。画面遷移は知らない（Step 22 と同じ分担）。終わったら [onDone] を鳴らす。
     *
     * 掲示板は @Singleton なのでログアウトしても生き残る。白紙に戻さないと、
     * 次に同じ端末でログインした人のホームが、前の人の共有カレンダーを開こうとする（Step 33-E）。
     *
     * Step 41：サインアウトの「前」に、この端末の宛先を名刺から外す（通信するので launch の中）。
     * サインアウトしたあとは自分の名刺を書き換える権限が無く、ルールに断られるため。
     * 外し終わってから画面を移したいので、終わりの合図 [onDone] を引数でもらう
     * （画面がすぐ移ると、この頭脳ごと捨てられて viewModelScope が打ち切られる。Step 28 の ⚠️）。
     */
    fun logout(onDone: () -> Unit) {
        if (isLoggingOut) return
        isLoggingOut = true

        viewModelScope.launch {
            val myUid = user?.uid
            if (myUid != null) {
                // TODO(41-D-1) 誰の：宛先の登録係の ／ 何をする：この人の名刺から、この端末の宛先を外す
                //   失敗してもログアウトは続ける（結果は見ない。saveUser と同じ考え方）
                pushTokenRepository.unregisterToken(myUid)
            }

            // TODO(33-E) 誰の：掲示板の ／ 何をする：白紙に戻す（33-A で作った窓口。部屋を出る前に片付ける）
            selectedCalendarRepository.clear()
            authRepository.logout()

            // TODO(41-D-2) 終わりの合図を鳴らす（引数でもらった呼び鈴。() を付けて鳴らす）
            onDone()
        }
    }
}
