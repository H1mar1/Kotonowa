package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.Invite
import com.example.kotonowa.domain.repository.InviteRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore 上の名前。打ち間違い防止のため定数にする（§5-㉓）。 */
private const val COLLECTION_INVITES = "invites"

/**
 * [InviteRepository] の約束を Cloud Firestore で実際に果たすクラス。
 *
 * お手本は同じフォルダの `CalendarRepositoryImpl`。
 * 書く場所は 1 か所だけなので `runBatch` は要らない。
 */
@Singleton
class InviteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : InviteRepository {

    override suspend fun sendInvite(invite: Invite): Result<Unit> = try {
        firestore.collection(COLLECTION_INVITES)
            // TODO(35-C-1) 誰の：招待状の ／ 何を：id を、書類の住所にする
            .document(invite.id)
            // TODO(35-C-2) 誰の：招待状の ／ 何をする：Firestore に渡せる形に詰め替えて、書く
            //   詰め替え係は下の toMap
            .set(invite.toMap())
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** [Invite] を Firestore に渡せる形（Map）に詰め替える。お手本は `CalendarRepositoryImpl` の `Calendar.toMap`。 */
private fun Invite.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "calendarId" to calendarId,
    "calendarName" to calendarName,
    "invitedEmail" to invitedEmail,
    // TODO(35-C-3) enum は文字に直す（§4-(111)）。Calendar.toMap の "type" と同じ形
    "role" to role.name.lowercase(),
    "status" to status.name.lowercase(),
    "invitedBy" to invitedBy,
    // TODO(35-C-4) Instant は直接保存できない。Calendar.toMap の "createdAt" と同じ形
    "createdAt" to Date.from(createdAt),
)
