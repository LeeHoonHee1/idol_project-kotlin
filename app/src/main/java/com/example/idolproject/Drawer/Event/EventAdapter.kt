package com.example.idolproject.Drawer.Event

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R

class EventAdapter : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val items = mutableListOf<EventItem>()

    fun submitList(newItems: List<EventItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class EventViewHolder(
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
    ) {
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_event_status)
        private val tvReward: TextView = itemView.findViewById(R.id.tv_event_reward)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_event_item_title)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_event_item_desc)
        private val tvPeriod: TextView = itemView.findViewById(R.id.tv_event_period)

        fun bind(item: EventItem) {
            tvStatus.text = item.status.label
            tvReward.text = item.reward
            tvTitle.text = item.title
            tvDescription.text = item.description
            tvPeriod.text = "${item.startDate} ~ ${item.endDate}"
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {
        return EventViewHolder(parent)
    }

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}