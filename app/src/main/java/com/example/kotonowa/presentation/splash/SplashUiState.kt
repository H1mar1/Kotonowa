package com.example.kotonowa.presentation.splash

/**
 * スプラッシュ画面の「今の状態」。
 *
 * ログイン済みかどうかの判定結果は「3つのうちどれか1つ」なので、
 * 入れ物（data class）ではなく仲間分けの名札（sealed interface）で表す。
 *
 * sealed = この中に書いたものだけが仲間になれる。
 * だから when で分岐したとき、書き漏らすとコンパイルエラーで教えてもらえる。
 */
sealed interface SplashUiState {

    data object Checking : SplashUiState
    data object LoggedIn : SplashUiState
    data object NotLoggedIn : SplashUiState

}
