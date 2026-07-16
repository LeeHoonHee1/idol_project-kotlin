package com.example.idolproject.Drawer.Community

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
class CommunityFragment : Fragment(R.layout.fragment_community_room_list) {

    private val viewModel: CommunityViewModel by viewModels()

    private lateinit var rvChatRooms: RecyclerView
    private lateinit var tvDescription: TextView
    private lateinit var adapter: ChatRoomAdapter

    private val roomList = mutableListOf<ChatRoom>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        observeCommunityUiState()
        observeCommunityEvent()

        (activity as? AppCompatActivity)?.supportActionBar?.title = "커뮤니티"

        viewModel.start()
    }

    private fun bindViews(view: View) {
        rvChatRooms = view.findViewById(R.id.rv_chat_rooms)
        tvDescription = view.findViewById(R.id.tv_community_description)
    }

    private fun setupRecyclerView() {
        adapter = ChatRoomAdapter(roomList) { room ->
            viewModel.openChatRoom(room)
        }

        rvChatRooms.layoutManager = LinearLayoutManager(requireContext())
        rvChatRooms.adapter = adapter
    }

    private fun observeCommunityUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindCommunityUi(uiState)
                }
            }
        }
    }

    private fun observeCommunityEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is CommunityEvent.ShowToast -> {
                            Toast.makeText(
                                requireContext(),
                                event.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is CommunityEvent.OpenChatRoom -> {
                            openChatRoom(
                                roomId = event.roomId,
                                roomName = event.roomName
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bindCommunityUi(uiState: CommunityUiState) {
        tvDescription.text = uiState.descriptionText

        roomList.clear()
        roomList.addAll(uiState.chatRooms)
        adapter.notifyDataSetChanged()

        rvChatRooms.visibility = if (uiState.chatRooms.isEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun openChatRoom(
        roomId: String,
        roomName: String
    ) {
        startActivity(
            GroupChatActivity.newIntent(
                requireContext(),
                roomId,
                roomName
            )
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}