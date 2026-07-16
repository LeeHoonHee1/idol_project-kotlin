package com.example.idolproject.data.repository

import com.example.idolproject.Drawer.ComeBack.ComebackItem
import com.example.idolproject.Drawer.Group.GroupActivityType
import com.example.idolproject.Drawer.Group.GroupScheduleItem
import com.example.idolproject.UI.Home.HomeScheduleItem
import com.example.idolproject.UI.Home.HomeScheduleSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

class HomeRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCol = db.collection("users")
    private val comebackSchedulesCol = db.collection("comeback_schedules")
    private val groupSchedulesCol = db.collection("group_schedules")

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getHomeFavoriteGroup(): HomeFavoriteGroup? {
        val uid = getCurrentUserId() ?: return null

        val userSnap = usersCol
            .document(uid)
            .get()
            .await()

        val favoriteGroupIdFromList = (userSnap.get("favoriteGroupIds") as? List<*>)
            ?.filterIsInstance<String>()
            ?.firstOrNull()

        val favoriteGroupId = favoriteGroupIdFromList
            ?: userSnap.getString("favoriteGroupId")

        if (favoriteGroupId.isNullOrBlank()) {
            return null
        }

        val favoriteGroupName = userSnap.getString("favoriteGroupName")
            ?: getDisplayGroupName(favoriteGroupId)

        return HomeFavoriteGroup(
            groupId = favoriteGroupId,
            groupName = favoriteGroupName
        )
    }

    suspend fun getFavoriteGroupSchedules(
        favoriteGroupId: String,
        today: CalendarDay = CalendarDay.today(),
        days: Long = 3
    ): List<HomeScheduleItem> {
        val comebackItems = getComebackSchedules(
            favoriteGroupId = favoriteGroupId,
            today = today,
            days = days
        )

        val groupScheduleItems = getGroupSchedules(
            favoriteGroupId = favoriteGroupId,
            today = today,
            days = days
        )

        return (comebackItems + groupScheduleItems)
            .sortedWith(
                compareBy<HomeScheduleItem>(
                    { it.date.year },
                    { it.date.month },
                    { it.date.day },
                    { it.groupName },
                    { it.title }
                )
            )
    }

    private suspend fun getComebackSchedules(
        favoriteGroupId: String,
        today: CalendarDay,
        days: Long
    ): List<HomeScheduleItem> {
        val snap = comebackSchedulesCol
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            runCatching {
                val dateString = doc.getString("date") ?: return@mapNotNull null

                val item = ComebackItem(
                    id = doc.getString("id") ?: doc.id,
                    groupId = doc.getString("groupId") ?: "",
                    groupName = doc.getString("groupName") ?: "",
                    date = stringToCalendarDay(dateString),
                    title = doc.getString("title") ?: "",
                    memo = doc.getString("memo") ?: ""
                )

                if (item.groupId == favoriteGroupId && isWithinNextDays(item.date, today, days)) {
                    mapComebackToHomeSchedule(item)
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    private suspend fun getGroupSchedules(
        favoriteGroupId: String,
        today: CalendarDay,
        days: Long
    ): List<HomeScheduleItem> {
        val snap = groupSchedulesCol
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            runCatching {
                val dateString = doc.getString("date") ?: return@mapNotNull null
                val typeString = doc.getString("type") ?: GroupActivityType.OTHER.name

                val item = GroupScheduleItem(
                    id = doc.getString("id") ?: doc.id,
                    groupId = doc.getString("groupId") ?: "",
                    groupName = doc.getString("groupName") ?: "",
                    date = stringToCalendarDay(dateString),
                    type = runCatching {
                        GroupActivityType.valueOf(typeString)
                    }.getOrDefault(GroupActivityType.OTHER),
                    title = doc.getString("title") ?: "",
                    memo = doc.getString("memo") ?: ""
                )

                if (item.groupId == favoriteGroupId && isWithinNextDays(item.date, today, days)) {
                    mapGroupScheduleToHomeSchedule(item)
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    private fun mapComebackToHomeSchedule(item: ComebackItem): HomeScheduleItem {
        return HomeScheduleItem(
            source = HomeScheduleSource.COMEBACK,
            groupId = item.groupId,
            groupName = item.groupName,
            date = item.date,
            label = "컴백",
            title = item.title,
            memo = item.memo
        )
    }

    private fun mapGroupScheduleToHomeSchedule(item: GroupScheduleItem): HomeScheduleItem {
        return HomeScheduleItem(
            source = HomeScheduleSource.GROUP_SCHEDULE,
            groupId = item.groupId,
            groupName = item.groupName,
            date = item.date,
            label = item.type.displayName,
            title = item.title,
            memo = item.memo
        )
    }

    private fun stringToCalendarDay(date: String): CalendarDay {
        val parts = date.split("-")
        return CalendarDay.from(
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
    }

    private fun CalendarDay.toLocalDate(): LocalDate {
        return LocalDate.of(year, month, day)
    }

    private fun isWithinNextDays(date: CalendarDay, start: CalendarDay, days: Long): Boolean {
        val target = date.toLocalDate()
        val startDate = start.toLocalDate()
        val endDate = startDate.plusDays(days)

        return !target.isBefore(startDate) && !target.isAfter(endDate)
    }

    private fun getDisplayGroupName(groupId: String): String {
        return when (groupId) {
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "lesserafim" -> "LE SSERAFIM"
            "aespa" -> "aespa"
            "babymonster" -> "BABYMONSTER"
            else -> groupId
        }
    }
}

data class HomeFavoriteGroup(
    val groupId: String,
    val groupName: String
)