package com.example.idolproject.data.repository

import com.example.idolproject.Drawer.ComeBack.ComebackItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ComebackScheduleRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val comebacksCol = db.collection("comeback_schedules")
    private val usersCol = db.collection("users")

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun observeComebacks(): Flow<List<ComebackItem>> = callbackFlow {
        val listener = comebacksCol
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snap?.documents
                    ?.mapNotNull { doc ->
                        runCatching {
                            val dateString = doc.getString("date") ?: return@mapNotNull null

                            ComebackItem(
                                id = doc.getString("id") ?: doc.id,
                                groupId = doc.getString("groupId") ?: "",
                                groupName = doc.getString("groupName") ?: "",
                                date = stringToCalendarDay(dateString),
                                title = doc.getString("title") ?: "",
                                memo = doc.getString("memo") ?: ""
                            )
                        }.getOrNull()
                    }
                    ?.sortedWith(
                        compareBy<ComebackItem>(
                            { it.date.year },
                            { it.date.month },
                            { it.date.day },
                            { it.groupName },
                            { it.title }
                        )
                    )
                    .orEmpty()

                trySend(items)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun getMyFavoriteGroupIds(): List<String> {
        val uid = getCurrentUserId() ?: return emptyList()

        val userSnap = usersCol
            .document(uid)
            .get()
            .await()

        val favoriteGroupIds = userSnap.get("favoriteGroupIds") as? List<*>
        if (!favoriteGroupIds.isNullOrEmpty()) {
            return favoriteGroupIds.filterIsInstance<String>()
        }

        return userSnap.getString("favoriteGroupId")
            ?.let { listOf(it) }
            .orEmpty()
    }

    suspend fun isCurrentUserAdmin(): Boolean {
        val uid = getCurrentUserId() ?: return false

        val userSnap = usersCol
            .document(uid)
            .get()
            .await()

        return userSnap.getString("role") == "admin"
    }

    suspend fun addComeback(
        groupId: String,
        groupName: String,
        date: CalendarDay,
        title: String,
        memo: String
    ): ComebackItem {
        val docRef = comebacksCol.document()
        val uid = getCurrentUserId().orEmpty()

        val item = ComebackItem(
            id = docRef.id,
            groupId = groupId,
            groupName = groupName,
            date = date,
            title = title,
            memo = memo
        )

        val data = hashMapOf(
            "id" to item.id,
            "groupId" to item.groupId,
            "groupName" to item.groupName,
            "date" to calendarDayToString(item.date),
            "title" to item.title,
            "memo" to item.memo,
            "createdBy" to uid.ifBlank { "unknown" },
            "createdAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data).await()

        return item
    }

    suspend fun updateComeback(item: ComebackItem): ComebackItem {
        if (item.id.isBlank()) {
            throw IllegalArgumentException("수정할 컴백 일정 ID가 없습니다.")
        }

        val uid = getCurrentUserId().orEmpty()

        val data = hashMapOf(
            "id" to item.id,
            "groupId" to item.groupId,
            "groupName" to item.groupName,
            "date" to calendarDayToString(item.date),
            "title" to item.title,
            "memo" to item.memo,
            "updatedBy" to uid.ifBlank { "unknown" },
            "updatedAt" to FieldValue.serverTimestamp()
        )

        comebacksCol
            .document(item.id)
            .update(data as Map<String, Any>)
            .await()

        return item
    }

    suspend fun deleteComeback(itemId: String) {
        if (itemId.isBlank()) {
            throw IllegalArgumentException("삭제할 컴백 일정 ID가 없습니다.")
        }

        comebacksCol
            .document(itemId)
            .delete()
            .await()
    }

    private fun calendarDayToString(day: CalendarDay): String {
        return String.format("%04d-%02d-%02d", day.year, day.month, day.day)
    }

    private fun stringToCalendarDay(date: String): CalendarDay {
        val parts = date.split("-")
        return CalendarDay.from(
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
    }
}