package com.example.idolproject.Drawer.Group

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.idolproject.R
import com.example.idolproject.databinding.FragmentGroupBinding
import com.google.android.material.tabs.TabLayout
import com.prolificinteractive.materialcalendarview.CalendarDay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupFragment : Fragment(R.layout.fragment_group) {

    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GroupScheduleViewModel by viewModels()

    private lateinit var groupAdapter: GroupAdapter

    private var latestUiState: GroupScheduleUiState = GroupScheduleUiState()
    private var renderedFavoriteGroupIds: List<String> = emptyList()

    private val groupList = listOf("IVE", "NewJeans", "aespa", "LE SSERAFIM")
    private val typeList = GroupActivityType.values().map { it.displayName }

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
        setupCalendar()
        setupAdminAddButton()
        observeGroupScheduleUiState()
        observeGroupScheduleEvent()

        viewModel.start()
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(emptyList()) { item ->
            if (latestUiState.isAdmin) {
                showGroupScheduleOptionsDialog(item)
            }
        }

        binding.rvGroupSchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupAdapter
        }
    }

    private fun setupCalendar() {
        val today = CalendarDay.today()

        binding.calendarGroup.selectedDate = today

        binding.calendarGroup.setOnDateChangedListener { _, date, _ ->
            viewModel.selectDate(date)
        }
    }

    private fun setupAdminAddButton() {
        binding.fabAddGroupSchedule.visibility = View.GONE
        binding.fabAddGroupSchedule.setOnClickListener {
            showAddGroupScheduleDialog()
        }
    }

    private fun observeGroupScheduleUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindGroupScheduleUi(uiState)
                }
            }
        }
    }

    private fun observeGroupScheduleEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is GroupScheduleEvent.ShowToast -> {
                            toast(event.message)
                        }

                        is GroupScheduleEvent.ScheduleAdded -> {
                            binding.calendarGroup.selectedDate = event.item.date
                        }

                        is GroupScheduleEvent.ScheduleUpdated -> {
                            binding.calendarGroup.selectedDate = event.item.date
                        }

                        GroupScheduleEvent.ScheduleDeleted -> {
                            // 삭제 후 목록/캘린더는 Firestore snapshot → UiState 갱신으로 자동 반영
                        }
                    }
                }
            }
        }
    }

    private fun bindGroupScheduleUi(uiState: GroupScheduleUiState) {
        latestUiState = uiState

        if (renderedFavoriteGroupIds != uiState.favoriteGroupIds) {
            renderedFavoriteGroupIds = uiState.favoriteGroupIds
            setupTabs(uiState)
        }

        binding.fabAddGroupSchedule.visibility = if (uiState.isAdmin) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (binding.calendarGroup.selectedDate != uiState.selectedDate) {
            binding.calendarGroup.selectedDate = uiState.selectedDate
        }

        binding.tvGroupSelectedInfo.text = uiState.selectedDateInfoText
        groupAdapter.submitList(uiState.selectedDateItems)

        updateDecorators(uiState)
    }

    private fun setupTabs(uiState: GroupScheduleUiState) {
        val tabLayout = binding.tabGroupFilter
        tabLayout.removeAllTabs()

        tabLayout.addTab(tabLayout.newTab().setText("전체").setTag(null))

        uiState.favoriteGroupIds.forEach { groupId ->
            tabLayout.addTab(
                tabLayout.newTab()
                    .setText(getDisplayGroupName(groupId))
                    .setTag(groupId)
            )
        }

        tabLayout.clearOnTabSelectedListeners()
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedGroupId = tab?.tag as? String
                viewModel.selectGroup(selectedGroupId)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        val selectedIndex = if (uiState.selectedGroupId == null) {
            0
        } else {
            uiState.favoriteGroupIds.indexOf(uiState.selectedGroupId).let { index ->
                if (index >= 0) index + 1 else 0
            }
        }

        tabLayout.getTabAt(selectedIndex)?.select()
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
                viewModel.deleteGroupSchedule(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAddGroupScheduleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_group_schedule, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

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

        val initialDate = latestUiState.selectedDate
        var selectedYear = initialDate.year
        var selectedMonth = initialDate.month
        var selectedDay = initialDate.day

        tvSelectedDate.text = formatInputDate(selectedYear, selectedMonth, selectedDay)

        tvSelectedDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDate.text = formatInputDate(selectedYear, selectedMonth, selectedDay)
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
                    toast("제목을 입력하세요.")
                    return@setOnClickListener
                }

                val selectedType = GroupActivityType.values().firstOrNull {
                    it.displayName == selectedTypeDisplay
                } ?: GroupActivityType.OTHER

                val groupId = groupNameToId(selectedGroupName)
                val selectedDate = CalendarDay.from(selectedYear, selectedMonth, selectedDay)

                viewModel.addGroupSchedule(
                    groupId = groupId,
                    groupName = selectedGroupName,
                    date = selectedDate,
                    type = selectedType,
                    title = title,
                    memo = memo
                )

                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showEditGroupScheduleDialog(item: GroupScheduleItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_group_schedule, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDate = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

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

        val selectedGroupName = getDisplayGroupName(item.groupId)
        val groupIndex = groupList.indexOf(selectedGroupName)
        if (groupIndex >= 0) spinnerGroup.setSelection(groupIndex)

        val typeIndex = typeList.indexOf(item.type.displayName)
        if (typeIndex >= 0) spinnerType.setSelection(typeIndex)

        var selectedYear = item.date.year
        var selectedMonth = item.date.month
        var selectedDay = item.date.day

        tvSelectedDate.text = formatInputDate(selectedYear, selectedMonth, selectedDay)

        tvSelectedDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDate.text = formatInputDate(selectedYear, selectedMonth, selectedDay)
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
                    toast("제목을 입력하세요.")
                    return@setOnClickListener
                }

                val newGroupId = groupNameToId(newGroupName)

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

                viewModel.updateGroupSchedule(updatedItem)

                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updateDecorators(uiState: GroupScheduleUiState) {
        binding.calendarGroup.removeDecorators()

        val filteredItems = uiState.schedules.filter {
            uiState.selectedGroupId == null || it.groupId == uiState.selectedGroupId
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

    private fun getDisplayGroupName(groupId: String?): String {
        return when (groupId) {
            null -> "전체"
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "lesserafim" -> "LE SSERAFIM"
            "aespa" -> "aespa"
            "babymonster" -> "BABYMONSTER"
            else -> groupId
        }
    }

    private fun groupNameToId(groupName: String): String {
        return when (groupName) {
            "IVE" -> "ive"
            "NewJeans" -> "newjeans"
            "aespa" -> "aespa"
            "LE SSERAFIM" -> "lesserafim"
            "BABYMONSTER" -> "babymonster"
            else -> groupName.lowercase()
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

    private fun formatInputDate(year: Int, month: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}