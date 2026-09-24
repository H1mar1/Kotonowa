package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.User

/**
 * ユーザーの「名刺」（Firestore の `users` コレクション）まわりの約束（お品書き）。
 *
 * ⚠️ [AuthRepository] とは役割が違う。
 *  - [AuthRepository] … Firebase Authentication。「**自分が**誰か」「ログインできるか」
 *  - [UserRepository] … Cloud Firestore。「**他人を含めた**名簿」
 *
 * Authentication には「メールアドレスで他人を検索する」手段が無い（総当たりで
 * 会員名簿を作られるのを防ぐため意図的に塞がれている）。そのため
 * ログインのたびに自分の名刺を Firestore へ置いておき、Step 35 の招待では
 * そちらを検索する。
 */
interface UserRepository {

    /**
     * 自分の名刺を置く。同じ uid の名刺が既にあれば内容を更新する。
     */
    suspend fun saveUser(user: User): Result<Unit>

    /**
     * メールアドレスで名刺を 1 枚探す。
     *
     */
    suspend fun findUserByEmail(email: String): Result<User?>
}
