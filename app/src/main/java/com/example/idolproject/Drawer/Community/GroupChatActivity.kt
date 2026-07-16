package com.example.idolproject.Drawer.Community

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupChatActivity : AppCompatActivity() {

    private val viewModel: GroupChatViewModel by viewModels()

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var adapter: ChatMessageAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvChatRoomTitle: TextView
    private lateinit var layoutChatInput: View

    private var roomId: String? = null
    private var roomName: String? = null

    private val PREF_CHAT_STATE = "chat_state"
    private val KEY_OPEN_ROOM_ID = "current_open_chat_room_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_community_chat)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)
            ?.isAppearanceLightStatusBars = true

        roomId = intent.getStringExtra(EXTRA_ROOM_ID)
        roomName = intent.getStringExtra(EXTRA_ROOM_NAME)

        bindViews()
        setupInsets()
        setupRecyclerView()
        setupInput()
        observeChatUiState()
        observeChatEvent()

        viewModel.start(
            roomId = roomId,
            roomName = roomName
        )
    }

    private fun bindViews() {
        rvChatMessages = findViewById(R.id.rv_chat_messages)
        etMessage = findViewById(R.id.et_message)
        btnSend = findViewById(R.id.btn_send)
        tvChatRoomTitle = findViewById(R.id.tvChatRoomTitle)
        layoutChatInput = findViewById(R.id.layout_chat_input)
    }

    private fun setupInsets() {
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
            view.updatePadding(
                left = view.paddingLeft,
                top = view.paddingTop,
                right = view.paddingRight,
                bottom = 4
            )
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatMessageAdapter(messageList)

        rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        rvChatMessages.adapter = adapter
    }

    private fun setupInput() {
        btnSend.isEnabled = false

        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                viewModel.onInputChanged(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        btnSend.setOnClickListener {
            viewModel.sendCurrentMessage()
        }
    }

    private fun observeChatUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindChatUi(uiState)
                }
            }
        }
    }

    private fun observeChatEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is ChatEvent.ShowToast -> {
                            Toast.makeText(
                                this@GroupChatActivity,
                                event.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        ChatEvent.ScrollToBottom -> {
                            scrollToBottom()
                        }

                        ChatEvent.ClearInput -> {
                            if (etMessage.text.isNotEmpty()) {
                                etMessage.text.clear()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindChatUi(uiState: ChatUiState) {
        tvChatRoomTitle.text = uiState.roomName.ifBlank { "채팅방" }
        btnSend.isEnabled = uiState.isSendEnabled

        messageList.clear()
        messageList.addAll(uiState.messages)
        adapter.notifyDataSetChanged()
    }

    private fun scrollToBottom() {
        if (messageList.isNotEmpty()) {
            rvChatMessages.scrollToPosition(messageList.size - 1)
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
            prefs.edit()
                .remove(KEY_OPEN_ROOM_ID)
                .apply()
        }
    }

    override fun onResume() {
        super.onResume()
        saveCurrentOpenRoomId()
        viewModel.markAsRead()
    }

    override fun onPause() {
        super.onPause()
        clearCurrentOpenRoomId()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearCurrentOpenRoomId()
    }

    companion object {
        private const val EXTRA_ROOM_ID = "room_id"
        private const val EXTRA_ROOM_NAME = "room_name"

        fun newIntent(
            context: Context,
            roomId: String,
            roomName: String
        ): Intent {
            return Intent(context, GroupChatActivity::class.java).apply {
                putExtra(EXTRA_ROOM_ID, roomId)
                putExtra(EXTRA_ROOM_NAME, roomName)
            }
        }
    }
}