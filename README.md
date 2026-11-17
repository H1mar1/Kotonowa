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
Firestore セキュリティルール（[`firestore.rules`](firestore.rules)）から `calendars/{id}/members/{uid}` を参照してロールを判定し、サーバー側で操作を制御します。

| 操作 | オーナー | 編集者 | 閲覧者 |
|---|:-:|:-:|:-:|
| 予定・タスクを見る | ✅ | ✅ | ✅ |
| 予定・タスクの作成・編集・削除・完了チェック（他メンバーの分も含む） | ✅ | ✅ | ❌ |
| メンバーの招待・ロール変更・外す | ✅ | ❌ | ❌ |

- **最後の砦はルール**：アプリを経由しない直接の書き込みもサーバー側で拒否する（`PERMISSION_DENIED`）
- **画面も同じ基準で出し分け**：ViewModel が名簿を購読して `canEdit` を求め、閲覧者には「＋」「編集」「削除」を出さず、完了チェックを無効化する。オーナーがロールを変えると相手の画面もその場で切り替わる
- 招待の承認は `getAfter` で「招待状を承認済みにする書き込みと同時」であることを確かめ、招待状に書かれたロールのとおりにしか名簿へ入れない

### スケジュール・タスク管理
予定（開始/終了時刻を持つ）とタスク（期限と完了状態を持つ）を、Kotlin の `sealed class ScheduleItem` として型安全に統一表現。`when` 式で網羅的に分岐処理します。

```kotlin
sealed class ScheduleItem {
    data class Event(val startAt: Instant, val endAt: Instant, ...) : ScheduleItem()
    data class Task(val dueAt: Instant, val isCompleted: Boolean, ...) : ScheduleItem()
}
```

### 共有
- 共有カレンダーの作成と、ホーム上部の「名前 ▼」メニューからの切り替え（マイカレンダー ⇄ 共有カレンダー）
- メールアドレスによるメンバー招待（編集者 / 閲覧者を指定）
- 招待の受信（▼ に赤い点で通知）と承認 / 拒否。承認は `runBatch` で「名簿の行」「部屋の `memberUids`」「招待状の状態」を同時に書き換える
- メンバーのロール変更・メンバーを外す（オーナーのみ）

### 通知
- **ローカル通知** — 自分の予定/タスクのリマインダー
- **プッシュ通知**（Phase 4・未実装） — 共有カレンダーへの招待、他メンバーによる予定の追加・編集（Firestore トリガー）

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
| `calendars` | トップレベル | calendarId, name, ownerUid, type, color, createdAt, memberUids[] |
| `calendars/{id}/members` | サブコレクション | uid, role, joinedAt, invitedBy |
| `events` | トップレベル | eventId, calendarId, type, title, startAt, endAt, dueAt, isCompleted, ... |
| `invites` | トップレベル | inviteId（`{calendarId}_{小文字のメール}`）, calendarId, calendarName, invitedEmail, role, status, invitedBy, createdAt |

**設計意図**
- `events` をフラットにすることで、「所属する全カレンダーの予定を横断表示」を **1クエリ** で実現
- `members` をサブコレクションにすることで、セキュリティルールから「このカレンダーの `members/{自分のuid}`」を自然に参照でき、認可判定が書きやすい
- `calendars.memberUids[]` は「自分が入っているカレンダー一覧」を `whereArrayContains` の 1 クエリで引くための写し。名簿と必ず `runBatch` で同時に更新する
- 招待状の id を `{calendarId}_{メール}` に固定し、ルールが住所を組み立てて「自分宛ての招待状」を直接参照できるようにしている（同じ人への再招待は上書きになる）
- 個人カレンダーの id はユーザーの uid（`calendars` の書類を持たない）。ルールでは「id ＝ 自分の uid」で判定する

---

## 開発ロードマップ

各フェーズの終了時点で必ず「動くアプリ」がある状態を維持する段階的ビルド方式を採っています。

- [x] **Phase 0** — プロジェクトセットアップ / Firebase 疎通確認
- [x] **Phase 1** — 認証（サインアップ・ログイン・ログアウト）
- [x] **Phase 2** — 個人のスケジュール/タスク管理 + ローカル通知
- [x] **Phase 3** — 共有カレンダー + ロールベース認可
- [ ] **Phase 4** — プッシュ通知（共有更新・招待）

## 画面一覧

| フェーズ | 画面 |
|---|---|
| Phase 1 | スプラッシュ（自動ログイン判定）/ サインアップ / ログイン / パスワードリセット |
| Phase 2 | ホーム（月表示カレンダー + 選択日の一覧）/ 予定・タスク作成編集 / 詳細 / 設定 |
| Phase 3 | カレンダーの管理（一覧）/ カレンダー作成 / メンバー管理（招待送信を含む）/ 届いた招待 |
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
5. Firestore の「ルール」に [`firestore.rules`](firestore.rules) の内容を貼り付けて公開
6. ビルド（初回に予定一覧を開くと複合インデックス作成用の URL がエラーに出るので、開いて作成する）

```bash
./gradlew :app:assembleDebug        # macOS / Linux
.\gradlew.bat :app:assembleDebug    # Windows
```

---

## ドキュメント

- [要件定義書](docs/requirements.md) — 仕様の正となるドキュメント
- [CLAUDE.md](CLAUDE.md) — AI アシスタント向けのプロジェクトコンテキスト

---

## ライセンス

個人学習・ポートフォリオ用途のプロジェクトです。
