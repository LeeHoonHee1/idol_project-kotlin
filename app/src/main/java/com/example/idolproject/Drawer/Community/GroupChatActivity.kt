package com.example.idolproject.Drawer.Community

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowCompat

class GroupChatActivity : AppCompatActivity() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var adapter: ChatMessageAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private var roomId: String? = null
    private var roomName: String? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var messageListener: ListenerRegistration? = null

    private val PREF_CHAT_STATE = "chat_state"
    private val KEY_OPEN_ROOM_ID = "current_open_chat_room_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_community_chat)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = true

        roomId = intent.getStringExtra(EXTRA_ROOM_ID)
        roomName = intent.getStringExtra(EXTRA_ROOM_NAME)

        rvChatMessages = findViewById(R.id.rv_chat_messages)
        val etMessage: EditText = findViewById(R.id.et_message)
        val btnSend: ImageButton = findViewById(R.id.btn_send)
        val tvChatRoomTitle: TextView = findViewById(R.id.tvChatRoomTitle)
        val layoutChatInput: View = findViewById(R.id.layout_chat_input)

        tvChatRoomTitle.text = roomName ?: "채팅방"

        ViewCompat.setOnApplyWindowInsetsListener(tvChatRoomTitle) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top + 14,
                left = view.paddingLeft,
                right = view.paddingRight,
                bottom = view.paddingBottom
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(layoutChatInput) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, imeInsets.bottom)

            view.updatePadding(
                left = view.paddingLeft,
                top = view.paddingTop,
                right = view.paddingRight,
                bottom = bottomInset
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(rvChatMessages) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                left = view.paddingLeft,
                top = view.paddingTop,
                right = view.paddingRight,
                bottom = 4
            )
            insets
        }


        adapter = ChatMessageAdapter(messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChatMessages.adapter = adapter

        listenMessages()

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            sendMessage(text)
            etMessage.text.clear()
        }
    }

    private fun listenMessages() {
        val currentRoomId = roomId ?: return
        val myUid = auth.currentUser?.uid ?: return

        messageListener?.remove()
        messageListener = db.collection("group_chats")
            .document(currentRoomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "메시지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val newList = mutableListOf<ChatMessage>()

                snapshots?.documents?.forEach { doc ->
                    val senderUid = doc.getString("senderUid").orEmpty()

                    val item = ChatMessage(
                        id = doc.getString("id").orEmpty(),
                        roomId = doc.getString("roomId").orEmpty(),
                        senderUid = senderUid,
                        senderName = doc.getString("senderName").orEmpty(),
                        message = doc.getString("message").orEmpty(),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isMe = senderUid == myUid
                    )
                    newList.add(item)
                }

                messageList.clear()
                messageList.addAll(newList)
                adapter.notifyDataSetChanged()

                if (messageList.isNotEmpty()) {
                    rvChatMessages.scrollToPosition(messageList.size - 1)
                }

                markAsRead()
            }
    }

    private fun sendMessage(text: String) {
        val currentRoomId = roomId ?: return
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { userSnap ->
                val nickname = userSnap.getString("nickname").orEmpty().ifBlank { "익명" }

                val messageDoc = db.collection("group_chats")
                    .document(currentRoomId)
                    .collection("messages")
                    .document()

                val now = System.currentTimeMillis()

                val messageData = hashMapOf(
                    "id" to messageDoc.id,
                    "roomId" to currentRoomId,
                    "senderUid" to user.uid,
                    "senderName" to nickname,
                    "message" to text,
                    "timestamp" to now
                )

                val roomData = hashMapOf(
                    "groupName" to (roomName ?: currentRoomId),
                    "roomName" to (roomName ?: "오픈채팅"),
                    "lastMessage" to text,
                    "lastMessageAt" to now,
                    "lastSenderUid" to user.uid,
                    "lastSenderName" to nickname
                )

                messageDoc.set(messageData)
                    .addOnSuccessListener {
                        db.collection("group_chats")
                            .document(currentRoomId)
                            .set(roomData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnFailureListener {
                                Toast.makeText(this, "방 정보 갱신에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markAsRead() {
        val currentRoomId = roomId ?: return
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { userSnap ->
                val nickname = userSnap.getString("nickname").orEmpty().ifBlank { "익명" }

                val memberData = hashMapOf(
                    "nickname" to nickname,
                    "lastReadAt" to System.currentTimeMillis()
                )

                db.collection("group_chats")
                    .document(currentRoomId)
                    .collection("members")
                    .document(user.uid)
                    .set(memberData, com.google.firebase.firestore.SetOptions.merge())
            }
    }

    private fun saveCurrentOpenRoomId() {
        val currentRoomId = roomId ?: return
        getSharedPreferences(PREF_CHAT_STATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_OPEN_ROOM_ID, currentRoomId)
            .apply()
    }

    private fun clearCurrentOpenRoomId() {
        val prefs = getSharedPreferences(PREF_CHAT_STATE, Context.MODE_PRIVATE)
        val savedRoomId = prefs.getString(KEY_OPEN_ROOM_ID, null)
        if (savedRoomId == roomId) {
            prefs.edit().remove(KEY_OPEN_ROOM_ID).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        saveCurrentOpenRoomId()
    }

    override fun onPause() {
        super.onPause()
        clearCurrentOpenRoomId()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearCurrentOpenRoomId()
        messageListener?.remove()
        messageListener = null
    }

    companion object {
        private const val EXTRA_ROOM_ID = "room_id"
        private const val EXTRA_ROOM_NAME = "room_name"

        fun newIntent(context: Context, roomId: String, roomName: String): Intent {
            return Intent(context, GroupChatActivity::class.java).apply {
                putExtra(EXTRA_ROOM_ID, roomId)
                putExtra(EXTRA_ROOM_NAME, roomName)
            }
        }
    }
}