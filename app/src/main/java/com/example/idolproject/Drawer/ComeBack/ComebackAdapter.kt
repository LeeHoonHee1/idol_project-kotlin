package com.example.idolproject.Drawer.ComeBack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ComebackAdapter(
    private val items: MutableList<ComebackItem> = mutableListOf(),
    private val onItemClick: ((ComebackItem) -> Unit)? = null
) : RecyclerView.Adapter<ComebackAdapter.ComebackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComebackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comeback, parent, false)
        return ComebackViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComebackViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ComebackItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ComebackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDday: TextView = itemView.findViewById(R.id.tv_comeback_dday)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tv_group_name)
        private val tvAlbumTitle: TextView = itemView.findViewById(R.id.tv_album_title)
        private val tvSubInfo: TextView = itemView.findViewById(R.id.tv_comeback_sub_info)
        private val tvNote: TextView = itemView.findViewById(R.id.tv_comeback_note)

        fun bind(item: ComebackItem) {
            tvDday.text = makeDdayText(item.date)
            tvGroupName.text = item.groupName
            tvAlbumTitle.text = item.title

            tvSubInfo.text = "${formatDisplayDate(item.date)} · 컴백 일정"

            if (item.memo.isBlank()) {
                tvNote.visibility = View.GONE
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = item.memo
            }
        }

        private fun makeDdayText(date: CalendarDay): String {
            val today = LocalDate.now()
            val comebackDate = LocalDate.of(date.year, date.month, date.day)
            val diff = ChronoUnit.DAYS.between(today, comebackDate).toInt()

            return when {
                diff == 0 -> "D-DAY"
                diff > 0 -> "D-$diff"
                else -> "D+${kotlin.math.abs(diff)}"
            }
        }

        private fun formatDisplayDate(date: CalendarDay): String {
            return String.format("%04d.%02d.%02d", date.year, date.month, date.day)
        }
    }
}