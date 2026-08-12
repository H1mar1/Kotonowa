# Kotlin / Compose 文法メモ

このファイルは2つの役割を持つ。

1. **Claude 向けの運用ルール** — コードを提示するときの文法解説の義務（§0）
2. **既出の文法の記録** — 一度説明した文法の一覧（§1 以降）。
   ユーザーが読み返す用であり、Claude が「何を既に説明したか」を知る用でもある。

---

## §0 運用ルール（Claude 向け・必須）

### コードを1行でも提示したら、必ず文法を解説する

ユーザーは「文法が分からないまま呪文を写している」状態を最も嫌う。
**「何をするコードか」だけでなく「なぜその書き方になるのか」を必ず添える。**

提示するコードに含まれる文法要素を、以下の手順で処理する。

1. **§1 以降に載っていない文法** → その場でかみ砕いて説明し、**このファイルに追記する**
2. **§1 以降に載っている文法** → 「§1-③ の『やって!』の合図」のように**番号で参照**する。
   毎回ゼロから説明し直さなくてよいが、参照は必ず示す（ユーザーが読み返せるように）
3. **記号は読み方を日本語で示す。** `.` は「〜の」、`?:` は「〜がなければ」など

### 説明の粒度

- 変数名・引数名は「どこから来た名前か」を示す（ライブラリが決めた名前か、ユーザーが決めた名前か）
- 同じ綴りが2回出るとき（`request = request` など）は必ず左右の違いを説明する
- 大文字始まり／小文字始まりの区別（§1-⑧）は混乱の元なので、該当したら毎回触れる
- 英語のエラーメッセージは**日本語に訳してから**原因を説明する

### やってはいけないこと

- 「よくある書き方です」「お決まりです」で済ませる
- 説明なしにコードだけ貼る
- 「§1 に書いてあるので参照して」とだけ言って本文で触れない

---

## §1 基本の部品

### ① `val 名前 = 中身` — 箱に名前をつける

```kotlin
val request = GetCredentialRequest.Builder().build()
```

読む順番は**右から左**。右で作ったものを、左の名前の箱にしまう。

- `val`（バル）＝ 一度入れたら中身を入れ替えない箱
- `var`（バー）＝ 入れ替えられる箱

### ② `.` — 「〜の」と読む

| コード | 読み方 |
|---|---|
| `context.getString(...)` | context **の** getString |
| `R.string.default_web_client_id` | R **の** string **の** default_web_client_id |

左が大きい入れ物、右がその中身。住所を絞り込むのと同じ。

### ③ `()` — 「やって!」の合図

名前のうしろに `()` があれば**命令**、なければ**物**。

| コード | 種類 |
|---|---|
| `build()` | カッコあり → 命令 |
| `LocalContext.current` | カッコなし → 物 |

カッコの中が空でも `()` は必ず書く（「引数はないが実行はする」の合図）。

### ④ `()` の中身 — 引数（ひきすう）

命令に必要な材料。2つ以上なら `,` で区切る。

```kotlin
getCredential(activity, request)
```

### ⑤ `名前 = 値` を `()` の中で使う — 名前付き引数

材料に荷札を付けて渡す書き方。④と意味は同じ。

```kotlin
getCredential(
    context = activity,
    request = request,
)
```

⚠️ **`()` の中の `=` は①の「箱に入れる」ではない。** 「この荷札の材料はこれ」という対応付け。
左が荷札の名前（ライブラリが決めた名前）、右が渡すもの（ユーザーが決めた箱の名前）。

### ⑥ `{ }` — 「あとでやる作業メモ」（ラムダ）

```kotlin
onGoogleLoginClick = {
    ...
}
```

`{ }` の中身は**その場では実行されない**。「押されたらこれをやってね」というメモを渡しているだけ。
これを**ラムダ**という。

補足：引数が `{ }` 1個だけのとき、カッコを省略できる。
`launch({ ... })` → `launch { ... }`

### ⑦ メソッドチェーン — `.` を縦に並べる

```kotlin
val googleOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(...)
    .build()
```

**行が `.` で始まっていたら前の行の続き。** この4行で1つの文。
`setXxx()` が毎回**自分自身を返す**のでバトンリレーのように繋げられる。

`build()` だけは「完成品」を返すので、その後ろには繋げられない。

### ⑧ 大文字始まりか小文字始まりか

| 見た目 | 正体 | 例 |
|---|---|---|
| **大**文字始まり | 設計図（型・種類の名前） | `CredentialManager`, `Modifier` |
| **小**文字始まり | 実物（箱の名前・命令の名前） | `credentialManager`, `modifier`, `build()` |

`modifier = Modifier` と `modifier = modifier` は「設計図を渡す」と「実物を渡す」で全く違う。

### ㊿ `{ a, b -> ... }` — 材料を受け取るラムダ

⑥ のラムダ（あとでやる作業メモ）は、**呼ばれるときに材料を渡してもらう**ことがある。
その材料に名前を付けるのが `->`（矢印）の左側。

```kotlin
.addSnapshotListener { snapshot, error ->
    // ここで snapshot と error が使える
}
```

読み下すと「**あとで `snapshot` と `error` を渡して呼んでね。そうしたらこれをやる**」。

- `->` の**左が受け取る材料の名前、右がやること**
- 名前は**自分で決めてよい**（`{ s, e -> }` でも動く）。中身が何かは呼ぶ側＝ライブラリが決めている
- 材料が**1個のときだけ** `->` ごと省略でき、`it`（イット＝それ）という名前で使える

```kotlin
list.map { it.title }        // 省略形
list.map { item -> item.title }  // 同じ意味
```

⚠️ §1-⑤ の `名前 = 値`（名前付き引数）と混同しないこと。あちらは「**渡す**ときの荷札」、
こちらは「**受け取った**ものに付ける名前」で、向きが逆。

**その名前が通じるのは `{ }` の中だけ**（スコープ＝有効範囲）。

```kotlin
.collect { list ->
    // ← ここでは list が使える
}
// ← ここでは list はもう存在しない（Unresolved reference: list）
```

`list` は「**流れてきたとき、その1回ぶんを呼ぶための呼び名**」でしかない。
`{ }` を抜けた場所には「流れてきたもの」自体が無いので、名前も消える。

💡 だから「流れてきたら○○する」という処理は、**必ず `{ }` の中に書く**。
外に出したくなったら、それは設計を間違えている合図。

### (52) `"名前"` と `名前` — クオートの有無で世界が変わる

同じ綴りでも、`"..."` で囲むかどうかで**別世界のもの**を指す。Firestore を触るコードで頻出。

```kotlin
"calendarId" to calendarId              // toMap（書き込む側）
.whereEqualTo("calendarId", calendarId) // クエリ（探す側）
```

| 書き方 | 正体 |
|---|---|
| `"calendarId"`（クオート**あり**） | ただの文字列。**Firestore の書類に書いてある名札**を指す |
| `calendarId`（クオート**なし**） | **Kotlin の箱**の名前（§1-①）。その中身が使われる |

Firestore は Kotlin の変数名を知らない。**名札は文字列で伝えるしかない**ので、
「書き込むときに使った綴り」と「探すときの綴り」を人間が合わせる責任がある。
打ち間違えてもコンパイルエラーにならず、静かに0件になるだけ。

だから `COLLECTION_EVENTS`（§5-㉓）のように**定数にまとめる**のが安全策になる。

💡 §1-⑧（大文字始まりか小文字始まりか）と同じく、**見た目の小さな違いが意味の大きな違い**になる例。

---

## §2 コルーチン（時間のかかる処理）

### ⑨ `suspend` — 待てる関数

`suspend`（サスペンド）が付いた関数は「終わるまで**待てる**関数」。

- 待てる関数は、**待てる場所でしか呼べない**
- ボタンの `onClick` は待てない普通の場所
- だから `scope.launch { }` で「待てる場所」を作り、その中で呼ぶ

`rememberCoroutineScope()` と `scope.launch { }` が必要になる理由はこれ1つ。

### ⑩ `viewModelScope.launch { }`

ViewModel の中で「待てる場所」を作る書き方。
ViewModel が捨てられるとき、中で走っている処理も自動で打ち切られる。

### ㊸ `.await()` — 「作業の引換券」が完了するまで待つ

Firebase の書き込み・読み込みを呼ぶと、**その場では通信していない**。
「受け付けました」と即座に返ってきて、実際の通信は裏で走る。
返ってくるのは結果ではなく **`Task`（作業の引換券）**。

`.await()`（アウェイト＝待つ）は「その引換券が『完了』になるまでここで待つ」という命令。
待つ処理なので `suspend`（⑨）が付いており、呼ぶ側の関数にも `suspend` が要る。

**書き忘れるとどうなるか。**

```kotlin
firestore.collection(...).document(...).set(...)   // await 無し
Result.success(Unit)     // ← まだ保存できていないのに「成功」を返してしまう
```

さらに、**通信が失敗しても `catch` に飛んでこない**（失敗したときにはもう `try` を抜けている）。
「成功したはずなのにデータが無い」という追いにくい不具合になる。
`.await()` があって初めて `try` / `catch`（§6-㉔）が機能する。

💡 これを使うために `kotlinx-coroutines-play-services` を入れてある。
import は `kotlinx.coroutines.tasks.await`。

### ㊹ メソッドチェーンは「手に持っているもの」が変わっていく

⑦ のチェーンは、`.` を 1 つ進むごとに手にしているものが変わる。ここを意識すると読める。

```kotlin
firestore.collection("events").document(id).set(map).await()
```

| 書いたところまで | 手に持っているもの | たとえ |
|---|---|---|
| `firestore` | Firestore 全体 | **図書館そのもの** |
| `.collection("events")` | events コレクション | 図書館の中の **1 つの棚** |
| `.document(id)` | 1 件のドキュメント | 棚に並んだ **1 冊の本** |
| `.set(map)` | 作業の引換券（`Task`） | 「書き換えといて」と頼んだ**受付票** |
| `.await()` | 作業完了 | 受付票を持って**終わるまで待つ**（㊸） |

`.collection()` と `.document()` は「**指すだけ**」で通信しない。住所を絞り込んでいるだけ（②）。
実際に通信するのは `.set()` から。

### ㉝ `Flow<T>` — 値が何度も流れてくる管

```kotlin
fun observeItems(calendarId: String): Flow<List<ScheduleItem>>
```

`suspend` との違いは「**返事の回数**」。

| | 回数 | たとえ |
|---|---|---|
| `suspend fun ...: List<T>` | **1回だけ** | 手紙を出して、返事を1通受け取る |
| `fun ...: Flow<List<T>>` | **何度でも** | 蛇口をひねる。以降、水が流れ続ける |

`Flow`（フロー＝流れ）が返すのは**値そのものではなく「管の口」**。
だから関数自体は一瞬で終わり、`suspend` は要らない。

受け取る側は「流れてきたら何をするか」を登録する。

```kotlin
repository.observeItems(id).collect { list ->
    // 新しい一覧が流れてくるたび、ここが実行される
}
```

- `collect`（コレクト＝集める）＝「管に口をつけて、流れてくるものを受け取り続ける」
- **`collect` は終わらない**（流れ続ける限り待ち続ける）ので `suspend` が付いている。
  §2-⑨ の通り「待てる場所」＝ `viewModelScope.launch { }` の中で呼ぶ

Firestore は「データが変わったら教える」機能を標準で持っているので、
それを `Flow` に流し込むと**画面が勝手に最新になる**。

### (62) `Flow` は `collect` するまで何もしない（コールド）

```kotlin
scheduleRepository.observeItems(calendarId, from, to)   // ① まだ何も起きていない
    .collect { list -> ... }                            // ② ここで初めて動き出す
```

- **①の時点では Firestore と一切通信していない。** 返ってくるのは「まだ水の流れていない蛇口」
- **②の `.collect` を付けた瞬間に、`callbackFlow { }`（㊾）の中身が実行される。**
  `addSnapshotListener` で見張りが付くのはこの瞬間

この性質を**コールド**（冷たい）という。「**誰かが受け取ろうとするまで何もしない**」ので、
誰も見ていない画面のために通信し続けることがない。

⚠️ 逆に言うと、**`collect` を書き忘れると永久に何も起きない**。エラーも出ないので気づきにくい。
「Repository は正しいのにデータが来ない」ときは、まずここを疑う。

💡 `StateFlow`（(58)）は逆で**ホット**。`collect` していなくても最新の値を持ち続けている。
画面が回転して作り直されても、すぐ今の状態を渡せるのはそのため。

### (58) `StateFlow` — 「今の値」を必ず持っている管

㉝ の `Flow` は**流れてくるだけ**で、「今いくつ？」と聞いても答えられない（蛇口は水を出すが、
出した水を覚えてはいない）。画面は「今の状態」をいつでも知る必要があるので、それでは困る。

`StateFlow`（ステートフロー＝状態の管）は「**最新の1つを常に手元に持っている管**」。

| | 今の値を聞けるか | たとえ |
|---|---|---|
| `Flow` | ✕ | 蛇口 |
| `StateFlow` | ○（`.value`） | **中身が見えるタンク付きの蛇口** |

画面が回転して作り直されても、`StateFlow` に聞けば最新の状態がすぐ手に入る。

**3点セットで使う。**

```kotlin
private val _uiState = MutableStateFlow(LoginUiState())        // ①内部用
val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()  // ②公開用

_uiState.update { it.copy(isLoading = true) }                  // ③書き換え
```

| | 役割 |
|---|---|
| `MutableStateFlow(初期値)` | **書き換えられる**版。`Mutable`（ミュータブル＝変えられる） |
| `.asStateFlow()` | 書き換え機能を隠して「**読むだけ**」の姿にする |
| `.update { }` | 今の値を `it` で受け取り、`copy()`（㉗）で一部だけ変えて入れ直す |

**なぜ2つに分けるのか。** 画面から勝手に状態を書き換えられると、どこで値が変わったのか
追えなくなる。「**書き換えていいのは ViewModel だけ**」という約束を型で強制している。
`_` 始まりの名前は「内部用」を示す慣習。

💡 画面側で受け取るときは `by ... collectAsStateWithLifecycle()`（§3-⑫）を使う。

### ㊾ `callbackFlow { }` — 「呼び返し」を `Flow` の管に変える変換器

㉝ で「Firestore の変更通知を `Flow` に流し込む」と書いたが、**そのままでは繋がらない**。
両者はデータの渡し方が違うからだ。

| | やり方 | たとえ |
|---|---|---|
| Firestore の見張り | **コールバック**（呼び返し）。「変化したら、この `{ }` を呼ぶね」 | 店から**電話がかかってくる** |
| `Flow` | 管。`collect` した側へ流れていく | **蛇口から水が出る** |

電話を水道に変える変換器が `callbackFlow`（コールバックフロー）。

```kotlin
fun observe(): Flow<T> = callbackFlow {
    val 解除券 = ライブラリ.見張りを付ける { 新しい値 ->
        trySend(新しい値)          // ← 電話で聞いた内容を管に流す
    }
    awaitClose { 解除券.外す() }   // ← 管が閉じられたら見張りを外す
}
```

- **`trySend(値)`** … try（やってみる）＋ send（送る）＝「**送ってみる**」。管に値を1つ流す。
  ここで流したものが、そのまま `collect`（㉝）している側に届く

  **なぜ「try」なのか。** 流し込みは失敗することがあるため。

  | 失敗する状況 | 何が起きているか |
  |---|---|
  | 管がもう閉じている | 画面が閉じられ、受け取り手がいなくなった後 |
  | 管が詰まっている | 受け取り手が遅く、溜め置き場（バッファ）が一杯 |

  `trySend` はどちらでも**例外を投げず黙って諦める**。戻り値で成否が分かるが、
  受け取り手がいないなら流す意味もないので、普通は捨ててよい。

  **`send()` との違い。**

  | | 待つか | 失敗したら |
  |---|---|---|
  | `send(値)` | **待つ**（`suspend`）。詰まっていれば空くまで待機 | 例外を投げる |
  | `trySend(値)` | **待たない** | 黙って諦める |

  コールバックの中は「待てない普通の場所」（§2-⑨）なので、`send()` は**呼べない**。
  `trySend` しか選択肢がない
- **`awaitClose { }`** … 「管が閉じられるまでここで待ち、閉じられたら `{ }` を実行する」。
  `callbackFlow` では**必須**。書かないと実行時にエラーになる

**なぜ `awaitClose` が要るのか。** 見張りは付けっぱなしにすると、画面が消えた後も
通信し続けて電池と通信量を食う（さらに古い画面が参照され続けてメモリリークになる）。
`collect`（㉝）をやめた瞬間に管が閉じ、`awaitClose` の `{ }` で後片付けが走る、という仕組み。

**「解除券」とは何か。** 見張りを付ける命令は、戻り値として「**この見張りを外すための券**」を返す
（Firestore では `ListenerRegistration`）。`val` で受けておかないと外せなくなる（§1-①）。

**`close(値)`** … `trySend` が「管に流す」なら、`close`（クローズ＝閉じる）は「**管を閉じる**」。

| 書き方 | 意味 |
|---|---|
| `close()` | 正常に終わった。`collect` 側は普通に終了する |
| `close(error)` | **異常で終わった**。`collect` 側に例外として届く |

閉じると `awaitClose { }` が走るので、後片付けも自動で行われる。

---

## §3 Compose 特有

### ⑪ `remember` 系 — 作り直しを防ぐ

Compose の画面は、状態が変わるたびに関数が**まるごと何度も再実行**される（再コンポーズ）。
何もしないと毎回新しく作られてしまうので、`remember` 系で「一度作ったら使い回す」と宣言する。

例：`rememberCoroutineScope()`, `rememberScrollState()`

### ⑫ `by` — 委譲（いじょう）

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

`by` は「この箱の出し入れは、右の人に任せる」という意味。
`=` で受け取ると `uiState.value` と書く必要があるが、`by` にすると `uiState` だけで中身を取り出せる。

### ⑬ `LaunchedEffect(キー) { }`

「キーが変わった瞬間に1回だけ、`{ }` の中を実行する」という部品。
再コンポーズのたびに何度も実行されては困る処理（画面遷移など）に使う。

### ⑭ `@Composable` — アノテーション

`@` で始まるものは**アノテーション**＝「この関数はこういう性質です」という付箋。
`@Composable` は「これは画面を描く関数です」という印。

### (66) `LazyColumn` — 見えている分だけ描く縦のリスト

`Column`（縦に並べる）との違いは「**いつ描くか**」。

| | 描くもの | 向いている場面 |
|---|---|---|
| `Column` | **中身を全部**。最初に全部作る | 数個の部品を縦に並べる |
| `LazyColumn` | **画面に見えている分だけ**。スクロールに応じて作る | 件数が分からない一覧 |

`Lazy`（レイジー＝怠け者）＝「必要になるまで作らない」。予定が 1000 件あっても、
画面に映る 10 件ぶんしか作らないので固まらない。

```kotlin
LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(
        items = uiState.items,
        key = { item -> item.id },
    ) { item ->
        Text(item.title)
    }
}
```

| 部分 | 意味 |
|---|---|
| `items(items = リスト) { 要素 -> }` | リストの 1 件ごとに `{ }` を呼んで行を作る（§1-㊿） |
| `key = { it.id }` | **各行を見分ける目印**。下の⚠️を参照 |
| `contentPadding` | **リストの内側**の余白。スクロールすると余白も一緒に動く |
| `verticalArrangement = Arrangement.spacedBy(8.dp)` | 行と行のすき間 |

⚠️ **`key` は付けること。** 無いと Compose は「上から何番目か」でしか行を見分けられない。
先頭に 1 件挿入すると全部が 1 つずつずれ、**全行を作り直す**ことになる。
`key` に `id` を渡しておけば「この行はさっきと同じもの」と分かり、
**動いた行だけ**を描き直せる（速いうえに、入力中の状態も保たれる）。

⚠️ `contentPadding` と `Modifier.padding` は別物。`Modifier.padding` はリスト**の外側**を縮めるので、
スクロールしたとき端で内容が切れて見える。一覧の余白は `contentPadding` を使う。

💡 `dp`（ディーピー）は画面の密度に依らない長さの単位。`16.dp` は「どの端末でも同じくらいの見た目の 16」。

### ㉜ `@Composable` 関数は `@Composable` の中でしか呼べない

`suspend`（§2-⑨）とまったく同じ形のルールがもう1つある。

| 種類 | 呼べる場所 |
|---|---|
| `suspend` 関数 | 待てる場所（`launch { }` の中、他の `suspend` 関数の中） |
| `@Composable` 関数 | 画面を描いている場所（他の `@Composable` 関数の中） |

`stringResource(R.string.xxx)`（文字列リソースを読む部品）は `@Composable` が付いている。
だから**ボタンの `onClick` や `scope.launch { }` の中では呼べない**。あそこは画面を描く場所ではなく、
「あとで実行される作業メモ」（§1-⑥ ラムダ）だから。

対処は**外で受け取っておく**こと。

```kotlin
@Composable
fun LoginScreen() {
    val webClientId = stringResource(R.string.default_web_client_id)  // ここは描画中なのでOK

    Button(onClick = {
        scope.launch {
            useIt(webClientId)   // 中では、外で取った値を使うだけ
        }
    })
}
```

「値を先に取り出して、ラムダには結果だけ持ち込む」——`@Composable` でも `suspend` でも使える定石。

---

## §4 その他

### ⑮ `?:` — エルビス演算子

```kotlin
error.message ?: "ログインに失敗しました"
```

「左が `null`（＝中身なし）だったら、右を使う」という意味。**最後の受け皿**。

### ㊼ `< >` — 「中身は何か」を指定する記号

`Result` は「成功か失敗かを包む封筒」（⑳）。でも封筒だけでは「中に何が入るか」が決まらない。
それを指定するのが `< >`（山カッコ）。

| 書き方 | 意味 |
|---|---|
| `Result<ScheduleItem>` | 成功したら **`ScheduleItem` が 1 件**入っている封筒 |
| `Result<Unit>` | 成功しても**中身は無い**封筒（㊶） |
| `List<ScheduleItem>` | `ScheduleItem` が**順番に並んだ列** |
| `Map<String, Any?>` | 名札が `String`、中身が `Any?` の辞書（㉟。2 つ要るので `,` で区切る） |
| `Flow<List<ScheduleItem>>` | `List<ScheduleItem>` が何度も流れてくる管（§2-㉝） |

**箱と中身を別々に指定する**仕組み。「入れ物の設計図」に「中身の設計図」を差し込んでいる。
だから `Result` だけでは書けず、必ず `< >` が要る。

💡 `Flow<List<ScheduleItem>>` のように二重にもなる。「管の中を流れるのはリストで、
リストの中身は `ScheduleItem`」と**内側から**読む。`>>` はカッコが 2 つ閉じているだけ。

⚠️ ここで宣言した中身の型と、実際に入れるものは対応している。
`Result<ScheduleItem>` の関数で `Result.success(Unit)` と書くとコンパイルエラーになる。

### (54) `return@ラベル` — ラムダだけを抜ける

㊵ の `return` は「**この関数の答えはこれ**」だった。ではラムダ（§1-⑥）の中で
「ここで打ち切りたい」ときはどう書くか。

```kotlin
.addSnapshotListener { snapshot, error ->
    if (error != null) {
        close(error)
        return@addSnapshotListener   // ← このラムダだけを抜ける
    }
    // エラーが無いときだけ、ここから下へ進む
}
```

`return` とだけ書くと「**外側の関数から抜ける**」意味になってしまうため、
`@` のうしろに「**どこを抜けるか**」を書いて区別する。これを**ラベル**という。

- `@` のうしろに書くのは、**そのラムダを渡した命令の名前**（`addSnapshotListener`）
- 読み方は「`addSnapshotListener` **に対して** return」

💡 この「異常なら早めに切り上げて、下は正常系だけにする」書き方を**ガード節**という。
`if` の入れ子が深くならず読みやすい。

⚠️ 打ち切る処理（`close` など）を書いても、`return@` を忘れると**下の行も実行される**。セットで書く。

### (59) `>` の直後の `=` — スペースが意味を変える唯一の場面

Kotlin は普通、スペースの有無で意味が変わらない。`a=b` も `a = b` も同じ。
**例外は「記号どうしが隣り合うとき」**で、2文字が合体して別の記号になる。

```kotlin
val uiState: StateFlow<CalendarUiState>=_uiState.asStateFlow()   // ❌
val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow() // ✅
```

❌ の方は `>` と `=` が **`>=`（以上、§5-(53)）** として読まれ、こうなる。

```
Syntax error: Expecting a '>'.        → '>' が必要です（< > が閉じていない）
Property must be initialized...       → 中身が決まらないので連鎖して出る
```

⚠️ **エラーが3つ出ても原因は1箇所**ということがよくある。上から順に、
**最初のエラーだけ**を見て直すのが早い。

💡 `< >`（§4-㊼）付きの型を書いた直後に `=` を置くときは必ずスペースを空ける。
**Ctrl + Alt + L**（Reformat Code）で自動的に正しくなる。

### ⑯ `?` — null（ヌル）かもしれない印

`String?` は「文字列、または中身なし」。`String` は「必ず文字列が入っている」。
Kotlin は中身なしの可能性を型で区別するので、うっかりアクセスして落ちるのを防げる。

### ㊻ `?.` — 「あれば、その〜」（セーフコール）

```kotlin
getDate("updatedAt")?.toInstant()
```

②の `.`（「〜の」）に `?` が付いた形。読み方は「**あれば、その〜**」。

- 左が中身を持っていれば → 右を実行する
- 左が `null` なら → **右を実行せず、全体が `null` になる**

`null` に対して `.toInstant()` を呼ぶと落ちる。かといって毎回 `if` で確かめるのは面倒。
`?.` は「中身があるときだけ先へ進む」を 1 文字で書ける。

⑮の `?:` と繋げるのが定石。

```kotlin
val updatedAt = getDate("updatedAt")?.toInstant()
    ?: throw IllegalStateException("updatedAt が無い")
```

読み下すと「日時として取り、**あれば** Instant に変換し、**それが無ければ**例外を投げる」。

| 記号 | 読み方 | やること |
|---|---|---|
| `?.` | 「あれば、その〜」 | null なら先へ進まず null を返す |
| `?:` | 「〜が無ければ」 | null だったときの代わりを出す |

### (61) `java.time` — 日時の型の使い分け

日時には**種類がいくつもある**。「情報をどこまで持っているか」が違う。

| 型 | 持っている情報 | 例 | たとえ |
|---|---|---|---|
| `Instant` | **世界共通の時刻の一点** | 2026-08-01T15:00:00Z | 「地球上のこの瞬間」 |
| `LocalDate` | 日付だけ | 2026-08-01 | **カレンダーの升目**。何時かは言っていない |
| `LocalDateTime` | 日付＋時刻 | 2026-08-01 00:00 | 壁掛け時計。**どこの国かは言っていない** |
| `ZonedDateTime` | 日付＋時刻＋**場所** | 2026-08-01 00:00 +09:00 | 「日本時間の8月1日0時」 |
| `ZoneId` | タイムゾーンそのもの | Asia/Tokyo | 「どこの国の時計か」 |
| `YearMonth` | 年と月だけ | 2026-08 | 「今月」 |

**なぜ分かれているのか。** 「8月1日 0:00」は**世界共通の瞬間ではない**。
日本の 0:00 とロンドンの 0:00 は 9 時間ずれた別の瞬間。
だから `LocalDateTime` のままでは「いつ」が決まらず、`Instant` に変換できない。
**タイムゾーンを当てて初めて一点に定まる。**

```kotlin
val zone = ZoneId.systemDefault()                    // 端末のタイムゾーン
val month = YearMonth.now(zone)                      // 2026-08
val from = month.atDay(1).atStartOfDay(zone).toInstant()
```

| 書いたところまで | 手に持っているもの |
|---|---|
| `YearMonth.now(zone)` | 2026-08（年と月） |
| `.atDay(1)` | 2026-08-01（`LocalDate`。at day＝その日の） |
| `.atStartOfDay(zone)` | 2026-08-01 00:00 +09:00（`ZonedDateTime`。start of day＝その日の始まり） |
| `.toInstant()` | 世界共通の時刻（`Instant`） |

`.plusMonths(1)` は「1か月足す」＝翌月にずらす（`plus`＝足す）。

💡 **アプリの中では常に `Instant` で持ち、表示するときだけタイムゾーンを当てる**のが定石
（`ScheduleItem.kt:12` のコメントがこれ）。国をまたいでも予定の時刻がずれない。

💡 minSdk 26 なのでそのまま使える（古い Android 向けの desugaring 設定は不要）。

**時間を足す・引く。**

```kotlin
Instant.now().plus(1, ChronoUnit.HOURS)   // 1 時間後
```

`plus(数, 単位)` の形で、単位は `ChronoUnit`（クロノユニット＝時間の単位）から選ぶ
（`MINUTES` / `HOURS` / `DAYS` など）。

⚠️ **`Instant` には月・年の単位が使えない。** 月は 28〜31 日と長さがバラバラで、
「世界共通の時刻の一点」だけでは何日足せばよいか決まらないため。
月単位でずらしたいときは `YearMonth` や `LocalDate` の `.plusMonths(1)` を使う。

### (67) `DateTimeFormatter` — 日時を「読める文字」にする型紙

`Instant`（(61)）はコンピュータ用の時刻で、そのまま出すと
`2026-08-13T05:00:00Z` のような人に優しくない文字になる。表示用に整えるのが `DateTimeFormatter`。

```kotlin
private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("M/d(E) HH:mm")

private fun Instant.toDisplayText(): String =
    atZone(ZoneId.systemDefault()).format(DISPLAY_FORMATTER)
```

**型紙の記号**（`ofPattern` に渡す文字列）

| 記号 | 意味 | 例 |
|---|---|---|
| `M` / `MM` | 月 | `8` / `08` |
| `d` / `dd` | 日 | `3` / `03` |
| `E` | 曜日 | `木` |
| `HH` | 時（24時間） | `14` |
| `mm` | 分 | `05` |

**同じ文字を重ねると桁が揃う。** `M` は `8`、`MM` は `08`。

⚠️ **記号でない文字を混ぜたいときは `'` で囲む。** `"M月d日"` の「月」「日」は
たまたま記号と衝突しないが、`"HH時"` の `時` は安全でも `"HH'時'"` と書くのが確実。

**2つの手順に分かれている。**

1. `atZone(zone)` … 世界共通の時刻に「**どこの時計で見るか**」を当てて `ZonedDateTime` にする
2. `.format(型紙)` … 型紙どおりの文字列にする

タイムゾーンを当てないと「何日の何時」が決まらない（(61) の通り）。

💡 **型紙は関数の外（ファイル直下）に `private val` で置く。** 作るのに手間がかかるうえ、
中身が変わらないため。関数の中に書くと、一覧の行を描くたびに毎回作り直すことになる。
`DateTimeFormatter` は中身を書き換えないので使い回して安全（§5-㉓ の定数に近い扱い）。

### (63) `UUID.randomUUID().toString()` — 重複しない ID を作る

```kotlin
val id = UUID.randomUUID().toString()   // 例: "3f2b1c8e-...-9a7d"
```

`UUID`（ユーユーアイディー）は「世界中で重複しない」ように作られた識別子。
`randomUUID()` でランダムに1つ作り、`.toString()` で文字列にする。

**なぜアプリ側で作るのか。** `addItem` は `.document(item.id).set(...)` と書いており、
**保存先の場所を `item.id` で決めている**（`ScheduleRepositoryImpl.kt`）。
渡す前に id が決まっていなければならない。

Firestore に自動採番させる方法（`.add()`）もあるが、それだと「保存するまで id が分からない」
ことになり、`ScheduleItem` が `id` を必ず持つ設計（`abstract val id`、§7-㉚）と噛み合わない。

### (65) スマートキャストが効かないとき — ローカルの `val` に受け直す

⑱ の通り、`if (x != null)` で確かめた後は Kotlin が `x` を「中身あり」として扱ってくれる
（スマートキャスト）。**ただし、それが効くのは「途中で変わらないと保証できるもの」だけ。**

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

when {
    uiState.errorMessage != null -> Text(uiState.errorMessage)   // ❌ 効かない
}
```

> `Smart cast to 'String' is impossible, because 'uiState.errorMessage' is a property
> that has open or custom getter`
> （`uiState.errorMessage` は独自の取り出し方を持つプロパティなので、スマートキャストできません）

`uiState` は `by`（§3-⑫）で委譲された箱で、**触るたびに中身を取り直している**。
Kotlin から見ると「確かめた直後に別の値へ変わっているかもしれない」ので、保証できない。

**対処は、いったんローカルの `val` に受けること。**

```kotlin
val message = uiState.errorMessage      // ここで 1 回だけ取り出す

when {
    message != null -> Text(message)    // ✅ message は変わらないので効く
}
```

`val`（§1-①）は一度入れたら変わらない箱なので、Kotlin が安心して確定できる。

💡 同じ対処は `@Composable` の中で何度も同じ値を読むときにも有効
（毎回取り直す無駄が減る）。§3-㉜ の「値を先に取り出しておく」と同じ発想。

### ⑰ `as` — 型を言い換える

```kotlin
val activity = context as Activity
```

「この `context` は、実は `Activity` として扱えるので、そう扱ってくれ」という宣言。
実際に違うものだった場合は実行時にエラーになる。

### ⑱ `is` — 型を確かめる

```kotlin
if (credential is CustomCredential) { ... }
```

「中身が `CustomCredential` という種類なら」という判定。
`is` で確認したブロックの中では、Kotlin が自動的にその型として扱ってくれる（スマートキャスト）。

### ⑲ `::` — 関数そのものを渡す

```kotlin
onEmailChange = viewModel::onEmailChange
```

`viewModel.onEmailChange()` は「実行する」だが、`viewModel::onEmailChange` は「実行せずに関数を渡す」。
`{ text -> viewModel.onEmailChange(text) }` の短縮形。

### ㉞ `TODO()` — 「まだ書いていない」印

```kotlin
override suspend fun getItem(itemId: String): Result<ScheduleItem> = TODO("あとで実装")
```

Kotlin が標準で用意している関数で、**呼ばれたらその場でアプリを止める**。

なぜ便利か：`interface` の約束を全部実装しないとコンパイルが通らないが、
一度に全部書くのはつらい。`TODO()` を置いておけば**形だけ整って先に進める**。

- コメントの `// TODO:` は**ただのメモ**（何も起きない）
- `TODO()` は**実行されると止まる**（カッコがある＝命令、§1-③）

書き忘れたまま動かすと `NotImplementedError`（未実装エラー）で落ちるので、
「あとで書くつもりが忘れていた」を確実に気づける。

### ㉟ `mapOf("キー" to 値)` — 名札付きの箱の集まり

```kotlin
val data = mapOf(
    "title" to "会議",
    "isCompleted" to false,
)
```

`Map`（マップ）＝ **名札と中身の組を集めたもの**。辞書と同じで「`title` を引くと `会議` が出る」。

- `to`（トゥー）は「**組にする**」という意味。`"title" to "会議"` で1組
- `mapOf(...)` はその組をいくつも受け取って Map を作る

Firestore は**この形でしかデータを受け取らない**。だから Kotlin の `ScheduleItem` を
そのまま渡すことはできず、`Map` に詰め替えてから送る。

`List`（リスト＝順番に並んだ列）との違いは、**取り出し方**。
`List` は「3番目」と番号で取り、`Map` は「`title` の」と名札で取る。

### ㊵ `return` — 「この関数の答えはこれ」

関数の書き方には 2 通りあり、`return`（リターン＝返す）が要るかどうかが違う。

| 関数の形 | `return` |
|---|---|
| `fun f(): T { ... }` … `{ }` で書く | **要る**。最後に置いただけでは返らない |
| `fun f(): T = ...` … `=` で書く（§6-㉔-補） | **要らない**。`=` の右がそのまま答え |

```kotlin
private fun ScheduleItem.toMap(): Map<String, Any?> {
    val base = mapOf(...)
    val extra = when (this) { ... }
    return base + extra          // ← これが答え
}
```

⚠️ **`{ }` の関数で `return` を書き忘れても、コンパイルエラーになるとは限らない。**
式を書きっぱなしにすると「作って捨てる」だけの行になり、警告どまりのことがある。

```kotlin
mapOf("id" to id)          // ❌ 作っただけ。誰も受け取らないので消える
val base = mapOf("id" to id)   // ✅ 箱に入れた（§1-①）
return mapOf("id" to id)       // ✅ 答えとして返した
```

作った結果は必ず「**`val` で受ける**」か「**`return` で返す**」かのどちらかにする。

### ㊱ 拡張関数 — 既存の型に、後から命令を生やす

```kotlin
private fun ScheduleItem.toMap(): Map<String, Any?> { ... }
```

`fun` と関数名の間に **`型名.`** が挟まっているのが目印。

| 書き方 | 呼び方 |
|---|---|
| `fun toMap(item: ScheduleItem)` … 普通の関数 | `toMap(item)` |
| `fun ScheduleItem.toMap()` … 拡張関数 | `item.toMap()`（§1-② の「〜の」で読める） |

**なぜわざわざこう書くのか。** `ScheduleItem` は domain 層のもので、Firestore の存在を
知ってはいけない（依存の向きが `domain ← data`）。だから `toMap()` を `ScheduleItem.kt` 本体には
書けない。でも data 層のファイルに拡張関数として書けば、**domain を汚さずに** `item.toMap()` と書ける。

`private` は「このファイルの中だけで使える」という印。

### ㊲ `this` — 拡張関数の中の「本人」

拡張関数の中で `this`（ジス）と書くと、**`.` の左に書かれたもの本人**を指す。
`item.toMap()` と呼べば `this` は `item`。

しかも **`this.` は省略できる**。

```kotlin
"title" to title      // this.title と同じ。item の title が入る
```

### ㊴ `+` で Map どうしを合体できる

```kotlin
mapOf("a" to 1) + mapOf("b" to 2)   // → {"a"=1, "b"=2} という新しい Map
```

数字の足し算と同じ記号だが、Map（§4-㉟）では「**2 つを合わせた新しい Map を作る**」という意味。
元の 2 つは変わらない。同じ名札が両方にあれば**右側が勝つ**。

「共通フィールドの Map」＋「種類ごとの Map」を 1 つにまとめるのに使う。

### (55) `map` / `mapNotNull` — リストを丸ごと作り変える

```kotlin
listOf(1, 2, 3).map { it * 2 }   // → [2, 4, 6]
```

`map`（マップ）は「**1件ずつに同じ加工をして、新しいリストを作る**」命令。
`{ }` の中に「1件をどう加工するか」を書く（§1-㊿。材料1つなので `it` が使える）。

- **元のリストは変わらない。** 新しいリストが作られる
- `mapNotNull` … 同じことをしつつ、**加工結果が `null` のものを入れない**。
  「変換できたものだけ集める」がこれ1つで書ける

```kotlin
snapshot.documents.mapNotNull { doc -> runCatching { doc.toScheduleItem() }.getOrNull() }
```

⚠️ **`Map`（大文字）とは別物。** §1-⑧ のとおり大文字始まりは設計図の名前で、
`Map` は §4-㉟ の「名札付きの箱の集まり」。小文字の `map` は「リストを変換する命令」。
綴りが同じだけで**まったく無関係**。

💡 `emptyList()` は中身が0件のリストを作る命令（§1-③）。`?:`（⑮）の受け皿によく使う。

### (56) `runCatching { }` — `try` / `catch` の短縮形

```kotlin
runCatching { doc.toScheduleItem() }.getOrNull()
```

`{ }` の中を**やってみて（run）、例外が飛んだら受け止める（catching）**。
§6-㉔ の `try { } catch { }` と同じことを、式1つで書ける。返ってくるのは `Result`（⑳）。

| 書き方 | 意味 |
|---|---|
| `runCatching { ... }` | やってみた結果を `Result` の封筒で返す |
| `.getOrNull()` | **成功なら中身、失敗なら `null`** |
| `.getOrDefault(x)` | 成功なら中身、失敗なら `x` |

`mapNotNull`（(55)）と繋ぐと「**変換してみて、ダメな1件だけ捨てる**」が書ける。

⚠️ **`try`/`catch` を書くべき場面が消えるわけではない。** 何が起きたか（`e`）を見て
処理を分けたいときは `try`/`catch` の方が読みやすい。`runCatching` は
「**失敗したら無かったことにする**」だけで済むときに使う。

### ⑳ `Result<T>` / `.onSuccess {} .onFailure {}`

成功か失敗かを包んで返す入れ物。
`.onSuccess { }` は成功したときだけ、`.onFailure { }` は失敗したときだけ `{ }` の中が動く。

### ㊶ `Unit` — 「返すものが無い」を表す型

Kotlin では「**何も返さない**」ということ自体を 1 つの型として扱う。それが `Unit`（ユニット）。

`Result<○○>` は「成功したら ○○ が入っている封筒」（⑳）。`○○` はその関数の性格で決まる。

| 関数 | 型 | 成功したとき封筒に入っているもの |
|---|---|---|
| `getItem` | `Result<ScheduleItem>` | 取ってきた予定 1 件 |
| `addItem` | `Result<Unit>` | **何も無い** |

`addItem` は「保存する」だけの命令。呼び出し側が知りたいのは成功/失敗だけで、
受け取りたいデータは無い。だから `Unit`。「中身は空だが、封筒自体は必要」。

💡 `Unit` は大文字始まり（§1-⑧）なので設計図の名前だが、**中身が世の中に 1 個しかない**
特殊な型で、`Unit` と書くとその 1 個そのものを指す。だから値としてそのまま渡せる。

### ㊷ `Result.success()` / `Result.failure()` — 封筒を**作る**側

⑳ は「受け取った `Result` をどう**使う**か」だった。Repository は「**作って渡す**」側になる。

```kotlin
Result.success(Unit)   // 「うまくいった」封筒。中身は無い（㊶）
Result.failure(e)      // 「失敗した」封筒。中にエラー e を入れる
```

- `Result` が大文字始まり（§1-⑧）＝設計図。そこにぶら下がった `success` / `failure` が封筒を作る命令
- `success(...)` の中身が**成功時に渡したいもの**
- `failure(...)` の中身が**起きたエラー**。`catch (e: Exception)`（§6-㉔）で受けた `e` をそのまま渡す

ここで作った封筒が、そのまま ViewModel の `.onSuccess { }` / `.onFailure { }` に届く。

### (51) `Any?` — 「何でもいい」型。**チェックが効かなくなる**

`Any`（エニー＝どれでも）は**すべての型の大元の親分**。`String` も `Int` も `Date` も `Any` の一種。
`Any?` はそれに「中身なしでもよい」（⑯）を足したもの＝**文字どおり何でも入る**。

```kotlin
private fun ScheduleItem.toMap(): Map<String, Any?>
```

Firestore には文字列も日時も真偽値も混ぜて渡すので、中身の型を1つに決められない。だから `Any?` にする。

⚠️ **代償として、間違った型を入れてもコンパイルエラーにならない。**

```kotlin
"sortAt" to Date.from(dueAt)   // ✅ 意図どおり
"sortAt" to dueAt              // ⚠️ Instant のまま。Any? なので通ってしまう
```

型の見張りが効かないぶん、**Map に詰める行は自分で見比べて確認する**必要がある。
`Map<String, Any?>` を扱うときの心構えとして覚えておく。

---

## §5 判定（条件分岐）

### ㉑ `if (条件) { }` — 条件が成り立つときだけ実行

```kotlin
if (uiState.isLoginSuccess) onLoginSuccess()
```

`()` の中が「はい/いいえ」で答えられる問い。「はい」のときだけ `{ }`（§1-⑥）の中が動く。
やることが1行だけなら `{ }` を省略できる（上の例がそれ）。

### ㉒ `==` と `&&`

| 記号 | 読み方 | 意味 |
|---|---|---|
| `==` | 「〜と同じ」 | 左右の中身が等しいか |
| `!=` | 「〜と違う」 | 等しくないか |
| `&&` | 「かつ」 | 左も右も成り立つか |
| `\|\|` | 「または」 | どちらか一方でも成り立つか |

⚠️ `=`（1個）は「箱に入れる」（§1-①）、`==`（2個）は「同じか確かめる」。**まったく別物。**

### ㊽ `!` — 「〜でない」（はい/いいえをひっくり返す）

名前の**前**に付く `!`（エクスクラメーション）は、はい/いいえを反転する記号。

| 書き方 | 読み方 |
|---|---|
| `snapshot.exists()` | 存在する？ |
| `!snapshot.exists()` | 存在**しない**？ |

`exists()` のように `Boolean`（はい/いいえ）を返すものに付けて、`if` の条件を反転させる。

```kotlin
if (!snapshot.exists()) { ... }   // 存在しないなら、中を実行
```

⚠️ **㉒ の `!=` とは別物。**

| 記号 | 相手の数 | 意味 |
|---|---|---|
| `!` | **1 つ** | そのはい/いいえを反転する |
| `!=` | **2 つ** | 左右が等しくないか調べる |

💡 `if (snapshot.exists() == false)` と書いても同じ意味だが、`!` の方が短いのでこちらが普通。

### ㊳ `when` は「値を返す式」としても使える

㉙ では「分岐＝やることを選ぶ」として出てきたが、`when` は**答えを返す**使い方もできる。

```kotlin
val extra = when (this) {
    is ScheduleItem.Event -> mapOf("type" to "event", ...)   // Event ならこれが答え
    is ScheduleItem.Task  -> mapOf("type" to "task",  ...)   // Task ならこれが答え
}
```

`->` の**右側が、その場合の答え**。左の `val extra`（§1-①）にその答えが入る。

`sealed`（§7-㉘）な型に対しては全部の枝を書けば `else` は要らない。
逆に書き漏らすとコンパイルエラーになる（値を返す `when` は「答えが決まらない場合」を許さないため）。

### (53) `where〇〇("A", B)` の読み方 — 名前が述語になっている

Firestore の絞り込みでは、`==` や `>=` といった**記号を書かない**。
記号の意味は**命令の名前そのものに入っている**。

> **`.where〇〇("A", B)` ＝ 「A が B 〇〇」**

- 1つ目（`"..."` の名札）が**主語**（§1-(52)）
- 2つ目が**比べる相手**
- **命令の名前が述語**

| 命令 | 名前の意味 | 記号でいうと | 読み方 |
|---|---|---|---|
| `whereEqualTo` | equal to | `==` | A が B **と等しい** |
| `whereGreaterThanOrEqualTo` | greater than or equal to | `>=` | A が B **以上** |
| `whereGreaterThan` | greater than | `>` | A が B **より大きい（後）** |
| `whereLessThan` | less than | `<` | A が B **より小さい（前）** |
| `whereLessThanOrEqualTo` | less than or equal to | `<=` | A が B **以下（以前）** |

⚠️ 順番は入れ替わらない。`whereLessThan("sortAt", x)` は必ず「`sortAt` が `x` より小さい」。

💡 `LessThan` の本来の意味は「より小さい」。**入れた値の種類で日本語訳だけ変わる。**
日時なら「より前」、数値なら「より小さい」、文字列なら「辞書順で手前」。

⚠️ カッコの中に記号を書くと壊れる。`whereEqualTo("calendarId" == calendarId)` は
「判定の答え（`false`）を材料1つだけ渡した」ことになり、`No value passed for parameter 'value'`
（パラメータ `value` に値が渡されていません）というエラーになる。

### (64) 主語なしの `when { }` — 条件を上から順に試す

㉙ の `when (item) { ... }` は「**この値**が何か」で分岐する形だった。
`( )` を書かずに `when { }` とすると、**条件を上から順に試す**形になる。

```kotlin
when {
    uiState.isLoading -> ぐるぐるを出す
    uiState.errorMessage != null -> エラー文言を出す
    uiState.items.isEmpty() -> 「予定はありません」を出す
    else -> 一覧を出す
}
```

- `->` の**左が条件**（はい/いいえで答えられる問い。§5-㉑）
- **上から順に試し、最初に当てはまった 1 つだけ**が実行される（§6-㉕ の `catch` と同じ）
- `if / else if / else` を何個も並べるのと同じ意味だが、**縦に揃って読みやすい**

⚠️ **順番が意味を持つ。** 上の例で `items.isEmpty()` を先頭に置くと、
読み込み中（まだ 0 件）のときに「予定はありません」と出てしまう。
**「まだ分からない」→「異常」→「空」→「正常」**の順に並べるのが定石。

⚠️ 値を返す `when`（㊳）として使うときは `else` が必須。条件の羅列では
「全部の場合を書いた」と Kotlin が保証できないため。

### ㉓ 全部大文字の名前 — 定数

```kotlin
GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
```

`ABC_DEF` のように**全部大文字＋アンダースコア**の名前は、**中身が絶対に変わらない値**（定数）という印。
中身は決まった文字列や数値で、ライブラリ側が「この合言葉を使ってね」と用意している。
自分で文字列を手打ちすると打ち間違えるので、この名前を使う。

---

## §6 例外（エラーの受け止め）

### ㉔ `try { } catch (e: 型) { }` — エラーを受け止める

```kotlin
try {
    // 失敗するかもしれない処理
} catch (e: NoCredentialException) {
    // 失敗したときにやること
}
```

- `try`（トライ）＝「やってみる」。`{ }` の中で問題が起きたら、**その場で中断**して `catch` に飛ぶ
- `catch`（キャッチ）＝「受け止める」。`(e: 型)` は「飛んできたエラーを `e` という名前の箱で受け取る」（§1-①⑤に近い書き方）
- **受け止め手がいないエラーはアプリを終了させる。** ログの `FATAL EXCEPTION` がそれ

`try` の中で中断が起きると、**そこから下の行は実行されない**。だから
「失敗するかもしれない処理」と「成功した後にやること」は、まとめて `try` の中に入れる。

### ㉔-補 `= try { } catch { }` — `try` も値を返す

㊳ の `when` と同じで、`try` も**答えを返す式**として使える。

```kotlin
override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
    firebaseAuth.sendPasswordResetEmail(email).await()
    Result.success(Unit)                   // ← 無事に終わったときの答え（try の最後の行）
} catch (e: Exception) {
    Result.failure(e.toAuthException())    // ← コケたときの答え（catch の最後の行）
}
```

関数名のうしろの `=`（§1-①）が「この関数の答えは右側」という意味。
**try が最後まで行けば try の最後の値、途中でコケたら catch の最後の値**が関数の答えになる。

`return` を書かずに済むので、Repository の「やってみて、成功か失敗を `Result` で返す」形と相性がいい。

### ㊺ `throw` — 例外を**投げる**側

```kotlin
throw IllegalStateException("title が入っていません")
```

`throw`（スロー＝投げる）は「**ここで異常が起きた**」と宣言して処理をその場で中断する命令。
㉔ の `try` / `catch` が「受け止める」側なら、これが「飛ばす」側。

投げられた例外は、誰かが `catch` するまで**呼び出し元へ順に遡っていく**。

```
toScheduleItem() の中で throw
  ↓ 呼び出し元へ遡る
getItem() の try の中だったので catch が受け止める
  ↓
Result.failure(e) になって画面まで届く（㊷）
```

つまり **`throw` してもアプリは落ちない**（受け皿がある限り）。
「壊れたデータで無理やりモデルを作る」より「失敗として正直に返す」方が安全、という判断。

`IllegalStateException`（イリーガル・ステート＝ありえない状態）は標準で用意されている例外の型。
「起きるはずのないことが起きた」ときに使う。

**`?:` と組み合わせると型が変わる。**

```kotlin
val title = getString("title") ?: throw IllegalStateException("title が無い")
```

`getString()` の戻り値は `String?` だが、この 1 行を通ると `title` は **`String`（`?` なし）**になる。
`null` の場合は先に進まないと Kotlin が理解するため。
**`?:` + `throw` は「null を潰して確実な値にする」定石**（⑯⑮㊻と合わせて使う）。

### ㉕ `catch` は複数並べられる — 上から順に照合される

```kotlin
try {
    ...
} catch (e: NoCredentialException) {      // 具体的
    ...
} catch (e: GetCredentialException) {     // おおまか（上の親分）
    ...
}
```

飛んできたエラーが**上から順に**「この型に当てはまるか」照合され、
**最初に当てはまった1つだけ**が実行される。

⚠️ **具体的なものを上、おおまかなものを下に書く。**
順番を逆にすると、おおまかな方が先に全部受け止めてしまい、下の具体的な `catch` に永久に来ない。

### ㉖ `${ }` — 文字列の中に値を差し込む

```kotlin
"ログインに失敗しました（${e.type}）"
```

`"..."` の中で `${ }` と書くと、その中身が計算されて文字列に埋め込まれる（文字列テンプレート）。
`"..." + e.type + "..."` と繋ぐより読みやすい。

---

## §7 クラスの種類

### ㉗ `data class` — データを入れる箱

```kotlin
data class User(val uid: String, val email: String?)
```

「値を持ち運ぶだけ」のクラス。`data` を付けると、Kotlin が以下を自動で用意してくれる。

- `toString()` … 中身を読める文字列にする（ログに出すときに便利）
- `equals()` … **中身が同じなら同じ**とみなす比較（付けないと「同じ箱かどうか」の比較になる）
- `copy()` … 一部だけ変えた複製を作る（`it.copy(email = "...")` はこれ）

`_uiState.update { it.copy(...) }` が書けるのは `UiState` が `data class` だから。

⚠️ **プロパティ（引数）が最低1つ必要。** カッコの中が空だと
`Data class must have at least one primary constructor parameter`
（data class には少なくとも1つのコンストラクタ引数が必要です）というエラーになる。

中身が1つも無ければ、比較すべきものも複製すべきものも無く、`data` を付ける意味が消えるため。

### (57) `val x: T = 既定値` — 省略できる引数（デフォルト値）

```kotlin
data class CalendarUiState(
    val items: List<ScheduleItem> = emptyList(),
    val isLoading: Boolean = true,
)
```

`=` の右は「**書かなかったときに使われる値**」。渡さずに済ませられる。

| 呼び方 | 結果 |
|---|---|
| `CalendarUiState()` | 全部デフォルト値 |
| `CalendarUiState(isLoading = false)` | 指定したものだけ差し替え（§1-⑤ の名前付き引数と併用） |

⚠️ **これで `=` は3種類目。** 見分けは「どこに書いてあるか」。

| 場所 | 意味 | 参照 |
|---|---|---|
| 文の途中 `val a = b` | 箱に入れる | §1-① |
| `()` の中・**呼ぶ側** | 荷札（名前付き引数） | §1-⑤ |
| `()` の中・**宣言側** | **既定値** | (57) |

💡 これがあるので `MutableStateFlow(LoginUiState())` のように**空のカッコ**で初期状態を作れる。
`data class` の `copy()`（㉗）が「一部だけ変えた複製」を作れるのも、この仕組みのおかげ。

### ㉘ `sealed class` — 仲間を数え上げられる型

```kotlin
sealed class ScheduleItem {
    data class Event(...) : ScheduleItem()
    data class Task(...) : ScheduleItem()
}
```

`sealed`（シールド）＝「封をした」。**仲間はここに書いた分だけで、外から増やせない**と宣言する。

嬉しいことが2つある。

1. **`when` で分岐すると、その中では型が確定する。** `Event` の枝では `startAt` が必ず存在する（`?` が要らない）
2. **分岐の書き忘れがコンパイルエラーになる。** 仲間の数が決まっているので Kotlin が数え上げられる

`: ScheduleItem()` は「ScheduleItem の仲間です」という宣言（継承）。

補足：`sealed interface` もある。中身（プロパティ）を持たせる必要がなく、
複数の親を持たせたいときはこちら。`SplashUiState` がその例。

### (60) `init { }` — 作られた瞬間に 1 回だけ走る場所

```kotlin
class CalendarViewModel(...) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())

    init {
        observeThisMonth()   // ← この ViewModel が作られた瞬間に実行される
    }
}
```

`init`（イニット＝ initialize＝初期化）は**クラスが作られるときに走る `{ }`**。
「箱を用意するだけでは足りず、**最初に一度やっておきたいこと**がある」ときに使う。

- 上から順に、`val` の初期化と混ざって**書いた順に**実行される
- だから `init { }` は、そこで使う `val` より**下**に書く（上に書くとまだ空）

**なぜカレンダー画面で必要か。** ログイン画面は「ユーザーがボタンを押すまで何もしない」ので
`init` が要らなかった。カレンダーは**開いた瞬間に読み込みを始めたい**ので、
誰かがボタンを押すのを待たず、`init` で自分から動き出す。

💡 `CalendarUiState.isLoading` の既定値を `true` にしたのはこのため（§7-(57)）。

### ㉚ `abstract val` — 「持っていることだけ決める」宣言

```kotlin
sealed class ScheduleItem {
    abstract val id: String   // 中身は書かない
}
```

`abstract`（アブストラクト＝抽象）は「**約束だけして、中身は子に書かせる**」という印。

- `val id: String` だけなら「id という箱があり、中身はこれ」まで決めることになる
- `abstract` を付けると「**id という箱を必ず持つこと**。何を入れるかは子が決める」になる

親（`ScheduleItem`）は「予定もタスクも id を持つ」というルールだけ決めたい。
実際の値は `Event` と `Task` がそれぞれ持つ。だから `abstract`。

嬉しいのは、**親の型のまま共通部分を触れる**こと。

```kotlin
fun show(item: ScheduleItem) {
    println(item.title)   // Event か Task か分からなくても title は必ずある
}
```

`abstract` を付けた箱を子が用意し忘れると**コンパイルエラー**になる（書き忘れ防止）。

### ㉛ `override` — 「親の約束に応える」印

```kotlin
data class Event(
    override val id: String,
    ...
) : ScheduleItem()
```

`override`（オーバーライド＝上書き）は「これは**親が `abstract` で約束した箱**の中身です」という宣言。

- Kotlin では**付け忘れるとコンパイルエラー**になる。「たまたま同じ名前の別の箱を作った」のか
  「親の約束に応えている」のかを、書き手にはっきりさせるため
- 逆に、親が約束していない名前に `override` を付けてもエラーになる

⚠️ `override val id: String` の `val`（§1-①）は省略できない。
省略すると「ただの引数」になり、箱として保持されなくなる。

### ㉙ `when` — 場合分け

```kotlin
when (item) {
    is Event -> ...
    is Task  -> ...
}
```

`if` を何個も並べる代わりの書き方。`is`（§4-⑱）と組み合わせると、
枝の中で自動的にその型として扱える（スマートキャスト）。

`sealed` な型に対して使うと**網羅チェック**が効く。値を返す `when` では、
全部の枝を書かないとコンパイルが通らない。

**型ではなく「値そのもの」でも分岐できる。**

```kotlin
when (getString("type")) {
    "event" -> ...        // 中身が "event" と等しければ
    "task"  -> ...
    else    -> ...        // どれにも当てはまらなければ
}
```

`is` を書かずに値を並べると「`==`（§5-㉒）で比べる」という意味になる。

⚠️ この形では **`else` が必須**。`String` は `sealed`（㉘）ではないので、Kotlin は
「候補がこれで全部」と保証できない。`"banana"` が入っている可能性を潰せないため。
その `else` が、想定外のデータを `throw`（§6-㊺）で止める受け皿になる。

---

## 追記のしかた（Claude 向け）

新しい文法が出たら、適切な §（節）に**次の丸数字**で追記する。
番号は振り直さない（ユーザーが番号で参照するため）。

丸数字は ㊿（50）で打ち止めなので、**51 以降は `(51)` のように半角カッコで書く**。
