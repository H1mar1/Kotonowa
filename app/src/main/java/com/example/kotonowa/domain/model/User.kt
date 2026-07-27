package com.example.kotonowa.domain.model

/**
 * アプリの中で扱うユーザー情報。
 *
 * Firebase の `FirebaseUser` をそのまま画面まで持ち回すと、
 * 「Firebase をやめる」となったときに画面のコードまで書き直しになる。
 * そのため Firebase に依存しない自前の形に詰め替えて使う。
 */
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
