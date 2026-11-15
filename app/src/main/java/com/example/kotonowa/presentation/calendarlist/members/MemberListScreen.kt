package com.example.kotonowa.presentation.calendarlist.members

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.ui.theme.KotonowaTheme

/**
 * メンバー画面。ViewModel 係。
 *
 * お手本は同じフォルダの上の `CalendarListScreen`（Screen ＋ Content の 2 段構え）。
 * 画面は `navController` も `calendarId` も受け取らない。calendarId は Hilt が
 * SavedStateHandle に詰めて、頭脳が自分で取り出す（Step 18-E と同じ）。
 */
@Composable
fun MemberListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MemberListContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        // TODO(34-D-1) 誰の：頭脳の ／ 何をする：「肩書きを変える」関数をそのまま手渡す
        //   型が (String, MemberRole) -> Unit で頭脳の changeRole と一致するので、関数を指さすだけでよい（§1-(78) の①）
        onChangeRole = viewModel::changeRole,
        // TODO(34-D-1) 誰の：頭脳の ／ 何をする：「外す」関数をそのまま手渡す
        onRemove = viewModel::removeMember,
        // TODO(35-E-1) 誰の：頭脳の ／ 何をする：「メールアドレスが打たれた」関数をそのまま手渡す（35-D-2 で書いた関数。§1-(78) の①）
        onInviteEmailChange = viewModel::onInviteEmailChange,
        // TODO(35-E-1) 誰の：頭脳の ／ 何をする：「肩書きが選ばれた」関数をそのまま手渡す
        onInviteRoleChange = viewModel::onInviteRoleChange,
        // TODO(35-E-1) 誰の：頭脳の ／ 何をする：「招待状を出す」関数をそのまま手渡す（35-D-3）
        onInvite = viewModel::sendInvite,
        modifier = modifier,
    )
}

/**
 * メンバー画面の見た目。ViewModel を受け取らないので `@Preview` で確認できる。
 *
 * @param onChangeRole 「誰の（uid）」「何に（role）」を添えて鳴らす呼び鈴
 * @param onRemove 「誰を（uid）」を添えて鳴らす呼び鈴
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberListContent(
    uiState: MemberListUiState,
    onNavigateBack: () -> Unit,
    onChangeRole: (String, MemberRole) -> Unit,
    onRemove: (String) -> Unit,
    onInviteEmailChange: (String) -> Unit,   // 打たれた文字を添えて鳴らす
    onInviteRoleChange: (MemberRole) -> Unit, // 選ばれた肩書きを添えて鳴らす
    onInvite: () -> Unit,                     // 「招待」が押されたら鳴らす（添えるもの無し）
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("メンバー") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("戻る") }
                },
            )
        },
    ) { innerPadding ->

        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // エラーは一覧の「上に 1 行」出す。一覧と入れ替えない。
                // 肩書きの変更に失敗しただけで、名簿まで見えなくなると困るため（§3-(120) の item）
                val message = uiState.errorMessage
                if (message != null) {
                    item {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                // 招待の欄は名簿の上に 1 つだけ置く（§3-(120) の item）。オーナーにしか出さない。
                // TODO(35-E-2) 出す条件。誰の：状態の ／ 何か：自分がオーナーか（34-D-3 の showControls の前半と同じ）
                if (uiState.isOwner) {
                    item {
                        InviteSection(
                            email = uiState.inviteEmail,
                            role = uiState.inviteRole,
                            isInviting = uiState.isInviting,
                            message = uiState.inviteMessage,
                            onEmailChange = onInviteEmailChange,
                            onRoleChange = onInviteRoleChange,
                            onInvite = onInvite,
                        )
                    }
                }

                // TODO(34-D-2) 行の見分け方。誰の：この行の人の ／ 何で見分ける：uid（名簿の住所でもある）
                //   お手本は CalendarListScreen の key = { calendar -> calendar.id }（§3-(66)）
                items(uiState.members, key = { member -> member.uid }) { member ->
                    MemberRow(
                        member = member,
                        // TODO(34-D-3) ボタンを出すか。「自分がオーナー」かつ「この行の人はオーナーではない」
                        //   オーナー自身の行に出すと、自分を閲覧者に落としたり外したりできてしまう（ルールでも禁じる。34-E）
                        //   「〜ではない」は !=（§5-㉒ の == の逆）
                        showControls = uiState.isOwner && member.role != MemberRole.OWNER,
                        // TODO(34-D-4) 誰の：この画面の呼び鈴を ／ 何をする：「この行の人の uid」と「行から来た肩書き」を添えて鳴らす
                        onChangeRole = { role -> onChangeRole(member.uid, role) },
                        // TODO(34-D-4) 誰の：この画面の呼び鈴を ／ 何をする：「この行の人の uid」を添えて鳴らす
                        onRemove = { onRemove(member.uid) },
                    )
                }
            }
        }
    }
}

/**
 * メンバー 1 行。名前と肩書き、オーナーが見ているときだけ右側にボタン 2 つ。
 *
 * 肩書きの変更は DropdownMenu にせず、「もう一方にする」ボタン 1 つにしている。
 * オーナー以外の肩書きは編集者と閲覧者の 2 つしか無いので、切り替えで足りる。
 * メニューにすると「どの行のメニューが開いているか」を UiState に持つ必要が出てくる（Step 26 の isReminderMenuOpen と同じ）。
 */
@Composable
private fun MemberRow(
    member: MemberItem,
    showControls: Boolean,
    onChangeRole: (MemberRole) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左：名前と肩書き。weight(1f) で残りの幅を全部もらい、ボタンを右端に押しやる（§3-(96)）
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium)
                Text(member.role.label(), style = MaterialTheme.typography.labelMedium)
            }

            if (showControls) {
                // TODO(34-D-5) 切り替え先の肩書き。今が編集者なら閲覧者、そうでなければ編集者（値を返す if。§5-(68)）
                //   誰の：この行の人の肩書きが ／ 何か：編集者（MemberRole.EDITOR）と同じか
                val nextRole =
                    if (member.role == MemberRole.EDITOR) MemberRole.VIEWER else MemberRole.EDITOR

                // TODO(34-D-5) 誰の：この行の呼び鈴を ／ 何をする：切り替え先の肩書きを添えて鳴らす
                TextButton(onClick = { onChangeRole(nextRole) }) {
                    Text("${nextRole.label()}にする") // §6-㉖
                }

                // TODO(34-D-5) 「外す」の呼び鈴。添えるものが無い () -> Unit 同士なので、そのまま渡す（§1-(78) の①）
                TextButton(onClick = onRemove) {
                    Text("外す", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * 招待の欄。メールアドレス・肩書き（編集者／閲覧者）・「招待」ボタン・結果の一言を縦に並べる。
 *
 * 状態は持たない（§3-(76)）。今の値は引数でもらい、変化は呼び鈴で返す。
 * UiState を丸ごと受け取らず、使う 4 つだけ受け取る。こうすると、この部品は「招待」以外の箱を知らずに済む。
 */
@Composable
private fun InviteSection(
    email: String,
    role: MemberRole,
    isInviting: Boolean,
    message: String?,
    onEmailChange: (String) -> Unit,
    onRoleChange: (MemberRole) -> Unit,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("メンバーを招待", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                // TODO(35-E-3) 今の値と、変化の返し先。ScheduleEditScreen の「タイトル」の欄と同じ形（value / onValueChange）
                value = email,
                onValueChange = onEmailChange,
                label = { Text("メールアドレス") },
                singleLine = true,
                // 送っている途中は打てないようにする（送った宛先と欄の文字がズレないように）
                enabled = !isInviting,
                // キーボードを「@ が打ちやすい」メールアドレス用にする（§3-(129)）
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // 肩書きは 2 つから選ぶ。お手本は ScheduleEditScreen の「予定／タスク」の切り替え（Step 17-E-2）
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    // TODO(35-E-4) 選ばれているか。誰の：今の肩書きが ／ 何か：編集者と同じか
                    selected = role== MemberRole.EDITOR,
                    // TODO(35-E-4) 誰の：この部品の呼び鈴を ／ 何をする：編集者を添えて鳴らす（{ } で包む。§1-(78) の②）
                    onClick = { onRoleChange(MemberRole.EDITOR) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text("編集者")
                }
                SegmentedButton(
                    // TODO(35-E-4) 上の 2 行の「閲覧者」版
                    selected = role== MemberRole.VIEWER,
                    onClick = { onRoleChange(MemberRole.VIEWER) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text("閲覧者")
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                // TODO(35-E-5) 誰の：この部品の呼び鈴を ／ 何をする：そのまま渡す（添えるもの無しの () -> Unit 同士。§1-(78) の①）
                onClick = onInvite,
                // TODO(35-E-5) 押せる条件。「送っている途中ではない」とき（上の OutlinedTextField の enabled と同じ式）
                enabled = !isInviting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // 送っている途中は文字を変えて、押したことが伝わるようにする（値を返す if。§5-(68)）
                Text(if (isInviting) "送信中…" else "招待")
            }

            // 結果の一言。無いときは何も出さない。
            // TODO(35-E-6) 一度ローカルの val に受けてから null でないか確かめる（Step 18-D の description / errorMessage と同じ。スマートキャストのため）
            //   ※ 引数の message はもう val なので、受け直さず if (message != null) でも通る。どちらで書いてもよい
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * 肩書きを画面に出す日本語にする（拡張関数。§4-㊱）。
 * Firestore に保存する小文字（`owner`）とは別。こちらは表示専用。
 */
private fun MemberRole.label(): String = when (this) {
    MemberRole.OWNER -> "オーナー"
    MemberRole.EDITOR -> "編集者"
    MemberRole.VIEWER -> "閲覧者"
}

// ---- Preview 用のダミー（この画面の中だけで使う） ----

private val PREVIEW_MEMBERS = listOf(
    MemberItem(uid = "me", name = "ひまり", role = MemberRole.OWNER),
    MemberItem(uid = "a", name = "田中", role = MemberRole.EDITOR),
    MemberItem(uid = "b", name = "suzuki@example.com", role = MemberRole.VIEWER),
)

@Preview(showBackground = true, name = "オーナーが見た")
@Composable
private fun MemberListOwnerPreview() {
    KotonowaTheme {
        MemberListContent(
            uiState = MemberListUiState(
                members = PREVIEW_MEMBERS,
                isOwner = true,
                isLoading = false
            ),
            onNavigateBack = {},
            onChangeRole = { _, _ -> },
            onRemove = {},
            onInviteEmailChange = {},
            onInviteRoleChange = {},
            onInvite = {},
        )
    }
}

@Preview(showBackground = true, name = "編集者が見た")
@Composable
private fun MemberListEditorPreview() {
    KotonowaTheme {
        MemberListContent(
            uiState = MemberListUiState(
                members = PREVIEW_MEMBERS,
                isOwner = false,
                isLoading = false
            ),
            onNavigateBack = {},
            onChangeRole = { _, _ -> },
            onRemove = {},
            onInviteEmailChange = {},
            onInviteRoleChange = {},
            onInvite = {},
        )
    }
}

@Preview(showBackground = true, name = "招待を送った直後")
@Composable
private fun MemberListInvitedPreview() {
    KotonowaTheme {
        MemberListContent(
            uiState = MemberListUiState(
                members = PREVIEW_MEMBERS,
                isOwner = true,
                isLoading = false,
                inviteRole = MemberRole.EDITOR,
                inviteMessage = "招待を送りました",
            ),
            onNavigateBack = {},
            onChangeRole = { _, _ -> },
            onRemove = {},
            onInviteEmailChange = {},
            onInviteRoleChange = {},
            onInvite = {},
        )
    }
}
