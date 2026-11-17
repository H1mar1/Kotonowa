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

    //パスワードリセット
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /** Google から受け取った ID トークンでログインする。 */
    suspend fun loginWithGoogle(idToken: String): Result<User>

    /** ログアウトする。 */
    fun logout()

    // ---- メールアドレスの持ち主確認（Step 39） ----

    /**
     * ログイン中の人のメールアドレスが確認済みか（確認メールのリンクを押したか）。
     * 端末が覚えている値なので、リンクを押した直後は古いまま。新しくするには [refreshEmailVerified]。
     * Google でログインした人は最初から「はい」。
     */
    // TODO(39-A-1) 型は「はい／いいえ」。currentUser と同じく val で「値を読むだけ」の形（get の中身は Impl が書く）
    val isEmailVerified: Boolean

    /** ログイン中の人のメールアドレスに確認メールを送る。 */
    // TODO(39-A-2) 通信するので suspend。成功しても返すものは無い（sendPasswordResetEmail と同じ戻り値）
    suspend fun sendEmailVerification(): Result<Unit>

    /**
     * Firebase に問い合わせ直して、確認済みかどうかを新しくする。答え（はい／いいえ）を返す。
     * 「確認しました」ボタンから呼ぶ。
     */
    // TODO(39-A-3) 封筒の中身は「はい／いいえ」（login が Result<User> なのと同じ形）
    suspend fun refreshEmailVerified(): Result<Boolean>


}
