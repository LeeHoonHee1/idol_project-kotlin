package com.example.idolproject.Drawer.Community

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.idolproject.Drawer.Community.GroupChatActivity

class CommunityFragment : Fragment(R.layout.fragment_community_room_list) {

    private lateinit var rvChatRooms: RecyclerView
    private lateinit var tvDescription: TextView
    private lateinit var adapter: ChatRoomAdapter
    private val roomList = mutableListOf<ChatRoom>()

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var roomListener: ListenerRegistration? = null
    private var memberListener: ListenerRegistration? = null
    private var unreadListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvChatRooms = view.findViewById(R.id.rv_chat_rooms)
        tvDescription = view.findViewById(R.id.tv_community_description)

        adapter = ChatRoomAdapter(roomList) { room ->
            openChatRoom(room)
        }

        rvChatRooms.layoutManager = LinearLayoutManager(requireContext())
        rvChatRooms.adapter = adapter

        loadMyFavoriteGroupAndStartListening()

        (activity as? AppCompatActivity)?.supportActionBar?.title = "커뮤니티"
    }

    private fun loadMyFavoriteGroupAndStartListening() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userSnap ->
                val favoriteGroupId = userSnap.getString("favoriteGroupId").orEmpty()
                val favoriteGroupName = userSnap.getString("favoriteGroupName").orEmpty()

                if (favoriteGroupId.isBlank()) {
                    roomList.clear()
                    tvDescription.text = "내 페이지에서 최애 그룹을 먼저 설정해 주세요."
                    adapter.notifyDataSetChanged()
                    stopRoomListeners()
                    return@addOnSuccessListener
                }

                tvDescription.text = "내 최애 그룹 기준으로 팬톡방이 표시돼요."
                startRoomListeners(favoriteGroupId, favoriteGroupName)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "최애 그룹 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startRoomListeners(groupId: String, groupName: String) {
        val uid = auth.currentUser?.uid ?: return
        val roomRef = db.collection("group_chats").document(groupId)
        val memberRef = roomRef.collection("members").document(uid)

        stopRoomListeners()

        updateSingleRoom(
            ChatRoom(
                id = groupId,
                groupName = groupName.ifBlank { groupId },
                roomName = "${groupName.ifBlank { groupId }} 팬톡방",
                lastMessage = "팬들과 실시간으로 대화해 보세요.",
                unreadCount = 0,
                imageResId = R.drawable.person_24dp,
                lastMessageAt = 0L
            )
        )

        roomListener = roomRef.addSnapshotListener { roomSnap, e ->
            if (e != null || roomSnap == null) return@addSnapshotListener

            val current = roomList.firstOrNull() ?: return@addSnapshotListener

            val updated = current.copy(
                groupName = roomSnap.getString("groupName").orEmpty().ifBlank { current.groupName },
                roomName = roomSnap.getString("roomName").orEmpty().ifBlank { current.roomName },
                lastMessage = roomSnap.getString("lastMessage").orEmpty().ifBlank { current.lastMessage },
                lastMessageAt = roomSnap.getLong("lastMessageAt") ?: current.lastMessageAt
            )

            updateSingleRoom(updated)
        }

        memberListener = memberRef.addSnapshotListener { memberSnap, e ->
            if (e != null) return@addSnapshotListener

            val lastReadAt = memberSnap?.getLong("lastReadAt") ?: 0L

            unreadListener?.remove()
            unreadListener = roomRef.collection("messages")
                .whereGreaterThan("timestamp", lastReadAt)
                .addSnapshotListener { msgSnap, err ->
                    if (err != null || msgSnap == null) return@addSnapshotListener

                    val unreadCount = msgSnap.documents.count { doc ->
                        doc.getString("senderUid").orEmpty() != uid
                    }

                    val current = roomList.firstOrNull() ?: return@addSnapshotListener
                    val displayUnread = if (unreadCount > 99) 99 else unreadCount
                    val updated = current.copy(unreadCount = displayUnread)

                    updateSingleRoom(updated)
                }
        }
    }

    private fun updateSingleRoom(room: ChatRoom) {
        roomList.clear()
        roomList.add(room)
        adapter.notifyDataSetChanged()
    }

    private fun stopRoomListeners() {
        roomListener?.remove()
        roomListener = null

        memberListener?.remove()
        memberListener = null

        unreadListener?.remove()
        unreadListener = null
    }

    private fun openChatRoom(room: ChatRoom) {
        startActivity(
            GroupChatActivity.newIntent(
                requireContext(),
                room.id,
                room.roomName
            )
        )
    }

    override fun onResume() {
        super.onResume()
        loadMyFavoriteGroupAndStartListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRoomListeners()
    }
}