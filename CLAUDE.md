# CLAUDE.md

Kotonowa（ことのわ）のリポジトリ。作業前に以下を必ず読むこと。

- `docs/requirements.md` — 要件定義書＝仕様の正
- `docs/kotlin-grammar.md` — **文法解説のルールと既出文法の一覧。コードを提示する前に必ず読む。**

## このプロジェクトでの進め方（重要）

### 1. コードはユーザー本人が書く

**Claude は先回りして実装しない。**

- ユーザーは Android/Kotlin の学習目的でこのアプリを作っている。手を動かすこと自体が目的。
- Claude の役割は「次に何をどう書くか」の**ガイドと設計の相談相手**。
- **ファイルとパッケージの作成は Claude が行う**（2026-09-20 にユーザーが指示）。
  新しい画面や層を足すとき、フォルダを切ってファイルを置くところまでは Claude の担当。
  ただし**中身は骨組みと穴埋め（`____`）まで**で、埋めるのはユーザー。
- 勝手に中身まで書いて完成させない。ユーザーが「書いて」と明示したときだけ実装する。
- **Firestore のセキュリティルールは例外で、Claude が完成形を書く**（2026-10-14 にユーザーが指示）。
  ユーザーは 1 行ずつの解説（grammar §4-(118)）で理解する。Kotlin のコードは引き続き穴埋め。
- **Cloud Functions（`functions/` の TypeScript）も同じく Claude が完成形を書く**（2026-11-18 にユーザーが選択）。
  1 行ずつ解説する。FCM の受信・トークン登録など Android 側は穴埋め。
- 出すのは**空の骨組みと `TODO` コメント、穴埋めの `____`**まで。中身のロジックはユーザーが書く。
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

### 2.6 コードを出す前に「何の動きを作っているのか」を先に説明する（2026-09-12 追加）

**コード（穴埋め・骨組み・ヒントを含む）を提示する前に、必ずその前置きを書く。**
「書き方」から始めると、ユーザーは何のために手を動かしているのか分からないまま写すことになる。

順番は必ず以下にする。

1. **ユーザー目線の動き** — これができると**アプリ上で何が起きるようになるのか**を1〜2行で。
   例：「編集画面を開いたら、前に保存した内容が最初から入力欄に入っている」
2. **全体の流れ** — どこから呼ばれ、どの順で動き、どこへ繋がるか。矢印の図か表で示す。
   たとえを添える（編集画面＝白紙の用紙、`load`＝倉庫から前の紙を取ってくる係）。
3. **出てくる箱の役割** — 変数・プロパティが**それぞれ何のためにあるか**を表で。
   「なぜ別々に持つ必要があるのか」まで書く（例：`originalItem` は `_uiState` と違い、ユーザーが触らない部分を覚えておく係）。
4. **そのうえでコード／穴埋めを出す**（§2.5 の文法解説つき）。

⚠️ **画面上で何も変わらない作業のときは、それを明言する。**
「今は配管を作っている段階なので、実機では何も変わりません」と先に言っておく。
言わないと「書いたのに動かない＝失敗した」と誤解させる（§3 の土台作りの話と同じ）。

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

**Phase 3（共有カレンダー＋ロールベース認可）に着手（2026-09-23〜）。ブランチは `feature/phase3-shared-calendar`。
Step 27〜30（モデル・`users` 自己登録・`CalendarRepository`）完了、Step 31（カレンダー一覧画面）完了（2026-10-04）。
Step 32（カレンダー作成画面）完了（2026-10-14）。Step 33（表示するカレンダーの切り替え）完了（2026-10-22）。
Step 34（メンバー一覧・ロール変更・外す）完了（2026-11-02）。Step 35（招待送信）完了（2026-11-16）。Step 36（招待受信・承認/拒否）完了（2026-11-17）。Step 37（ロールベースのルール）完了（2026-11-17）。Step 38（画面のロール出し分け）完了（2026-11-17）— Phase 3 の予定ステップはすべて完了。手直しとして Step 39（メールアドレスの持ち主確認）完了（2026-11-17）。**Phase 4（プッシュ通知）に着手（2026-11-18〜）。ブランチは `feature/phase4-push-notification`。**

Phase 1（認証）完了（2026-08-02）。Phase 2（個人のスケジュール/タスク管理＋ローカル通知）
完了（2026-09-22）— Step 26 でリマインダー通知が実機で鳴るところまで確認した。
次は Phase 3（共有カレンダー＋ロールベース認可）。以下は Phase 2 の経緯 —
作成→保存→一覧反映に加え、
行タップ→詳細→削除まで実装（2026-09-02）。Step 18 は 2026-09-03 の実機確認（18-F）で完了。
Step 19（編集）は 2026-09-15 の実機確認（19-F）で完了 — 一覧→詳細→編集→上書き保存が通った。
Step 20 で詳細画面を 1 件購読（`observeItem`）に変え、「保存後に戻った詳細画面が古いまま」を解消（2026-09-15）。
Step 21 で一覧の行からタスクの完了/未完了を切り替えられるようにした（2026-09-19）。
Step 22 で `TopAppBar` にログアウトボタンを復活させた（2026-09-19）。
Step 25 で設定画面を作り、ログアウトをそこへ移した（2026-09-21）。`TopAppBar` は「設定」になった。
Step 23 で月の升目カレンダーを実装（2026-09-20）。仕様書 §5 の「月表示カレンダー＋下部に
選択日の予定/タスク一覧」がこれで揃った。**

### Phase 2 の進捗

| Step | 内容 | 状態 |
|---|---|---|
| 13 | `domain/model/ScheduleItem`（sealed class。Event / Task） | ✅ |
| 14 | `domain/repository/ScheduleRepository`（interface） | ✅ |
| 15 | `data/repository/ScheduleRepositoryImpl`（Firestore 実装） | ✅ |
| 16 | カレンダー画面（`presentation/calendar/`） | ✅ |
| 17 | 作成画面（`presentation/calendar/edit/`） | ✅ |
| 18 | 詳細・削除画面（`presentation/calendar/detail/`） | ✅ |
| 19 | 編集画面（作成画面 `presentation/calendar/edit/` を 1 枚で 2 役） | ✅ |
| 20 | 詳細画面を 1 件購読（`observeItem`）に変更 | ✅ |
| 21 | タスクの完了チェック（一覧の行の `Checkbox`） | ✅ |
| 22 | ログアウトボタンの復活（`TopAppBar`） | ✅ |
| 23 | 月の升目カレンダー（升目＋選択日で一覧を絞る） | ✅ |
| 24 | 升目の見た目の調整（今日・選択中・予定ありの出し分け） | ✅ |
| 25 | 設定画面（プロフィール・通知設定の枠・ログアウト） | ✅ |
| 26 | ローカル通知（WorkManager でリマインダーを予約） | ✅ |

Step 15 の内訳：A/B 骨組み → C `addItem`/`toMap` → D `updateItem`/`deleteItem` →
E `getItem`/`toScheduleItem` → F `observeItems`（`callbackFlow` + `addSnapshotListener`）。

#### Step 16 の内訳

| | 内容 | 状態 |
|---|---|---|
| 16-A | `CalendarUiState`（items / isLoading / errorMessage） | ✅ |
| 16-B | `CalendarViewModel` の骨組み（Repository 2つ・StateFlow・calendarId） | ✅ |
| 16-C | `init` で `observeItems` を collect して UiState に反映 | ✅ |
| 16-D | `CalendarScreen`（一覧＋動作確認用の仮「＋」ボタン） | ✅ |
| 16-D-3-b | 行の見た目（`ScheduleItemRow`。予定/タスクの出し分け、日時の整形） | ✅ 08-14 |
| 16-D-3-c | `@Preview` で 4 パターンを一度に確認できるようにする | ✅ 08-14 |
| 16-D-3-d | 状態ごとの色分け（ラベルをバッジ化＋完了タスクの打ち消し線） | ✅ 08-15 |
| 16-E | `KotonowaNavHost` の HOME を差し替え、`presentation/home/` を削除 | ✅ |
| 16-F | 実機で確認＋Firestore の複合インデックス作成 | ✅ |

**Step 16 の本体は 2026-08-12 に動作確認まで完了。** 「＋」を押すと Firestore に保存され、
`observeItems` の Flow 経由で一覧が自動更新されることを実機相当のエミュレータで確認した。

**行の見た目（16-D-3-b/c/d）は 2026-08-14 に着手。** 表示ルールは以下の通り（日時は書式の例）。

| 状態 | ラベル | 日時 |
|---|---|---|
| Event / 時間あり | 予定 | `8/14(金) 14:00～15:00` |
| Event / 終日 | 予定 | `8/14(金) 終日` |
| Task / 未完了 | タスク未完了 | `期限 8/16(日) 23:59` |
| Task / 完了 | タスク完了 | `期限 8/14(金) 8:00` |

日時の型紙は `DateTimeFormatter` を 3 枚（日付＋時刻 / 日付のみ / 時刻のみ）用意し、
`Instant` の拡張関数から使っている。完了状態は**ラベル側**に出す
（完了しても期限は変わらないので、日時側で分岐しない）。

**`@Preview` を入れた理由。** `addDummyItem` は `Event` / `allDay = false` しか作れず、
エミュレータでは 4 パターンのうち 1 つしか確認できなかった。
`PREVIEW_ITEMS` に 4 件を固定値（`Instant.parse`）で持たせ、Preview パネルで一度に見比べる。
Step 17 以降も画面を作るたびに使う。

**16-D-3-d（色分け）は 2026-08-15 に完了。** 状態ごとの見た目は以下の通り。

| 状態 | バッジの色 | タイトル |
|---|---|---|
| 予定（Event） | `primaryContainer` | `onSurface` |
| タスク未完了 | `tertiaryContainer` | `onSurface` |
| タスク完了 | `surfaceVariant` | `onSurfaceVariant` ＋打ち消し線 |

色は `Color` の直書きではなく **`MaterialTheme.colorScheme` から借りる**。
ダークテーマで破綻せず、`ui/theme/` の 1 か所で調整できるため。
背景色と文字色は必ずペア（`〇〇` と `on〇〇`）で使う。

**`RowStyle`（`data class`）に状態ごとの見た目をまとめている。**
ラベル用・バッジ色用・タイトル装飾用と `when` を分けて書くと、状態が増えたとき
一部だけ直し忘れる。判定を 1 回にして結果を詰める形にすると、
`RowStyle` に項目を足した時点で**全分岐がコンパイルエラーになる**ので直し忘れが起きない。
`Surface` / `Text` 側には条件分岐を持たせず、`rowStyle.〜` を読むだけにしてある。

ログアウトボタンは `HomeScreen` の削除に伴い一時的に消えている。設定画面か `TopAppBar` に置き直す。

**Firestore 側でやったこと（2026-08-12）**

- セキュリティルールを `allow read, write: if request.auth != null;` に変更
  （本番モードの初期値 `if false` のままだと `PERMISSION_DENIED`）
- 複合インデックス（`events`: `calendarId` 昇順 ＋ `sortAt` 昇順）を作成
  `observeItems` は等価条件と範囲条件を組み合わせるため必須。
  エラーメッセージ内の URL を開けば設定入力済みの画面が出る

2026-08-16 に `events` コレクションのダミー（`addDummyItem` 由来の「テスト予定」）を
コンソールから全削除した。アプリ側に削除機能ができるまでは、**溜まったテストデータは
Firestore コンソールから消す**（コレクションを消してもインデックスは残るので作り直し不要）。

**仮「＋」ボタン**（`CalendarViewModel.addDummyItem`）は **2026-08-16 に廃止**。
「＋」は作成画面を開くだけになった（Step 17-F）。

**Firestore セキュリティルールを本設計した（2026-09-04）。** 要件定義書 §7 のバックログ項目。
それまでの `allow read, write: if request.auth != null;`（ログインしていれば誰の予定/タスクでも
読み書きできる）を、`events` コレクションに絞った所有者ベースのルールに置き換えた。

```
match /events/{eventId} {
  allow read, update, delete: if request.auth != null
    && request.auth.uid == resource.data.calendarId;
  allow create: if request.auth != null
    && request.auth.uid == request.resource.data.calendarId
    && request.auth.uid == request.resource.data.createdBy;
}
```

Phase2 は個人カレンダーの `calendarId` がそのユーザーの `uid` そのもの（要件定義書 §4）なので、
「`calendarId` が自分の `uid` と一致するか」がそのまま「自分の予定/タスクか」の判定になる。
`create` だけ `createdBy` も自分と一致することを追加で要求している。`calendarId` は「どの部屋に
置くか」、`createdBy` は「誰が置いたと名乗るか」で、Kotlin 側のチェックを迂回して直接 Firestore に
書き込まれた場合に `createdBy` を詐称されるのを防ぐため。
`calendars` / `users` / `invites` はまだコードから一度も書き込んでいない（Phase3 以降で使う）ため、
ルールを書いていない（Firestore はマッチしないパスをデフォルトで拒否する）。
コンソールに貼り替え、実機で読み込み・作成・更新・削除が引き続き動くことを確認した。

#### Step 17 の内訳

| | 内容 | 状態 |
|---|---|---|
| 17-A〜C | `ScheduleEditUiState` ＋ `ScheduleItemType`（enum） | ✅ 08-15 |
| 17-D-1/2 | `ScheduleEditViewModel`（入力の保持・`save()` の組み立て） | ✅ 08-15 |
| 17-D-3 | `addItem` を呼び、結果を UiState に反映 | ✅ 08-16 |
| 17-E-1 | `ScheduleEditScreen`（入力欄・保存/戻る・`@Preview`） | ✅ 08-16 |
| 17-E-2 | 予定 / タスクの切り替えボタン（`SingleChoiceSegmentedButtonRow`） | ✅ 08-17 |
| 17-E-3 | 終日スイッチ（予定のときだけ出す） | ✅ 08-16 |
| 17-E-4 | `isSaved` を見て `onSaved()` を呼ぶ（`LaunchedEffect`） | ✅ 08-16 |
| 17-F | `NavHost` に登録し、一覧の「＋」から開けるようにする | ✅ 08-16 |
| 17-G-1/2 | 入力した日時を `save()` で使う（UiState に日付・時刻を持つ） | ✅ 08-19 |
| 17-G-3 | `DateTimeField`（「開始 ｜ 8/21(金) ｜ 15:00」の行） | ✅ 08-19 |
| 17-G-4-1/2 | `PickerTarget`（enum）でどのピッカーを開くか持つ | ✅ 08-19 |
| 17-G-4-3-1 | `DateSelectDialog`（`DatePickerDialog` + `DatePicker`） | ✅ 08-21 |
| 17-G-4-3-2 | 4 つの入力欄から呼び出す＋「終了」の行 | ✅ 08-22 |
| 17-G-4-3-3 | `TimeSelectDialog`（`TimePickerDialog` + `TimePicker`） | ✅ 08-22 |
| 17-H | タスクの「期限」の行 | ✅ 08-30 |
| 17-I | 開始 > 終了 のバリデーション | ✅ 08-30 |

**17-F まで完了した時点で経路が 1 本通った（2026-08-16 に動作確認）。**
＋ → 入力 → 保存 → 一覧に反映、までをエミュレータで確認済み。
保存後に一覧を読み直す処理は**書いていない**。`observeItems` の Flow（Step 15-F）が
Firestore の変更を拾って勝手に流してくれるため。

**17-E は 2026-08-17 に完了。** 予定/タスクの切り替えと終日スイッチが入り、
**タスクが作れるようになった**（それまでは `itemType` が `EVENT` から動かなかった）。
Step 16-D-3-d の色分けを実データで確認できたのもこの時点。

入力部品はすべて**状態を持たない**。`OutlinedTextField` の `value`/`onValueChange`、
`Switch` の `checked`/`onCheckedChange`、`SegmentedButton` の `selected`/`onClick` は
どれも同じ形で、**今の値は UiState から渡し、変化は ViewModel へ返す**（状態ホイスティング）。
画面側に `remember` の状態を持たせないこと。持たせると UiState とズレる。

**17-G は 2026-08-22 に完了。** 開始・終了の日付と時刻を選んで保存できるようになり、
「今から 1 時間」に固定されていた制限がすべて解消した。一覧も選んだ日時の順に並ぶ。

日付と時刻でピッカーの扱いが違う。

| | 覚え書きの型 | 選ばれた値 | 取り出し方 |
|---|---|---|---|
| 日付 | `DatePickerState` | `selectedDateMillis`（`Long?`） | `?.let` ＋ `Instant.ofEpochMilli` → `atZone(ZoneOffset.UTC)` → `toLocalDate()` |
| 時刻 | `TimePickerState` | `hour` / `minute`（`Int`） | `LocalTime.of(hour, minute)` の 1 行 |

日付側で `ZoneOffset.UTC` を使うのは、**ピッカーが UTC の 0 時と決めて数値を渡してくる**ため。
保存時に端末のタイムゾーンを使う（`ScheduleEditViewModel` の `toInstant`）のとは逆なので注意。

`TimePickerDialog` は Material3 1.4.0 にある（自作の `AlertDialog` は不要）。ただし
**`title` に既定値が無く必須**。省略すると `android.app.TimePickerDialog` と区別できず
「None of the following candidates is applicable」になる。
`TimePicker` / `rememberTimePickerState` は実験中の API なので
`@OptIn(ExperimentalMaterial3Api::class)` が要る。

ダイアログを出す `when (uiState.pickerTarget)` は **`else` を書かず枝を名指し**する。
`17-H` で `DUE_DATE` / `DUE_TIME` を足したとき、黙って `else` に吸い込まれず
「枝が足りない」とコンパイルエラーで気づけた（現在は 6 本＋ `null -> {}`）。

**17-H は 2026-08-30 に完了。** タスクの期限を開始の入力から独立させた。
`ScheduleEditUiState` に `dueDate` / `dueTime`（既定 23:59）を足し、TASK の枝に「期限」の
`DateTimeField` を出し、`PickerTarget` に `DUE_DATE` / `DUE_TIME` を追加。最後に `save()` の
`dueAt` を `startDate` 流用から `dueDate` / `dueTime` に差し替えた（H-5）。
これで作成画面の日時入力（開始・終了・期限）はすべて独立した。

**17-I は 2026-08-30 に完了。** `save()` で `startAt` / `endAt`（`allDay` 補正込み）を
`launch` の外に作り、`EVENT` かつ 開始 < 終了 でないなら `errorMessage` を立てて `return` する。
タイトル未入力チェックと同じく **`isSaving` を立てる前に弾く**（あとで戻さなくてよい）。
同値（開始 == 終了）も不正扱い。タスクは期限が一点なのでこのチェックは EVENT のみ。
**これで Step 17（作成画面）は完了。** 作成→保存→一覧反映の経路に穴が無くなった。

**画面遷移の分担。** 画面には `navController` を渡さず、**`() -> Unit` の呼び鈴だけ**を持たせる。

| 層 | 知っていること |
|---|---|
| `ScheduleEditViewModel` | `isSaved` の旗を立てるだけ。画面遷移を知らない |
| `ScheduleEditScreen` | 旗が立ったら `onSaved()` を鳴らすだけ。行き先を知らない |
| `KotonowaNavHost` | 鳴ったら `popBackStack()`。**行き先を決めるのはここだけ** |

`CalendarScreen` も同じで、「＋」が押されたら `onAddClick()` を鳴らすだけ。
こうしておくと画面が単体で `@Preview` でき、遷移先を変えても画面側を触らずに済む。
Step 18 以降の画面も同じ形で足すこと。

#### Step 18 の内訳（詳細・削除画面）

| | 内容 | 状態 |
|---|---|---|
| 18-A | 一覧の行タップを `NavHost` まで通す | ✅ 09-01 |
| 18-B/C | `ScheduleDetailUiState` ＋ `ScheduleDetailViewModel`（`load()` / `delete()`） | ✅ 09-01 |
| 18-D | `ScheduleDetailScreen`（`Screen` ＋ `Content` の2段） | ✅ 09-01 |
| 18-E | `NavHost` に `composable(SCHEDULE_DETAIL)` を登録 | ✅ 09-02 |
| 18-F | 実機で「行タップ→詳細→削除→一覧へ戻る」を確認 | ✅ 09-03 |

**18-A。** `ScheduleItemRow` に `onClick`、`CalendarScreen` に `onItemClick(id)` を足し、
`Card` の押せる版に繋いだ。`Routes` に `SCHEDULE_DETAIL`（`"schedule_detail/{itemId}"`）と
文字列を組み立てる `scheduleDetail(id)` ヘルパーを追加。この時点では着地先が無いので押しても何も起きない。

**18-B/C。** `ScheduleDetailViewModel` は `SavedStateHandle` から
`checkNotNull(savedStateHandle["itemId"])` で `itemId` を取り出す（詳細画面は必ず `itemId` 付きで
開かれるので、無ければ navigate の書き間違い＝バグ。静かに握りつぶさず即落とす。grammar §(87)）。
`init` で `load()` → `getItem` の結果を `onSuccess`/`onFailure` で `uiState` に反映。
`delete()` は `save()` と同じく門番2つ（二度押し防止・`isDeleting`）→ `deleteItem` → 成功で `isDeleted`。
`ScheduleDetailUiState` の `isLoading` は **`true` 始まり**（開いた瞬間 `getItem` が走るため。
`CalendarUiState` が `false` 始まりだったのと逆）。削除は `isSaving`/`isSaved` と同じ形で `isDeleting`/`isDeleted`。

**18-D。** `ScheduleEditScreen` と同じ2段構え（`Screen` = ViewModel 係、`Content` = 見た目係）。
`ScheduleDetailContent` は条件だけの `when {}`（§5-(64)）で `isLoading` → `item == null` → それ以外 を出し分け。
本体は `when (item)` で Event / Task の日時文字列を組み立て（`CalendarScreen` の `subText` とほぼ同じ）、
タイトル・日時・メモ・エラー・削除ボタン・戻るボタンを縦に並べる。
`description` / `errorMessage` は一度ローカル `val` に受けてから `if (x != null)`（スマートキャストのため）。

**案B の判断。** Preview 用データ（`PREVIEW_EVENT` / `PREVIEW_TASK`）と日時フォーマッタは
**この画面が自前で持つ**。`PREVIEW_ITEMS` は `CalendarScreen` に `private`、詳細は1件表示で必要な形も違う。
3画面目で同じものが要るようになったら共通ファイルに切り出す（今は早い）。

**18-E。** `composable(Routes.SCHEDULE_DETAIL)` に
`arguments = listOf(navArgument("itemId") { type = NavType.StringType })` を宣言。
`onNavigateBack` / `onDeleted` はどちらも `popBackStack()`（行き先を決めるのは `NavHost` だけ、の分担）。
`itemId` は画面に渡さない。Hilt が `hiltViewModel()` を作るとき、ルートの `{itemId}` を
`SavedStateHandle` に自動で詰めるので、ViewModel が直接拾える。

削除後に一覧から行を消す処理は**書いていない**。`observeItems` の Flow（Step 15-F）が
Firestore の変更を拾って流してくれる（作成時と同じ）。

**18-F は 2026-09-03 に Mac 上のエミュレータ（Pixel_7 / Android 16）で確認した。**
これまでの Windows 環境の記述（PowerShell・`adb.exe` のフルパス）とは別マシン。
Mac では JDK が入っておらず `./gradlew` がそのままでは失敗するため、
**Android Studio 同梱の JBR を `JAVA_HOME` に指定**する必要があった
（`/Applications/Android Studio.app/Contents/jbr/Contents/Home`）。
一覧→＋→保存→行タップ→詳細→削除→一覧へ自動で戻る、の一連を確認し、
削除後に一覧に残らないこと・Logcat に `FATAL EXCEPTION` や Firestore 関連のエラーが
出ないことを確認した。**これで Step 18（詳細・削除画面）は完了。**

#### Step 19 の内訳（編集画面）

**画面はもう 1 枚作らない。作成画面（`presentation/calendar/edit/`）を 1 枚で 2 役にする。**
`itemId` 付きで開かれたら編集モード、無ければ作成モード。入力欄・バリデーション・日時ピッカーは
まったく同じものが要るので、2 枚に分けると Step 17 で作ったものを丸ごと複製することになる。

| | 内容 | 状態 |
|---|---|---|
| 19-A | `Routes` に `SCHEDULE_EDIT_ITEM`（`"schedule_edit/{itemId}"`）＋ `scheduleEditItem(id)` | ✅ 09-12 |
| 19-B | `ScheduleEditUiState` に `isLoading`（**`false` 始まり**） | ✅ 09-12 |
| 19-C | `ScheduleEditViewModel` が `itemId` を受け取り、`load()` で入力欄を埋める | ✅ 09-13 |
| 19-D | `save()` を「新規追加」と「上書き」に分ける | ✅ 09-13 |
| 19-E | 詳細画面に「編集」ボタン＋`NavHost` に `composable(SCHEDULE_EDIT_ITEM)` を登録 | ✅ 09-15 |
| 19-F | 実機で「詳細→編集→内容が埋まる→保存で上書き」を確認 | ✅ 09-15 |

**19-B。** `isLoading` が `false` 始まりなのは、**作成モードでは読み込み自体が起きない**ため
（`ScheduleDetailUiState` が `true` 始まりだったのと逆）。編集モードのときだけ `load()` の先頭で立てる。

**19-C。** `SavedStateHandle` から `itemId` を取るのは詳細画面と同じだが、
**`checkNotNull` は使わない**（grammar §4-(87)）。この画面は「＋」からも開かれるので、
`itemId` が無いのは**バグではなく作成モード**。`private val itemId: String?` のまま持ち、
`init` で `if (itemId != null) load(itemId)`。
`if` の中で確定した値を**引数で渡す**ので、`load` の中では `String`（`?` なし）として扱える。

`load()` は `getItem` の結果を `originalItem`（`var`）に控え、`ScheduleItem` → `ScheduleEditUiState` へ
**逆詰め替え**する。`_uiState.update { }` の答えを `when (item)` そのものにしているので
（grammar §5-㊳）、Event / Task の枝を書き漏らすとコンパイルエラーになる。

| 変換 | 書き方 | 理由 |
|---|---|---|
| `Instant` → 日付欄・時刻欄 | `atZone(zone).toLocalDate()` / `.toLocalTime()` | 入力欄は日付と時刻が別（grammar §4-(89)） |
| `description`（`String?`）→ 入力欄（`String`） | `?: ""` | 保存時の `ifBlank { null }` の逆変換 |
| `zone` | `ZoneId.systemDefault()` | 入力したときと同じ時計で戻さないとズレる。UTC はピッカー用 |

`isCompleted` は移さない（編集画面に完了のチェックが無い）。`copy()` は書かなかった項目を
そのまま残すので、Task の枝で `allDay` / 開始・終了を書かなくてよい（grammar §7-㉗）。

**19-D。** Firestore の保存先は `.document(item.id)`（`ScheduleRepositoryImpl.kt:37`）なので、
**id が書類の住所**。編集で新しい UUID を作ると別の書類になり、上書きではなく 1 件増える。

```kotlin
id         = original?.id         ?: UUID.randomUUID().toString()
calendarId = original?.calendarId ?: id      // id はログイン中の uid（175 行目）
createdBy  = original?.createdBy  ?: id
val result = if (original == null) addItem(item) else updateItem(item)
```

| | Firestore の命令 | 書類が無いとき |
|---|---|---|
| `addItem` | `.set(map)` | 新しく作る |
| `updateItem` | `.update(map)` | **エラー**（存在する時だけ書き換える） |

編集は「必ず元の書類がある」場面なので `updateItem`。無ければ異常（消された後に保存した等）として
気づける方がよい。`originalItem` は `var` なので、`save()` の冒頭で `val original` に写して固定する
（grammar §4-(65) の `var` の項）。`_uiState.value` を `val state` に受けるのと同じ理由。

⚠️ **`calendarId` / `createdBy` を「自分の uid」で埋め直さず、元の値を引き継ぐこと。**
Phase2 は 3 つとも同じ値なので取り違えても動いてしまうが、Phase3 の共有カレンダーでは
`calendarId`＝共有カレンダーの id、`createdBy`＝作った個人の uid で**別の値**になる。
埋め直すと予定が共有カレンダーから自分の個人カレンダーへ移動し、
セキュリティルールの `create`（`request.auth.uid == createdBy`）にも弾かれる。
型がどちらも `String` なので**コンパイラは取り違えを見つけられない**。
`id` / `calendarId` / `createdBy` の 3 行は縦に読み、左の名前と `original?.〇〇` の綴りを目で照合する。

💡 `save()` 175 行目の `val id = calendarId ?: return` の `id` は**ログイン中のユーザーの uid**で、
組み立て側の `id =`（予定の id）とは別物。紛らわしいので `val uid` への改名を検討中。

**19-E。** 詳細画面に `onEditClick: (String) -> Unit` を足し、`Button` から `onEditClick(item.id)` を鳴らす。
`NavHost` 側が `{ editId -> navController.navigate(Routes.scheduleEditItem(editId)) }` で行き先を決める
（一覧の `onItemClick` と同じ形）。**`ScheduleEditScreen` は 1 文字も変えていない** — `itemId` は
Hilt が `hiltViewModel()` を作るときに `SavedStateHandle` へ詰めるので、画面は知らなくてよい。

`"schedule_edit"`（作成）と `"schedule_edit/{itemId}"`（編集）は**区切りの数が違うので共存できる**。
「＋」は前者、「編集」は後者に着地する。

**19-F は 2026-09-15 にエミュレータ（Pixel_7）で確認した。** 詳細→編集→入力欄が既存の内容で
埋まる→タイトルを変えて保存→**一覧の行が書き換わる（増えない）**まで、EVENT と TASK の両方で確認。
Logcat に `FATAL EXCEPTION` / Firestore のエラーは出ていない。**これで Step 19 は完了。**

⚠️ **既知の問題（次に直す）。保存後に戻った詳細画面が、編集前の古い内容のまま表示される。**
`ScheduleDetailViewModel.load()` は `init` でしか走らないが、`popBackStack()` で戻ったときの
詳細画面は**バックスタックに残っていた同じ ViewModel** なので `init` が再実行されない。
一覧が自動更新されるのは `observeItems` の Flow（Step 15-F）が効いているからで、
詳細画面は 1 回きりの `getItem` なので取り残される。直し方の候補は 3 つ
（保存後に一覧まで戻す／画面が再表示されたときに読み直す／`getItem` をやめて 1 件を購読する）。

**ハマりどころ：全角スペース（U+3000）。** 日本語入力のまま `=` の後にスペースを打つと混入し、
`Syntax error: Expecting an expression` になる。見た目で判別できないので、
エラーの列番号に何も無いように見えたらこれを疑う。`grep -n '　' <file>` で見つかる。

**この Step で文法メモに追記したもの** … §7-(88)（コンストラクタの `val` あり/なし）、
§4-(89)（`Instant` を `LocalDate` / `LocalTime` にバラす）、§4-(65) に `var` のスマートキャストの項。

#### Step 20 の内訳（詳細画面を購読にする）

**Step 19 の既知の問題（保存後に戻った詳細画面が古いまま）への対処。**
`popBackStack()` で戻った詳細画面はバックスタックに残っていた**同じ ViewModel** なので
`init` が再実行されず、1 回きりの `getItem` では取り残される。一覧が自動更新されるのは
`observeItems` の Flow（Step 15-F）が効いているからで、**同じ仕組みを 1 件版にも広げた。**

| | 内容 | 状態 |
|---|---|---|
| 20-A | `ScheduleRepository` に `observeItem(itemId): Flow<ScheduleItem?>` | ✅ 09-15 |
| 20-B | `ScheduleRepositoryImpl.observeItem`（`callbackFlow` ＋ `document(itemId)`） | ✅ 09-15 |
| 20-C | `ScheduleDetailViewModel.load()` を `getItem` から `collect` に変更 | ✅ 09-15 |
| 20-D | 実機で「編集→保存→詳細が新しくなる」を確認 | ✅ 09-15 |

**選択肢は 3 つあった**（A: 保存後に一覧まで戻す ／ B: 再表示時に読み直す ／ C: 1 件を購読する）。
**C を選んだ理由**は、Step 15-F で作った「Firestore の変更が勝手に流れてくる」という考え方に
アプリ全体を揃えられるため。他端末の変更も自動で届くので Phase3 の共有カレンダーでそのまま活きる。

**戻り値を `Flow<ScheduleItem?>`（`?` 付き）にした理由。** 削除されたとき「無くなった」を
流す必要がある。`Flow<ScheduleItem>` では削除を表現できない。`null` が流れると
`ScheduleDetailContent` の `item == null` の枝（§5-(64)）が「見つかりませんでした」を出す。

**`Result` で包まない理由。** 1 回きりの `getItem` は `Result.failure(e)` を返すが、
流れ続ける `Flow` は `close(error)` で管を閉じ、`collect` 側の `try`/`catch` に飛ばす（§6-㉔）。
`Flow<Result<T>>` は二重の封筒になって読みにくい。`observeItems` も同じ方針。

**一覧版との違いは 3 点だけ。** `whereEqualTo(...)` → `document(itemId)`、
`snapshot?.documents?.mapNotNull` → `snapshot != null && snapshot.exists()` の 2 段構え、
流すものが `List` ではなく 1 件（または `null`）。単一ドキュメントの見張りは**書類が消えたときにも
呼ばれる**ので、`exists()` の確認が要る。

⚠️ **`getItem` は残す。** 編集画面（`ScheduleEditViewModel.load()`）が使い続ける。
入力欄は「開いた瞬間の 1 回」だけ読めばよく、**入力中に外から流れてくると打った内容が消える**。
「1 回きり」と「購読」は用途で使い分ける。

#### Step 21 の内訳（タスクの完了チェック）

**`isCompleted` を切り替える手段がどこにも無かった**ため追加した。置き場所は
「一覧の行 ／ 詳細画面 ／ 両方」から**一覧の行**を選択（いちばんよく使う動線で、手数が最少）。

| | 内容 | 状態 |
|---|---|---|
| 21-A | `CalendarViewModel.toggleCompleted(task)` | ✅ 09-16 |
| 21-B | `ScheduleItemRow` にタスクのときだけ `Checkbox` を出す | ✅ 09-19 |
| 21-C | `CalendarScreen` と `@Preview` の呼び出しを接続 | ✅ 09-19 |
| 21-D | 実機確認（切り替え・色・打ち消し線） | ✅ 09-19 |

**引数の型を `ScheduleItem.Task` にしている**（`ScheduleItem` ではない）。親には `isCompleted` が
無く、`Event` を渡されても困る。**`Task` しか渡せない形にすれば、間違いが呼び出し側の
コンパイルエラーになる**（§7-㉘ の考え方）。

`task.copy(isCompleted = !task.isCompleted, updatedAt = Instant.now())` の 1 行で済むのは
`data class` の `copy()`（§7-㉗）のおかげ。Step 19-D で `original?.id ?: …` と手で引き継いだのと対照的。

**`onSuccess` を書いていない。** 成功してもやることが無いため（一覧の更新は `observeItems` の
Flow が行う。Step 17-F と同じ）。**二度押し防止の門番も置いていない** — 連打されても
「最後に押した状態」が残ればよく、画面遷移も伴わないため。`save()` / `delete()` とは用途が違う。

**画面はチェックの状態を持たない。** `Checkbox` は押しても自分では切り替わらず、
`onToggleCompleted(item)` を鳴らすだけ（§3-(76) 状態ホイスティング）。
Firestore に保存 → Flow で流れてくる → `checked = item.isCompleted` が変わって初めて絵が変わる。

`onCheckedChange = { onToggleCompleted(item) }` と `{ }` で包むのは、`Checkbox` が渡してくる
`Boolean` を捨てて**タスクそのもの**を渡すため（§1-(78) の②）。反転の計算は ViewModel 側にある。
隣の `onToggleCompleted = viewModel::toggleCompleted`（型が一致するのでそのまま渡す＝①）と
対比になっている。

**Step 16-D-3-d で作った `RowStyle` の色分けと打ち消し線が、ここで初めて実データで切り替わる。**

#### Step 22 の内訳（ログアウトボタンの復活）

**Step 16-E で `HomeScreen` を消したときに失われた機能の復活。** Phase1 で作った
`AuthRepository.logout()` が、久しぶりに画面から呼ばれるようになった。

| | 内容 | 状態 |
|---|---|---|
| 22-A | `CalendarViewModel.logout()`（旧 `HomeViewModel` から移植） | ✅ 09-19 |
| 22-B | `CalendarScreen` に `TopAppBar` ＋ 呼び鈴 `onLogout` | ✅ 09-19 |
| 22-C | `KotonowaNavHost` で行き先を決める（`popUpTo` ＋ `inclusive`） | ✅ 09-19 |
| 22-D | 実機確認 | ✅ 09-19 |

**`logout()` に `suspend` が要らない。** `AuthRepository.kt` は `fun logout()`。Firebase の
`signOut()` は端末内のログイン情報を消すだけで**通信しない**ため（`login` などが
`suspend fun …: Result<User>` なのと対照的）。だから `viewModelScope.launch { }` も不要。

```kotlin
onLogout = {
    navController.navigate(Routes.LOGIN) {
        popUpTo(Routes.HOME) { inclusive = true }
    }
}
```

⚠️ **`inclusive = true` が肝。** これが無いとカレンダー画面が履歴に残り、ログアウト後に
端末の「戻る」で中身が見えてしまう。認証を重視するこのアプリでは看過できない。
ログイン成功時に `popUpTo(Routes.LOGIN) { inclusive = true }` で LOGIN を消すのと同じ形。

画面側は `viewModel.logout()` → `onLogout()` の順に呼ぶだけで**行き先を知らない**
（削除された `HomeScreen` と同じ形）。`TopAppBar` は実験中の API なので
`@OptIn(ExperimentalMaterial3Api::class)` が要る（grammar §7-(86)、§3-(90) に追記）。

#### Step 23 の内訳（月の升目カレンダー）

**仕様は「月表示カレンダー＋下部に選択日の予定/タスク一覧」**（`docs/requirements.md` の画面一覧）。
升目は `LazyVerticalGrid` ではなく **`Column` ＋ `Row` ＋ `weight(1f)`** で組んだ。
1 か月は最大 6 週 × 7 日と**件数が決まっており**、`Lazy`（見えている分だけ描く）の利点が無いため。

| | 内容 | 状態 |
|---|---|---|
| 23-A | `CalendarUiState` に `currentMonth` / `selectedDate` | ✅ 09-19 |
| 23-B | `observeMonth(month)` に変更＋月移動＋購読の張り直し | ✅ 09-19 |
| 23-C-1 | `MonthHeader` / `WeekdayHeader` | ✅ 09-19 |
| 23-C-2 | `MonthGrid` / `DayCell` | ✅ 09-20 |
| 23-C-3 | `datesWithItems` ＋ 画面への組み込み | ✅ 09-20 |
| 23-D | 選択日で下の一覧を絞る（`filter`） | ✅ 09-20 |
| 23-E | 実機確認 | ✅ 09-20 |

**升目の作り方。** 1 日の曜日から `dayOfWeek.value % 7` で左端からのずれを出し、
42 個（6 週 × 7 日）の `List<LocalDate?>` を作って `chunked(7)` で 6 段に配る。
**42 個に固定**しているので、5 週で収まる月でも高さが変わらない。
日曜始まりにするため `% 7` が要る（`DayOfWeek.value` は月曜が 1、日曜が 7）。grammar §4-(92)(93)(94)。

**月を移動したら購読を張り直す。** `observeJob?.cancel()` → 新しい月で `observeItems` を `collect`。
止めないと 2 本の管から交互に流れて一覧がちらつく（grammar §2-(91)）。
**日付の選択では張り直さない** — `items` は月まるごと持っているので、`filter`（§4-(95)）で
絞れば足りる。タップのたびに通信するのは無駄。

**`datesWithItems`（点を打つ日）は ViewModel で作る。** `collect` の中で `items` から
`Event` は `startAt`、`Task` は `dueAt` を取り、`toLocalDate()` して `Set` にする。
`Set` なのは、同じ日に何件あっても点は 1 つでよく、升目 42 個ぶん `in` を呼ぶため。
いっぽう **`dayItems`（選択日の絞り込み）は画面側**。新しい情報を作るのではなく、
今ある `items` を見る角度を変えているだけだから。
**「新しい情報を作るなら ViewModel、今ある情報を絞る/並べ替えるだけなら画面」**で使い分ける。

⚠️ **`cancel()` を `catch (e: Exception)` で受けてはいけない**（2026-09-20 に実際に踏んだ）。
止められた `collect` は `CancellationException` を投げるが、これは失敗ではなく
「意図して取り止めた」合図。`Exception` で受けると画面に
`StandaloneCoroutine was cancelled` と出て、しかも `when` の `message != null` の枝が
先に当たるので**一覧がまったく描かれなくなる**。`catch (e: CancellationException) { throw e }` を
`catch (e: Exception)` より**上**に置く（順番が命。§6-㉕）。grammar §2-(91) に追記済み。

⚠️ **升目は `when` の外に出す。** 読み込み中やエラーでも**カレンダーの骨格は出したまま**にし、
下半分だけ出し分ける。`when` の中に入れると読み込みのたびに升目が消えてちらつく。

💡 終日や複数日にまたがる予定は、いまは**開始日にだけ**点が付き、その日にだけ一覧に出る。
点と絞り込みで扱いが揃っているので破綻はしていない。必要になったら広げる。

**この Step で文法メモに追記したもの** … §2-(91)（`Job` と `cancel`、キャンセル例外の扱い）、
§4-(92)（`lengthOfMonth` / `dayOfWeek` / `dayOfMonth`）、§4-(93)（`..` / `until` / `in`）、
§4-(94)（`Set` と `chunked`）、§4-(95)（`filter`）、§3-(90)（`Scaffold` の `topBar`）。

#### Step 24（升目の見た目の調整）

| | 内容 | 状態 |
|---|---|---|
| B-1 | 一覧に残りの高さを全部渡す（`Box` に `fillMaxWidth()` ＋ `weight(1f)`） | ✅ 09-20 |
| B-2 | 今日の日付を**太字**にする | ✅ 09-20 |
| B-3 | 選択中の日を**丸く塗る**（`Surface` ＋ `CircleShape`） | ✅ 09-20 |
| B-4 | 升目の高さを固定する（`Modifier.height(56.dp)`） | ✅ 09-20 |
| B-5 | 点が切れるのを直す（`contentPadding = PaddingValues(0.dp)` ＋ `labelSmall`） | ✅ 09-20 |
| B-6 | 今日に**枠線の丸**を常に付ける（`BorderStroke`） | ✅ 09-20 |

**B-1 で分かったこと。** `Column` の中で `fillMaxSize()` を使うと「画面全部」を要求して
はみ出す。「上の部品が取った**残り**」が欲しいときは `weight(1f)`。
さらに **`weight` は縦しか決めない**ので、横は `fillMaxWidth()` を別に重ねる
（付け忘れると `Box` が中身の幅しか持たず、中央寄せしたつもりが左に寄る）。grammar §3-(96)。

**B-2〜B-4 は `DayCell` の書き換え 1 か所で片付いた。** 設計は以下の通り。

- **今日＝太字、選択中＝丸く塗る**と**役割を分ける**。同じ強調だと区別できない
- `MonthGrid` で `val today = LocalDate.now()` を `forEach` の**外**に作り、
  `isToday = date == today` を `DayCell` に渡す（42 回作り直さないため）
- 選択中でない日も **`Color.Transparent` の `Surface` を置いたまま**にする。
  `if` で `Surface` ごと出し分けると、選択した瞬間に升目の大きさが変わってガタつく
- `CircleShape` は縦横が同じでないと楕円になるので `Modifier.size(28.dp)` で正方形にしてから塗る
- `color` と `contentColor` はペアで指定（選択中 `primary` / `onPrimary`、それ以外 `Color.Transparent` / `onSurface`）

**B-5：予定マークの点が下で切れていた。** 原因は **`TextButton` が押しやすさのために
自前の上下余白（約 16dp）を持っている**こと。升目の高さを 56dp に固定していたので、
中身に使えるのは約 40dp しかなく、丸 28dp ＋ 点の行 約 20dp が収まらなかった。
`contentPadding = PaddingValues(0.dp)` で余白を無くし、点の `style` を `labelSmall` に
小さくして解決。**高さを増やす（64dp にする）方法もあるが、升目 6 段ぶん縦に伸びる**ので、
まず内側の余白を疑う方がよい。

**B-6：今日の印を「ずっと出る」形にした。** 太字だけでは主張が弱く、他の日を選ぶと
埋もれてしまうため、**今日＝枠線の丸、選択中＝塗りつぶしの丸**と役割を分けた（grammar §3-(97)）。
`border = if (isToday && !isSelected) BorderStroke(1.dp, primary) else null`。
`!isSelected` を付けるのは、**選択中は同じ `primary` で塗りつぶされ、同色の枠線が埋もれる**ため。
そのときは塗りつぶし＋太字で今日と分かる。

💡 **升目の見た目は「塗り」「枠」「太さ」「点」の 4 つを別々の印として使い分けている。**
状態が増えても、どれか 1 つを割り当てれば重ねて表現できる。

#### Step 25 の内訳（設定画面）

**仕様は「設定 ＝ プロフィール、通知設定、ログアウト」**（`docs/requirements.md` の画面一覧）。
Step 16-E で `HomeScreen` を消して以来、置き場所の無かったログアウトがここに収まった。

| | 内容 | 状態 |
|---|---|---|
| 25-A | `SettingsViewModel`（`currentUser` を 1 回読む／`logout()`） | ✅ 09-20 |
| 25-B | `SettingsScreen`（`Screen` ＋ `Content` の 2 段） | ✅ 09-20 |
| 25-C | `Routes.SETTING` ＋ `NavHost` に登録 | ✅ 09-21 |
| 25-D | `CalendarScreen` の `TopAppBar` を「設定」に差し替え | ✅ 09-21 |
| 25-E | 実機確認 | ✅ 09-21 |

**`StateFlow` を使っていない。** 表示するのはログイン中のプロフィールだけで、
**画面が開いている間ずっと変わらない**ため、`val user: User? = authRepository.currentUser` と
1 回読むだけにした（削除された `HomeViewModel` と同じ形）。
**「変わるか変わらないか」で道具を選ぶ** — 変わらないものに管（Flow）を用意しても、
流れるのは最初の 1 回だけで仕組みだけが増える。

**通知設定は行だけ置き、`enabled = false` で押せなくして「準備中」と出す。**
中身（WorkManager）がまだ無いので、押して空振りさせない。
通知を実装する回に `enabled = true` にして行き先を繋ぐだけで完成する。

⚠️ **ログアウト後の `popUpTo` は `HOME` を指す（`SETTING` ではない）。**
履歴は `[LOGIN] → [HOME] → [SETTING]` と積まれているので、`popUpTo(SETTING)` だと
**カレンダーが履歴に残り、戻るボタンで中身が見えてしまう**。`popUpTo(HOME) { inclusive = true }` で
カレンダーごと消す。認証を重視するこのアプリで最も落としてはいけない確認項目。

**ログアウトは「引っ越し」であって作り直しではない。** Step 22 で `CalendarScreen` に書いた
`viewModel.logout()` → 呼び鈴 の 2 行と `popUpTo` の処理は、そのまま
`SettingsScreen` と SETTING の `composable` へ移した。移したあと **`CalendarScreen` 側の
`onLogout` と `CalendarViewModel.logout()` は削除**し、同じ処理が 2 か所に残らないようにした。

#### Step 26 の内訳（ローカル通知）— Phase 2 の最後

**通知を出すには 4 つの仕組みが順に要る。どれが欠けてもエラーは出ず、静かに鳴らない。**

| | 内容 | 状態 |
|---|---|---|
| 26-A | 依存追加（WorkManager 2.10.0 ＋ hilt-work 1.3.0） | ✅ 09-21 |
| 26-B | 通知チャンネル（`data/local/NotificationChannels.kt`） | ✅ 09-21 |
| 26-C | 通知権限（`POST_NOTIFICATIONS`） | ✅ 09-21 |
| 26-D | `ReminderWorker`（通知を出す）＋ Hilt × WorkManager の初期化 | ✅ 09-22 |
| 26-E | `ReminderScheduler` / `ReminderSchedulerImpl`（予約する係） | ✅ 09-22 |
| 26-F | 作成画面のリマインダー選択（`DropdownMenu`） | ✅ 09-22 |
| 26-G | 保存・削除と予約の連動 | ✅ 09-22 |
| 26-H | 実機確認 | ✅ 09-22 |

**構成。** `domain/repository/ReminderScheduler`（約束）と `data/local/ReminderSchedulerImpl`（実装）に
分け、WorkManager に触れるのは data 層までにした。ViewModel は WorkManager を知らない
（依存の向き `presentation → domain ← data` を保つ）。Repository と同じ形なので
`RepositoryModule` に 1 行足すだけで繋がる。

**Worker は OS が作るので普通の `@Inject` が効かない。** `@HiltWorker` ＋ `@AssistedInject` で
「OS から来る分」と「Hilt が配る分」を分け、`KotonowaApplication` に `Configuration.Provider` を
実装して `HiltWorkerFactory` を渡す。**マニフェストで WorkManager の既定の自動初期化を
`tools:node="remove"` で止めるのを忘れないこと**（止めないと工場を知らないまま初期化され、
Worker が動く瞬間に失敗する）。grammar §8-(103)(104)。

**予約は名前を付けて積む。** `"reminder_<予定id>"` ＋ `ExistingWorkPolicy.REPLACE` にしたので、
**編集時は古い予約が自動で置き換わる**。削除時は `cancelUniqueWork` で取り消す
（`ScheduleDetailViewModel.delete()` の `onSuccess` に追加）。
`reminderMinutesBefore` が null のときや通知時刻が過去のときは、`schedule()` の中で
予約せず取り消すだけにしてある。だから「通知あり→なし」の変更も同じ経路で消える。

⚠️ **予約は必ず `onSuccess` の中で行う。** 保存や削除が失敗したのに予約だけ動くと、
実体の無い予定の通知が鳴る。`ReminderSchedulerImpl` は Firestore を見ないため。

⚠️ **WorkManager は時刻ぴったりを保証しない**（省電力状態で数分遅れる）。
分単位の正確さが要るようになったら `AlarmManager` の `setExactAndAllowWhileIdle` に寄せる。
要件定義書 §2 が両方を挙げているのはこのため。

**通知の重要度は `IMPORTANCE_HIGH`（音＋画面上部にポップアップ）。**
26-H では当初 `IMPORTANCE_DEFAULT` にしていて「音だけ鳴った」——これは設計どおりで、
通知自体は通知領域に入っていた。ポップアップさせたかったので `HIGH` に変更した。

⚠️ **コードを変えるだけでは反映されない。** 作成済みチャンネルの重要度は、
ユーザーが設定で変えた値をアプリが勝手に戻さないよう保護されている。
開発中なのでアプリを入れ直して作り直した（`dumpsys` で `mImportance=3 → 4` を確認）。
**実運用で変えるときは `CHANNEL_ID_REMINDER` を変えて別チャンネルとして作り直す。**
grammar §8-(99)。

⚠️ **入れ直すと通知権限も消える**ので、再許可が要る（`granted=false` に戻る）。

**確認のしかた。** 最短の選択肢が「5分前」なので、**6〜7 分後の予定**を作って試す。
5 分以内の予定だと通知時刻が過去になり、仕様どおり予約されない。
鳴らないときは Logcat の `WM-WorkerWrapper`（Worker が動いたか）と
`dumpsys notification`（通知が投稿されたか）を見ると、どこで止まっているか切り分けられる。

### Phase 4 の進捗（プッシュ通知）— ブランチ `feature/phase4-push-notification`（2026-11-18〜）

**設計（2026-11-18 にユーザーが選択）。**
- 通知を送るのは 3 種類：**招待が届いた**（招待された人へ）／**他のメンバーが予定・タスクを追加**／**他のメンバーが編集**（自分以外のメンバーへ）。削除は送らない。マイカレンダーは対象外
- 送るのは **Cloud Functions**（Firestore の書き込みをきっかけに動くサーバーの関数）。コードは **Claude が完成形を書く**（TypeScript）
- 届け先の住所は **FCM トークン**（端末ごとの宛先）。`users/{uid}.fcmTokens[]` に端末が自分で登録する（Step 28 で `merge` にしておいた理由）
- 「誰が編集したか」を Functions が知るため、`events` に **`updatedBy`** を足す（今は `createdBy` しか無く、編集者が分からない）
- 最後に、設定画面の「通知設定」で**カレンダーごとの ON/OFF** を作る
- 道具：Node.js（Homebrew）＋ Firebase CLI（`npm install -g firebase-tools`）。2026-11-18 に Claude が導入（node v26.13.1 / firebase-tools 15.36.0）

| Step | 内容 | 状態 |
|---|---|---|
| 40 | Firebase CLI の準備（ログイン・`firebase init`・ルールを CLI で公開できるように） | ✅ 11-18 |
| 41 | Android：FCM の依存＋トークンを `users/{uid}.fcmTokens` に登録（ログアウトで外す） | ✅ 11-20 |
| 42 | Android：通知を受け取って出す（`FirebaseMessagingService`＋新しい通知チャンネル） | ✅ 11-20 |
| 43 | Functions：招待が届いたら通知 | ✅ 11-20 |
| 44 | `events` に `updatedBy` ＋ Functions：他のメンバーの追加・編集で通知 | ✅ 11-20 |
| 45 | 通知設定（カレンダーごとの ON/OFF） | ✅ 11-20 |

**Step 40（2026-11-18）。** `firebase login` はユーザーが**ターミナル.app** で実行（この会話の `!` は入力を受け付けないので、
Gemini・統計の質問やコードの貼り付けで止まる。どちらも `n` でよい）。`firebase init` は対話式なので、Claude が同じファイルを手で作った。

| ファイル | 役割 |
|---|---|
| `.firebaserc` | このフォルダの既定のプロジェクト ＝ `kotonowa-3b2fb` |
| `firebase.json` | 何をどこから送り込むか（ルール ＝ `firestore.rules`、Functions ＝ `functions/`。送る前に `npm run build`） |
| `functions/package.json` | Functions の材料（`firebase-functions` 7.4.0 / `firebase-admin` 14.5.1 / `typescript` 7.0.2）と動かす Node の版（22） |
| `functions/tsconfig.json` | TypeScript → JavaScript の変換設定（`src/` → `lib/`。TS 7 は `rootDir` の指定が必須） |
| `functions/src/index.ts` | 関数を書く場所（今は `initializeApp()` だけ） |

**ルールはコマンドで公開できるようになった。** `firebase deploy --only firestore:rules`（`--dry-run` を付けると公開せず文法チェックだけ）。
今後は**リポジトリの `firestore.rules` が正**で、コピー＆貼り付けは不要。公開は外に出る操作なので、毎回ユーザーに確認してから行う。
💡 `npm install` で `protobufjs` などの install script が保留された（npm 11 の新しい安全機能）。ビルドには不要なのでそのまま。

#### Step 41 の内訳（FCM トークンの登録）

| | 内容 | 状態 |
|---|---|---|
| 41-A | 依存 `firebase-messaging` ＋ `FirebaseModule` に `FirebaseMessaging`（Claude） | ✅ 11-18 |
| 41-B | `domain/repository/PushTokenRepository` ＋ `data/repository/PushTokenRepositoryImpl` ＋ `RepositoryModule` | ✅ 11-19（最後の直しは Claude） |
| 41-C | ホームを開くたびに `registerToken`（`CalendarViewModel.init`） | ✅ 11-20 |
| 41-D | ログアウトの直前に `unregisterToken`（`SettingsViewModel.logout`）＋ `logout(onDone)` | ✅ 11-20 |
| 41-E | 実機確認（コンソールで `users/{uid}.fcmTokens` に住所が 1 つ入る／ログアウトで消える） | ✅ 11-20（Claude が adb で操作、ユーザーがコンソールを確認） |

- 41-B で踏んだところ：`unregisterToken` に登録の 3 行（`set` ＋ `arrayUnion`）をそのまま写した（ログアウトで住所が**増える**）、
  `.update(token, arrayUnion(uid))`（項目名に住所・値に uid）、`FIELD_FCM_TOKENS.token`（定数を住所をもらう行に入れた）。
  **どれもビルドが通る**（Firestore の API は `Any`）。`.update(項目名, 値)` は「誰の・どの項目に・何をする」と声に出して読む
- 住所の出し入れは `FieldValue.arrayUnion` / `arrayRemove`（Step 30 の `memberUids` と同じ）。登録は名刺が無くても失敗しないよう `set(…, merge)`
- 登録は**ホームを開くたび**（自動ログイン・ログイン・サインアップのすべてがホームを通る／住所は Firebase の都合で変わることがある）
- ⚠️ ログアウトでは**サインアウトより前に**外す（サインアウト後は自分の名刺を書き換える権限が無い）
- **41-E（2026-11-20）。** `+tester@gmail.com` で ホームを開く→住所が 1 つ／ログアウト→`[]`／ログインし直す→同じ住所が 1 つ、を確認。
  ログアウトの瞬間の `PERMISSION_DENIED` 3 行（予定・カレンダー・招待の見張り）は Step 28 の 💡 と同じもので、名刺の書き換えは断られていない。**これで Step 41 は完了。**
  ログアウトは `logout(onDone)`：外す → 掲示板を白紙 → サインアウト → `onDone()` の順（すぐ画面を移すと `viewModelScope` ごと通信が打ち切られる）
- ⚠️ **`users` は「ログインしていれば誰でも読める」**（Step 28。招待の検索のため）ので、`fcmTokens` も他人から読める。
  住所だけでは通知を送れない（送るにはサーバーの鍵が要る）ので今は許容し、要件定義書 §7 の「`users` の読み取りの絞り込み」と一緒に後で直す

#### Step 42 の内訳（通知を受け取って出す）

| | 内容 | 状態 |
|---|---|---|
| 42-A | 通知チャンネル `CHANNEL_ID_PUSH`（`"shared_updates"`）を `createNotificationChannels` に足す | ✅ 11-20（直しは Claude） |
| 42-B | `data/remote/KotonowaMessagingService`（`onNewToken` で再登録／`onMessageReceived` で通知を出す） | ✅ 11-20（直しは Claude） |
| 42-C | マニフェストに `<service>` ＋ `default_notification_channel_id`（Claude） | ✅ 11-20 |
| 42-D | 実機確認（Firebase コンソールの「テスト メッセージ」をこの端末の住所へ送る） | ✅ 11-20 |

- 中身の受け取りは `data` を先に、無ければ `notification` を見る。Functions は `data` だけで送る（裏にいても必ず自分の係を通るため。grammar §8-(136)）
- Service はコンストラクタで材料を受け取れないので `@AndroidEntryPoint` ＋ `@Inject lateinit var`（§8-(137)）。`viewModelScope` が無いので自前の `scope` を作り `onDestroy` で止める（§8-(138)）
- 通知をタップしたら `MainActivity` を開く（`PendingIntent`。§8-(139)）
- 42-A/B で踏んだところ（ビルドが通るものが 2 つ）：`createNotificationChannel(channel)`（リマインダーの郵便受けを 2 回設置。プッシュ用が作られず**通知が静かに消える**）、
  `scope.launch { CHANNEL_ID_PUSH }`（値を置いただけで何も頼んでいない）、`message.date[KEY_PUSH_BODY]`（綴り違い＋見出しに本文の名札）
- **42-D（2026-11-20）。** コンソールの Messaging（`https://console.firebase.google.com/project/kotonowa-3b2fb/messaging`。Firestore の画面の中には無い）→
  「Firebase Notification メッセージ」→「テスト メッセージを送信」に `fcmTokens` の住所を貼って送信。アプリが前にいるとき（係が出す）・裏にいるとき（Firebase が代わりに出す）の両方で表示され、
  `dumpsys notification` で**どちらも `shared_updates` の郵便受け**に入ったことを確認。**これで Step 42 は完了。**

#### Step 43 の内訳（招待が届いたら通知）

| | 内容 | 状態 |
|---|---|---|
| 43-A | `functions/src/index.ts`：`sendToUser`（名刺の全端末へ送る＋死んだ住所の掃除）＋ `notifyInvite`（Claude） | ✅ 11-20（`npm run build` 通過） |
| 43-B | 公開（`firebase deploy --only functions`）＋古いイメージの自動削除（1 日） | ✅ 11-20 |
| 43-C | 実機確認（コンソールで tester 宛ての招待状の `status` を `accepted` → `pending` に書き換え → エミュレータに通知） | ✅ 11-20 |

- `onDocumentWritten`（作成・上書きの両方）にして、「返事待ちになった瞬間」だけ送る（断られた招待の送り直しも知らせる。返事待ちのままの上書きでは送らない）
- 地域は `asia-northeast1`（Firestore と同じ）、`maxInstances: 10`（料金の暴走防止）
- ⛔ **2026-11-20、`--dry-run` で `Billing account for project '936774280451' is not open` で止まった。**
  Functions に要る API（Artifact Registry・Cloud Build・Cloud Functions）を有効にするには、プロジェクトに**開いている請求先アカウント**が要る。
  Google Cloud コンソールの「お支払い」で、請求先アカウントの状態とプロジェクトへの紐付けを確認する
  → 原因は **Google Cloud の無料トライアル（90 日）の終了**でアカウントが自動で閉じていたこと。ユーザーがアップグレードして解決
  （一覧は既定で「ステータス：アクティブ」に絞られており、閉じたアカウントはフィルタを外さないと見えない）
- 初回の公開は `Permission denied while using the Eventarc Service Agent`（きっかけを届ける係の権限がまだ行き渡っていない）で失敗し、**3 分待って再実行で成功**。
  続けて `firebase functions:artifacts:setpolicy --location asia-northeast1 --days 1 --force` で古いコンテナイメージの自動削除を設定（保管料の抑制）
- **43-C（2026-11-20）。** エミュレータ 1 台では招待する側に切り替えるとログアウトで住所が外れるため、コンソールで既存の招待状の `status` を `pending` に書き換えて確認。
  通知が表示され、`firebase functions:log --only notifyInvite` に `通知を送った success:1` を確認。**これで Step 43 は完了。**

#### Step 44 の内訳（他のメンバーの追加・編集で通知）

| | 内容 | 状態 |
|---|---|---|
| 44-A | `ScheduleRepositoryImpl`：`toMapWithWriter()`（中身＋`updatedBy`＝今ログインしている自分）で保存 | ✅ 11-20 |
| 44-B | ルール：`events` の `create` / `update` で `updatedBy == 自分` を必須に（Claude） | ✅ 11-20 |
| 44-C | Functions：`notifyScheduleChange`（共有カレンダーのみ・書いた本人以外へ・追加／編集／完了の言い分け）（Claude） | ✅ 11-20 |
| 44-D | 公開（アプリを先に入れ替えてから、ルールと Functions） | ✅ 11-20 |
| 44-E | 実機確認 | ✅ 11-20 |

- `updatedBy` は `ScheduleItem` に持たせず data 層で付ける（`type` / `sortAt` と同じ。頭脳の入れ忘れ・他人の uid を防ぐ）。`toggleCompleted` も `updateItem` を通るので自動で付く
- ⚠️ **公開の順番。** ルールを先に公開すると、`updatedBy` を送らない古いアプリの保存がすべて断られる。アプリを入れ替えてから公開する
- 書いた人の名前は名刺の `displayName` → 無ければ `email`（メールで登録した人は `displayName` が無い）
- 44-A で踏んだところ：`toMap(FIELD_UPDATED_BY to myUid)`（`+` で足さずに引数にした）、`.set(addItem(item))`（**addItem の中で addItem を呼ぶ＝終わらない呼び出し**。
  `set` は `Any` を受け取るのでビルドが通る。`update` は形が合わずビルドで止まった）。`( )` に入れるのは仕事ではなく中身（Map）
- **44-E（2026-11-20）。** ① Claude が adb で tester として school に「NotifyTest」を保存 → 新ルールで通る・本人に通知なし・関数の記録に他の 2 人あて（住所なしで送らず）。
  ② ユーザーがコンソールでその書類の `updatedBy` を test の uid（school の `ownerUid`）に書き換え → エミュレータに「school ／ test@example.comさんが予定「NotifyTest」を編集しました」。
  確認用の予定は adb で削除。💡 `events` の一覧は id で並ぶので、コンソールのフィルタ（`title == …`）で探す。**これで Step 44 は完了。**

#### Step 45 の内訳（通知設定：カレンダーごとの ON/OFF）

**設計（2026-11-20 にユーザーが選択）。** 止めたカレンダーの id を**名刺の `users/{uid}.mutedCalendarIds`** に並べる（今のルールのまま自分で書ける・Functions は名刺を 1 回読むだけ）。
切り替えは設定の「**通知設定**」画面（Step 25 で置いた「準備中」の行を使う）に共有カレンダーを並べ、1 行ずつスイッチ。招待の通知は止めない（いつも届く）。
⚠️ `users` は誰でも読めるので、止めているカレンダーも他人から見える（`fcmTokens` と同じ既知の弱点）。

| | 内容 | 状態 |
|---|---|---|
| 45-A | `NotificationSettingRepository`（`observeMutedCalendarIds` / `setCalendarMuted`）＋実装＋`RepositoryModule` | ✅ 11-20 |
| 45-B | 通知設定画面（`presentation/settings/notification/`。UiState / ViewModel / Screen） | ✅ 11-20（画面側の直しは Claude） |
| 45-C | `Routes` ＋ `NavHost` ＋ 設定画面の行を押せるように | ✅ 11-20 |
| 45-D | Functions：`sendToUser` が止めている人を飛ばす（Claude）＋各関数の先頭に「受け取った」の記録 | ✅ 11-20 |
| 45-E | 公開＋実機確認 | ✅ 11-20 |

- 45-A で踏んだところ：`if (muted) arrayUnion else arrayUnion`（両方の枝が同じ命令。ビルドが通り、一度止めると戻せなくなる）
- ⚠️ **45-E で分かったこと（2026-11-20）。** `firebase functions:log` は**新しい記録を取ってこないことがある**（10:44 の公開の記録すら出なかった）。
  これを根拠に「関数が呼ばれていない」と誤診し、不要な公開し直しをした。**関数の記録はブラウザの Logs Explorer で見る**：
  `https://console.cloud.google.com/logs/query;query=resource.labels.service_name%3D%22notifyschedulechange%22?project=kotonowa-3b2fb`
  （関数の先頭に「〜の書き込みを受け取った」の記録を足したので、呼ばれたかどうかはそこで分かる）
- ⚠️ **エミュレータは FCM の配達が数分遅れることがある。** 関数は 20:28:40 に「通知を送った」のに、表示は 20:30:56（Logcat に `FcmRetry`）。
  10 秒で「来ない」と判断しない。`dumpsys notification --noredact` で届いているか確かめる
- **45-E（2026-11-20）。** ① スイッチ OFF → 名刺の `mutedCalendarIds` に school の id。② test が書いた扱いで編集 → 記録に「このカレンダーの通知を止めているので送らない」、通知は来ない。
  ③ ON に戻す（一覧が空）→ 編集 → 「通知を送った」→ 約 2 分後に表示。確認用の予定は削除、スイッチは ON に戻した。**これで Step 45、Phase 4 の予定ステップはすべて完了。**
- 45-B で踏んだところ：`uiState.mutedCalendarIds in calendar`（`in` の左右が逆。左が 1 つ・右が集まり）、`onCheckedChange = onToggle(item { })`（`{ }` で包まず鳴らした＋`LazyColumn` の `item` を混ぜた）

### Step 47：タスク一覧画面（ブランチ `feature/task-list`、2026-11-22〜）

**設計（2026-11-22 にユーザーが選択）。**
- 対象は**入っている全部のカレンダー**（マイカレンダー＋共有カレンダー全部）のタスク。行にカレンダー名も出す
- 上で「**未完了**」「**完了**」を切り替え。未完了は「期限切れ」（赤）と「これから」に分け、期限の近い順。完了は期限の新しい順
- 入口は**画面の下のタブ**（カレンダー／タスク。`NavigationBar`）
- 行はホームと同じく、チェックで完了切り替え・押すと詳細。閲覧者のカレンダーのタスクはチェックを押せない（Step 38）
- 取り方：カレンダーごとに `observeTasks(calendarId)`（`calendarId ==` かつ `type == "task"`。等しい条件だけなので**複合インデックス不要**）を見張り、頭脳で束ねる。並べ替えは頭脳／画面

| | 内容 | 状態 |
|---|---|---|
| 47-A | `ScheduleRepository.observeTasks(calendarId)` ＋ 実装 | ✅ 11-22 |
| 47-B | `TaskListUiState`（行＝タスク・カレンダー名・`canEdit`／未完了か完了か） | ✅ 11-22 |
| 47-C | `TaskListViewModel`（全カレンダーのタスクと肩書きを束ねる・完了切り替え・タブ切り替え） | ✅ 11-22（束ねる部分は Claude） |
| 47-D | `TaskListScreen`（タブ・期限切れ／これから・行） | 🔧 骨組みを作成 |
| 47-E | 下のタブ（カレンダー／タスク）＋ `Routes` ＋ `NavHost` | |
| 47-F | 実機確認 | |

- 47-A で踏んだところ：`val calendarId = firestore.collection(calendarId)`（絞り込みではなく別の棚を指し、`.` の鎖も切れた）、
  `.whereEqualTo("Task", type)`（項目名と値の取り違え＋`" "` 忘れ）、`filterIsInstance<String>(ScheduleItem.Task)`（型は `< >` に、`( )` は空）
- 47-B/C で踏んだところ：`rows: List<Calendar>`（お手本の型を写した）、`filter: Boolean`（2 択でも enum にすると名前で読める）、
  `it.copy(TaskListUiState = TaskListUiState)`（`copy` の中は「項目名 = 値」。型の名前ではない）、`launch` の中に `launch` を写した。入力中に Android Studio が無関係な `import`（`Keyboard`）を取り込んでいた
- ⏸ **再開メモ（2026-11-22 中断）。** 47-D の穴埋め 6 か所（`presentation/tasks/TaskListScreen.kt`）が未記入のまま WIP コミットした（**ビルドは通らない**）。
  再開したら、まずこの 6 か所の説明（§2.6 の順。新しい文法は §4-(144)）を表示してから進める：
  47-D-1 期限切れ／これから（`row.task.dueAt < now` / `>= now`）・47-D-2 完了を期限の新しい順（`sortedByDescending`）・
  47-D-3〜5 未完了／完了のボタン（`selected` / `onClick`）・47-D-6 `TaskRow` で `ScheduleItemRow` に 4 つ渡す（`row.task` など）。
  そのあと 47-E（下のタブ：`NavigationBar`＋`Routes`＋`NavHost`）→ 47-F（実機確認）
- 47-D：ホームの行の部品 `ScheduleItemRow` を `private` → `internal` にして使い回す。カレンダー名を出す引数 `calendarName: String? = null` を足した（ホームは省略）

### Step 46：`users` を本人だけ読めるようにする（Phase 4 の後の手直し。ブランチ `feature/restrict-users-read`、2026-11-20〜）

**理由。** `users` は「ログインしていれば誰でも読めた」（Step 28。招待の検索のため）ので、全員のメールアドレス・`fcmTokens`・`mutedCalendarIds` が他人から読めた。
アプリが他人の名刺を読んでいたのは 2 か所：メンバー画面の名前（`getUser`。Step 34）と、招待の相手探し（`findUserByEmail`。Step 35）。

**設計（2026-11-20 にユーザーが選択）。**
- **招待は丸ごとサーバーの関数（`onCall` の `sendInvite`）に任せる。** アプリは「カレンダー・メール・肩書き」を渡し、結果（送った／未登録／すでにメンバー）だけ受け取る。他人の uid もアプリに渡らない
- **メンバー画面の名前・メールは、名簿の行（`members/{uid}`）に写しておく**（`displayName` / `email`）。名簿に入るとき（作成・承認）に本人が自分の分を書く。メンバーなら名簿は読めるので、名刺は要らない
- 最後にルールで `users` の `read` を「本人だけ」に、`invites` の `create` / オーナーの送り直しの `update` を削除（招待状はサーバーだけが書く）

| | 内容 | 状態 |
|---|---|---|
| 46-A | Functions：`sendInvite`（`onCall`。オーナー確認 → 相手探し → メンバー確認 → 招待状）（Claude） | ✅ 11-22 |
| 46-B | 依存 `firebase-functions` ＋ `FirebaseModule` に `FirebaseFunctions`（東京）（Claude） | ✅ 11-20 |
| 46-C | `InviteSendResult`（enum）＋ `InviteRepository.sendInvite(calendarId, email, role)` を関数呼び出しに | ✅ 11-22 |
| 46-D | `MemberListViewModel.sendInvite` を新しい窓口に（結果の 3 つで一言を出し分け） | ✅ 11-22 |
| 46-E | 名簿の行に `displayName` / `email`（`CalendarMember`・作成・承認）＋メンバー画面は名簿から名前を出す | ✅ 11-22（直しは Claude） |
| 46-F | ルール：`users` の `read` を本人だけ、`invites` の作成系を削除、名簿の `displayName` / `email` は本人のもの（Claude） | ✅ 11-22 |
| 46-G | 公開（アプリ → 関数 → ルールの順）＋既存の名簿の行に名前を足す＋実機確認 | ✅ 11-22 |

- 46-C で踏んだところ：`"role" to role`（enum をそのまま渡した。Map は `Any` を受け取るのでビルドは通るが、JSON にできず**呼ぶたびに失敗**）、
  直すときに名札 `"role" to` まで消した（`mapOf` の中は 1 行ずつ「名札 to 値」）
- 46-E：名前とメールは data 層が「今ログインしている自分」（`firebaseAuth.currentUser`）から写す。名簿の行を書くのは作成（オーナー本人）と承認（招待された本人）の 2 か所だけで、どちらも書くのは本人の行
- `Invite.toMap`（招待状 → Map）、`UserRepository.findUserByEmail` / `getUser` と `toUser` は使われなくなったので削除した
- 46-E で踏んだところ：消した箱 `user?.displayName` をそのまま書いた（名前は名簿の行 `member.` にある）、メールの `?.lowercase()` 忘れ（46-F のルールで小文字と照合するので、大文字まじりだと断られる）
- 46-F：名簿の行の `create`（作成・承認の 2 本）に `isOwnNameAndEmail()`（`email` は無いか本人の小文字のメール、`displayName` は無いか通行証の `name`）を足した。
  `request.resource.data.get(項目, 既定値)` は項目が無いときに既定値を使う（書かない古いアプリでも通る）
- **46-G（2026-11-22）。** アプリ → 関数（`sendInvite` 新規）→ ルールの順に公開。tester でメンバー画面を開くと読めるが 3 人とも「（名前未登録）」
  （Step 46 より前の行には名前が無い）→ ユーザーがコンソールで school の `members` 3 行に `email` を手で足す → その場でメールが出た。
  ⚠️ コンソールで足すとき、school の**カレンダーの書類そのもの**に足してしまった（`members` の棚に入っていなかった）。画面上部の場所が `calendars > id > members > uid` か確かめる。
  test でログインし直して招待：未登録 →「登録されていません」、すでにメンバー →「すでにメンバーです」、`tester@example.com` を外して招待 →「招待を送りました」
  （招待状はコンソールで確認後に削除）。`PERMISSION_DENIED` なし。**これで Step 46 は完了。** 今の school のメンバーは test（オーナー）と `+tester`（編集者）の 2 人。
  エミュレータは test でログイン中

### Phase 3 の進捗（共有カレンダー＋ロールベース認可）

| Step | 内容 | 状態 |
|---|---|---|
| 27 | ドメインモデル（`Calendar` / `CalendarType` / `CalendarMember` / `MemberRole`） | ✅ 09-23 |
| 28 | `users` コレクションへの自己登録（`UserRepository`） | ✅ 09-24 |
| 29 | `CalendarRepository`（interface） | ✅ 09-24 |
| 30 | `CalendarRepositoryImpl`（作成時に members も一緒に書く） | ✅ 09-28 |
| 31 | カレンダー一覧画面 | ✅ 10-04 |
| 32 | カレンダー作成画面 | ✅ 10-14 |
| 33 | 表示するカレンダーの切り替え（`CalendarViewModel` の `calendarId` を可変に） | ✅ 10-22 |
| 33-H | 切り替えを設定からホームの「名前 ▼」のメニューへ移す | ✅ 10-22 |
| 34 | メンバー一覧・ロール変更・外す（オーナーのみ） | ✅ 11-02 |
| 35 | 招待送信（`invites`） | ✅ 11-16 |
| 36 | 招待受信・承認/拒否 | ✅ 11-17 |
| 37 | セキュリティルールをロールベースに作り替える | ✅ 11-17 |
| 38 | 画面のロール出し分け（閲覧者には「＋」も「編集」も出さない） | ✅ 11-17 |
| 39 | メールアドレスの持ち主確認（招待の受け取りを確認済みの人に限る） | |

#### Step 27（ドメインモデル）

`Calendar`（部屋）と `CalendarMember`（名簿の 1 行）を**別の型に分けている**。Firestore 側も
`calendars/{id}` と `calendars/{id}/members/{uid}` に分かれており（要件定義書 §4）、
名簿をサブコレクションにしておくとルールから `members/$(request.auth.uid)` を一発で引ける（Step 37）。

`CalendarType` は `Calendar.kt` に同居、`MemberRole` は単独ファイル。**単独で意味があり
あちこちから使うものはファイルを分ける**（grammar §7-(108)）。

⚠️ **Phase 2 のデータは「部屋が無いのに紙だけある」状態。** `events` の `calendarId` には
ユーザーの uid が入っているが、`calendars/{uid}` のドキュメントは存在しない（2026-08-08 の決定どおり）。
Phase 3 で `calendars/{uid}`（type=PERSONAL）を後から作れば既存データと矛盾しない。

⚠️ **今のセキュリティルールのままでは共有カレンダーは動かない。** `events` のルールは
`request.auth.uid == resource.data.calendarId`（＝自分の uid の部屋だけ）なので、
共有カレンダーを作った瞬間その中の予定は本人ですら読めなくなる。Step 37 で
「`members/{自分のuid}` があるか、その `role` は何か」で判定する形に置き換える。

#### Step 29 / 30（`CalendarRepository` と Firestore 実装）

**「自分が入っているカレンダーの一覧」の取り方に C 案（`memberUids` 配列）を採用**（2026-09-24。
詳細は `docs/requirements.md` §4）。`members` はサブコレクションなので**子から親を逆引きできない**。
コレクショングループ検索（案A）だと名簿の行しか返らず部屋の情報を取りに行く通信が部屋の数だけ増える。
C 案なら `whereArrayContains("memberUids", uid)` の 1 クエリで部屋の情報ごと返り、
セキュリティルールも `allow read: if request.auth.uid in resource.data.memberUids;` の 1 行で書ける。

⚠️ **`memberUids` は Kotlin の `Calendar` に持たせない。** 名簿に誰が載っているかを写しただけの
重複情報なので、`events` の `type` / `sortAt` と同じく data 層だけで面倒を見る。
両方をモデルに持たせると「名簿には居るのに配列には居ない」矛盾を画面側から作れてしまう。

| | 内容 | 状態 |
|---|---|---|
| 30-A | 骨組み・定数・変換係の器 | ✅ |
| 30-B | `createCalendar` ＋ `Calendar.toMap` / `CalendarMember.toMap` | ✅ 09-24 |
| 30-C | `observeMyCalendars` / `observeMembers` ＋ `toCalendar` / `toCalendarMember` | ✅ 09-27 |
| 30-D | `addMember` / `updateMemberRole` / `removeMember` | ✅ 09-28 |

**`runBatch` で「名簿」と「配列」を必ず同時に書く**（grammar §4-(112)）。片方だけ書かれると、
名簿にだけ居る人は**自分の一覧にそのカレンダーが出ず**、配列にだけ居る人は**肩書きが無くルールに弾かれる**。
`createCalendar`（部屋＋オーナーの行）、`addMember`、`removeMember` の 3 つが対象。
`updateMemberRole` は人の出入りではないので配列を触らず、`runBatch` も要らない。

**enum は境界で文字に変換する**（grammar §4-(111)）。保存は `role.name.lowercase()`、
読み込みは `MemberRole.valueOf(文字.uppercase())`。Step 37 のルールが `role == 'owner'` と
小文字で照合するため。

**翻訳に失敗した 1 件は `runCatching { }.getOrNull()` で捨てる。** 壊れたデータ 1 件で
一覧が丸ごと出なくなるのを防ぐ（`observeItems` と同じ方針）。

⚠️ **Firestore の API は `Any` を受け取るので、コンパイラが間違いを素通しする。**
実際に `batch.set(memberDoc, calendarDoc)`（名簿の行に部屋の住所を書く）と
`.update("role", "role" to ...)`（値ではなくペアを渡す）の 2 つがビルドを通ってしまった。
どちらも実行時に壊れ、原因を追いにくい。**Firestore に渡す直前の行は声に出して読む**こと。
`trySend` が型違いを止めてくれたのは `Flow<List<Calendar>>` と具体的に書いていたから。

⚠️ **`to〇〇()` は 2 方向あり、`this` が誰かで書き方が変わる。**
`Calendar.toMap()` の中では `name` と名前だけで書けるが（§4-㊲）、
`DocumentSnapshot.toCalendar()` の中では `getString("name")` になる。
関数の 1 行目を見て「今どちらの中にいるか」を確かめてから書く。

#### Step 32 の内訳（カレンダー作成画面）

`presentation/calendarlist/create/`。お手本は Step 17 の予定作成画面（作成モードだけの縮小版）。

| | 内容 | 状態 |
|---|---|---|
| 32-A | `CalendarCreateUiState`（name / color / isSaving / isSaved / errorMessage） | ✅ 10-04 |
| 32-B | `CalendarCreateViewModel`（`onNameChange` / `onColorChange` / `save()`） | ✅ 10-04 |
| 32-C | `CalendarCreateScreen`（入力欄・色の丸 ×5・保存/戻る・Preview 2 つ） | ✅ 10-14 |
| 32-D | `Routes` ＋ `NavHost` に登録し、一覧の「＋」から開く | ✅ 10-14 |
| 32-E | Firestore ルールに `calendars` / `members` の `create` を追加 | ✅ 10-14 |
| 32-F | 実機確認（保存 → 一覧に 1 行増える） | ✅ 10-14 |

**色は自由入力にせず `CALENDAR_COLORS`（5 色の文字列）から選ばせる。** `color` の既定値は
`CALENDAR_COLORS.first()` で、「色が無いカレンダー」を最初から作れないようにしている。
表示の直前だけ `Color(hex.toColorInt())` で変換する（grammar §3-(115)）。

**32-C で踏んだところ（2026-10-14）。** 色の丸の `onClick` に升目（Step 23）と同じ
`if (hex != null)` を書いていた。升目は `List<LocalDate?>`（空の升目が `null`）だが、
`CALENDAR_COLORS` は `List<String>` で `null` が入らないので不要。**チェックが要るかは型の `?` で決まる。**

⚠️ **共有カレンダーの `id` は UUID。`uid` を入れてはいけない。** 個人カレンダーの id ＝ uid なので、
共有カレンダーまで uid にすると Phase 2 の予定が全部その中身として扱われる。`type` は `SHARED`。

**Firestore ルールに `calendars` の `create` と `members` の `create` を追加（2026-10-14 適用）。**

```
match /calendars/{calendarId} {
  allow read: if request.auth != null
    && request.auth.uid in resource.data.memberUids;

  allow create: if request.auth != null
    && request.resource.data.ownerUid == request.auth.uid
    && request.resource.data.memberUids == [request.auth.uid]
    && request.resource.data.id == calendarId;

  match /members/{memberUid} {
    allow create: if request.auth != null
      && memberUid == request.auth.uid
      && request.resource.data.role == 'owner'
      && getAfter(/databases/$(database)/documents/calendars/$(calendarId)).data.ownerUid
           == request.auth.uid;
  }
}
```

作成時は「オーナーは自分・`memberUids` は自分だけ・住所と中身の id が一致」。名簿の行は
「自分の行・`'owner'`・部屋の `ownerUid` が自分」。最後の条件が無いと、**他人のカレンダーに
自分をオーナーとして書き足せてしまう**（乗っ取り）。部屋と名簿は `runBatch` で同時に書かれるので、
部屋の確認は `get` ではなく **`getAfter`**（書き込み後の状態を読む）でないと必ず拒否される（grammar §4-(118)）。
部屋の `update` / `delete`、名簿の `read`、招待による他人の追加は**まだ拒否される**（Step 34 / 37 で開ける）。

💡 `CalendarRepositoryImpl.kt` の `import java.sql.Date`（データベース接続用の別物）を `java.util.Date` に直し、
未使用の `import android.R` を削除した（2026-10-14）。`Date.from` は親の `java.util.Date` から受け継いでいるので、
`java.sql` のままでも動いてしまっていた。

#### Step 33 の内訳（表示するカレンダーの切り替え）

**設計（2026-10-14 にユーザーが選択）。**
- 「今どのカレンダーを表示しているか」は**掲示板**（`SelectedCalendarRepository`。`@Singleton` でアプリに 1 枚）に貼る。
  ホームは見張って購読を張り直し、作成画面は保存時に 1 回読む。画面の住所（`home/{calendarId}`）に載せる案は、
  全部の住所に `calendarId` を通す配管が増えるため不採用。アプリを閉じると個人に戻る（保存はしない）。
- 掲示板が**白紙（`null`）＝個人カレンダー**。個人カレンダーは `calendars` の書類を持たないため。
- 一覧の先頭に「マイカレンダー」を**画面側で固定の行**として出す。`calendars/{uid}` の書類は作らない
  （「無ければ作る」の確認が今のルールでは拒否される／Phase 2 のデータとルールをそのまま使える）。
- ⚠️ **ログアウト時に掲示板を白紙に戻す。** `@Singleton` はログアウトしても生き残るので、戻さないと
  次にログインした人のホームが前の人の共有カレンダーを開こうとする。

| | 内容 | 状態 |
|---|---|---|
| 33-A | 掲示板（`domain/repository/SelectedCalendarRepository` ＋ `data/local/…Impl` ＋ `RepositoryModule`） | ✅ 10-14 |
| 33-B | 一覧の先頭に「マイカレンダー」＋行タップで掲示板に貼ってホームへ戻る | ✅ 10-18 |
| 33-C | `CalendarViewModel` が掲示板を見張り、貼り替わったら購読を張り直す＋上のバーに名前 | ✅ 10-20 |
| 33-D | `ScheduleEditViewModel` の新規作成で、掲示板のカレンダーの id を使う | ✅ 10-22 |
| 33-E | ログアウトで掲示板を白紙に戻す | ✅ 10-22 |
| 33-F | `events` のルールを「そのカレンダーのメンバーなら読み書きできる」に広げる（Claude が書く） | ✅ 10-22 |
| 33-G | 実機確認（家族に切り替え→予定作成→個人に戻ると出ない） | ✅ 10-22 |

**33-A で踏んだところ（2026-10-14）。** 約束側の型に `CalendarUiState`（ホーム画面の状態の箱）を書き、
domain から presentation を `import` してしまった。`Calendar`（カレンダー 1 つ）と名前が似ているので注意。
**domain のファイルに `presentation` の import が入っていたら、依存の向きが逆になっている印。**
`clear()` に `MutableStateFlow<Calendar?>(null)` を丸ごと書いたのも同じ回。`.value =` の右は**中身（`null`）**で、
掲示板そのものではない。

**33-B。** 一覧の先頭に `PersonalCalendarRow`（固定の行。押すと `null`）、その下に共有カレンダー。
空っぽの案内は `when` の枝から `LazyColumn` の中へ移した（マイカレンダーの行は 0 件でも出すため）。
画面は `viewModel.selectCalendar(calendar)` → `onCalendarSelected()` の 2 行（Step 25 の logout と同じ形）。
NavHost は `popBackStack(Routes.HOME, inclusive = false)` で設定画面も飛ばしてホームへ戻す（grammar §3-(120)）。

⚠️ **33-B で 3 回踏んだ「ビルドは通るのに何も起きない」書き方（2026-10-18）。** コンパイラは止めてくれない。

| 書いたもの | 何が起きるか | 正しくは |
|---|---|---|
| `onCalendarSelected`（`()` 無し） | 呼び鈴を指さすだけで鳴らない | `onCalendarSelected()` |
| `{ null }` / `{ calendar.id }` / `{ Routes.HOME }` | 値を思い浮かべただけで、誰にも渡していない | `{ onCalendarClick(null) }` など |
| `navigate(Routes.HOME)` | ホームが**もう 1 枚積まれ**、「戻る」で一覧が出てくる | `popBackStack(Routes.HOME, inclusive = false)` |

**`{ }` の中は「誰の・何をする」と声に出して読めるか**で確かめる。

**33-C。** `CalendarViewModel` に `uid`（自分＝マイカレンダーの id。`val`）と `calendarId`（今表示中。`var`）を分けた。
`init` で掲示板を `collect` し、流れてくるたびに `calendarId = calendar?.id ?: uid`・
`calendarName = calendar?.name ?: "マイカレンダー"`・`observeMonth(今の月)`。
**張り直しは Step 23 の `observeMonth`（先頭で `observeJob?.cancel()`）をそのまま使う。**
`init` の `observeMonth` 直呼びは消した（StateFlow は collect した瞬間に今の値を 1 回流すので二重になる。grammar §2-(121)）。
`observeMonth` の先頭で `val id = calendarId` に写す（`var` はスマートキャストできない。§4-(65)）。
上のバーは「ことのわ」固定から `uiState.calendarName` に。

⚠️ **33-C で踏んだところ。** `calendarId = calendarId ?: uid`（右も箱から始めてしまい、掲示板を見ていない）、
`calendarName = calendarId ?: …`（バーに uid が出る）、`Text(uiState,id)`（`.` と `,` の取り違え）。
①②は「掲示板から箱へ」、④の `val id = calendarId` は「箱から手元へ」で**向きが逆**。

**33-D。** `ScheduleEditViewModel` の `calendarId`（中身は自分の uid）を **`uid` に改名**（Step 19-D の 💡 を回収）。
`save()` で `myUid = uid ?: return`、`targetCalendarId = selectedCalendarRepository.selected.value?.id ?: myUid`。
新規なら `calendarId = targetCalendarId`（置き場所）、`createdBy = myUid`（作った人）。編集なら `original?.〇〇` を引き継ぐ。
作成画面は掲示板を **`.value` で保存の瞬間に 1 回読むだけ**（ホームは `collect` で見張る。Step 20 の「1 回きり／購読」と同じ使い分け）。
⚠️ `.value` の代わりに 33-C の `calendar`（`collect { calendar -> }` で**自分で付けた**名前）を書きかけた。
`.value` は Kotlin が決めた取り出し口、`calendar` は受け取るときに付けた名前で、`collect` の外には存在しない。

**33-E。** `SettingsViewModel.logout()` の先頭で `selectedCalendarRepository.clear()`。画面と NavHost は変更なし。
共有カレンダーを表示したままログアウト→再ログインで、ホームが「マイカレンダー」から始まることを実機で確認。

**33-F：`events` のルールをメンバーベースに作り替えた（2026-10-22 適用）。** 2026-09-04 の
「`calendarId` が自分の uid なら OK」を置き換え。コンソールのルール全文は以下（`users` / `calendars` は従来どおり）。

```
function isMember(calendarId) {
  return request.auth.uid == calendarId
    || request.auth.uid in get(/databases/$(database)/documents/calendars/$(calendarId)).data.memberUids;
}

match /events/{eventId} {
  allow read, delete: if request.auth != null
    && isMember(resource.data.calendarId);

  allow create: if request.auth != null
    && isMember(request.resource.data.calendarId)
    && request.resource.data.createdBy == request.auth.uid;

  allow update: if request.auth != null
    && isMember(resource.data.calendarId)
    && request.resource.data.calendarId == resource.data.calendarId
    && request.resource.data.createdBy == resource.data.createdBy;
}
```

- `isMember` の左はマイカレンダー（id ＝ uid）、右は共有カレンダー（部屋の `memberUids`）。
  **`||` は左が「はい」なら右を見ない**ので、`calendars` の書類が無いマイカレンダーでも `get` が失敗しない（grammar §4-(122)）。
- `update` は置き場所（`calendarId`）と作った人（`createdBy`）の**書き換えを禁止**（別カレンダーへの引っ越し・作者の詐称を防ぐ）。
- ⚠️ **ロールはまだ見ていない。** 閲覧者でもメンバーなら作成・編集・削除できる。Step 37 で `members/{uid}` の `role` を見る形にする。

#### Step 33-H（切り替えをホームの ▼ へ移す。2026-10-22 にユーザーが要望）

「毎日使う切り替えが設定の奥にあるのは遠い」ため。**設計（ユーザーが選択）**：ホームの上のバーを
「家族 ▼」にし、押すとその場に `DropdownMenu`（Step 26 と同じ部品）を開く。並びは
マイカレンダー → 共有カレンダー → 区切り線 →「＋ 新しいカレンダー」「カレンダーの管理」。選択中に ✓。
**設定画面の「カレンダー」の行は消す**（入口を 2 つ持たない）。一覧画面（Step 31）は「カレンダーの管理」として残し、
Step 34 のメンバー管理の入口にする。

| | 内容 | 状態 |
|---|---|---|
| H-1 | `CalendarUiState` に `selectedCalendarId` / `calendars` / `isCalendarMenuOpen` | ✅（Claude） |
| H-2 | `CalendarViewModel`：`observeCalendars`・メニューの開閉・`selectCalendar` | ✅（Claude） |
| H-3 | `CalendarScreen`：「名前 ▼」＋ `DropdownMenu` ＋ 呼び鈴 2 つ | ✅（Claude） |
| H-4 | `NavHost` の配線＋設定画面の「カレンダー」の行を削除 | ✅（Claude） |
| H-5 | 実機確認（Claude が adb で操作し、8 項目すべて確認） | ✅ 10-22 |

**H-2〜H-4 はユーザーの依頼で Claude が実装した（2026-10-22。「このボタンのお引越し部分の実装はそっちで頼みたい」）。**
- 選んだあとの切り替えは**新しく書いていない**。`selectCalendar` は掲示板に貼ってメニューを閉じるだけで、
  33-C の `collect` が気づいてバーの名前と予定を張り直す。一覧画面（33-B）からの選択と入口が同じ。
- `observeCalendars` の失敗は `errorMessage` に入れない（メニューに共有が出ないだけで予定は読める。
  入れると下の一覧がエラー表示に置き換わる。ログアウトの瞬間の `PERMISSION_DENIED` もここに来る）。
- 別画面へ行く 2 項目は、`closeCalendarMenu()` → 呼び鈴 の順。閉じずに行くと戻ってきたとき開いたまま。
- ✓ は全行に `CheckMark` を置いて中身だけ出し分ける（行の文字位置を揃えるため。grammar §3-(123)）。

#### Step 34 の内訳（メンバー一覧・ロール変更・外す）

**設計（2026-10-22 にユーザーが選択）。**
- 「カレンダーの管理」（一覧画面）の行を押すと**そのカレンダーのメンバー画面**へ。切り替えは ▼ に移ったため。
  マイカレンダーにはメンバーがいないので、**管理一覧から個人の行を消す**（33-B の `PersonalCalendarRow` は不要になる）。
- ロール変更に加えて**メンバーを外す**も作る（`removeMember` は Step 30 で実装済み）。
- 招待がまだ無いので、**Firestore コンソールで仮のメンバーを足して**試す（`members/{uid}` と `memberUids` の両方）。
- 名簿には uid と肩書きしか無いので、名前は `users`（Step 28 の名刺）を **uid で引く**（`UserRepository.getUser` を追加）。

| | 内容 | 状態 |
|---|---|---|
| 34-A | `UserRepository.getUser(uid): Result<User?>` ＋ 実装 | ✅ 10-23 |
| 34-B | 管理一覧：個人の行を消し、行タップでメンバー画面へ（`Routes` に `calendar_members/{calendarId}`） | ✅ 10-23 |
| 34-C | `MemberListUiState` ＋ `MemberListViewModel`（名簿の購読・名前の引き当て・ロール変更・外す） | ✅ 10-23 |
| 34-D | `MemberListScreen`（名前・肩書き。オーナーだけ変更・外すを出す） | ✅ 10-23 |
| 34-E | ルール：名簿の `read`（メンバー）・`update` / `delete`（オーナーのみ。オーナー自身の行は不可）＋部屋の `memberUids` の `update`（オーナーのみ）（Claude が書く） | ✅ 11-02 |
| 34-F | コンソールで仮メンバーを足して実機確認 | ✅ 11-02 |

**34-F の確認（2026-11-02）。** 閲覧者の表示、オーナーでない人にボタンが出ないこと、肩書きの変更、外す（名簿の行と `memberUids` の両方が消える）まで通した。

⚠️ **コンソールで手で足すときの落とし穴（34-F で踏んだ）。** `members` の棚を開かずに「コレクションを開始」すると別の棚ができ、アプリから見えない。
`memberUids` の編集は上書きになりやすく、オーナーが抜けると本人がカレンダーを読めなくなる。読めない行は `runCatching` で黙って捨てられるので（Step 30）、
**行が出ないのにエラーも出ないときは、まずコンソールで住所と項目を確認する。**

**34-E のルールで足す分（`users` / `events` / `calendars` の既存分はそのまま）。**

```
function isCalendarOwner(calendarId) {
  return get(/databases/$(database)/documents/calendars/$(calendarId)).data.ownerUid == request.auth.uid;
}

match /calendars/{calendarId} {
  allow update: if request.auth != null
    && resource.data.ownerUid == request.auth.uid
    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['memberUids'])
    && request.auth.uid in request.resource.data.memberUids;

  match /members/{memberUid} {
    allow read: if request.auth != null && isMember(calendarId);

    allow update: if request.auth != null
      && isCalendarOwner(calendarId)
      && memberUid != request.auth.uid
      && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['role'])
      && request.resource.data.role in ['editor', 'viewer'];

    allow delete: if request.auth != null
      && isCalendarOwner(calendarId)
      && memberUid != request.auth.uid;
  }
}
```

⚠️ **「外す」は名簿だけ開けても動かない。** `removeMember` は `runBatch` で「名簿の行を消す」と「部屋の `memberUids` から抜く」を
同時に行い、**片方でも拒否されると両方失敗する**。だから部屋の `update` も要る。ただし名前や `ownerUid` まで書き換えられないよう、
変えてよい項目を `memberUids` だけに絞っている（grammar §4-(127)）。自分を配列から抜けないようにもしてある。
肩書きは `editor` / `viewer` だけ許す。`owner` を許すと、オーナーが 2 人いる状態を作れてしまう。

#### Step 35 の内訳（招待送信）

**設計（2026-11-02）。** 入口はメンバー画面のオーナーだけに見える「招待」。未登録のメールアドレスには招待状を作らない。
招待状に `calendarName` を写す。招待された人はまだメンバーではなく部屋の書類を読めないので、Step 36 で名前を出すために要る。

| | 内容 | 状態 |
|---|---|---|
| 35-A | `domain/model/Invite` ＋ `InviteStatus` | ✅ 11-02 |
| 35-B | `domain/repository/InviteRepository`（interface） | ✅ 11-02 |
| 35-C | `data/repository/InviteRepositoryImpl` ＋ `RepositoryModule` | ✅ 11-02 |
| 35-D-1 | `CalendarRepository.getCalendar`（招待状に写すカレンダー名を読む） | ✅ 11-02 |
| 35-D-2 | 招待の入力欄の箱（`MemberListUiState` に 4 項目＋ `onInviteEmailChange` / `onInviteRoleChange`） | ✅ 11-08 |
| 35-D-3 | `sendInvite()`（相手を探す→カレンダー名を読む→招待状を書く） | ✅ 11-08（残りは Claude が補完） |
| 35-E | 招待の入力欄（`MemberListScreen`。オーナーだけ） | ✅ 11-16 |
| 35-F | ルール：`invites` の `create`（Claude が書く） | ✅ 11-16 |
| 35-G | 実機確認（`invites` に招待状が 1 枚できる） | ✅ 11-16 |

**35-D-3（2026-11-08）。** ユーザーが書いたあと、残りを依頼により Claude が補完した。踏んだところ：
`findUserByEmail(email).toString()`（封筒が文字になり `isFailure` が無い）、「すでにメンバーか」を
`member.uid == uid`（自分）で比べていた（オーナーは必ず名簿に居るので**誰も招待できなくなる**）、
招待状の欄の取り違え（`calendarName` に肩書きの名前、`role` に `OWNER`、`status` に `ACCEPTED`）。
⚠️ `role = OWNER` のまま承認されると**招待された人がオーナーになる**。35-F のルールで `role in ['editor', 'viewer']` と
`status == 'pending'` を必ず確かめる（コード側の間違いをルールで止める）。
`invitedBy` は `calendar.ownerUid` ではなく自分の `uid`（今はたまたま同じ値だが、意味は「招待した人」）。

**35-E（2026-11-16）。** 名簿の上に `item { InviteSection(…) }` をオーナーのときだけ出す。`InviteSection` は UiState を丸ごと受けず、
招待の 4 項目と呼び鈴 3 つだけを受け取る。踏んだところ：「招待」ボタンに `onClick = onInvite()` と書いた
（`()` で**描いた瞬間に鳴らしてしまい**、ボタンには結果の `Unit` が渡る。`Argument type mismatch: actual type is 'Unit'`）。
**呼び鈴を「渡す」ときは `()` を付けない、「鳴らす」ときは付ける。** 33-B の `onCalendarSelected`（`()` 無しで鳴らない）の逆。

**35-F：`invites` の `create` を追加（2026-11-16 適用）。** 既存分はそのまま、`calendars` の外に足した。

```
match /invites/{inviteId} {
  allow create: if request.auth != null
    && isCalendarOwner(request.resource.data.calendarId)
    && request.resource.data.invitedBy == request.auth.uid
    && request.resource.data.id == inviteId
    && request.resource.data.role in ['editor', 'viewer']
    && request.resource.data.status == 'pending'
    && request.resource.data.calendarName == get(/databases/$(database)/documents/calendars/$(request.resource.data.calendarId)).data.name
    && request.resource.data.invitedEmail == request.resource.data.invitedEmail.lower();
}
```

- 招待できるのはオーナーだけ。`role` は `editor` / `viewer` だけ、`status` は `pending` だけ（35-D-3 の `OWNER` / `ACCEPTED` の取り違えをルールでも止める）
- `calendarName` は本物の部屋の名前と照合する。招待された人は部屋を読めず、この写しを信じて承認するしかないため
- `invitedEmail` は小文字だけ（grammar §4-(130)）。Step 36 で「自分のメール宛て」を探すとき、大文字まじりだと見つからない
- `invites` の `read` / `update` / `delete` はまだ拒否される（Step 36 で開ける）

⚠️ **ルール全文をターミナルからコピーすると、長い行が折り返し位置で改行され `Unexpected '=='` などが連発する**（35-F で踏んだ）。
ファイルに書いて `pbcopy < ファイル` でクリップボードに入れ、コンソールで ⌘A → ⌘V する。

**35-G（2026-11-16）。** 招待状が 1 枚でき、`role` / `status` / 小文字の `invitedEmail` / `calendarName` を確認。
未登録のアドレス・すでにメンバーの人では招待状ができないこと、オーナー以外にカードが出ないことも確認した。
途中で 2 回 DNS が切れた（検証環境のハマりどころ 2026-11-16 を参照）。**これで Step 35 は完了。**

#### Step 36 の内訳（招待受信・承認/拒否）

**設計（2026-11-16 にユーザーが選択）。**
- 入口はホームの ▼ メニューの「届いた招待（n）」（33-H のメニューに足す。0 件なら行を出さない）。
- **メニューを開かなくても届いていると分かるように、上のバーの ▼ の角に赤い点（`Badge`）を付ける**
  （2026-11-16 にユーザーが指定）。件数は点には出さず、メニューの行で見せる。0 件なら点も出さない。
- 承認したあとは**一覧に残る**（ホームは切り替えない。招待状が一覧から消えるだけ）。
- **招待状の id を `「カレンダーid」_「小文字のメール」` にする**（35-D-3 の UUID から変更）。
  ルールが住所を組み立てて招待状を直接引けるため（承認時に「その人宛ての招待状があるか」を確かめる）。
  同じ人を同じカレンダーへ 2 回招待しても同じ住所に上書きされ、招待状が 2 枚にならない。
- 承認は `runBatch` で 3 つ同時：名簿 `members/{自分}` を作る・部屋の `memberUids` に自分を足す・招待状を `accepted` に。
  拒否は招待状を `declined` にするだけ。

| | 内容 | 状態 |
|---|---|---|
| 36-A | `sendInvite` の id を `「カレンダーid」_「メール」` に変更＋コンソールの UUID の招待状を削除 | ✅ 11-16 |
| 36-B | `InviteRepository` に `observeMyInvites` / `acceptInvite` / `declineInvite` | ✅ 11-17 |
| 36-C | `InviteRepositoryImpl`（`toInvite`・購読・承認の `runBatch`・拒否） | ✅ 11-17（直しは Claude） |
| 36-D | ▼ の赤い点＋メニューの「届いた招待（n）」（`CalendarViewModel` が件数を購読） | ✅ 11-17（実機の点は 36-G 後） |
| 36-E | 招待一覧画面（`presentation/invites/`。UiState / ViewModel / Screen） | ✅ 11-17 |
| 36-F | `Routes` ＋ `NavHost` に登録 | ✅ 11-17 |
| 36-G | ルール：`invites` の `read` / `update`、招待による `members` の `create`・部屋の `memberUids` の `update`（Claude が書く） | ✅ 11-17 |
| 36-H | 実機確認（2 アカウントで 招待→承認→▼ にカレンダーが出る／拒否） | ✅ 11-17（Claude が adb で操作） |

**36-A（2026-11-16）。** `id = "${calendarId}_$email"`。踏んだところ（どれもビルドが通る）：
§4-(124) の画面の住所 `"calendar_members/$calendarId"` を写した（メール無し・`/` は Firestore の id に使えない）、
`"{$calendarId}_email"`（`$` と `{` が逆で `{ }` が文字のまま残る／`$` の無い `email` は 5 文字そのもの）。
**`${ }` は `$` と `{` がくっついて 1 つの合図。** 文字列テンプレートは、できあがる文字を 1 回書き出して確かめる。

**36-C（2026-11-17）。** ユーザーが書いたあと、直しを依頼により Claude が行った。踏んだところ：
- `whereArrayContains` を `whereEqualTo` の代わりに使った（`invitedEmail` / `status` は配列ではなく文字 1 つ。**0 件に見えるだけでエラーにならない**）
- `update` / `whereEqualTo` に enum（`InviteStatus.PENDING`）をそのまま渡した（大文字の `"PENDING"` になり保存値 `"pending"` と合わない。`.name.lowercase()` が要る）。
  **Firestore の API は `Any` を受け取るのでビルドが通る**（Step 30 の ⚠️ と同じ）
- `valueOf(…).uppercase()`（大文字化は**文字に**かける。`valueOf((… ?: throw …).uppercase())` の順）／ `status` に `MemberRole.valueOf`
- `acceptInvite` の住所 3 つがずれた（`calendarDoc` に `invite.id`、`inviteDoc` に名簿の行の住所）。
  `memberDoc` は `calendarDoc.collection(SUBCOLLECTION_MEMBERS).document(uid)` と**部屋から 1 段降りる**形にすると取り違えにくい
- `COLLECTION_CALENDARS` などの定数と `CalendarMember.toMap()` を `internal` にして招待の承認から借りた（grammar §4-(131)）

**36-D（2026-11-17）。** `CalendarViewModel.observeInvites()` が `observeMyInvites(myEmail)` を見張り、`list.size` を `pendingInviteCount` に入れる。
上のバーは名前と ▼ を別の `Text` に分け、▼ を `BadgedBox` で包んで数 > 0 のとき `Badge()`（grammar §3-(132)）。メニューの行も同じ式で出し分ける。
今のルールでは `invites` を読めないので、見張りは失敗して（握りつぶして）0 件のまま。**点が実機で出るのは 36-G のあと。**
踏んだところ：型の名前 `InviteRepository.observeMyInvites` で呼んだ（呼ぶのは窓口の人 `inviteRepository`）、
引数に `uid` を渡した（宛先はメール。`String` 同士でビルドが通る）、`myEmail` の `?.lowercase()` 忘れ、
メニューの行で `onCreateCalendarClick()` を鳴らした（`() -> Unit` 同士でビルドが通る）。

**36-E（2026-11-17）。** `presentation/invites/`。承認・拒否のあとに一覧を読み直す処理は書かない（`pending` でなくなると
`observeMyInvites` の絞り込みから外れ、Flow が 1 枚減った束を流す。ホームの赤い点も同時に消える）。
「送っている途中」は `Boolean` ではなく **`processingInviteId: String?`**（押した 1 枚のボタンだけ止めるため）。
結果の一言 `actionMessage` は `errorMessage` と分ける（出ても一覧を消さない）。肩書きの日本語化 `MemberRole.label()` は
`MemberListScreen` と重複して持つ（`private`。3 画面目で共通化）。新しい部品 `OutlinedButton`（grammar §3-(133)）。
踏んだところ：呼ぶ場所に**宣言の形** `acceptInvite(invite: Invite, uid: String）` を写した（呼ぶときは型を書かず箱を渡す）＋全角の `）`。

**36-G：ルールを追加（2026-11-17 適用）。** 足した分（既存分はそのまま。全文はコンソール）。

```
function myEmail() { return request.auth.token.email.lower(); }
function myInvitePath(calendarId) {
  return /databases/$(database)/documents/invites/$(calendarId + '_' + myEmail());
}
function isAcceptingInvite(calendarId) {
  return get(myInvitePath(calendarId)).data.status == 'pending'
    && getAfter(myInvitePath(calendarId)).data.status == 'accepted';
}
// isValidNewInvite(inviteId) … 35-F の create の条件を関数化＋「住所 == calendarId_invitedEmail」を追加

// calendars：招待された人が memberUids に自分だけを足す
allow update: if request.auth != null
  && !(request.auth.uid in resource.data.memberUids)
  && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['memberUids'])
  && request.resource.data.memberUids.hasAll(resource.data.memberUids)
  && request.resource.data.memberUids.size() == resource.data.memberUids.size() + 1
  && request.auth.uid in request.resource.data.memberUids
  && isAcceptingInvite(calendarId);

// members：招待された人が自分の行を作る（肩書き・招待した人は招待状のとおり）
allow create: if request.auth != null
  && memberUid == request.auth.uid
  && request.resource.data.uid == request.auth.uid
  && request.resource.data.role == get(myInvitePath(calendarId)).data.role
  && request.resource.data.invitedBy == get(myInvitePath(calendarId)).data.invitedBy
  && isAcceptingInvite(calendarId);

// invites
allow read: if request.auth != null && resource.data.invitedEmail == myEmail();
allow create: if request.auth != null && isValidNewInvite(inviteId);
allow update: if request.auth != null && isValidNewInvite(inviteId)      // オーナーの送り直し（同じ住所に上書き）
  && request.resource.data.calendarId == resource.data.calendarId
  && request.resource.data.invitedEmail == resource.data.invitedEmail;
allow update: if request.auth != null                                    // 本人の返事
  && resource.data.invitedEmail == myEmail()
  && resource.data.status == 'pending'
  && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status'])
  && request.resource.data.status in ['accepted', 'declined'];
```

- 同じ `allow update:` を 2 行書くと「どちらかが『はい』なら通る」（grammar §4-(134)）
- `get`（前）が `pending` で `getAfter`（後）が `accepted` ＝ **招待状を承認済みにする書き込みと同時でないと名簿に入れない**。使い回しもできない
- ⚠️ **弱点：メールアドレスの持ち主確認をしていない。** 他人のアドレスで先に登録すると、その人宛ての招待を受けられる。
  直すにはサインアップ時の確認メール＋ルールの `request.auth.token.email_verified == true`（テスト用アカウントが使いにくくなるので保留）

**36-H（2026-11-17）。** ユーザーの依頼で Claude が adb（`uiautomator dump` で文字の位置を探してタップ）で操作した。
テスト用アカウントは **`test@example.com`（school のオーナー）** と **`tester@example.com`（36-H でサインアップして作成）**。パスワードはどちらも同じ。
1. test が school のメンバー画面で `Tester@example.com`（大文字まじり）を編集者で招待 →「招待を送りました」
2. tester でログイン → ▼ に赤い点、メニューに「届いた招待(1)」→ 一覧に「school ／ 編集者として招待されています」
3. ［承認］→「「school」に参加しました」、カードが消える。ホームの赤い点が消え、▼ に school が増える
4. test のメンバー画面に `tester@example.com ／ 編集者`
5. test が tester を外す → 閲覧者で**送り直し**（同じ住所への上書き）→ 通る
6. tester で［拒否］→「招待をお断りしました」。▼ に school は出ない
すべて Logcat に `PERMISSION_DENIED` なし。**これで Step 36 は完了。**

⚠️ 最初は「このメールアドレスの人は登録されていません」になった。`tester` のアカウント自体が無かったため（Auth に無い＝`users` の名刺も無い）。
**招待のテストでは、相手が一度ログイン（またはサインアップ）して名刺がある状態にしておく。**

#### Step 37 の内訳（ロールベースのルール）

**設計（2026-11-17 にユーザーが選択。要件定義書 §3.2 に表で記載）。**
編集者は**他のメンバーが作った予定・タスクも**編集・削除できる。閲覧者は**読むだけ**（タスクの完了チェックも不可）。

| | 内容 | 状態 |
|---|---|---|
| 37-A | `events` の作成・編集・削除を `isMember` → `canEdit`（肩書きを見る）に（Claude が書く） | ✅ 11-17 |
| 37-B | 実機確認（閲覧者は作成できない／編集者はできる／マイカレンダーは今までどおり） | ✅ 11-17（Claude が adb で操作） |

```
function myRole(calendarId) {
  return get(/databases/$(database)/documents/calendars/$(calendarId)/members/$(request.auth.uid)).data.role;
}
function canEdit(calendarId) {
  return request.auth.uid == calendarId
    || myRole(calendarId) in ['owner', 'editor'];
}
// events：read は isMember のまま。create / update / delete を canEdit に
```

**37-B（2026-11-17）。** tester を school に閲覧者で招待→承認して確認。

| 誰が | どこで | 操作 | 結果 |
|---|---|---|---|
| tester（閲覧者） | school | 予定を保存 | ✅ 拒否（「保存できていません」＋ Logcat に `PERMISSION_DENIED`） |
| tester（閲覧者） | マイカレンダー | 予定を保存 | ✅ 保存できる（`canEdit` の左側で通る） |
| test（オーナー） | school | 予定を保存 | ✅ 保存できる |
| tester（編集者に変更後） | school | 予定を保存／**オーナーが作った予定を削除** | ✅ どちらもできる |

確認用の予定は削除済み。**今の状態：tester は school の編集者**。
⚠️ 閲覧者にも「＋」「編集」「削除」「完了チェック」が出たままで、押すと保存時に断られる（**Step 38 で隠す**）。
エラーの文言が「保存できていません」なのも、閲覧者向けには分かりにくい。

#### Step 38 の内訳（画面のロール出し分け）— Phase 3 の最後

**設計（2026-11-17 にユーザーが選択）。**
- ホームと詳細の頭脳が、それぞれ「このカレンダーでの自分の肩書き」を調べて **`canEdit: Boolean`** を UiState に持つ。
  調べ方は `observeMembers`（Step 30）から自分の行を探すだけ。窓口に関数は足さない。マイカレンダーはいつも `true`。
- 閲覧者には「＋」と詳細の［編集］［削除］を**出さない**。上のバーのカレンダー名の下に小さく **「閲覧のみ」**。
- タスクの完了チェックは**出したまま灰色で押せなくする**（`enabled = canEdit`）。行の形を変えないため。
- 作成・編集画面は入口（＋・編集）を消せば届かないので変更しない。ルール（Step 37）が最後の砦なのは変わらない。

| | 内容 | 状態 |
|---|---|---|
| 38-A | ホームの頭脳：`canEdit` ＋ カレンダーを切り替えるたびに肩書きを見張り直す | ✅ 11-17 |
| 38-B | ホームの画面：「＋」を出し分け・「閲覧のみ」・チェックの `enabled` | ✅ 11-17（実機は 38-E） |
| 38-C | 詳細の頭脳：予定の `calendarId` から `canEdit` を求める | ✅ 11-17 |
| 38-D | 詳細の画面：［編集］［削除］を出し分け | ✅ 11-17 |
| 38-E | 実機確認（tester を閲覧者に戻して確認） | ✅ 11-17（ユーザーが確認） |

**38-A（2026-11-17）。** `observeRole(calendar)` を `observeMonth` の直後で呼ぶ。中は `roleJob?.cancel()` → マイカレンダーなら `true` →
共有なら一旦 `false`（閲覧者に一瞬「＋」が出るちらつき防止）→ `observeMembers` を見張り
`members.firstOrNull { member -> member.uid == myUid }?.role` が `OWNER` か `EDITOR` なら `true`（ルールの `in ['owner', 'editor']` と同じ基準）。
踏んだところ：呼ぶ場所に `observeMonth(_uiState.collect { calendar -> calendarId } }` と書いた（係の取り違え＋すでに `collect` の中なのに受け取り直そうとした）。

**38-B（2026-11-17）。** 上のバーのボタンの中身を `Column { Row { 名前 ▼ } ; if (!uiState.canEdit) Text("閲覧のみ") }` に組み替え、
「＋」は `if (uiState.canEdit)`、`ScheduleItemRow` に `canEdit: Boolean`（**既定値なし**＝渡し忘れをビルドで止める）を足して `Checkbox(enabled = canEdit)`。
踏んだところ：`uiState.canEdit != MemberRole.VIEWER`（`Boolean` と肩書きを比べた。画面は肩書きを知らず「はい／いいえ」だけ見る）、`!` の付け忘れで出す条件が逆。

**38-C（2026-11-17）。** 詳細の頭脳は掲示板を見ず、**予定の `calendarId`** から肩書きを調べる。マイカレンダーの見分けは `calendarId == myUid`
（Step 33 の「マイカレンダーの id ＝ uid」。ルールの `canEdit` の左側と同じ）。`canEdit` は **`false` 始まり**（開いた瞬間は予定も肩書きも不明）。
予定は編集のたびに流れてくるが置き場所は変わらないので、`roleCalendarId` で同じなら見張り直さない。一発で正解。

**38-D（2026-11-17）。** ［編集］［削除］を `if (uiState.canEdit) { }` で包む（［戻る］は誰にでも出す）。Preview は予定＝編集できる人、タスク＝閲覧者。

**38-E（2026-11-17）。** ユーザーが実機で動作確認まで完了。**これで Step 38、Phase 3 の予定ステップはすべて完了。**
確認の前にエミュレータが「ネットに接続できません」になった（Device Manager から起動され、テザリング時の `--host-dns=172.20.10.1` を握っていた）。
`-dns-server 8.8.8.8,1.1.1.1` で起動し直して解消。**Device Manager の ▶ で起動すると DNS 指定が外れる**ので、ネットワークを行き来する日はコマンドで起動する。

#### Step 39 の内訳（メールアドレスの持ち主確認）— Phase 3 の手直し

**36-G の弱点（他人のアドレスで先に登録すると、その人宛ての招待を受けられる）を塞ぐ。設計（2026-11-17 にユーザーが選択）。**
- 確認を求めるのは**招待の受け取りだけ**。確認していなくてもアプリ自体は使える
- サインアップ成功時に確認メールを送る。招待一覧で未確認なら「確認メールを再送」「確認しました」を出す
- ▼ メニューの「届いた招待」は、未確認なら件数が読めないので「届いた招待（要メール確認）」として出す（入口が消えないように）
- ルール：`invites` の `read` / 本人の返事、`isAcceptingInvite` に `request.auth.token.email_verified == true` を足す
- ⚠️ 確認状態は**通行証（ID トークン）に焼き込まれている**。リンクを押しただけではルール側は古いまま。
  「確認しました」で `reload()` ＋ `getIdToken(true)`（作り直し）が要る
- **テストは Gmail の `+` 別名**（自分のアドレスに `+tester` などを付けたもの）でアカウントを作り直す。
  `example.com` のアカウントは確認メールを受け取れないので、招待を受けられなくなる

| | 内容 | 状態 |
|---|---|---|
| 39-A | `AuthRepository` に `isEmailVerified` / `sendEmailVerification` / `refreshEmailVerified` ＋ 実装 | ✅ 11-17 |
| 39-B | サインアップ成功時に確認メールを送る | ✅ 11-17 |
| 39-C | 招待一覧の頭脳：未確認なら見張らず、再送・「確認しました」 | ✅ 11-17 |
| 39-D | 招待一覧の画面（未確認のカード）＋ ▼ メニューの入口 | ✅ 11-17 |
| 39-E | ルールに `email_verified` を足す（Claude が書く） | ✅ 11-17 |
| 39-F | 実機確認（Gmail の `+` 別名のアカウントで） | ✅ 11-17（Claude が adb で操作） |

**39-A（2026-11-17）。** 確認メールは `firebaseUser.sendEmailVerification()`（本人に頼む。パスワードリセットは受付 `firebaseAuth` に宛先を渡す）。
`isEmailVerified` は `get()` で読むたびに計算（grammar §4-(135)）。踏んだところ：約束側の戻り値を `Result<Unit>` のままにした
（エラーは**実装側**の行に「overridden member と合わない」と出る。interface 側も開いて見比べる）、お手本の `sendPasswordResetEmail` の
`Result.success(Unit)` をうっかり `Result.success():` に書き換えた。

**39-F（2026-11-17）。** 受け取り役として **`himari20051226+tester@gmail.com`**（Gmail の `+` 別名。パスワードは test と同じ）を作成。
1. サインアップ → ホームへ。▼ に「届いた招待（要メール確認）」→ 確認のカード
2. リンクを押す前に［確認しました］→「まだ確認できていません」
3. test が school に編集者で招待 →「招待を送りました」
4. tester（`example.com`・未確認）→ ▼ は「要メール確認」、一覧はカードのまま（見張りを始めないので `PERMISSION_DENIED` も 0 件）
5. ユーザーが Gmail でリンクを押す →［確認しました］→ school の招待が出る →［承認］→「「school」に参加しました」、▼ に school、
   メニューの「届いた招待」の行は消える（ホームが ▼ を開いたときに確認済みを写し直した）。`PERMISSION_DENIED` 0 件
**これで Step 39 は完了。** `test` / `tester`（`example.com`）は確認メールを受け取れないので、今後**招待を受け取る役**は `+tester@gmail.com` を使う。
💡 adb で入力するときは Gboard に加えて**音声入力（Google Voice Typing）も無効化**しておく。Gboard だけ止めると、文字欄を押した瞬間に音声入力の画面が開いて入力が途中で切れる（39-F で踏んだ。終わったら `ime enable` で戻す）。

#### Step 31（カレンダー一覧画面）

設定 →「カレンダー」で `presentation/calendarlist/` の一覧を開く。`CalendarListViewModel` は
`init` で `observeMyCalendars(uid)` を 1 回 collect するだけ（月の移動が無いので `observeJob` は持たない）。
`CalendarListUiState.isLoading` は `true` 始まり。一覧の「＋」は Step 32 で作成画面へ繋ぐ（今は空の `{ }`）。

**Firestore ルールに `calendars` の読み取りを追加（2026-10-04）。** ルールの無いパスは拒否されるため。

```
match /calendars/{calendarId} {
  allow read: if request.auth != null
    && request.auth.uid in resource.data.memberUids;
}
```

書き込みは Step 32（作成）・Step 37（ロールベース）で足す。`calendars` の書類はまだ 1 件も無いので、
一覧は「カレンダーがまだありません」と出るのが正しい。

⚠️ **行の名前に `calendar.ownerUid` を渡しても型が `String` 同士なのでビルドが通る。**
一覧に uid が出るだけで気づきにくい（Step 19-D の取り違えと同じ種類）。Preview で目視確認する。

#### Step 28（`users` への自己登録）

Firebase Authentication には**他人をメールアドレスで検索する手段が無い**（総当たりで会員名簿を
作られるのを防ぐため）。Step 35 の招待はメールアドレスで相手を指名する仕様（要件定義書 §3.4）なので、
**検索できる名簿を自分たちで `users` に作る**。

| | 内容 | 状態 |
|---|---|---|
| 28-A | `domain/repository/UserRepository`（`saveUser` / `findUserByEmail`） | ✅ |
| 28-B | `data/repository/UserRepositoryImpl`（Firestore 実装） | ✅ |
| 28-C | `RepositoryModule` に `bindUserRepositoryImpl` を追加 | ✅ |
| 28-D | ログイン / Google ログイン / サインアップの成功時に `saveUser` を呼ぶ | ✅ |
| 28-E | セキュリティルールに `users` を追加 | ✅ |
| 28-F | 実機確認（`users` に名刺ができ、再ログインで `createdAt` が変わらない） | ✅ 09-24 |

**`AuthRepository` と `UserRepository` は別物。** 前者は Firebase Authentication で
「**自分が**誰か」、後者は Firestore で「**他人を含めた**名簿」。同じ `User` 型を扱うが、
Step 28 は「Auth から来た `User` を Firestore へ写す」作業。

**`findUserByEmail` の戻り値は `Result<User?>`。** 封筒（`Result`）が「通信できたか」、
中身の `?` が「その人が居たか」。`getItem` が `Result<ScheduleItem>`（`?` 無し）なのと対照的で、
**「見つからないのは異常か、普通のことか」**で決めている。招待相手が未登録なのは普通に起きる。

**`.set(map, SetOptions.merge())` を使う**（grammar §4-(109)）。`createdAt` は
`if (snapshot.exists())` で**初回だけ**入れる。毎回入れると登録日がログインのたびに
今日へ書き換わる。Phase4 の `fcmTokens` を消さないためにも `merge` が要る。

⚠️ **`email` は保存側・検索側の両方で `lowercase()` する。** Firestore の検索は大文字小文字を
区別するため、片方だけだと「登録したのに招待で見つからない」になる。保存側は `String?` なので `?.`。

⚠️ **`saveUser` は `isLoginSuccess = true` より前に呼ぶ。** 逆にすると画面が HOME へ移って
ログイン画面が履歴から消え、`LoginViewModel` が捨てられ `viewModelScope` が打ち切られる。
**画面が消えるより前に通信を終わらせる。**

**`saveUser` の失敗は握り潰している（`onFailure` を書いていない）。** ログイン自体は成功しており、
実害は「招待で見つけてもらえない」だけ。ログインのたびに呼ぶので次回に自己修復する。
**毎回呼ぶ・何度呼んでも同じ結果（`merge`）だからこそ、エラー処理を省ける。**

**セキュリティルール（2026-09-24 適用）**

```
match /users/{userId} {
  allow read: if request.auth != null;
  allow create, update: if request.auth != null && request.auth.uid == userId;
  allow delete: if false;
}
```

`read` が「ログインしていれば誰でも」なのは、招待でメールアドレス検索をするため
（自分以外の名刺を読めないと成立しない）。**代償として全ユーザーのメールアドレスが読める**ので、
Phase4 で Cloud Functions を入れるときに検索をサーバー側へ寄せる（要件定義書 §7 に記載）。
`delete` を禁じているのは、名刺が消えるとメンバー一覧が名無しになるため。

💡 **ログアウトの瞬間に `events` の購読が `PERMISSION_DENIED` で失敗する**（2026-09-24 の
Logcat で確認）。ルールが正しく効いている証拠だが、一瞬エラーが画面に出うる。
Step 33 で購読の止め方を整理するときに直す。

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

### 検証環境のハマりどころ（2026-09-19 追記）

**アプリは正常なのにログイン・サインアップだけ「ネットワークに接続できません」で失敗する。**
2026-09-19 に発生。原因は**エミュレータが起動時の DNS サーバーを握ったままだったこと**。

エミュレータは起動時の Mac の DNS を `netsimd --host-dns=…` に固定する。
起動しっぱなしのまま Mac のネットワークが変わると、**もう存在しないルーターに
問い合わせ続ける**。Firebase はホスト名で通信するので、名前解決に失敗して
`FirebaseNetworkException` → `AuthRepositoryImpl` が「ネットワークに接続できません」と日本語化する。

**切り分け手順**（アプリ側を疑う前にこれを流す）

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB shell ping -c 2 8.8.8.8              # IP 直打ち → 通れば回線は生きている
$ADB shell ping -c 2 www.googleapis.com   # unknown host なら DNS の問題
ps -ax | grep netsimd                     # --host-dns=… が起動時の値
scutil --dns | grep nameserver            # Mac の「今の」DNS。上と食い違えば確定
```

**対処：エミュレータを DNS 指定で起動し直す**（DNS 転送先は起動時に決まるので再起動が要る）。

```bash
$ADB emu kill                             # 正規に終了（qemu が残らないことを確認）
cd ~/Library/Android/sdk/emulator
./emulator -avd Pixel_7 -dns-server 192.168.1.253 -no-snapshot-load &
```

⚠️ **`ping` での検証は単発にしない。** 起動直後は `Netsim Wifi … CANCELLED` の影響で
1 回だけ失敗することがある（実際に誤診しかけた）。3 回試して判断する。
また `identitytoolkit.googleapis.com` は **ICMP を返さない**ので、ping が無応答でも異常ではない。

💡 コールドブート（`-no-snapshot-load`）するとエミュレータ内のログイン状態は消える。

**2026-10-22 に再発。** 33-E の確認中、ログアウト→再ログインで「ネットワークに接続できません」。
`netsimd --host-dns=202.13.160.201,…`（前のネットワーク）に対し Mac は `192.168.1.253` で、上の手順どおり確定。
`adb emu kill` → `-dns-server 192.168.1.253 -no-snapshot-load` で起動し直して解消した。
**ログアウトの直後に起きると「33-E の変更で壊した」と誤解しやすい**が、アプリとは無関係。

**2026-11-16 に再々発。** 35-G で「招待」を押しても `invites` ができず、Logcat に
`Unable to resolve host "firestore.googleapis.com"`。`--host-dns=202.13.160.200,…` に対し Mac は `192.168.1.253`。同じ手順で解消。
**ルールを変えた直後だと「ルールのせい」と疑いやすい**が、ルールに断られたなら `PERMISSION_DENIED` が出る。
`UnknownHostException` ／ `Unable to resolve host` なら DNS を疑う。
さらに同日、`192.168.1.253` で起動し直した 20 分後に **Mac が `202.13.160.200` 側へ戻り**、また引けなくなった
（2 つのネットワークを行き来している）。**今後は公開 DNS で起動する**（どちらのネットワークからでも届く）。

```bash
./emulator -avd Pixel_7 -dns-server 8.8.8.8,1.1.1.1 -no-snapshot-load &
```

💡 オフライン中に「招待」を押すと**「送信中…」のまま止まる**。`.set().await()` はサーバーの受け取り確認を待ち続け、
エラーにならないため（「Firestore が繋がっていなくてもエラーにならない」と同じ性質）。書き込みは端末内に溜まり、
繋がったときに送られるので、**送り直す前にコンソールで届いていないか確かめる**（二重に招待状ができる）。

### 検証環境のハマりどころ（2026-08-12 追記）

**「... is already running as process NNNN」でビルド/実行できない。**
エミュレータのウィンドウを閉じても qemu プロセスだけ残ることがある（スリープ復帰後、
Android Studio の強制終了後に起きやすい）。2026-08-10 と 08-12 に発生。

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices                       # offline と出ていればこの症状
Stop-Process -Id <PID> -Force        # メッセージに出ている PID
& $adb kill-server; & $adb start-server
```

そのあと Device Manager から起動し直す。`.lock` ファイルが残っていれば
`~\.android\avd\<AVD名>.avd\` 配下から削除する。

**ビルドキャッシュの破損。** 症状によって対処を変える（軽い方から試す）。

| 症状 | 対処 |
|---|---|
| `NoSuchFileException`（Hilt/KSP の生成ファイルが無い） | `.\gradlew.bat :app:clean` |
| `Incremental compilation failed` / `EOFException` | `.\gradlew.bat --stop` → `app\build`, `build`, `.kotlin` を削除 → 再ビルド |
| Android Studio の表示だけおかしい | File → Invalidate Caches |

⚠️ `clean` の実行中に Android Studio でビルドやエディタ操作をしない。
同じフォルダを 2 プロセスが同時に触ると破損する（実際に発生した）。

**Firestore が繋がっていなくてもエラーにならない。**
Firestore SDK はオフライン時、端末内のキャッシュを返して裏で再接続を試み続ける。
そのため画面上は「0 件」に見え、`close(error)` も呼ばれない。
**「エラーが出ない＝繋がっている」ではない。** 疑わしいときは Logcat を `Firestore` で絞る。

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

- WorkManager 2.10.0 ＋ androidx.hilt:hilt-work 1.3.0（ローカル通知。Step 26）

### これから導入（仕様書 §2 の予定）
- AlarmManager（`setExactAndAllowWhileIdle`）— WorkManager の遅れが問題になったら
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
Phase2: 個人のスケジュール/タスク管理 + ローカル通知  ← 完了（2026-09-22）
Phase3: 共有カレンダー + ロールベース認可  ← 完了（2026-11-17。Step 39 の手直しまで）
Phase4: プッシュ通知                      ← 完了（2026-11-20。Step 40〜45）
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
- **Firestore データベースは 2026-08-12 に作成**（ロケーション `asia-northeast1` / テストモード）。
  Phase1 では Auth しか使っておらず、データベース自体が存在しなかった。
  作成には **Blaze プラン（従量課金）が必須**だったため 2026-08-12 にアップグレード済み。
  無料枠内なら実費はほぼ 0 円だが、GCP の予算設定は**アラートのみで自動停止はしない**。
- ~~Firestore のセキュリティルールは未設計（テストモードのまま）。テストモードは作成から30日
  （＝2026-09-11 まで）で書き込みが拒否される。~~ → **2026-09-04 に本設計・適用済み**（`events` を
  所有者（`calendarId`）ベースで絞るルールに変更。詳細は Step 18 直後の記述を参照）。
  `calendars` / `users` / `invites` 向けのロールベース認可は Phase3 で共有機能を実装する際に設計する。

## ドキュメント

- `docs/requirements.md` — 要件定義書。**仕様の正**。決定が変わったら都度更新する。
- `README.md` — 対外向け（ポートフォリオ）の紹介。
