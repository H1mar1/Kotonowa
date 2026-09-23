package com.example.kotonowa.domain.model

/**
 * カレンダーの中でのその人の肩書き（ロール）。
 *
 * 要件定義書 §3.2 の「オーナー / 編集者 / 閲覧者」を表す。
 * Step 38 で画面の出し分けに、Step 37 で Firestore セキュリティルールの
 * 判定に使う（ルール側は文字列で見るので、data 層で変換する）。
 */
enum class MemberRole {
    //  ・作った人。メンバーの追加・ロール変更・カレンダーの削除ができる
    //  ・予定/タスクの作成・編集・削除ができる。メンバーは触れない
    //  ・見るだけ
    OWNER,
    EDITOR,
    VIEWER,
}
