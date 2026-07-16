package com.example.idolproject.UI.Home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.Drawer.Community.GroupChatActivity
import com.example.idolproject.R
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyHome: TextView
    private lateinit var homeFeedAdapter: HomeFeedAdapter

    private lateinit var cardFanTalkShortcut: MaterialCardView
    private lateinit var layoutEmptyHome: View
    private lateinit var tvEmptyHomeTitle: TextView

    private lateinit var tvFanTalkTitle: TextView
    private lateinit var tvFanTalkSub: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupFanTalkShortcut()
        observeHomeUiState()
        observeHomeEvent()

        viewModel.loadHome()
    }

    private fun bindViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerHomeFeed)
        tvEmptyHome = view.findViewById(R.id.tvEmptyHome)
        cardFanTalkShortcut = view.findViewById(R.id.cardFanTalkShortcut)
        layoutEmptyHome = view.findViewById(R.id.layoutEmptyHome)
        tvEmptyHomeTitle = view.findViewById(R.id.tvEmptyHomeTitle)
        tvFanTalkTitle = view.findViewById(R.id.tvFanTalkTitle)
        tvFanTalkSub = view.findViewById(R.id.tvFanTalkSub)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeFeedAdapter = HomeFeedAdapter(emptyList())
        recyclerView.adapter = homeFeedAdapter
    }

    private fun setupFanTalkShortcut() {
        cardFanTalkShortcut.setOnClickListener {
            viewModel.openFavoriteGroupChat()
        }
    }

    private fun observeHomeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindHomeUi(uiState)
                }
            }
        }
    }

    private fun observeHomeEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is HomeEvent.ShowToast -> {
                            toast(event.message)
                        }

                        is HomeEvent.OpenGroupChat -> {
                            openGroupChat(
                                groupId = event.groupId,
                                roomName = event.roomName
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bindHomeUi(uiState: HomeUiState) {
        if (uiState.isLoading) {
            cardFanTalkShortcut.visibility = View.INVISIBLE
            layoutEmptyHome.visibility = View.GONE
            recyclerView.visibility = View.GONE
            return
        }

        cardFanTalkShortcut.visibility = View.VISIBLE

        tvFanTalkTitle.text = uiState.fanTalkTitle
        tvFanTalkSub.text = uiState.fanTalkSub

        homeFeedAdapter.updateList(uiState.listItems)

        if (uiState.isEmpty) {
            showEmptyState(
                title = uiState.emptyTitle,
                message = uiState.emptyMessage
            )
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState(
        title: String,
        message: String
    ) {
        layoutEmptyHome.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmptyHomeTitle.text = title
        tvEmptyHome.text = message
    }

    private fun hideEmptyState() {
        layoutEmptyHome.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun openGroupChat(
        groupId: String,
        roomName: String
    ) {
        startActivity(
            GroupChatActivity.newIntent(
                requireContext(),
                groupId,
                roomName
            )
        )
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}