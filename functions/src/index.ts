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

// サーバー側の Firebase を使えるようにする（1 回だけ）。
// ここで使うのは管理者の権限で、セキュリティルールに縛られない（だから宛先の名刺を自由に読める）
initializeApp();
