package com.example.idolproject.Drawer.Group

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R

class GroupAdapter(
    private var items: List<GroupScheduleItem>,
    private val onItemClick: ((GroupScheduleItem) -> Unit)? = null
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvType: TextView = itemView.findViewById(R.id.tv_group_schedule_type)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_group_schedule_title)
        val tvMemo: TextView = itemView.findViewById(R.id.tv_group_schedule_memo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvType.text = item.type.displayName
        holder.tvTitle.text = item.title
        holder.tvMemo.text = if (item.memo.isBlank()) {
            "${item.groupName} 활동 일정"
        } else {
            "${item.groupName} · ${item.memo}"
        }

        when (item.type) {
            GroupActivityType.FAN_SIGN -> {
                holder.tvType.setBackgroundResource(R.drawable.bg_group_type_fansign)
                holder.tvType.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }

            GroupActivityType.VARIETY -> {
                holder.tvType.setBackgroundResource(R.drawable.bg_group_type_variety)
                holder.tvType.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }

            GroupActivityType.RADIO -> {
                holder.tvType.setBackgroundResource(R.drawable.bg_group_type_radio)
                holder.tvType.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }

            GroupActivityType.MUSIC_SHOW -> {
                holder.tvType.setBackgroundResource(R.drawable.bg_group_type_music_show)
                holder.tvType.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }

            GroupActivityType.OTHER -> {
                holder.tvType.setBackgroundResource(R.drawable.bg_group_type_other)
                holder.tvType.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<GroupScheduleItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}