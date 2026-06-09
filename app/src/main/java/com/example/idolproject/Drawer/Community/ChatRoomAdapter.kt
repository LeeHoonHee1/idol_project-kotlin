package com.example.idolproject.Drawer.Community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R

class ChatRoomAdapter(
    private val items: List<ChatRoom>,
    private val onItemClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    inner class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivGroupImage: ImageView = itemView.findViewById(R.id.iv_group_image)
        private val tvRoomName: TextView = itemView.findViewById(R.id.tv_room_name)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tv_last_message)
        private val tvUnreadBadge: TextView = itemView.findViewById(R.id.tv_unread_badge)

        fun bind(room: ChatRoom) {
            ivGroupImage.setImageResource(room.imageResId)

            tvRoomName.text = room.roomName.ifBlank { "${room.groupName} 팬톡방" }

            tvLastMessage.text = room.lastMessage.ifBlank {
                "팬들과 실시간으로 대화해 보세요."
            }

            if (room.unreadCount > 0) {
                tvUnreadBadge.visibility = View.VISIBLE
                tvUnreadBadge.text = if (room.unreadCount > 99) {
                    "99+"
                } else {
                    room.unreadCount.toString()
                }
            } else {
                tvUnreadBadge.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(room)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_room, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}