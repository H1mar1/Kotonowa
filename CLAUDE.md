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

**Phase 1（認証）完了（2026-08-02）。Phase 2 進行中 — 作成→保存→一覧反映に加え、
行タップ→詳細→削除まで実装（2026-09-02）。Step 18 は 2026-09-03 の実機確認（18-F）で完了。
Step 19（編集）は 2026-09-15 の実機確認（19-F）で完了 — 一覧→詳細→編集→上書き保存が通った。
Step 20 で詳細画面を 1 件購読（`observeItem`）に変え、「保存後に戻った詳細画面が古いまま」を解消（2026-09-15）。**

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
