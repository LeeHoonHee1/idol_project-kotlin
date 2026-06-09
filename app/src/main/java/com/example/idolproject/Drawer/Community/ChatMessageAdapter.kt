package com.example.idolproject.Drawer.Community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessageAdapter(
    private val items: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_OTHER = 0
        private const val VIEW_TYPE_ME = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isMe) VIEW_TYPE_ME else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ME) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_right, parent, false)
            MeViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_left, parent, false)
            OtherViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        when (holder) {
            is MeViewHolder -> holder.bind(message)
            is OtherViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = items.size

    fun addMessage(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("a h:mm", Locale.KOREA)
        return sdf.format(Date(timestamp))
    }

    inner class OtherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNickname: TextView = itemView.findViewById(R.id.tv_nickname)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)

        fun bind(msg: ChatMessage) {
            tvNickname.text = msg.senderName
            tvMessage.text = msg.message
            tvTime.text = formatTime(msg.timestamp)
        }
    }

    inner class MeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNickname: TextView = itemView.findViewById(R.id.tv_nickname)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)

        fun bind(msg: ChatMessage) {
            tvNickname.text = "나"
            tvMessage.text = msg.message
            tvTime.text = formatTime(msg.timestamp)
        }
    }
}