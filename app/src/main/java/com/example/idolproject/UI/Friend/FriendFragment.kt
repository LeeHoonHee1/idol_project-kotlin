package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import com.google.android.material.chip.ChipGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendFragment : Fragment(R.layout.fragment_friend) {

    private val viewModel: FriendViewModel by viewModels()
    private lateinit var recyclerFriends: RecyclerView
    private lateinit var tvFriendCount: TextView
    private lateinit var adapter: FriendAdapter
    private lateinit var btnSearchFriend: MaterialButton
    private lateinit var btnAddFriend: MaterialButton
    private lateinit var layoutFriendEmpty: View
    private lateinit var btnEmptyFindFriend: MaterialButton
    private lateinit var tvFriendEmptyTitle: TextView
    private lateinit var tvFriendEmptyDesc: TextView
    private lateinit var chipGroupFriendFilter: ChipGroup
    private var allFriends: List<Friend> = emptyList()
    private var currentFriendCount: Int = 0
    private var currentPendingRequestCount: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerFriends = view.findViewById(R.id.recycler_friends)
        tvFriendCount = view.findViewById(R.id.tv_friend_count)

        btnSearchFriend = view.findViewById(R.id.btn_search_friend)
        btnAddFriend = view.findViewById(R.id.btn_add_friend)

        layoutFriendEmpty = view.findViewById(R.id.layout_friend_empty)
        btnEmptyFindFriend = view.findViewById(R.id.btn_empty_find_friend)
        tvFriendEmptyTitle = view.findViewById(R.id.tv_friend_empty_title)
        tvFriendEmptyDesc = view.findViewById(R.id.tv_friend_empty_desc)

        chipGroupFriendFilter = view.findViewById(R.id.chip_group_friend_filter)

        btnSearchFriend.setOnClickListener {
            openFriendSearch()
        }

        btnAddFriend.setOnClickListener {
            openFriendSearch()
        }

        btnEmptyFindFriend.setOnClickListener {
            openFriendSearch()
        }

        chipGroupFriendFilter.setOnCheckedStateChangeListener { _, _ ->
            applyFriendFilter()
        }

        adapter = FriendAdapter(
            onItemClick = { friend ->
                viewModel.openFriendProfile(friend.uid)
            },
            onDeleteClick = { friend ->
                showDeleteConfirm(friend)
            }
        )

        recyclerFriends.layoutManager = LinearLayoutManager(requireContext())
        recyclerFriends.adapter = adapter

        observeFriendListUiState()
        observeFriendEvent()

        viewModel.startObserveFriendList()
        viewModel.startObservePendingRequestCount()
    }

    private fun observeFriendListUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friendListUiState.collect { uiState ->
                    bindFriendListUi(uiState)
                }
            }
        }
    }

    private fun observeFriendEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is FriendEvent.ShowToast -> {
                            toast(event.message)
                        }

                        is FriendEvent.OpenFriendProfile -> {
                            openFriendProfile(event.friendUid)
                        }
                    }
                }
            }
        }
    }

    private fun bindFriendListUi(uiState: FriendListUiState) {
        allFriends = uiState.friends
        currentFriendCount = uiState.friends.size
        currentPendingRequestCount = uiState.pendingRequestCount

        updateFriendSummaryText()
        applyFriendFilter()
    }

    private fun updateFriendSummaryText() {
        tvFriendCount.text = if (currentPendingRequestCount > 0) {
            "${currentFriendCount}명 · 요청 ${currentPendingRequestCount}개"
        } else {
            "${currentFriendCount}명"
        }
    }

    private fun applyFriendFilter() {
        val filteredList = when (chipGroupFriendFilter.checkedChipId) {
            R.id.chip_friend_same_favorite -> {
                allFriends
                    .filter { it.isSameFavorite }
                    .sortedWith(
                        compareByDescending<Friend> { it.level }
                            .thenBy { it.nickname }
                    )
            }

            R.id.chip_friend_level_desc -> {
                allFriends
                    .sortedWith(
                        compareByDescending<Friend> { it.level }
                            .thenBy { it.nickname }
                    )
            }

            else -> {
                allFriends
                    .sortedWith(
                        compareByDescending<Friend> { it.isSameFavorite }
                            .thenByDescending { it.level }
                            .thenBy { it.nickname }
                    )
            }
        }

        adapter.submitList(filteredList)
        updateEmptyState(filteredList.size)
    }

    private fun showDeleteConfirm(friend: Friend) {
        AlertDialog.Builder(requireContext())
            .setTitle("친구 삭제")
            .setMessage("${friend.nickname}님을 친구에서 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteFriend(friend.uid)
            }
            .show()
    }

    private fun openFriendSearch() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FriendSearchFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openFriendProfile(friendUid: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FriendProfileFragment.newInstance(friendUid))
            .addToBackStack(null)
            .commit()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun openFriendRequests() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FriendRequestsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun updateEmptyState(displayCount: Int) {
        if (displayCount == 0) {
            recyclerFriends.visibility = View.GONE
            layoutFriendEmpty.visibility = View.VISIBLE

            when (chipGroupFriendFilter.checkedChipId) {
                R.id.chip_friend_same_favorite -> {
                    if (allFriends.isEmpty()) {
                        tvFriendEmptyTitle.text = "아직 친구가 없어요"
                        tvFriendEmptyDesc.text = "닉네임으로 팬 친구를 찾아보세요"
                        btnEmptyFindFriend.visibility = View.VISIBLE
                    } else {
                        tvFriendEmptyTitle.text = "같은 최애 친구가 아직 없어요"
                        tvFriendEmptyDesc.text = "다른 필터를 선택하거나 새 친구를 찾아보세요"
                        btnEmptyFindFriend.visibility = View.VISIBLE
                    }
                }

                R.id.chip_friend_level_desc -> {
                    if (allFriends.isEmpty()) {
                        tvFriendEmptyTitle.text = "아직 친구가 없어요"
                        tvFriendEmptyDesc.text = "닉네임으로 팬 친구를 찾아보세요"
                        btnEmptyFindFriend.visibility = View.VISIBLE
                    } else {
                        tvFriendEmptyTitle.text = "표시할 친구가 없어요"
                        tvFriendEmptyDesc.text = "전체 필터로 다시 확인해보세요"
                        btnEmptyFindFriend.visibility = View.GONE
                    }
                }

                else -> {
                    tvFriendEmptyTitle.text = "아직 친구가 없어요"
                    tvFriendEmptyDesc.text = "닉네임으로 팬 친구를 찾아보세요"
                    btnEmptyFindFriend.visibility = View.VISIBLE
                }
            }
        } else {
            recyclerFriends.visibility = View.VISIBLE
            layoutFriendEmpty.visibility = View.GONE
        }
    }
}