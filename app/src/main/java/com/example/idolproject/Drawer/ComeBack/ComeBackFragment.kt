package com.example.idolproject.Drawer.ComeBack

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.time.LocalDate
import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog

class ComeBackFragment : Fragment(R.layout.fragment_comeback) {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvEmptyComeback: TextView
    private lateinit var rvComebackList: RecyclerView
    private lateinit var tabComebackFilter: TabLayout
    private lateinit var adapter: ComebackAdapter

    // 임시: 나중에 마이페이지/Firestore에서 가져올 최애 그룹 ID
    private var selectedGroupId: String? = null
    private var favoriteGroupIds: List<String> = emptyList()

    private var currentSelectedDate: CalendarDay = CalendarDay.today()

    private lateinit var fabAddComeback: FloatingActionButton

    private val comebackItems: List<ComebackItem>
        get() = firestoreComebackItems

    private val db by lazy { FirebaseFirestore.getInstance() }
    private var firestoreComebackItems: List<ComebackItem> = emptyList()

    private var isAdmin = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendar_comeback)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        tvEmptyComeback = view.findViewById(R.id.tv_empty_comeback)
        rvComebackList = view.findViewById(R.id.rv_comeback_list)
        tabComebackFilter = view.findViewById(R.id.tab_comeback_filter)
        fabAddComeback = view.findViewById(R.id.fab_add_comeback)

        setupRecyclerView()
        setupCalendar()
        setupAdminAddButton()

        loadFavoriteGroupsAndApply()
    }

    private fun setupRecyclerView() {
        adapter = ComebackAdapter { item ->
            if (isAdmin) {
                showComebackOptionsDialog(item)
            }
        }
        rvComebackList.layoutManager = LinearLayoutManager(requireContext())
        rvComebackList.adapter = adapter
    }

    private fun showComebackOptionsDialog(item: ComebackItem) {
        val options = arrayOf("수정", "삭제")

        AlertDialog.Builder(requireContext())
            .setTitle("${item.groupName} - ${item.title}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditComebackDialog(item)
                    1 -> showDeleteComebackDialog(item)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteComebackDialog(item: ComebackItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("컴백 일정 삭제")
            .setMessage("선택한 일정을 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                deleteComebackFromFirestore(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteComebackFromFirestore(item: ComebackItem) {
        if (item.id.isBlank()) {
            Toast.makeText(requireContext(), "삭제할 일정 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("comeback_schedules")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "컴백 일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()

                loadComebacksFromFirestore {
                    refreshComebackUiAfterDelete()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ComeBackDelete", "delete failed. id=${item.id}", e)
                Toast.makeText(
                    requireContext(),
                    "삭제 실패: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun refreshComebackUiAfterDelete() {
        val visibleItems = getFilteredItemsForSelectedDate(currentSelectedDate)

        if (visibleItems.isNotEmpty()) {
            refreshComebackUi()
            return
        }

        val nextDate = findNearestDateWithItems()

        if (nextDate != null) {
            currentSelectedDate = nextDate
            calendarView.selectedDate = nextDate
        }

        refreshComebackUi()
    }

    private fun getFilteredItemsForSelectedDate(date: CalendarDay): List<ComebackItem> {
        return comebackItems.filter { item ->
            item.date == date && (selectedGroupId == null || item.groupId == selectedGroupId)
        }
    }

    private fun findNearestDateWithItems(): CalendarDay? {
        return comebackItems
            .filter { selectedGroupId == null || it.groupId == selectedGroupId }
            .map { it.date }
            .sortedWith(compareBy<CalendarDay>({ it.year }, { it.month }, { it.day }))
            .firstOrNull()
    }

    private fun showEditComebackDialog(item: ComebackItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comeback, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDateDialog = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

        val groupList = listOf("IVE", "NewJeans", "aespa", "LE SSERAFIM")

        spinnerGroup.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            groupList
        )

        val selectedGroupName = when (item.groupId) {
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "aespa" -> "aespa"
            "lesserafim" -> "LE SSERAFIM"
            else -> item.groupName
        }

        val selectedIndex = groupList.indexOf(selectedGroupName)
        if (selectedIndex >= 0) {
            spinnerGroup.setSelection(selectedIndex)
        }

        var selectedYear = item.date.year
        var selectedMonth = item.date.month
        var selectedDay = item.date.day

        tvSelectedDateDialog.text = String.format(
            "%04d-%02d-%02d",
            selectedYear,
            selectedMonth,
            selectedDay
        )

        tvSelectedDateDialog.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDateDialog.text = String.format(
                        "%04d-%02d-%02d",
                        selectedYear,
                        selectedMonth,
                        selectedDay
                    )
                },
                selectedYear,
                selectedMonth - 1,
                selectedDay
            ).show()
        }

        etTitle.setText(item.title)
        etMemo.setText(item.memo)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("컴백 일정 수정")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장", null)
            .create()

        dialog.setOnShowListener {
            val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnSave.setOnClickListener {
                val newGroupName = spinnerGroup.selectedItem.toString()
                val title = etTitle.text.toString().trim()
                val memo = etMemo.text.toString().trim()

                if (title.isBlank()) {
                    Toast.makeText(requireContext(), "제목을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newGroupId = when (newGroupName) {
                    "IVE" -> "ive"
                    "NewJeans" -> "newjeans"
                    "aespa" -> "aespa"
                    "LE SSERAFIM" -> "lesserafim"
                    else -> newGroupName.lowercase()
                }

                val updatedItem = item.copy(
                    groupId = newGroupId,
                    groupName = newGroupName,
                    date = CalendarDay.from(selectedYear, selectedMonth, selectedDay),
                    title = title,
                    memo = memo
                )

                updateComebackInFirestore(updatedItem, dialog)
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updateComebackInFirestore(
        item: ComebackItem,
        dialog: AlertDialog? = null
    ) {
        if (item.id.isBlank()) {
            Toast.makeText(requireContext(), "수정할 일정 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "id" to item.id,
            "groupId" to item.groupId,
            "groupName" to item.groupName,
            "date" to item.date.toKey(),
            "title" to item.title,
            "memo" to item.memo
        )

        db.collection("comeback_schedules")
            .document(item.id)
            .set(data)
            .addOnSuccessListener {
                dialog?.dismiss()
                Toast.makeText(requireContext(), "컴백 일정이 수정되었습니다.", Toast.LENGTH_SHORT).show()

                loadComebacksFromFirestore {
                    currentSelectedDate = item.date
                    calendarView.selectedDate = item.date
                    refreshComebackUi()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "수정 실패: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setupTabs() {
        tabComebackFilter.removeAllTabs()

        tabComebackFilter.addTab(
            tabComebackFilter.newTab().setText("전체").setTag(null)
        )

        favoriteGroupIds.forEach { groupId ->
            tabComebackFilter.addTab(
                tabComebackFilter.newTab()
                    .setText(getDisplayGroupName(groupId))
                    .setTag(groupId)
            )
        }

        tabComebackFilter.clearOnTabSelectedListeners()
        tabComebackFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedGroupId = tab?.tag as? String
                refreshComebackUi()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        val selectedIndex = if (selectedGroupId == null) {
            0
        } else {
            favoriteGroupIds.indexOf(selectedGroupId).let { index ->
                if (index >= 0) index + 1 else 0
            }
        }

        tabComebackFilter.getTabAt(selectedIndex)?.select()
    }

    private fun setupCalendar() {
        val today = CalendarDay.today()
        currentSelectedDate = today
        calendarView.selectedDate = today

        calendarView.setOnDateChangedListener { _, date, _ ->
            currentSelectedDate = date
            updateSelectedDateAndList(date)
        }
    }

    private fun getComebackMap(): Map<String, List<ComebackItem>> {
        return comebackItems.groupBy { item ->
            item.date.toKey()
        }
    }

    /**
     * 실제 컴백 날짜 기준으로 하루 전 날짜에 점 표시
     */
    private fun addComebackDecorators() {
        val filteredMap = getComebackMap().mapValues { (_, items) ->
            applyGroupFilter(items)
        }

        for ((key, items) in filteredMap) {
            if (items.isEmpty()) continue

            val realDate = keyToCalendarDay(key) ?: continue
            val count = items.size.coerceAtMost(3)

            calendarView.addDecorator(
                SingleDayMultiDotDecorator(realDate, count)
            )
        }
    }

    /**
     * 사용자가 선택한 날짜 기준:
     * - 실제 컴백 날짜 = 선택 날짜 + 1일
     * - 해당 날짜의 일정 리스트를 가져온 뒤
     * - 최애 그룹만 / 전체 그룹 필터 적용
     */
    private fun updateSelectedDateAndList(selected: CalendarDay) {
        val key = selected.toKey()

        val rawItems = getComebackMap()[key].orEmpty()
        val filteredItems = applyGroupFilter(rawItems)

        val dateText = formatDisplayDate(selected)
        tvSelectedDate.text = makeSelectedDateText(dateText, filteredItems.size)

        adapter.updateItems(filteredItems)
        updateEmptyState(filteredItems.isEmpty())
    }

    private fun makeSelectedDateText(dateText: String, count: Int): String {
        val groupLabel = getDisplayGroupName(selectedGroupId)

        return when {
            count <= 0 -> "$groupLabel · $dateText · 등록된 컴백 일정 없음"
            count == 1 -> "$groupLabel · $dateText · 컴백 1건"
            else -> "$groupLabel · $dateText · 컴백 ${count}건"
        }
    }

    private fun applyGroupFilter(items: List<ComebackItem>): List<ComebackItem> {
        return selectedGroupId?.let { groupId ->
            items.filter { it.groupId == groupId }
        } ?: items
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            tvEmptyComeback.visibility = View.VISIBLE
            rvComebackList.visibility = View.GONE
        } else {
            tvEmptyComeback.visibility = View.GONE
            rvComebackList.visibility = View.VISIBLE
        }
    }

    private fun getDisplayGroupName(groupId: String?): String {
        return when (groupId) {
            null -> "전체"
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "lesserafim" -> "LE SSERAFIM"
            "aespa" -> "aespa"
            else -> groupId
        }
    }

    private fun keyToCalendarDay(key: String): CalendarDay? {
        val parts = key.split("-")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        return CalendarDay.from(year, month, day)
    }

    private fun CalendarDay.toKey(): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    private fun formatDisplayDate(date: CalendarDay): String {
        return String.format("%04d.%02d.%02d", date.year, date.month, date.day)
    }

    private fun CalendarDay.minusDays(days: Long): CalendarDay {
        val base = LocalDate.of(year, month, day).minusDays(days)
        return CalendarDay.from(base.year, base.monthValue, base.dayOfMonth)
    }

    private fun CalendarDay.plusDays(days: Long): CalendarDay {
        val base = LocalDate.of(year, month, day).plusDays(days)
        return CalendarDay.from(base.year, base.monthValue, base.dayOfMonth)
    }

    private fun loadFavoriteGroupsAndApply() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            selectedGroupId = null
            favoriteGroupIds = emptyList()
            setupTabs()

            loadComebacksFromFirestore {
                refreshComebackUi()
            }
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                favoriteGroupIds =
                    document.getString("favoriteGroupId")?.let { listOf(it) }.orEmpty()

                selectedGroupId = null
                setupTabs()

                loadComebacksFromFirestore {
                    refreshComebackUi()
                }
            }
            .addOnFailureListener {
                favoriteGroupIds = emptyList()
                selectedGroupId = null
                setupTabs()

                loadComebacksFromFirestore {
                    refreshComebackUi()
                }
            }
    }
    private fun refreshComebackUi() {
        calendarView.removeDecorators()
        addComebackDecorators()
        updateSelectedDateAndList(currentSelectedDate)
    }

    private fun setupAdminAddButton() {
        fabAddComeback.visibility = View.GONE
        fabAddComeback.setOnClickListener(null)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                isAdmin = document.getString("role") == "admin"

                fabAddComeback.visibility =
                    if (isAdmin) View.VISIBLE else View.GONE

                if (isAdmin) {
                    fabAddComeback.setOnClickListener {
                        showAddComebackDialog()
                    }
                } else {
                    fabAddComeback.setOnClickListener(null)
                }
            }
            .addOnFailureListener {
                isAdmin = false
                fabAddComeback.visibility = View.GONE
                fabAddComeback.setOnClickListener(null)
            }
    }

    private fun showAddComebackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comeback, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDateDialog = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

        val groupList = listOf("IVE", "NewJeans", "aespa", "LE SSERAFIM")

        spinnerGroup.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            groupList
        )

        val initialDate = currentSelectedDate
        var selectedYear = initialDate.year
        var selectedMonth = initialDate.month
        var selectedDay = initialDate.day

        tvSelectedDateDialog.text = String.format(
            "%04d-%02d-%02d",
            selectedYear,
            selectedMonth,
            selectedDay
        )

        tvSelectedDateDialog.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDateDialog.text = String.format(
                        "%04d-%02d-%02d",
                        selectedYear,
                        selectedMonth,
                        selectedDay
                    )
                },
                selectedYear,
                selectedMonth - 1,
                selectedDay
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("컴백 일정 추가")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장", null)
            .create()

        dialog.setOnShowListener {
            val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnSave.setOnClickListener {
                val selectedGroupName = spinnerGroup.selectedItem.toString()
                val title = etTitle.text.toString().trim()
                val memo = etMemo.text.toString().trim()

                if (title.isBlank()) {
                    Toast.makeText(requireContext(), "제목을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val groupId = when (selectedGroupName) {
                    "IVE" -> "ive"
                    "NewJeans" -> "newjeans"
                    "aespa" -> "aespa"
                    "LE SSERAFIM" -> "lesserafim"
                    else -> selectedGroupName.lowercase()
                }

                val selectedDate = CalendarDay.from(selectedYear, selectedMonth, selectedDay)

                saveComebackToFirestore(
                    groupId = groupId,
                    groupName = selectedGroupName,
                    date = selectedDate,
                    title = title,
                    memo = memo
                ) {
                    Toast.makeText(requireContext(), "컴백 일정이 추가되었습니다.", Toast.LENGTH_SHORT).show()

                    loadComebacksFromFirestore {
                        refreshComebackUiAfterAdd(
                            ComebackItem(
                                id = "",
                                groupId = groupId,
                                groupName = selectedGroupName,
                                date = selectedDate,
                                title = title,
                                memo = memo
                            )
                        )
                    }

                    dialog.dismiss()
                }
            }
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun refreshComebackUiAfterAdd(newItem: ComebackItem) {
        currentSelectedDate = newItem.date
        calendarView.selectedDate = newItem.date
        refreshComebackUi()
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

    private fun saveComebackToFirestore(
        groupId: String,
        groupName: String,
        date: CalendarDay,
        title: String,
        memo: String,
        onSuccess: () -> Unit
    ) {
        val docRef = db.collection("comeback_schedules").document()

        val data = hashMapOf(
            "id" to docRef.id,
            "groupId" to groupId,
            "groupName" to groupName,
            "date" to calendarDayToString(date),
            "title" to title,
            "memo" to memo,
            "createdBy" to "test_admin",
            "createdAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadComebacksFromFirestore(onComplete: (() -> Unit)? = null) {
        db.collection("comeback_schedules")
            .get()
            .addOnSuccessListener { result ->
                firestoreComebackItems = result.documents.mapNotNull { doc ->
                    try {
                        val dateString = doc.getString("date") ?: return@mapNotNull null

                        ComebackItem(
                            id = doc.getString("id") ?: doc.id,
                            groupId = doc.getString("groupId") ?: "",
                            groupName = doc.getString("groupName") ?: "",
                            date = stringToCalendarDay(dateString),
                            title = doc.getString("title") ?: "",
                            memo = doc.getString("memo") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "불러오기 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}