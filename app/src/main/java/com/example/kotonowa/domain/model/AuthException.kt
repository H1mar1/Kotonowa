package com.example.kotonowa.domain.model

/**
 * 認証の失敗を表すエラー。
 *
 * `message` にはそのまま画面に出せる日本語を入れる。
 * Firebase 固有の例外はこのクラスに変換してから domain より上へ渡すことで、
 * presentation 層が Firebase を知らずに済むようにしている。
 */
class AuthException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
