/**
 * Kotonowa の Cloud Functions（Phase 4：プッシュ通知）。
 *
 * Firestore に何かが書き込まれたのをきっかけに、Firebase のサーバーで動く関数をここに並べる。
 * Android アプリからは呼ばない（アプリは今までどおり Firestore に書くだけ）。
 *
 * Step 43：招待が届いたら通知
 * Step 44：他のメンバーが予定・タスクを追加・編集したら通知
 */
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions";
import { onDocumentWritten } from "firebase-functions/firestore";
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
 */
async function sendToUser(uid: string, title: string, body: string): Promise<void> {
  const userRef = getFirestore().collection("users").doc(uid);
  const userSnap = await userRef.get();

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
