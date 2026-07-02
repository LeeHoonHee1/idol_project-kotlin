package com.example.idolproject.Drawer.ComeBack

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
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
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ComeBackFragment : Fragment(R.layout.fragment_comeback) {

    private val viewModel: ComebackScheduleViewModel by viewModels()

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvEmptyComeback: TextView
    private lateinit var rvComebackList: RecyclerView
    private lateinit var tabComebackFilter: TabLayout
    private lateinit var adapter: ComebackAdapter
    private lateinit var fabAddComeback: FloatingActionButton

    private var latestUiState: ComebackScheduleUiState = ComebackScheduleUiState()
    private var renderedFavoriteGroupIds: List<String> = emptyList()

    private val groupList = listOf("IVE", "NewJeans", "aespa", "LE SSERAFIM")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupCalendar()
        setupAdminAddButton()
        observeComebackScheduleUiState()
        observeComebackScheduleEvent()

        viewModel.start()
    }

    private fun bindViews(view: View) {
        calendarView = view.findViewById(R.id.calendar_comeback)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        tvEmptyComeback = view.findViewById(R.id.tv_empty_comeback)
        rvComebackList = view.findViewById(R.id.rv_comeback_list)
        tabComebackFilter = view.findViewById(R.id.tab_comeback_filter)
        fabAddComeback = view.findViewById(R.id.fab_add_comeback)
    }

    private fun setupRecyclerView() {
        adapter = ComebackAdapter { item ->
            if (latestUiState.isAdmin) {
                showComebackOptionsDialog(item)
            }
        }

        rvComebackList.layoutManager = LinearLayoutManager(requireContext())
        rvComebackList.adapter = adapter
    }

    private fun setupCalendar() {
        val today = CalendarDay.today()

        calendarView.selectedDate = today

        calendarView.setOnDateChangedListener { _, date, _ ->
            viewModel.selectDate(date)
        }
    }

    private fun setupAdminAddButton() {
        fabAddComeback.visibility = View.GONE
        fabAddComeback.setOnClickListener {
            showAddComebackDialog()
        }
    }

    private fun observeComebackScheduleUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindComebackScheduleUi(uiState)
                }
            }
        }
    }

    private fun observeComebackScheduleEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is ComebackScheduleEvent.ShowToast -> {
                            toast(event.message)
                        }

                        is ComebackScheduleEvent.ComebackAdded -> {
                            calendarView.selectedDate = event.item.date
                        }

                        is ComebackScheduleEvent.ComebackUpdated -> {
                            calendarView.selectedDate = event.item.date
                        }

                        ComebackScheduleEvent.ComebackDeleted -> {
                            // 삭제 후 목록/캘린더는 Firestore snapshot → UiState 갱신으로 자동 반영
                        }
                    }
                }
            }
        }
    }

    private fun bindComebackScheduleUi(uiState: ComebackScheduleUiState) {
        latestUiState = uiState

        if (renderedFavoriteGroupIds != uiState.favoriteGroupIds) {
            renderedFavoriteGroupIds = uiState.favoriteGroupIds
            setupTabs(uiState)
        }

        fabAddComeback.visibility = if (uiState.isAdmin) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (calendarView.selectedDate != uiState.selectedDate) {
            calendarView.selectedDate = uiState.selectedDate
        }

        tvSelectedDate.text = uiState.selectedDateInfoText
        adapter.updateItems(uiState.selectedDateItems)
        updateEmptyState(uiState.isSelectedDateEmpty)
        updateDecorators(uiState)
    }

    private fun setupTabs(uiState: ComebackScheduleUiState) {
        tabComebackFilter.removeAllTabs()

        tabComebackFilter.addTab(
            tabComebackFilter.newTab().setText("전체").setTag(null)
        )

        uiState.favoriteGroupIds.forEach { groupId ->
            tabComebackFilter.addTab(
                tabComebackFilter.newTab()
                    .setText(getDisplayGroupName(groupId))
                    .setTag(groupId)
            )
        }

        tabComebackFilter.clearOnTabSelectedListeners()
        tabComebackFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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

        tabComebackFilter.getTabAt(selectedIndex)?.select()
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
                viewModel.deleteComeback(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAddComebackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comeback, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDateDialog = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

        spinnerGroup.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            groupList
        )

        val initialDate = latestUiState.selectedDate
        var selectedYear = initialDate.year
        var selectedMonth = initialDate.month
        var selectedDay = initialDate.day

        tvSelectedDateDialog.text = formatInputDate(selectedYear, selectedMonth, selectedDay)

        tvSelectedDateDialog.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDateDialog.text =
                        formatInputDate(selectedYear, selectedMonth, selectedDay)
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
                    toast("제목을 입력하세요.")
                    return@setOnClickListener
                }

                val groupId = groupNameToId(selectedGroupName)
                val selectedDate = CalendarDay.from(selectedYear, selectedMonth, selectedDay)

                viewModel.addComeback(
                    groupId = groupId,
                    groupName = selectedGroupName,
                    date = selectedDate,
                    title = title,
                    memo = memo
                )

                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showEditComebackDialog(item: ComebackItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comeback, null)

        val spinnerGroup = dialogView.findViewById<Spinner>(R.id.spinner_group)
        val tvSelectedDateDialog = dialogView.findViewById<TextView>(R.id.tv_selected_date)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etMemo = dialogView.findViewById<EditText>(R.id.et_memo)

        spinnerGroup.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            groupList
        )

        val selectedGroupName = getDisplayGroupName(item.groupId)
        val selectedIndex = groupList.indexOf(selectedGroupName)
        if (selectedIndex >= 0) {
            spinnerGroup.setSelection(selectedIndex)
        }

        var selectedYear = item.date.year
        var selectedMonth = item.date.month
        var selectedDay = item.date.day

        tvSelectedDateDialog.text = formatInputDate(selectedYear, selectedMonth, selectedDay)

        tvSelectedDateDialog.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month + 1
                    selectedDay = dayOfMonth
                    tvSelectedDateDialog.text =
                        formatInputDate(selectedYear, selectedMonth, selectedDay)
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
                    toast("제목을 입력하세요.")
                    return@setOnClickListener
                }

                val updatedItem = item.copy(
                    groupId = groupNameToId(newGroupName),
                    groupName = newGroupName,
                    date = CalendarDay.from(selectedYear, selectedMonth, selectedDay),
                    title = title,
                    memo = memo
                )

                viewModel.updateComeback(updatedItem)

                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updateDecorators(uiState: ComebackScheduleUiState) {
        calendarView.removeDecorators()

        val filteredItems = uiState.comebacks.filter {
            uiState.selectedGroupId == null || it.groupId == uiState.selectedGroupId
        }

        val groupedByDate = filteredItems.groupBy { it.date }

        groupedByDate.forEach { (date, items) ->
            val count = items.size.coerceAtMost(3)

            calendarView.addDecorator(
                SingleDayMultiDotDecorator(date, count)
            )
        }
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

    private fun formatInputDate(year: Int, month: Int, day: Int): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}