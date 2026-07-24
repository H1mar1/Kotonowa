# Kotonowa（ことのわ）

複数人での予定共有を見据えた、Android ネイティブのスケジュール・タスク管理アプリ。

> **名前の由来**
> 「こと」＝ Kotlin の語感と日本語の「事」（予定・タスク）の掛詞。
> 「輪」は共有機能で人と人がつながる様子を表しています。

---

## コンセプト

TimeTree のような複数人でのカレンダー共有を目標に、**認証・認可（ロールベースの権限管理）** と **通知** の2点を軸に設計しています。単なる CRUD アプリではなく、「誰が・どのカレンダーに対して・何をできるか」をサーバー側のセキュリティルールで宣言的に制御することを主眼に置いています。

## 主な機能

### 認証
- メール/パスワードによるサインアップ・ログイン
- Google アカウントでのログイン
- パスワードリセット（メール送信）

### 認可
共有カレンダー単位で **オーナー / 編集者 / 閲覧者** の3ロールを管理。
Firestore セキュリティルールから `calendars/{id}/members/{uid}` を参照してロールを判定し、サーバー側で操作を制御します。

### スケジュール・タスク管理
予定（開始/終了時刻を持つ）とタスク（期限と完了状態を持つ）を、Kotlin の `sealed class ScheduleItem` として型安全に統一表現。`when` 式で網羅的に分岐処理します。

```kotlin
sealed class ScheduleItem {
    data class Event(val startAt: Instant, val endAt: Instant, ...) : ScheduleItem()
    data class Task(val dueAt: Instant, val isCompleted: Boolean, ...) : ScheduleItem()
}
```

### 共有
カレンダーの作成、メールアドレスによるメンバー招待（ロール指定）、招待の承認/拒否、メンバーのロール変更。

### 通知
- **ローカル通知** — 自分の予定/タスクのリマインダー
- **プッシュ通知** — 共有カレンダーへの招待、他メンバーによる予定の追加・編集（Firestore トリガー）

---

## 技術スタック

| レイヤー | 技術 | 選定理由 |
|---|---|---|
| UI | Jetpack Compose + Material3 | 現行標準の UI ツールキット。StateFlow との相性が良い |
| アーキテクチャ | MVVM + Clean Architecture | UI / ドメイン / データ層を分離し、認可・共有ロジックを整理しやすくする |
| DI | Hilt | ViewModel・WorkManager など Jetpack との公式統合が強力 |
| 認証 | Firebase Authentication | 複数プロバイダの管理・切り替えなど実務に近い認証フローを学べる |
| DB | Cloud Firestore | リアルタイム同期＋セキュリティルールによる宣言的な認可設計 |
| ローカル通知 | WorkManager / AlarmManager | 個人リマインダー用 |
| プッシュ通知 | Firebase Cloud Messaging + Cloud Functions | 共有更新・招待の配信 |
| 日時 | java.time | minSdk 26 のため desugaring 不要で直接利用可 |
| 言語 / ビルド | Kotlin 2.2.10 / Gradle Kotlin DSL (AGP 9.2.1) | |

**minSdk 26（Android 8.0）** — 通知チャンネル API が標準搭載された最古のバージョン。互換分岐コードを減らせるため。

### アーキテクチャ構成

```
com.example.kotonowa/
├── data/          # Firestore/Auth との通信、Repository 実装
├── domain/        # モデル、Repository インターフェース、UseCase
├── presentation/  # Compose 画面と ViewModel
└── di/            # Hilt モジュール
```

依存の向きは `presentation → domain ← data`。ドメイン層は Android にも Firebase にも依存しません。

---

## データモデル（Cloud Firestore）

メンバー情報はサブコレクション、予定/タスクはトップレベルという **ハイブリッド構造** を採用しています。

| コレクション | 構造 | 主なフィールド |
|---|---|---|
| `users` | トップレベル | uid, displayName, email, photoUrl, fcmTokens[], createdAt |
| `calendars` | トップレベル | calendarId, name, ownerUid, type, color, createdAt |
| `calendars/{id}/members` | サブコレクション | uid, role, joinedAt, invitedBy |
| `events` | トップレベル | eventId, calendarId, type, title, startAt, endAt, dueAt, isCompleted, ... |
| `invites` | トップレベル | inviteId, calendarId, invitedEmail, role, status, ... |

**設計意図**
- `events` をフラットにすることで、「所属する全カレンダーの予定を横断表示」を **1クエリ** で実現
- `members` をサブコレクションにすることで、セキュリティルールから「このカレンダーの `members/{自分のuid}`」を自然に参照でき、認可判定が書きやすい

---

## 開発ロードマップ

各フェーズの終了時点で必ず「動くアプリ」がある状態を維持する段階的ビルド方式を採っています。

- [x] **Phase 0** — プロジェクトセットアップ / Firebase 疎通確認
- [ ] **Phase 1** — 認証（サインアップ・ログイン・ログアウト）
- [ ] **Phase 2** — 個人のスケジュール/タスク管理 + ローカル通知
- [ ] **Phase 3** — 共有カレンダー + ロールベース認可
- [ ] **Phase 4** — プッシュ通知（共有更新・招待）

## 画面一覧

| フェーズ | 画面 |
|---|---|
| Phase 1 | スプラッシュ（自動ログイン判定）/ サインアップ / ログイン / パスワードリセット |
| Phase 2 | ホーム（月表示カレンダー + 選択日の一覧）/ 予定・タスク作成編集 / 詳細 / 設定 |
| Phase 3 | カレンダー一覧 / カレンダー作成 / メンバー管理 / 招待送信 / 招待受信 |
| Phase 4 | 通知設定（カレンダーごとの ON/OFF） |

---

## セットアップ

### 必要環境
- Android Studio（AGP 9.2.1 に対応するバージョン）
- JDK 11
- Android SDK 36

### 手順

1. リポジトリをクローン
2. [Firebase Console](https://console.firebase.google.com/) でプロジェクトを作成し、パッケージ名 `com.example.kotonowa` で Android アプリを登録
3. ダウンロードした `google-services.json` を `app/` 直下に配置
4. Firebase Console で **Authentication**（メール/パスワード・Google）と **Cloud Firestore** を有効化
5. ビルド

```powershell
.\gradlew.bat :app:assembleDebug
```

---

## ドキュメント

- [要件定義書](docs/requirements.md) — 仕様の正となるドキュメント
- [CLAUDE.md](CLAUDE.md) — AI アシスタント向けのプロジェクトコンテキスト

---

## ライセンス

個人学習・ポートフォリオ用途のプロジェクトです。
