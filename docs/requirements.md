# Kotonowa（ことのわ） 要件定義書

## 1. プロジェクト概要

| 項目 | 内容 |
|---|---|
| プロジェクト名 | **Kotonowa（ことのわ）** |
| 名前の由来 | 「こと」＝Kotlinの語感＋日本語の「事」（予定・タスク）の掛詞。「輪」は共有機能で人と人がつながる様子を表す |
| 目的 | 夏休みを利用したKotlinの技術力向上（ポートフォリオ用途も想定） |
| アプリ種別 | スケジュール・タスク管理アプリ（TimeTree的な複数人での予定共有を将来的に想定） |
| 重視する要素 | 認証・認可（権限管理）、通知機能 |
| プラットフォーム | Android ネイティブ |

---

## 2. 技術スタック

| レイヤー | 選定技術 | 選定理由（要約） |
|---|---|---|
| プラットフォーム | Android ネイティブ + Firebase | 夏休みの期間内で完走しやすく、認証・認可・通知をすべて実践できる |
| 認証 | Firebase Authentication（メール/パスワード + Google Sign-In） | 複数プロバイダの管理・切り替えなど、実務に近い認証フローを学べる |
| バックエンド/DB | Cloud Firestore | リアルタイム同期、セキュリティルールによる宣言的な認可設計が可能 |
| 通知（ローカル） | WorkManager / AlarmManager | 個人のリマインダー通知に使用 |
| 通知（プッシュ） | Firebase Cloud Messaging（FCM）+ Cloud Functions | 共有カレンダーの更新・招待をFirestoreトリガーで他メンバーに通知 |
| アーキテクチャ | MVVM + Clean Architecture | UI層/ドメイン層(UseCase)/データ層(Repository)を分離し、認可・共有ロジックを整理しやすくする |
| DI | Hilt | Jetpack（ViewModel, WorkManager等）との公式統合が強力 |
| UI | Jetpack Compose | 現行の標準UIツールキット。StateFlowとの相性が良い |
| 最低対応バージョン | minSdk 26（Android 8.0） | 通知チャンネルAPIが標準搭載されている最古のバージョン。互換分岐コードを減らせる |
| 日時API | java.time（標準API） | minSdk 26によりdesugaring不要で直接使用可能 |

---

## 3. 機能要件

### 3.1 認証
- メール/パスワードでのサインアップ・ログイン
- Googleアカウントでのログイン
- パスワードリセット（メール送信）
- ログアウト

### 3.2 認可（権限管理）
- 共有カレンダー単位でロールを管理: **オーナー / 編集者 / 閲覧者**
- Firestoreセキュリティルールで、操作しようとしているユーザーのロールを判定して制御
- ルールの具体的な記述は実装フェーズで詳細設計する（§7 バックログ参照）

### 3.3 スケジュール・タスク管理
- **予定（Event）**: 開始/終了時刻を持つ
- **タスク（Task）**: 期限（1点）と完了/未完了の状態を持つ
- Kotlin側では `sealed class ScheduleItem`（`Event` / `Task`）として型安全に統一表現し、`when`式で網羅的に分岐処理する

### 3.4 共有機能（将来的な拡張を見据えた設計）
- カレンダーの新規作成
- メールアドレスによるメンバー招待（ロール指定）
- 招待の承認・拒否
- メンバー一覧・ロール変更（オーナーのみ操作可）

### 3.5 通知
- ローカル通知: 自分の予定/タスクのリマインダー
- プッシュ通知: 共有カレンダーへの招待、他メンバーによる予定/タスクの追加・編集

---

## 4. データモデル（Cloud Firestore）

コレクション構造は **ハイブリッド型**（メンバー情報はカレンダーのサブコレクション、予定/タスクはフラットなトップレベルコレクション）を採用。

| コレクション | 構造 | 主なフィールド |
|---|---|---|
| `users` | トップレベル | uid, displayName, email, photoUrl, fcmTokens[], createdAt |
| `calendars` | トップレベル | calendarId, name, ownerUid, type, color, createdAt |
| `calendars/{calendarId}/members` | サブコレクション | uid, role(owner/editor/viewer), joinedAt, invitedBy |
| `events` | トップレベル（calendarId参照） | eventId, calendarId, type(event/task), title, description, startAt, endAt, allDay, dueAt, isCompleted, createdBy, reminderMinutesBefore, updatedAt |
| `invites` | トップレベル | inviteId, calendarId, invitedEmail, role, status, invitedBy, createdAt |

**設計メモ**
- `events`はフラット構造にすることで、「自分が所属する全カレンダーの予定/タスクを横断表示」するクエリを1回で書けるようにしている
- `members`はサブコレクションにすることで、Firestoreセキュリティルールから「このカレンダーの`members/{自分のuid}`を参照してロールを確認する」という認可判定を自然に書ける
- FCMトークンはユーザードキュメントの配列フィールドで管理（複数端末対応、シンプルさ優先）
- リマインダー時間はイベント単位の共通設定（メンバーごとの個別設定は将来拡張）

---

## 5. 画面一覧

| フェーズ | 画面 | 概要 |
|---|---|---|
| Phase1: 認証 | スプラッシュ | 自動ログイン判定 |
| | サインアップ | メール/パスワード or Googleでの新規登録 |
| | ログイン | メール/パスワード or Googleでログイン |
| | パスワードリセット | メールでリセットリンク送信 |
| Phase2: 個人管理 | ホーム（カレンダー） | 月表示カレンダー＋下部に選択日の予定/タスク一覧 |
| | 予定/タスク作成・編集 | タイトル、日時 or 期限、リマインダー設定など |
| | 予定/タスク詳細 | 内容確認、編集・削除への導線 |
| | 設定 | プロフィール、通知設定、ログアウト |
| Phase3: 共有 | カレンダー一覧 | 自分が所属する個人/共有カレンダー一覧 |
| | カレンダー作成 | 新規共有カレンダーの作成 |
| | メンバー管理 | メンバー一覧、ロール変更（オーナーのみ） |
| | 招待送信 | メールアドレスで招待、ロール指定 |
| | 招待受信（一覧） | 届いた招待の承認/拒否 |
| Phase4: 通知 | 通知設定（詳細） | カレンダーごとの通知ON/OFF |

---

## 6. 実装ロードマップ

段階的ビルド方式を採用し、各フェーズ終了時に必ず「動くアプリ」がある状態を維持する。

```
(任意) FCM疎通PoC
  → Phase1: 認証（サインアップ/ログイン/ログアウト）
  → Phase2: 個人のスケジュール/タスク管理 + ローカル通知
  → Phase3: 共有カレンダー + ロールベース認可
  → Phase4: プッシュ通知（共有更新・招待）
```

Cloud Functionsのデプロイ・FCM送受信は初見だと詰まりやすいため、Phase1に入る前に軽い疎通確認（PoC）を行うことを推奨。

---

## 7. 今後の検討事項（バックログ）

実装を進める中で、必要になったタイミングで詳細化する項目。

- Firestoreセキュリティルールの具体的な記述（ロール判定ロジック）
- テスト方針（Unit Test / UI Testの範囲、対象レイヤー）
- オフライン対応（Firestoreのオフラインキャッシュ活用方針）
- イベント単位での公開範囲制御（カレンダー共有中でも一部の予定を非公開にする機能）
- メンバーごとの個別リマインダー設定
- ダークモード・多言語対応
- CI/CD（GitHub Actions等）の導入

---

## 8. プロジェクトセットアップ手順

### 8.1 Android Studio New Projectウィザード

| 項目 | 設定値 |
|---|---|
| Template | Empty Activity（Compose） |
| Name | Kotonowa |
| Package name | `com.<あなたの名前やハンドルネーム>.kotonowa`（Firebase登録時と完全一致させる） |
| Language | Kotlin |
| Build configuration language | Kotlin DSL（build.gradle.kts） |
| Minimum SDK | API 26（Android 8.0） |

### 8.2 プロジェクト作成後にやること

1. **Hilt導入**: `app/build.gradle.kts`に`com.google.dagger.hilt.android`プラグイン＋KSPを追加。`Application`クラスに`@HiltAndroidApp`、`MainActivity`に`@AndroidEntryPoint`を付与
2. **Firebase導入**: Firebase Consoleでプロジェクト作成→パッケージ名を一致させてAndroidアプリ登録→`google-services.json`配置→Auth（メール/パスワード・Google）とFirestoreをConsole側で有効化
3. **Compose Navigation導入**: `androidx.navigation:navigation-compose`を追加
4. **パッケージ構成（Clean Architecture）**:
   ```
   com.example.kotonowa/
   ├── data/       # local(WorkManager), remote(Firestore/Auth), repository
   ├── domain/     # model(ScheduleItem等), repository interface, usecase
   ├── presentation/  # auth, home, calendar, common
   └── di/         # Hiltモジュール
   ```
5. **通知権限**: Android 13以降で必須の`POST_NOTIFICATIONS`ランタイム権限リクエストは、Phase2の早い段階で実装しておく

---

*このドキュメントは要件定義セッションの合意事項をまとめたものです。実装を進める中で決定が変わった場合は、都度このドキュメントを更新してください。*
