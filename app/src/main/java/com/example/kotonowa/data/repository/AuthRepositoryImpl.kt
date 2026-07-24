package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.AuthException
import com.example.kotonowa.domain.model.User
import com.example.kotonowa.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `AuthRepository` の約束を Firebase Authentication で実際に果たすクラス。
 *
 * Firebase に触るのはこのファイルまで。ここから先（domain / presentation）へは
 * 自前の [User] と [AuthException] だけを渡す。
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override val currentUser: User?
        get() = firebaseAuth.currentUser?.toUser()

    override suspend fun login(email: String, password: String): Result<User> = try {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user
            ?: throw AuthException("ログインできましたが、ユーザー情報を取得できませんでした")
        Result.success(firebaseUser.toUser())
    } catch (e: Exception) {
        Result.failure(e.toAuthException())
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}

/** Firebase のユーザー情報を、アプリ内で使う [User] に詰め替える。 */
private fun FirebaseUser.toUser(): User = User(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
)

/** Firebase が投げてくる例外を、そのまま画面に出せる日本語のエラーに変換する。 */
private fun Throwable.toAuthException(): AuthException = when (this) {
    is AuthException -> this

    is FirebaseAuthInvalidUserException ->
        AuthException("このメールアドレスのアカウントは見つかりませんでした", this)

    is FirebaseAuthInvalidCredentialsException ->
        AuthException("メールアドレスまたはパスワードが正しくありません", this)

    is FirebaseAuthUserCollisionException ->
        AuthException("このメールアドレスはすでに登録されています", this)

    is FirebaseNetworkException ->
        AuthException("ネットワークに接続できませんでした。通信環境を確認してください", this)

    is FirebaseTooManyRequestsException ->
        AuthException("試行回数が多すぎます。しばらく待ってからもう一度お試しください", this)

    else ->
        AuthException("ログインに失敗しました（${this::class.simpleName}）", this)
}
