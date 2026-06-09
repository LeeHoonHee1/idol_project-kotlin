package com.example.idolproject.UI.Home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.time.LocalDate
import com.example.idolproject.Drawer.ComeBack.ComebackItem
import com.example.idolproject.Drawer.Group.GroupActivityType
import com.example.idolproject.Drawer.Group.GroupScheduleItem
import com.google.android.material.card.MaterialCardView
import com.example.idolproject.Drawer.Community.GroupChatActivity

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyHome: TextView
    private lateinit var homeFeedAdapter: HomeFeedAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var favoriteGroupId: String? = null
    private var favoriteGroupScheduleFeedList: List<HomeFeedItem> = emptyList()
    private lateinit var cardFanTalkShortcut: MaterialCardView

    private lateinit var layoutEmptyHome: View
    private lateinit var tvEmptyHomeTitle: TextView

    private var favoriteGroupName: String? = null

    private lateinit var tvFanTalkTitle: TextView
    private lateinit var tvFanTalkSub: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerHomeFeed)
        tvEmptyHome = view.findViewById(R.id.tvEmptyHome)
        cardFanTalkShortcut = view.findViewById(R.id.cardFanTalkShortcut)
        layoutEmptyHome = view.findViewById(R.id.layoutEmptyHome)
        tvEmptyHomeTitle = view.findViewById(R.id.tvEmptyHomeTitle)

        tvFanTalkTitle = view.findViewById(R.id.tvFanTalkTitle)
        tvFanTalkSub = view.findViewById(R.id.tvFanTalkSub)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeFeedAdapter = HomeFeedAdapter(emptyList<HomeListItem>())
        recyclerView.adapter = homeFeedAdapter

        cardFanTalkShortcut.setOnClickListener {
            openFavoriteGroupChat()
        }

        loadFavoriteGroupHomeSchedule()
    }

    private fun loadFavoriteGroupHomeSchedule() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showEmptyState("로그인이 필요합니다.")
            return
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                favoriteGroupId = document.getString("favoriteGroupId")
                favoriteGroupName = document.getString("favoriteGroupName")

                if (favoriteGroupId.isNullOrEmpty()) {
                    favoriteGroupScheduleFeedList = emptyList()
                    homeFeedAdapter.updateList(emptyList())

                    tvFanTalkTitle.text = "최애 팬톡 바로가기"
                    tvFanTalkSub.text = "내 페이지에서 최애 그룹을 설정하면 팬톡방에 입장할 수 있어요."

                    showEmptyState("최애 그룹을 먼저 설정해 주세요.")
                    return@addOnSuccessListener
                }

                val displayGroupName = favoriteGroupName ?: favoriteGroupId ?: "최애 그룹"
                tvFanTalkTitle.text = "$displayGroupName 팬톡방"
                tvFanTalkSub.text = "팬들과 실시간으로 대화해보세요."

                val today = CalendarDay.today()
                loadHomeSchedulesFromFirestore(today)
            }
            .addOnFailureListener {
                showEmptyState("최애 그룹 정보를 불러오지 못했습니다.")
            }
    }

    private fun loadHomeSchedulesFromFirestore(today: CalendarDay) {
        val favoriteId = favoriteGroupId

        if (favoriteId.isNullOrBlank()) {
            favoriteGroupScheduleFeedList = emptyList()
            homeFeedAdapter.updateList(emptyList())
            showEmptyState("최애 그룹을 먼저 설정해 주세요.")
            return
        }

        val comebackList = mutableListOf<HomeScheduleItem>()
        val groupScheduleList = mutableListOf<HomeScheduleItem>()

        db.collection("comeback_schedules")
            .get()
            .addOnSuccessListener { comebackResult ->
                comebackList.addAll(
                    comebackResult.documents.mapNotNull { doc ->
                        try {
                            val dateString = doc.getString("date") ?: return@mapNotNull null
                            val item = ComebackItem(
                                id = doc.getString("id") ?: doc.id,
                                groupId = doc.getString("groupId") ?: "",
                                groupName = doc.getString("groupName") ?: "",
                                date = stringToCalendarDay(dateString),
                                title = doc.getString("title") ?: "",
                                memo = doc.getString("memo") ?: ""
                            )

                            if (item.groupId == favoriteId && isWithinNextDays(item.date, today, 3)) {
                                mapComebackToHomeSchedule(item)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                )

                db.collection("group_schedules")
                    .get()
                    .addOnSuccessListener { groupResult ->
                        groupScheduleList.addAll(
                            groupResult.documents.mapNotNull { doc ->
                                try {
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

                                    if (item.groupId == favoriteId && isWithinNextDays(item.date, today, 3)) {
                                        mapGroupScheduleToHomeSchedule(item)
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        )

                        val mergedSchedules = (comebackList + groupScheduleList)
                            .sortedBy { it.date.toLocalDate() }

                        favoriteGroupScheduleFeedList =
                            mergedSchedules.map { mapHomeScheduleToFeed(it) }

                        val displayList = buildSectionedList(favoriteGroupScheduleFeedList)
                        homeFeedAdapter.updateList(displayList)

                        if (favoriteGroupScheduleFeedList.isEmpty()) {
                            showEmptyState("오늘은 일정이 없습니다.")
                        } else {
                            hideEmptyState()
                        }
                    }
                    .addOnFailureListener {
                        showEmptyState("그룹 일정을 불러오지 못했습니다.")
                    }
            }
            .addOnFailureListener {
                showEmptyState("컴백 일정을 불러오지 못했습니다.")
            }
    }

    private fun buildSectionedList(feedItems: List<HomeFeedItem>): List<HomeListItem> {
        val todayPrefix = formatMonthDay(CalendarDay.today())

        val todayItems = feedItems.filter { it.time.startsWith(todayPrefix) }
        val upcomingItems = feedItems.filterNot { it.time.startsWith(todayPrefix) }

        val result = mutableListOf<HomeListItem>()

        if (todayItems.isNotEmpty()) {
            result.add(HomeListItem.Header("오늘 일정", true))
            result.addAll(todayItems.map { HomeListItem.Feed(it) })
        }

        if (upcomingItems.isNotEmpty()) {
            result.add(HomeListItem.Header("다가오는 일정", false))
            result.addAll(upcomingItems.map { HomeListItem.Feed(it) })
        }

        return result
    }

    private fun showEmptyState(message: String) {
        layoutEmptyHome.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmptyHomeTitle.text = "표시할 일정이 없어요"
        tvEmptyHome.text = message
    }

    private fun hideEmptyState() {
        layoutEmptyHome.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun CalendarDay.toLocalDate(): LocalDate {
        return LocalDate.of(year, month, day)
    }

    private fun formatMonthDay(date: CalendarDay): String {
        return String.format("%02d/%02d", date.month, date.day)
    }

    private fun stringToCalendarDay(date: String): CalendarDay {
        val parts = date.split("-")
        return CalendarDay.from(
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
    }

    private fun isWithinNextDays(date: CalendarDay, start: CalendarDay, days: Long): Boolean {
        val target = date.toLocalDate()
        val startDate = start.toLocalDate()
        val endDate = startDate.plusDays(days)

        return !target.isBefore(startDate) && !target.isAfter(endDate)
    }

    private fun mapComebackToHomeSchedule(
        item: com.example.idolproject.Drawer.ComeBack.ComebackItem
    ): HomeScheduleItem {
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

    private fun mapGroupScheduleToHomeSchedule(
        item: com.example.idolproject.Drawer.Group.GroupScheduleItem
    ): HomeScheduleItem {
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

    private fun mapHomeScheduleToFeed(item: HomeScheduleItem): HomeFeedItem {
        val today = CalendarDay.today()

        val contentText = if (item.memo.isBlank()) {
            item.title
        } else {
            "${item.title} · ${item.memo}"
        }

        return HomeFeedItem(
            category = HomeCategory.SCHEDULE,
            groupId = item.groupId,
            groupName = item.groupName,
            time = "${formatMonthDay(item.date)} · ${item.label}",
            content = contentText,
            imageResId = null,
            isToday = item.date == today
        )
    }

    private fun openFavoriteGroupChat() {
        val groupId = favoriteGroupId
        if (groupId.isNullOrBlank()) {
            showEmptyState("최애 그룹을 먼저 설정해 주세요.")
            return
        }

        val roomName = "${favoriteGroupName ?: groupId} 팬톡방"

        startActivity(
            GroupChatActivity.newIntent(
                requireContext(),
                groupId,
                roomName
            )
        )
    }

}