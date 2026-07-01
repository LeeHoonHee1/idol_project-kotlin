package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendRequestsFragment : Fragment(R.layout.fragment_friend_requests) {

    private val viewModel: FriendViewModel by viewModels()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: FriendRequestsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rv_requests)

        adapter = FriendRequestsAdapter(
            onAccept = { item ->
                viewModel.acceptRequest(item.senderUid)
            },
            onReject = { item ->
                viewModel.rejectRequest(item.senderUid)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        observeFriendRequestsUiState()
        observeFriendEvent()

        viewModel.markAllPendingChecked()
        viewModel.startObserveFriendRequests()
    }

    private fun observeFriendRequestsUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friendRequestsUiState.collect { uiState ->
                    bindFriendRequestsUi(uiState)
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
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_container,
                                    FriendProfileFragment.newInstance(event.friendUid)
                                )
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
            }
        }
    }

    private fun bindFriendRequestsUi(uiState: FriendRequestsUiState) {
        adapter.submitList(uiState.requests)

        if (uiState.errorMessage != null) {
            toast(uiState.errorMessage)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}