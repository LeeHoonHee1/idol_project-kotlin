package com.example.idolproject.Drawer.Group

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.idolproject.R
import com.example.idolproject.databinding.FragmentGroupBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Locale
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.time.LocalDate
import android.widget.Toast
import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class GroupFragment : Fragment(R.layout.fragment_group) {

    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!

    private lateinit var groupAdapter: GroupAdapter

    private var selectedDate: CalendarDay? = null
    private var selectedGroupId: String? = null
    private var favoriteGroupIds: List<String> = emptyList()
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var firestoreGroupScheduleItems: List<GroupScheduleItem> = emptyList()

    private val groupScheduleItems: List<GroupScheduleItem>
        get() = firestoreGroupScheduleItems

    private var isAdmin = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupAdminAddButton()
        setupCalendar()

        loadFavoriteGroupsAndApply()
    }

    private fun setupTabs() {
        val tabLayout = binding.tabGroupFilter
        tabLayout.removeAllTabs()

        tabLayout.addTab(tabLayout.newTab().setText("전체").setTag(null))

        favoriteGroupIds.forEach { groupId ->
            tabLayout.addTab(
                tabLayout.newTab()
                    .setText(getDisplayGroupName(groupId))
                    .setTag(groupId)
            )
        }

        tabLayout.clearOnTabSelectedListeners()
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedGroupId = tab?.tag as? String
                updateDecorators()

                val currentDate = binding.calendarGroup.selectedDate ?: CalendarDay.today()
                updateSelectedDateAndList(currentDate)
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

        tabLayout.getTabAt(selectedIndex)?.select()
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(emptyList()) { item ->
            if (isAdmin) {
                showGroupScheduleOptionsDialog(item)
            }
        }

        binding.rvGroupSchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }
    }

    private fun showGroupScheduleOptionsDialog(item: GroupScheduleItem) {
        val options = arrayOf("수정", "삭제")

        AlertDialog.Builder(requireContext())
            .setTitle("${item.groupName} - ${item.title}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditGroupScheduleDialog(item)
                    1 -> showDeleteGroupScheduleDialog(item)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteGroupScheduleDialog(item: GroupScheduleItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("그룹 일정 삭제")
            .setMessage("선택한 일정을 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                deleteGroupScheduleFromFirestore(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteGroupScheduleFromFirestore(item: GroupScheduleItem) {
        if (item.id.isBlank()) {
            Toast.makeText(requireContext(), "삭제할 일정 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("group_schedules")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()

                loadGroupSchedulesFromFirestore {
                    refreshGroupScheduleUiAfterDelete()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("GroupScheduleDelete", "delete failed. id=${item.id}", e)
                Toast.makeText(
                    requireContext(),
                    "삭제 실패: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun refreshGroupScheduleUiAfterDelete() {
        val current = selectedDate ?: CalendarDay.today()
        val visibleItems = getFilteredItemsForDate(current)

        if (visibleItems.isNotEmpty()) {
            updateDecorators()
            updateSelectedDateAndList(current)
            return
        }

        val nextDate = findFirstDateWithItems()
        if (nextDate != null) {
            selectedDate = nextDate
            binding.calendarGroup.selectedDate = nextDate
            updateDecorators()
            updateSelectedDateAndList(nextDate)
        } else {
            selectedDate = current
            updateDecorators()
            updateSelectedDateAndList(current)
        }
    }

    private fun getFilteredItemsForDate(date: CalendarDay): List<GroupScheduleItem> {
        return groupScheduleItems.filter {
            it.date == date && (selectedGroupId == null || it.groupId == selectedGroupId)
        }
    }

    private fun findFirstDateWithItems(): CalendarDay? {
        return groupScheduleItems
            .filter { selectedGroupId == null || it.groupId == selectedGroupId }
            .map { it.date }
            .sortedWith(compareBy<CalendarDay>({ it.year }, { it.month }, { it.day }))
            .firstOrNull()
    }

    private fun showEditGroupScheduleDialog(item: GroupScheduleItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_group_schedule, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

        val groupList = listOf("IVE", "NewJeans", "aespa", "LE SSERAFIM")
        val typeList = GroupActivityType.values().map { it.displayName }

        spinnerGroup.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            groupList
        )

        spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            typeList
        )

        val selectedGroupName = when (item.groupId) {
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "aespa" -> "aespa"
            "lesserafim" -> "LE SSERAFIM"
            else -> item.groupName
        }

        val groupIndex = groupList.indexOf(selectedGroupName)
        if (groupIndex >= 0) spinnerGroup.setSelection(groupIndex)

        val typeIndex = typeList.indexOf(item.type.displayName)
        if (typeIndex >= 0) spinnerType.setSelection(typeIndex)

        var selectedYear = item.date.year
        var selectedMonth = item.date.month
        var selectedDay = item.date.day

        tvSelectedDate.text = String.format(
            "%04d-%02d-%02d",
            selectedYear,
            selectedMonth,
            selectedDay
        )

        tvSelectedDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDate.text = String.format(
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
            .setTitle("그룹 일정 수정")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장", null)
            .create()

        dialog.setOnShowListener {
            val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnSave.setOnClickListener {
                val newGroupName = spinnerGroup.selectedItem.toString()
                val selectedTypeDisplay = spinnerType.selectedItem.toString()
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

                val newType = GroupActivityType.values().firstOrNull {
                    it.displayName == selectedTypeDisplay
                } ?: GroupActivityType.OTHER

                val updatedItem = item.copy(
                    groupId = newGroupId,
                    groupName = newGroupName,
                    date = CalendarDay.from(selectedYear, selectedMonth, selectedDay),
                    type = newType,
                    title = title,
                    memo = memo
                )

                updateGroupScheduleInFirestore(updatedItem, dialog)
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updateGroupScheduleInFirestore(
        item: GroupScheduleItem,
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
            "date" to calendarDayToString(item.date),
            "type" to item.type.name,
            "title" to item.title,
            "memo" to item.memo,
            "createdBy" to "test_admin"
        )

        db.collection("group_schedules")
            .document(item.id)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "일정이 수정되었습니다.", Toast.LENGTH_SHORT).show()

                loadGroupSchedulesFromFirestore {
                    selectedDate = item.date
                    binding.calendarGroup.selectedDate = item.date
                    updateDecorators()
                    updateSelectedDateAndList(item.date)
                    dialog?.dismiss()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "수정 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupCalendar() {
        val calendar: MaterialCalendarView = binding.calendarGroup

        val today = CalendarDay.today()
        selectedDate = today
        calendar.selectedDate = today

        updateDecorators()
        updateSelectedDateAndList(today)

        calendar.setOnDateChangedListener { _, date, _ ->
            selectedDate = date
            updateSelectedDateAndList(date)
        }
    }

    private fun getGroupScheduleMap(): Map<String, List<GroupScheduleItem>> {
        return groupScheduleItems.groupBy { item ->
            item.date.toKey()
        }
    }

    private fun updateSelectedDateAndList(selected: CalendarDay) {
        val key = selected.toKey()
        val allItemsForDate = getGroupScheduleMap()[key].orEmpty()

        val filtered = selectedGroupId?.let { groupId ->
            allItemsForDate.filter { it.groupId == groupId }
        } ?: allItemsForDate

        val dateText = formatDisplayDate(selected)
        val groupLabel = getDisplayGroupName(selectedGroupId)

        binding.tvGroupSelectedInfo.text = if (filtered.isEmpty()) {
            "$groupLabel · $dateText · 등록된 일정이 없어요"
        } else {
            "$groupLabel · $dateText · 일정 ${filtered.size}건"
        }

        groupAdapter.submitList(filtered)
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

    private fun getColorForType(type: GroupActivityType): Int {
        return when (type) {
            GroupActivityType.FAN_SIGN -> Color.parseColor("#F06292")
            GroupActivityType.VARIETY -> Color.parseColor("#7E57C2")
            GroupActivityType.RADIO -> Color.parseColor("#42A5F5")
            GroupActivityType.MUSIC_SHOW -> Color.parseColor("#26A69A")
            GroupActivityType.OTHER -> Color.parseColor("#9E9E9E")
        }
    }

    private fun updateDecorators() {
        binding.calendarGroup.removeDecorators()

        val filteredItems = groupScheduleItems.filter {
            selectedGroupId == null || it.groupId == selectedGroupId
        }

        val groupedByDate = filteredItems.groupBy { it.date }

        groupedByDate.forEach { (date, items) ->
            val colors = items
                .map { getColorForType(it.type) }
                .distinct()
                .take(3)

            if (colors.isNotEmpty()) {
                binding.calendarGroup.addDecorator(
                    GroupDotDecorator(date, colors)
                )
            }
        }
    }

    private fun CalendarDay.toKey(): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    private fun formatDisplayDate(date: CalendarDay): String {
        return String.format("%04d.%02d.%02d", date.year, date.month, date.day)
    }

    private fun CalendarDay.plusDays(days: Long): CalendarDay {
        val base = LocalDate.of(year, month, day).plusDays(days)
        return CalendarDay.from(base.year, base.monthValue, base.dayOfMonth)
    }

    private fun CalendarDay.minusDays(days: Long): CalendarDay {
        val base = LocalDate.of(year, month, day).minusDays(days)
        return CalendarDay.from(base.year, base.monthValue, base.dayOfMonth)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadFavoriteGroupsAndApply() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            favoriteGroupIds = emptyList()
            selectedGroupId = null
            setupTabs()

            loadGroupSchedulesFromFirestore {
                updateDecorators()
                val currentDate = binding.calendarGroup.selectedDate ?: CalendarDay.today()
                updateSelectedDateAndList(currentDate)
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

                loadGroupSchedulesFromFirestore {
                    updateDecorators()
                    val currentDate = binding.calendarGroup.selectedDate ?: CalendarDay.today()
                    updateSelectedDateAndList(currentDate)
                }
            }
            .addOnFailureListener {
                favoriteGroupIds = emptyList()
                selectedGroupId = null
                setupTabs()

                loadGroupSchedulesFromFirestore {
                    updateDecorators()
                    val currentDate = binding.calendarGroup.selectedDate ?: CalendarDay.today()
                    updateSelectedDateAndList(currentDate)
                }
            }
    }

    private fun setupAdminAddButton() {
        binding.fabAddGroupSchedule.visibility = View.GONE
        binding.fabAddGroupSchedule.setOnClickListener(null)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                isAdmin = document.getString("role") == "admin"

                binding.fabAddGroupSchedule.visibility =
                    if (isAdmin) View.VISIBLE else View.GONE

                if (isAdmin) {
                    binding.fabAddGroupSchedule.setOnClickListener {
                        showAddGroupScheduleDialog()
                    }
                } else {
                    binding.fabAddGroupSchedule.setOnClickListener(null)
                }
            }
            .addOnFailureListener {
                isAdmin = false
                binding.fabAddGroupSchedule.visibility = View.GONE
                binding.fabAddGroupSchedule.setOnClickListener(null)
            }
    }

    private fun showAddGroupScheduleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_group_schedule, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

        val groupList = listOf("IVE", "NewJeans", "aespa", "LE SSERAFIM")
        val typeList = GroupActivityType.values().map { it.displayName }

        spinnerGroup.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            groupList
        )

        spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            typeList
        )

        val initialDate = selectedDate ?: CalendarDay.today()
        var selectedYear = initialDate.year
        var selectedMonth = initialDate.month
        var selectedDay = initialDate.day

        tvSelectedDate.text = String.format(
            "%04d-%02d-%02d",
            selectedYear,
            selectedMonth,
            selectedDay
        )

        tvSelectedDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDate.text = String.format(
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
            .setTitle("그룹 일정 추가")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장", null)
            .create()

        dialog.setOnShowListener {
            val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnSave.setOnClickListener {
                val selectedGroupName = spinnerGroup.selectedItem.toString()
                val selectedTypeDisplay = spinnerType.selectedItem.toString()
                val title = etTitle.text.toString().trim()
                val memo = etMemo.text.toString().trim()

                if (title.isBlank()) {
                    Toast.makeText(requireContext(), "제목을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val selectedType = GroupActivityType.values().firstOrNull {
                    it.displayName == selectedTypeDisplay
                } ?: GroupActivityType.OTHER

                val groupId = when (selectedGroupName) {
                    "IVE" -> "ive"
                    "NewJeans" -> "newjeans"
                    "aespa" -> "aespa"
                    "LE SSERAFIM" -> "lesserafim"
                    else -> selectedGroupName.lowercase()
                }

                val selectedDate = CalendarDay.from(selectedYear, selectedMonth, selectedDay)

                saveGroupScheduleToFirestore(
                    groupId = groupId,
                    groupName = selectedGroupName,
                    date = selectedDate,
                    type = selectedType,
                    title = title,
                    memo = memo
                ) {
                    Toast.makeText(requireContext(), "일정이 추가되었습니다.", Toast.LENGTH_SHORT).show()
                    loadGroupSchedulesFromFirestore {
                        refreshScheduleUIAfterAdd(
                            GroupScheduleItem(
                                id = "",
                                groupId = groupId,
                                groupName = selectedGroupName,
                                date = selectedDate,
                                type = selectedType,
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

    private fun refreshScheduleUIAfterAdd(newItem: GroupScheduleItem) {
        selectedDate = newItem.date
        binding.calendarGroup.selectedDate = newItem.date
        updateDecorators()
        updateSelectedDateAndList(newItem.date)
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

    private fun saveGroupScheduleToFirestore(
        groupId: String,
        groupName: String,
        date: CalendarDay,
        type: GroupActivityType,
        title: String,
        memo: String,
        onSuccess: () -> Unit
    ) {
        val docRef = db.collection("group_schedules").document()

        val data = hashMapOf(
            "id" to docRef.id,
            "groupId" to groupId,
            "groupName" to groupName,
            "date" to calendarDayToString(date),
            "type" to type.name,
            "title" to title,
            "memo" to memo,
            "createdBy" to "test_admin",
            "createdAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "저장 실패", Toast.LENGTH_SHORT).show()
            }
    }
    private fun loadGroupSchedulesFromFirestore(onComplete: (() -> Unit)? = null) {
        db.collection("group_schedules")
            .get()
            .addOnSuccessListener { result ->
                firestoreGroupScheduleItems = result.documents.mapNotNull { doc ->
                    try {
                        val dateString = doc.getString("date") ?: return@mapNotNull null
                        val typeString = doc.getString("type") ?: GroupActivityType.OTHER.name

                        GroupScheduleItem(
                            id = doc.getString("id") ?: doc.id,
                            groupId = doc.getString("groupId") ?: "",
                            groupName = doc.getString("groupName") ?: "",
                            date = stringToCalendarDay(dateString),
                            type = runCatching { GroupActivityType.valueOf(typeString) }
                                .getOrDefault(GroupActivityType.OTHER),
                            title = doc.getString("title") ?: "",
                            memo = doc.getString("memo") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onComplete?.invoke()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "일정 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }
}