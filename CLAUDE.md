# CLAUDE.md

Kotonowa（ことのわ）のリポジトリ。作業前に以下を必ず読むこと。

- `docs/requirements.md` — 要件定義書＝仕様の正
- `docs/kotlin-grammar.md` — **文法解説のルールと既出文法の一覧。コードを提示する前に必ず読む。**

## このプロジェクトでの進め方（重要）

### 1. コードはユーザー本人が書く

**Claude は先回りして実装しない。**

- ユーザーは Android/Kotlin の学習目的でこのアプリを作っている。手を動かすこと自体が目的。
- Claude の役割は「次に何をどう書くか」の**ガイドと設計の相談相手**。
- 勝手にファイルを作って完成させない。ユーザーが「書いて」と明示したときだけ実装する。
- 出すのは**空の骨組みと `TODO` コメント**まで。中身のロジックはユーザーが書く。
- ユーザーが書いたコードのレビュー、エラーの原因説明、詰まったときのヒント出しは積極的に行う。答えを即出しせず、まずヒントから。

### 2. 説明は「中学生の初心者でもわかる」レベルで

専門用語を知っている前提で話さない。以下を徹底する。

- **専門用語は必ず初出時にかみ砕く。** 「DI」「非同期」「シングルトン」「アノテーション」「ビルド」なども例外ではない。
- **身近なたとえを使う。** 例：Repository =「窓口の人」、interface =「約束・お品書き」、ViewModel =「画面の頭脳」、DI =「材料を自動で届けてくれる仕組み」。
- **英語のエラーメッセージは日本語に訳してから**原因を説明する。
- 「〜は自明」「ご存知の通り」「普通は」といった、知っていて当然という前置きを使わない。
- 単語の読み方が難しいものはカタカナを添える（例：`suspend`（サスペンド））。
- コードを見せるときは**1行ずつ、その行が何をしているか**を説明する。

### 2.5 コードを提示したら、必ず文法も解説する（2026-08-02 追加）

ユーザーは「文法が分からないまま呪文を写している」状態を最も嫌う。
**「何をするコードか」に加えて「なぜその書き方になるのか」を毎回添える。**

- ルールと既出文法の一覧は `docs/kotlin-grammar.md` にある。**コードを出す前に必ず読む。**
- そこに**載っていない文法** → その場でかみ砕いて説明し、**同ファイルに追記する**。
- そこに**載っている文法** → 「§1-③」のように**番号で参照**する（毎回ゼロから説明し直さなくてよい）。
- 記号は読み方を日本語で示す（`.` は「〜の」、`?:` は「〜がなければ」）。
- 「よくある書き方です」「お決まりです」で済ませない。

### 3. 毎回「今どこ・次に何を・なぜ繋がるか」を示す

ユーザーが全体像を見失わないよう、**各ステップの説明に必ず以下3点を含める**。

1. **今どこにいるか** — Phase とステップ番号、ロードマップ上の現在地。
2. **次に何をするか** — 具体的な作業と、終わったらどう動作確認するか。
3. **それが何に繋がるか** — 今作っているものが後のどの機能で使われるのか。
   例：「今書く `AuthRepository` は、Step 6 の `LoginViewModel` から呼ばれ、Phase3 では共有カレンダーのメンバー判定にも使う」

土台作り（Hilt・Navigation など）は見た目が変わらず手応えがないため、**「これは後の何のためなのか」を特に丁寧に**説明する。

### 4. 進め方の設定（2026-07-24 にユーザーが選択）

- **解説の粒度**: 手厚め。新しい API は1行ずつ意味を説明し、Kotlin の文法（ラムダ、`suspend`、`by` 委譲など）も初出時に補足する。
- **実装順**: 仕様書 §8.2 の通り。先に Hilt・Navigation・パッケージ構成の土台を作ってから、ログイン画面を実装する。「まず動かして後からリファクタ」方式は採らない。

## プロジェクト概要

| 項目 | 内容 |
|---|---|
| 名称 | Kotonowa（ことのわ） |
| 種別 | スケジュール・タスク管理アプリ（TimeTree 的な複数人共有を将来的に想定） |
| 目的 | Kotlin/Android の技術力向上、ポートフォリオ |
| 重視点 | **認証・認可（ロール管理）** と **通知** |
| Firebase プロジェクト | `kotonowa-3b2fb` |

## 現在の状態

**Phase 1（認証）完了（2026-08-02）。Phase 2 進行中 — データ層まで完成（2026-08-08）。**

### Phase 2 の進捗

| Step | 内容 | 状態 |
|---|---|---|
| 13 | `domain/model/ScheduleItem`（sealed class。Event / Task） | ✅ |
| 14 | `domain/repository/ScheduleRepository`（interface） | ✅ |
| 15 | `data/repository/ScheduleRepositoryImpl`（Firestore 実装） | ✅ |
| 16 | カレンダー画面（`presentation/calendar/`） | ⬅️ 進行中 |

Step 15 の内訳：A/B 骨組み → C `addItem`/`toMap` → D `updateItem`/`deleteItem` →
E `getItem`/`toScheduleItem` → F `observeItems`（`callbackFlow` + `addSnapshotListener`）。

#### Step 16 の内訳

| | 内容 | 状態 |
|---|---|---|
| 16-A | `CalendarUiState`（items / isLoading / errorMessage） | ✅ |
| 16-B | `CalendarViewModel` の骨組み（Repository 2つ・StateFlow・calendarId） | ✅ |
| 16-C | `init` で `observeItems` を collect して UiState に反映 | ✅ |
| 16-D | `CalendarScreen`（一覧＋動作確認用の仮「＋」ボタン） | ⬅️ 次 |
| 16-E | `KotonowaNavHost` の HOME を差し替え、`presentation/home/` を削除 | — |
| 16-F | 実機で確認＋Firestore の複合インデックス作成 | — |

**16-D でやること**：`CalendarScreen` を作り、`uiState` を `collectAsStateWithLifecycle` で受けて
一覧を表示する。動作確認用に、押すとダミー予定を `addItem` する仮「＋」ボタンも置く
（Step 17 の作成画面ができたら外す）。

**16-F の注意**：`observeItems` のクエリは `calendarId` の等価条件と `sortAt` の範囲条件を
組み合わせるため、**Firestore の複合インデックスが必要**。初回実行時に出るエラーメッセージ内の
URL を開けば作成できる。

#### Phase 2 の設計判断（詳細は `docs/requirements.md` §4）

- 個人カレンダーの `calendarId` は**そのユーザーの `uid`**。`calendars` コレクションは作らない
- `sortAt`（Event は `startAt`、Task は `dueAt` と同値）で期間クエリと並べ替えを行う
- 画面はまず**一覧だけ**作る。月の升目は後回し

- ✅ Firebase 依存（BoM 34.16.0 / Auth・Firestore・Analytics）＋ `google-services.json`
- ✅ Hilt 導入済み（`KotonowaApplication`, `di/FirebaseModule`, `di/RepositoryModule`）
- ✅ Navigation Compose 導入済み（`presentation/navigation/KotonowaNavHost`）
- ✅ Clean Architecture のパッケージ構成
- ✅ 認証まわり一式
  - `domain/model/User`, `domain/model/AuthException`
  - `domain/repository/AuthRepository`（login / signUp / sendPasswordResetEmail / loginWithGoogle / logout / currentUser）
  - `data/repository/AuthRepositoryImpl`（Firebase Auth + 日本語エラー変換）
  - `presentation/auth/login/`（`LoginUiState`, `LoginViewModel`, `LoginScreen`）
  - `presentation/auth/signup/`（Step 9）
  - `presentation/auth/passwordreset/`（Step 10）
  - `presentation/home/`（ログイン確認用の仮ホーム。Phase2 でカレンダーに置き換える）
  - `presentation/splash/`（Step 11。`SplashUiState` は sealed interface）
- ✅ 自動ログイン判定（Step 11）— 起動時に `currentUser` を見て HOME / LOGIN を出し分ける
- ✅ Google Sign-In（Step 12）— 実機相当のエミュレータで動作確認済み

### Step 12（Google Sign-In）の実装メモ

Credential Manager（`androidx.credentials`）を使っている。`GoogleSignInClient` は非推奨なので
古い記事のやり方はそのままでは動かない。役割分担は以下の通り。

| 層 | 担当 |
|---|---|
| `LoginScreen` | Credential Manager を呼んでダイアログを出し、**ID トークンを取得**する。ダイアログには `Activity` が要るので画面の仕事 |
| `LoginViewModel.loginWithGoogle(idToken)` | 受け取ったトークンを Repository に渡す |
| `AuthRepositoryImpl.loginWithGoogle(idToken)` | `GoogleAuthProvider.getCredential` → `signInWithCredential` で Firebase にログイン |

- `setServerClientId` に渡すのは `R.string.default_web_client_id`（`google-services.json` の
  `client_type: 3` から自動生成される**ウェブ用**クライアント ID）。Android 用（`client_type: 1`）ではない。
- `setFilterByAuthorizedAccounts(false)` にしないと初回ログイン時に候補ゼロでダイアログが出ない。
- キャンセル・アカウント0件は**例外**で飛んでくる。`try/catch` で受けないとアプリが落ちる。
  `catch` は具体的な型（`GetCredentialCancellationException` → `NoCredentialException`）を先に、
  おおまかな `GetCredentialException` を最後に置く。

### 検証環境のハマりどころ（2026-08-02）

- **Android 17 プレビュー版（`dev-keys`）のエミュレータでは Google アカウントを追加できない。**
  ログイン処理が途中で異常終了する。正式リリース版（`release-keys`）のイメージを使うこと。
  確認方法: `adb shell getprop ro.build.tags`
- 動作確認は `Medium_Phone_API_36.1`（Android 16 / `google_apis_playstore`）で行った。
- エミュレータの Gboard が日本語ローマ字入力だと、`adb shell input text` の英字が
  ひらがなに変換される。`adb shell ime disable <IME>` で一時的に無効化すると直接入力できる（後で `ime enable` で戻す）。
- 設定アプリが開けないときは `adb shell am start -a android.settings.ADD_ACCOUNT_SETTINGS` で
  目的の画面だけ直接開ける。

## 技術スタック

### 導入済み
- Jetpack Compose（BOM 2026.02.01）/ Material3
- Firebase Authentication, Cloud Firestore, Analytics
- Kotlin 2.2.10 / AGP 9.2.1 / minSdk 26 / targetSdk 36

- Hilt 2.60.1（KSP 2.2.10-2.0.2）、Navigation Compose 2.9.5
- kotlinx-coroutines-play-services（Firebase の `Task` を `await()` で待つため）

### これから導入（仕様書 §2 の予定）
- WorkManager / AlarmManager（ローカル通知）— Phase2
- FCM + Cloud Functions（プッシュ通知）— Phase4
- java.time（minSdk 26 なので desugaring 不要）

### アーキテクチャ
MVVM + Clean Architecture。パッケージ構成は仕様書 §8.2 の通り：

```
com.example.kotonowa/
├── data/          # remote(Firestore/Auth), local(WorkManager), repository実装
├── domain/        # model(ScheduleItem等), repositoryのinterface, usecase
├── presentation/  # auth, home, calendar, common
└── di/            # Hiltモジュール
```

依存の向き： `presentation → domain ← data`（domain は何にも依存しない）

## ロードマップ

```
Phase0: セットアップ（Firebase疎通）      ← 完了
Phase1: 認証（サインアップ/ログイン/ログアウト）  ← 完了
Phase2: 個人のスケジュール/タスク管理 + ローカル通知  ← 今ここ
Phase3: 共有カレンダー + ロールベース認可
Phase4: プッシュ通知
```

各フェーズ終了時に必ず「動くアプリ」がある状態を保つ。

## データモデル

Firestore のコレクション構造は `docs/requirements.md` §4 が正。要点のみ：

- `events` は**フラットなトップレベル**（所属する全カレンダーを横断クエリするため）
- `calendars/{id}/members` は**サブコレクション**（セキュリティルールからロール判定しやすいため）
- Kotlin 側では予定とタスクを `sealed class ScheduleItem`（`Event` / `Task`）で統一表現し、`when` で網羅分岐する

## コマンド

```powershell
.\gradlew.bat :app:assembleDebug      # デバッグビルド
.\gradlew.bat :app:installDebug       # 実機/エミュへインストール
.\gradlew.bat test                    # ユニットテスト
.\gradlew.bat :app:lint               # Lint
```

`adb` は PATH にないので `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe` をフルパスで使う。

## 既知の懸案

- **パッケージ名が `com.example.kotonowa`**。Firebase 登録と一致しているため動作に支障はないが、`com.example` は Play Store に公開できない。本気で公開する場合は早い段階でのリネーム＋Firebase 再登録が必要。ポートフォリオ用途なら現状維持で問題ない。
- `gradle/libs.versions.toml` で `coreKtx` と `lifecycleRuntimeKtx` を意図的にダウングレード済み（1.19.0→1.18.0 / 2.11.0→2.10.0）。戻さないこと。
- **Hilt は 2.60.1 以上が必須。** 2.57.2 以下は AGP 9 で削除された `BaseExtension` API を使うため「Android BaseExtension not found」で失敗する。
- **`gradle.properties` の `android.disallowKotlinSourceSets=false` は KSP の回避策。** Kotlin 2.2.10 に対応する KSP は 2.0.2 が最新で、これは AGP 9 の built-in Kotlin に未対応（生成コードの登録に旧 `kotlin.sourceSets` DSL を使う）。KSP が AGP 9 に対応したらフラグを削除する。
- `hiltViewModel()` は `androidx.hilt.lifecycle.viewmodel.compose` から import する。`androidx.hilt.navigation.compose` の方は非推奨。
- Firestore のセキュリティルールは未設計（テストモードのまま）。テストモードは作成から30日で失効するので Phase3 までに本設計する。

## ドキュメント

- `docs/requirements.md` — 要件定義書。**仕様の正**。決定が変わったら都度更新する。
- `README.md` — 対外向け（ポートフォリオ）の紹介。
