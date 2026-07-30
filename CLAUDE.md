# CLAUDE.md

Kotonowa（ことのわ）のリポジトリ。作業前にこのファイルと `docs/requirements.md`（要件定義書＝仕様の正）を参照すること。

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

**Phase 1（認証）進行中。メール/パスワードの認証画面（ログイン・サインアップ・パスワードリセット）は実装済み。**

- ✅ Firebase 依存（BoM 34.16.0 / Auth・Firestore・Analytics）＋ `google-services.json`
- ✅ Hilt 導入済み（`KotonowaApplication`, `di/FirebaseModule`, `di/RepositoryModule`）
- ✅ Navigation Compose 導入済み（`presentation/navigation/KotonowaNavHost`）
- ✅ Clean Architecture のパッケージ構成
- ✅ 認証まわり一式
  - `domain/model/User`, `domain/model/AuthException`
  - `domain/repository/AuthRepository`（login / signUp / sendPasswordResetEmail / logout / currentUser）
  - `data/repository/AuthRepositoryImpl`（Firebase Auth + 日本語エラー変換）
  - `presentation/auth/login/`（`LoginUiState`, `LoginViewModel`, `LoginScreen`）
  - `presentation/auth/signup/`（Step 9）
  - `presentation/auth/passwordreset/`（Step 10）
  - `presentation/home/`（ログイン確認用の仮ホーム。Phase2 でカレンダーに置き換える）
- ❌ スプラッシュ／自動ログイン判定（Step 11）— 現状は起動時に必ずログイン画面から始まる
- ❌ Google Sign-In（Step 12）

### Phase1 の残ステップ

| # | やること |
|---|---|
| 11 | スプラッシュ画面で `currentUser` を見て自動ログイン判定 |
| 12 | Google Sign-In（`google-services.json` に OAuth クライアント設定済み） |

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
Phase1: 認証（サインアップ/ログイン/ログアウト）  ← 今ここ（Step 11・12 が残り）
Phase2: 個人のスケジュール/タスク管理 + ローカル通知
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
