/**
 * Kotonowa の Cloud Functions（Phase 4：プッシュ通知）。
 *
 * Firestore に何かが書き込まれたのをきっかけに、Firebase のサーバーで動く関数をここに並べる。
 * Android アプリからは呼ばない（アプリは今までどおり Firestore に書くだけ）。
 *
 * Step 43：招待が届いたら通知
 * Step 44：他のメンバーが予定・タスクを追加・編集したら通知
 * Step 46：招待を送る（アプリから呼び出す。users をアプリから読めなくするため、相手探しをサーバーへ移した）
 */
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions";
import { onDocumentWritten } from "firebase-functions/firestore";
import { HttpsError, onCall } from "firebase-functions/https";
import { setGlobalOptions } from "firebase-functions/options";

// サーバー側の Firebase を使えるようにする（1 回だけ）。
// ここで使うのは管理者の権限で、セキュリティルールに縛られない（だから宛先の名刺を自由に読める）
initializeApp();

// すべての関数を東京（asia-northeast1）で動かす。Firestore と同じ場所にすると、きっかけが届くのが速い。
// maxInstances は「同時に何台まで立ち上げてよいか」。暴走して料金が膨らむのを防ぐ上限
setGlobalOptions({ region: "asia-northeast1", maxInstances: 10 });

/**
 * Android の KotonowaMessagingService（Step 42）と同じ名札。
 * どちらかだけ変えると、通知は届くのに見出しが空で表示されない（Android 側の `?: return`）。
 */
const KEY_PUSH_TITLE = "title";
const KEY_PUSH_BODY = "body";

/** 肩書きの日本語。Firestore には小文字の英語で入っている（Step 30） */
const ROLE_LABELS: Record<string, string> = {
  editor: "編集者",
  viewer: "閲覧者",
};

/**
 * 1 人の名刺（users/{uid}）に登録されている全端末へ、通知を送る。
 * Step 44 の「他のメンバーへの通知」からも使う。
 *
 * @param calendarId どのカレンダーについての通知か（Step 45）。渡したときは、その人が通知を止めていれば送らない。
 *                   招待の通知は渡さない（止められない）
 */
async function sendToUser(
  uid: string,
  title: string,
  body: string,
  calendarId?: string,
): Promise<void> {
  const userRef = getFirestore().collection("users").doc(uid);
  const userSnap = await userRef.get();

  // Step 45：このカレンダーの通知を止めている人には送らない。
  // 名刺の mutedCalendarIds（Android の NotificationSettingRepositoryImpl と同じ名札）に入っていれば飛ばす
  if (calendarId !== undefined) {
    const muted: string[] = userSnap.get("mutedCalendarIds") ?? [];
    if (muted.includes(calendarId)) {
      logger.info("このカレンダーの通知を止めているので送らない", { uid, calendarId });
      return;
    }
  }

  // 名刺の fcmTokens を読む。無い・配列でない場合は空として扱う
  const tokens: string[] = userSnap.get("fcmTokens") ?? [];
  if (tokens.length === 0) {
    logger.info("宛先の端末が無いので送らない", { uid });
    return;
  }

  // 全端末へまとめて送る。data だけで送る（アプリが裏にいても KotonowaMessagingService を通るように。grammar §8-(136)）
  const response = await getMessaging().sendEachForMulticast({
    tokens,
    data: {
      [KEY_PUSH_TITLE]: title,
      [KEY_PUSH_BODY]: body,
    },
    // high にすると、端末が省電力で眠っていてもすぐ届く
    android: { priority: "high" },
  });

  // 「もう存在しない住所」（アプリを消した端末など）を名刺から掃除する
  const deadTokens = response.responses
    .map((result, index) => ({ result, token: tokens[index] }))
    .filter(({ result }) => {
      const code = result.error?.code;
      return (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      );
    })
    .map(({ token }) => token);

  if (deadTokens.length > 0) {
    await userRef.update({ fcmTokens: FieldValue.arrayRemove(...deadTokens) });
  }

  logger.info("通知を送った", {
    uid,
    success: response.successCount,
    failure: response.failureCount,
    removed: deadTokens.length,
  });
}

/**
 * Step 43：招待状が「返事待ち（pending）」になったら、招待された人へ通知する。
 *
 * onDocumentWritten は、作成・上書き・削除のどれでも呼ばれる。
 * 「作成」だけ（onDocumentCreated）にしないのは、断られた招待を同じ住所へ送り直したとき（Step 36 の上書き）にも知らせたいため。
 */
export const notifyInvite = onDocumentWritten("invites/{inviteId}", async (event) => {
  logger.info("招待状の書き込みを受け取った", { inviteId: event.params.inviteId });

  const before = event.data?.before;
  const after = event.data?.after;

  // 消された場合は何もしない
  if (!after?.exists) return;

  // 今が返事待ちでなければ何もしない（承認・拒否の書き込みでも呼ばれるため）
  if (after.get("status") !== "pending") return;

  // 前から返事待ちだった（＝返事待ちのまま書き直されただけ）なら、二重に知らせない
  if (before?.exists && before.get("status") === "pending") return;

  const invitedEmail: string = after.get("invitedEmail");
  const calendarName: string = after.get("calendarName");
  const roleLabel = ROLE_LABELS[after.get("role")] ?? "メンバー";

  // メールアドレスから名刺を探す（保存時に小文字にしてある。Step 28）
  const found = await getFirestore()
    .collection("users")
    .where("email", "==", invitedEmail)
    .limit(1)
    .get();

  if (found.empty) {
    logger.warn("招待された人の名刺が見つからない", { invitedEmail });
    return;
  }

  await sendToUser(
    found.docs[0].id,
    "カレンダーへの招待",
    `「${calendarName}」に${roleLabel}として招待されました`,
  );
});

/**
 * Step 44：共有カレンダーで予定・タスクが追加・編集されたら、書いた本人以外のメンバーへ通知する。
 *
 * 削除では送らない（設計。要件定義書 §3.5）。マイカレンダー（calendars に書類が無い）では送らない。
 */
export const notifyScheduleChange = onDocumentWritten("events/{eventId}", async (event) => {
  // 呼ばれたことを記録に残す（関数が動いていないのか、動いたが送らなかったのかを見分けるため。Step 45-E で追加）
  logger.info("予定の書き込みを受け取った", { eventId: event.params.eventId });

  const before = event.data?.before;
  const after = event.data?.after;

  // 消された場合は送らない
  if (!after?.exists) return;

  const calendarId: string = after.get("calendarId");
  const writerUid: string | undefined = after.get("updatedBy");
  const title: string = after.get("title");
  const isTask = after.get("type") === "task";

  // 書いた人が分からない（Step 44 より前のアプリで書かれた）なら、誰に送らないべきか決められないので送らない
  if (!writerUid) return;

  // マイカレンダーは calendars に書類が無い（Step 33）。書類が無ければ共有ではないので送らない
  const calendarSnap = await getFirestore().collection("calendars").doc(calendarId).get();
  if (!calendarSnap.exists) return;

  const calendarName: string = calendarSnap.get("name");
  const memberUids: string[] = calendarSnap.get("memberUids") ?? [];

  // 書いた人の表示名。名刺の displayName → 無ければメールアドレス → それも無ければ「メンバー」
  const writerSnap = await getFirestore().collection("users").doc(writerUid).get();
  const writerName: string = writerSnap.get("displayName") || writerSnap.get("email") || "メンバー";

  // 何をしたかの一言を決める
  const kind = isTask ? "タスク" : "予定";
  let action: string;
  if (!before?.exists) {
    action = "追加しました";
  } else if (isTask && before.get("isCompleted") !== after.get("isCompleted")) {
    // タスクの完了チェック（Step 21）だけ、言い方を変える
    action = after.get("isCompleted") ? "完了にしました" : "未完了に戻しました";
  } else {
    action = "編集しました";
  }

  // 書いた本人を除いた全員へ、同時に送る
  const receivers = memberUids.filter((uid) => uid !== writerUid);
  await Promise.all(
    receivers.map((uid) =>
      sendToUser(uid, calendarName, `${writerName}さんが${kind}「${title}」を${action}`, calendarId),
    ),
  );
});

/** sendInvite がアプリへ返す結果。Android の InviteSendResult と同じ 3 つ */
type SendInviteResult = "sent" | "not_registered" | "already_member";

/**
 * Step 46：招待を送る（アプリから呼び出す関数）。
 *
 * これまでアプリが自分でやっていた「相手を users から探す → すでにメンバーか確かめる → 招待状を書く」を、
 * まとめてサーバーで行う。こうすると users をアプリから読めなくしても招待できる（他人のメールや uid がアプリに渡らない）。
 *
 * onCall は「アプリから名前を指定して呼べる関数」。呼んだ人のログイン情報（request.auth）が自動で付いてくる。
 * 失敗は HttpsError で返す（アプリ側では例外として届く）。
 */
export const sendInvite = onCall(async (request): Promise<{ result: SendInviteResult }> => {
  // 呼んだ人。ログインしていなければ断る
  const myUid = request.auth?.uid;
  if (!myUid) {
    throw new HttpsError("unauthenticated", "ログインしてください");
  }

  // アプリから届いた 3 つ。形が違えば断る（アプリを通さずに呼ばれることもあるので、必ず確かめる）
  const calendarId = request.data?.calendarId;
  const role = request.data?.role;
  const email = String(request.data?.email ?? "").trim().toLowerCase();
  if (typeof calendarId !== "string" || calendarId === "" || email === "") {
    throw new HttpsError("invalid-argument", "カレンダーとメールアドレスが必要です");
  }
  if (role !== "editor" && role !== "viewer") {
    throw new HttpsError("invalid-argument", "肩書きは編集者か閲覧者です");
  }

  const db = getFirestore();

  // 招待できるのはそのカレンダーのオーナーだけ（35-F のルールと同じ条件）
  const calendarSnap = await db.collection("calendars").doc(calendarId).get();
  if (!calendarSnap.exists || calendarSnap.get("ownerUid") !== myUid) {
    throw new HttpsError("permission-denied", "招待できるのはオーナーだけです");
  }

  // 相手を名刺から探す（メールは保存時に小文字。Step 28）
  const found = await db.collection("users").where("email", "==", email).limit(1).get();
  if (found.empty) {
    return { result: "not_registered" };
  }

  // すでにメンバーなら招待状を作らない
  const memberUids: string[] = calendarSnap.get("memberUids") ?? [];
  if (memberUids.includes(found.docs[0].id)) {
    return { result: "already_member" };
  }

  // 招待状を書く。住所は「カレンダーid_メール」（Step 36-A。送り直しは同じ住所への上書き）
  // 書くとすぐ notifyInvite（Step 43）が動いて、相手に通知が届く
  const inviteId = `${calendarId}_${email}`;
  await db.collection("invites").doc(inviteId).set({
    id: inviteId,
    calendarId,
    calendarName: calendarSnap.get("name"),
    invitedEmail: email,
    role,
    status: "pending",
    invitedBy: myUid,
    createdAt: FieldValue.serverTimestamp(),
  });

  logger.info("招待状を書いた", { inviteId });
  return { result: "sent" };
});
