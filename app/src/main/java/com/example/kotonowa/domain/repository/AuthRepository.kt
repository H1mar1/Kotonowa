package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.User

/**
 * 認証まわりで「できること」の約束だけを並べたもの（お品書き）。
 *
 * ここには「どうやるか」は書かない。実際のやり方は data 層の
 * `AuthRepositoryImpl` が Firebase を使って実装する。
 * こうしておくと ViewModel は中身が Firebase かどうかを知らずに済み、
 * テストのときは偽物の実装に差し替えられる。
 */
interface AuthRepository {

    /** 現在ログイン中のユーザー。ログインしていなければ null。 */
    val currentUser: User?

    /** メールアドレスとパスワードでログインする。 */
    suspend fun login(email: String, password: String): Result<User>

    //サインアップ
    suspend fun signUp(email: String, password: String): Result<User>

    /** ログアウトする。 */
    fun logout()
}
