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
- **ロールごとにできること**（2026-11-17 決定）

  | 操作 | オーナー | 編集者 | 閲覧者 |
  |---|---|---|---|
  | 予定・タスクを見る | ○ | ○ | ○ |
  | 予定・タスクを作る | ○ | ○ | × |
  | 予定・タスクを編集・削除（**他のメンバーが作ったものも含む**） | ○ | ○ | × |
  | タスクの完了チェック | ○ | ○ | ×（読むだけ） |
  | メンバーの招待・肩書きの変更・外す | ○ | × | × |

  個人カレンダー（マイカレンダー）は本人だけが読み書きできる。

### 3.3 スケジュール・タスク管理
- **予定（Event）**: 開始/終了時刻を持つ
- **タスク（Task）**: 期限（1点）と完了/未完了の状態を持つ
- Kotlin側では `sealed class ScheduleItem`（`Event` / `Task`）として型安全に統一表現し、`when`式で網羅的に分岐処理する

#### 設計メモ（2026-08-02 決定）

**フィールドの振り分け**

| 区分 | フィールド |
|---|---|
| 共通（`ScheduleItem`） | `id`, `calendarId`, `title`, `description`, `createdBy`, `reminderMinutesBefore`, `updatedAt` |
| `Event` 専用 | `startAt`, `endAt`, `allDay` |
| `Task` 専用 | `dueAt`, `isCompleted` |

- `type`（`event`/`task`）は **Kotlin側のモデルには持たせない**。`sealed class` の型そのものが表現しており、二重に持つと「`Task` なのに `type = "event"`」という矛盾を作れてしまうため。Firestore には必要なので、`ScheduleRepositoryImpl`（data層）で保存直前に付与する
- `calendarId` は共通。カレンダーに属さない予定/タスクは存在しない設計（個人の予定も「個人用カレンダー」に属する）
- `reminderMinutesBefore` は共通。§3.5 の通りローカル通知は予定・タスク両方が対象のため

**日時の型：`java.time.Instant` で統一する**

`LocalDateTime` ではなく `Instant`（世界共通の時刻の一点）を使う。

- Firestore の `Timestamp` が同じくUTC基準の一点であり、変換が素直
- Phase3で時差のあるメンバーと共有したとき、`LocalDateTime` だと「誰の14:00か」が失われ、各自の端末で同じ数字が表示されてしまう
- 表示時にのみ端末の `ZoneId` を当てて変換する。「予定が作成された土地のタイムゾーン」を保存するかは将来の検討事項

**終日（`allDay`）の扱い：A案を採用**

終日の予定は本来「時刻の一点」ではなく「日付」であり、`Instant` とは相性が悪い（`8/5 00:00 JST` は世界標準時では `8/4 15:00` になり、時差のある相手には前日として表示されうる）。

| 案 | 内容 | 判断 |
|---|---|---|
| **A案** | `allDay = true` のとき `startAt` を「その日の00:00」として扱い、表示側で時刻部分を無視する | **採用**。個人利用の間は端末のタイムゾーンが一定なのでズレが表面化しない |
| B案 | 終日用に `LocalDate`（日付のみの型）のフィールドを別に持つ | 見送り。正確だがフィールドと分岐が増える |

**Phase3で共有カレンダーを実装する際に、B案へ移行するか再検討すること。**

#### 設計メモ（2026-08-05 決定）

**Firestore のドキュメントIDに `ScheduleItem.id` を使う**

`.document()` を引数なしで呼ぶとFirestoreが自動採番するが、あえて自前の `id` を渡す。
こうするとFirestore上のIDとKotlinモデルの `id` が常に一致し、更新・削除を `id` だけで書ける
（`ScheduleRepository` の `deleteItem(itemId: String)` が予定の中身を受け取らずに済むのはこのため）。

**更新は `.set()` ではなく `.update()` を使う**

`toMap()` は毎回全フィールドを揃えて返すため、書き込み結果はどちらでも同一になる。
違うのは**対象が存在しなかったとき**の振る舞いのみ。

| 命令 | ドキュメントが無いとき | |
|---|---|---|
| `.set(map)` | 黙って新規作成する | `addItem` で使用 |
| `.update(map)` | エラーになり `Result.failure` として画面まで届く | **`updateItem` で採用** |

- 「更新したつもりが新規作成されていた」を検出できるようにするため
- `addItem` と `updateItem` の意図がコード上で区別できるようにするため
  （両方 `.set()` だと2つの関数が完全に同一になり、存在理由が読めなくなる）

**`deleteItem` は存在しないIDを渡しても成功する**

Firestore の `.delete()` の仕様。「消したはずが実は無かった」は検出できない。
検出が必要になったら、削除前に `getItem` で存在確認する方式へ変更する。

**エラーの日本語化は `AuthRepository` と違い未実装**

`AuthRepositoryImpl` は `toAuthException()` で日本語メッセージに変換しているが、
`ScheduleRepositoryImpl` は素の例外をそのまま `Result.failure` で返している。
Phase2 のUI実装時に、必要なメッセージが出揃ってからまとめて設計する。

### 3.4 共有機能（将来的な拡張を見据えた設計）
- カレンダーの新規作成
- メールアドレスによるメンバー招待（ロール指定）
- 招待の承認・拒否
- メンバー一覧・ロール変更（オーナーのみ操作可）
- **招待を受け取れるのは、メールアドレスの持ち主確認を済ませた人だけ**（2026-11-17 決定・Step 39）。
  確認していなくてもアプリ自体は使える。サインアップ時に確認メールを送り、招待一覧で未確認なら
  「確認メールを再送」「確認しました」を出す。ルールでも招待の読み取り・返事・承認による参加に
  `request.auth.token.email_verified == true` を要求する（他人のアドレスで先に登録して招待を横取りされるのを防ぐ）。
  Google ログインの人は最初から確認済み。

### 3.5 通知
- ローカル通知: 自分の予定/タスクのリマインダー
- プッシュ通知: 共有カレンダーへの招待、他メンバーによる予定/タスクの追加・編集
  - 設計（2026-11-18 決定）：送るのは Cloud Functions（Firestore の作成・更新トリガー）。宛先は `users/{uid}.fcmTokens[]`。
    追加・編集は「自分以外のメンバー」へ。削除は送らない。マイカレンダーは対象外。
    編集した人を知るため `events` に `updatedBy` を追加する。カレンダーごとの ON/OFF は設定画面の「通知設定」で切り替える

### 3.6 チャット（Phase 5。2026-11-22 決定）
- **2 種類のトーク**
  - **グループ**：共有カレンダーごとに 1 つ。そのカレンダーのメンバー全員で話す（マイカレンダーには無い）
  - **個人どうし（DM）**：1 対 1。始められるのは**共有カレンダーを 1 つでも一緒にしている人だけ**（メンバー画面の行から始める）。
    名前は名簿の行（Step 46）にあるので、`users` を他人に開けない
- **書き込めるのはメンバー全員**。閲覧者も書ける（予定の編集権限とチャットは別。予定は読むだけ、話し合いには参加できる）
- できること
  - 文字を送る・一覧で読む（新しい発言が下。開いている間は自動で増える）
  - **自分の発言を消す**（送った本人だけ）
  - **新着のプッシュ通知**（自分以外のメンバーへ「グループ名 ／ ○○さん：本文」）。グループは通知設定のカレンダー ON/OFF に従う
  - **未読の印**（トーク一覧の行と下のタブに赤い点。Step 36 の `Badge` と同じ）
  - **予定・タスクを貼る**（発言に予定を添え、押すと詳細画面へ）
- **入口は画面の下のタブの 3 つ目「チャット」**（カレンダー ｜ タスク ｜ チャット）。押すと**トーク一覧**
  （LINE のように、グループと DM を 1 つの一覧に新しい発言の順で並べる）→ 行を押すとそのトーク
- **小さく区切って作る**（区切りごとに動くアプリを保つ）：
  ① グループで送る・読む ＋ トーク一覧 ＋ 3 つ目のタブ → ② 消す・新着の通知 → ③ DM → ④ 未読の印 → ⑤ 予定を貼る
- データの持ち方・ルールの細部は、各区切りの着手時にここへ追記する（下の §4 は現時点の案）
- **② 消す・新着の通知（Step 50。2026-11-24 にユーザーが選択）**
  - 消すと**跡を残さず**書類ごと削除（「削除されました」は残さない）。消せるのは送った本人だけ（メンバーである間）
  - 操作は**自分の吹き出しの長押し → 確認ダイアログ →［消す］**（他人の吹き出しは長押ししても何も出ない）
  - 新着の通知は Cloud Functions `notifyChatMessage`（発言の作成で動く）。宛先は送った本人以外のメンバー、
    見出し＝カレンダー名・本文＝「○○さん：本文」。通知設定でそのカレンダーを止めている人には送らない（`sendToUser` の `calendarId`）
  - 通知を**タップするとそのトーク画面を開く**。通知の `data` に `calendarId` を載せ、`MainActivity` → `NavHost` で `chat_room/{calendarId}` へ
- **③ DM（Step 51。2026-11-24 にユーザーが選択）**
  - 相手の名前は、DM の部屋を作るときに**部屋の書類へ 2 人分写す**（`memberNames`。名簿の行の `displayName` → 無ければ `email`）。カレンダーを抜けても名前は残る
  - 「共有カレンダーを一緒にしている」を確かめるのは**部屋を作るときだけ**。作ったあとは、どちらかがそのカレンダーを抜けても読み書きを続けられる（ルールは「部屋の 2 人か」だけを見る）
  - DM の新着通知は**いつも届く**（招待と同じ。通知設定の ON/OFF の対象外）
  - 入口はメンバー画面の他の人の行の［メッセージ］。部屋が無ければ作ってから開く

---

## 4. データモデル（Cloud Firestore）

コレクション構造は **ハイブリッド型**（メンバー情報はカレンダーのサブコレクション、予定/タスクはフラットなトップレベルコレクション）を採用。

| コレクション | 構造 | 主なフィールド |
|---|---|---|
| `users` | トップレベル | uid, displayName, email, photoUrl, fcmTokens[], createdAt |
| `calendars` | トップレベル | calendarId, name, ownerUid, type, color, createdAt, **memberUids[]** |
| `calendars/{calendarId}/members` | サブコレクション | uid, role(owner/editor/viewer), joinedAt, invitedBy |
| `events` | トップレベル（calendarId参照） | eventId, calendarId, type(event/task), title, description, startAt, endAt, allDay, dueAt, isCompleted, sortAt, createdBy, reminderMinutesBefore, updatedAt |
| `invites` | トップレベル | inviteId, calendarId, **calendarName**, invitedEmail, role, status, invitedBy, createdAt |
| `calendars/{calendarId}/messages`（Phase 5 案） | サブコレクション | messageId, senderUid, senderName, text, attachedItemId?, createdAt |
| `directChats`（Phase 5） | トップレベル | chatId（2 人の uid を小さい順に `_` でつなぐ）, memberUids[2], **memberNames{uid: 名前}**, viaCalendarId, lastMessageText, lastMessageAt, createdAt |
| `directChats/{chatId}/messages`（Phase 5 案） | サブコレクション | グループの messages と同じ |

**設計メモ**
- `events`はフラット構造にすることで、「自分が所属する全カレンダーの予定/タスクを横断表示」するクエリを1回で書けるようにしている
- `members`はサブコレクションにすることで、Firestoreセキュリティルールから「このカレンダーの`members/{自分のuid}`を参照してロールを確認する」という認可判定を自然に書ける
- FCMトークンはユーザードキュメントの配列フィールドで管理（複数端末対応、シンプルさ優先）
- リマインダー時間はイベント単位の共通設定（メンバーごとの個別設定は将来拡張）
- **個人カレンダーの `calendarId` は、そのユーザーの `uid` をそのまま使う**（2026-08-08 決定）。
  Phase2 は個人利用のみなので `calendars` ドキュメントは作らない。ViewModel は `AuthRepository.currentUser?.uid`
  を `calendarId` として `ScheduleRepository` に渡す。
  Phase3 で共有カレンダーを導入する際に `calendars/{uid}`（type=personal）を後から作れば、既存データと矛盾しない
- **`calendars.memberUids[]` は「自分が入っている部屋の一覧」を引くための検索用フィールド**（2026-09-24 決定）。
  名簿の本体はあくまで `calendars/{id}/members/{uid}`。`memberUids` はそこに誰が載っているかを
  写しただけの重複情報で、`whereArrayContains("memberUids", uid)` の 1 クエリで一覧を取るために置く。
  - 採用理由：`members` はサブコレクションなので「子から親を逆引き」できない。
    コレクショングループ検索（案A）だと名簿の行しか返らず、部屋の情報を取りに行く通信が部屋の数だけ増える。
    `users` に `calendarIds[]` を持つ案Bも同じ問題を抱える
  - セキュリティルールを `allow read: if request.auth.uid in resource.data.memberUids;` と
    1 行で書けるのも利点（案Aだとルールの中で名簿を `exists()` で読みに行く必要がある）
  - Kotlin 側の `Calendar` には持たせない。`type` / `sortAt` と同じく data 層の `toMap()` 相当で扱い、
    メンバーの増減時に `FieldValue.arrayUnion` / `arrayRemove` で名簿と同時に更新する
  - ⚠️ 代償：入退室のたびに `members` と `memberUids` の 2 か所を更新する必要がある。
    更新をまとめて書けるよう、`CalendarRepositoryImpl` の外からは配列の存在が見えない設計にする
- **チャットの置き場所（Phase 5 の案。2026-11-22）**
  - グループの発言は `calendars/{id}/messages` に置く。読み書きの判定は今の `isMember(calendarId)` がそのまま使え、
    メンバーの出入りに合わせて別の名簿を更新しなくてよい（`memberUids` を 2 か所に持たない）
  - トーク一覧の並び順・最後の発言の表示用に、`calendars` と `directChats` に `lastMessageText` / `lastMessageAt` を置く。
    書くのは**発言の作成をきっかけに動くサーバーの関数だけ**（新着の通知と同じ関数。アプリからは書かせない）
  - DM の住所は 2 人の uid から決める（同じ 2 人で 2 つ目の部屋ができない。招待状の住所と同じ考え方）。
    「共有カレンダーを一緒にしている」はルールで `viaCalendarId` の部屋の `memberUids` に 2 人とも居るかで確かめる
  - 未読は「最後に読んだ時刻」を自分の側に持ち、`lastMessageAt` と比べる（置き場所は ④ の着手時に決める）
- **`sortAt` は期間クエリ・並べ替え専用の共通フィールド**（2026-08-07 決定）。予定は `startAt`、タスクは `dueAt` と同じ値を入れる。
  Firestore の範囲絞り込みは単一フィールドにしか掛けられないため、`type` によって見る場所が変わるのを避ける狙い。
  - Kotlin 側の `ScheduleItem` には持たせない。`startAt` / `dueAt` から導出できる重複情報なので、data 層の `toMap()` で保存直前に付与する（`type` と同じ扱い）
  - ⚠️ 既知の割り切り：表示期間をまたぐ長い予定（例：7/28〜8/5 の旅行を8月のカレンダーで見る）は `sortAt` が範囲外になり取得できない。
    Phase2 では許容し、必要になった時点で `endAt` を併用する条件に拡張する

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
| 追加改善 | タスク一覧 | 全カレンダー横断。未完了（期限切れ／これから）・完了の切り替え。画面下のタブ（Step 47） |
| Phase5: チャット | トーク一覧 | 画面下のタブ 3 つ目。グループと DM を新しい発言順に。未読の印 |
| | トーク（グループ／DM） | 発言の一覧・送信・自分の発言の削除・予定を貼る |

---

## 6. 実装ロードマップ

段階的ビルド方式を採用し、各フェーズ終了時に必ず「動くアプリ」がある状態を維持する。

```
(任意) FCM疎通PoC
  → Phase1: 認証（サインアップ/ログイン/ログアウト）
  → Phase2: 個人のスケジュール/タスク管理 + ローカル通知
  → Phase3: 共有カレンダー + ロールベース認可
  → Phase4: プッシュ通知（共有更新・招待）
  → Phase5: チャット（グループ → 消す・通知 → DM → 未読 → 予定を貼る）
```

Cloud Functionsのデプロイ・FCM送受信は初見だと詰まりやすいため、Phase1に入る前に軽い疎通確認（PoC）を行うことを推奨。

---

## 7. 今後の検討事項（バックログ）

実装を進める中で、必要になったタイミングで詳細化する項目。

- Firestoreセキュリティルールの具体的な記述（ロール判定ロジック）
- テスト方針（Unit Test / UI Testの範囲、対象レイヤー）
- オフライン対応（Firestoreのオフラインキャッシュ活用方針）
- ~~`users` の読み取り権限の絞り込み（2026-09-24 追加）~~ → **2026-11-22 対応済み（Step 46）**。
  `users` は本人だけが読める。招待の相手探しはサーバーの関数 `sendInvite`（`onCall`）が行い、
  メンバー画面の名前・メールは名簿の行（`calendars/{id}/members/{uid}` の `displayName` / `email`）に写して持つ
- イベント単位での公開範囲制御（カレンダー共有中でも一部の予定を非公開にする機能）
- メンバーごとの個別リマインダー設定
- **予定・タスクを別のカレンダーにも入れる（2026-11-24 にユーザーが要望。Phase 5 の後）**
  - マイカレンダーの予定を、共有カレンダーにも追加する
  - 共有カレンダーで**自分が作った**予定を、マイカレンダーにも追加する
  - 共有カレンダーで**他のメンバーが作った**予定を、自分のマイカレンダーに取り込む
  - 着手時に決めること：
    - **写し**（別の書類として複製する。あとで元を直しても写しは変わらない）にするか、**つながり**（元が変わると追従する）にするか
    - 閲覧者でも取り込めるか（読めるものを自分の手元に写すだけなら、共有カレンダー側の権限は要らない）
    - 入口（詳細画面のボタン「ほかのカレンダーにも追加」など）
    - リマインダーと通知の扱い（両方に入れた予定で 2 回鳴らない・送らないか）
  - 💡 今の `events` は 1 件 ＝ 1 カレンダー（`calendarId` が 1 つ）。ルールでも置き場所の書き換えを禁じている（Step 33-F）。
    写しなら今の形のままで作れる。つながりにする場合はデータの持ち方から見直しが要る
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
