package com.example.idolproject.UI.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.android.material.card.MaterialCardView

class HomeFeedAdapter(
    private var items: List<HomeListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FEED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HomeListItem.Header -> TYPE_HEADER
            is HomeListItem.Feed -> TYPE_FEED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_home_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_FEED -> {
                val view = inflater.inflate(R.layout.item_home_feed, parent, false)
                HomeFeedViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val listItem = items[position]) {
            is HomeListItem.Header -> (holder as HeaderViewHolder).bind(listItem)
            is HomeListItem.Feed -> (holder as HomeFeedViewHolder).bind(listItem.item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<HomeListItem>) {
        items = newList
        notifyDataSetChanged()
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHomeHeader: TextView = itemView.findViewById(R.id.tvHomeHeader)
        private val tvHomeHeaderSub: TextView = itemView.findViewById(R.id.tvHomeHeaderSub)
        private val viewHomeHeaderLine: View = itemView.findViewById(R.id.viewHomeHeaderLine)

        fun bind(item: HomeListItem.Header) {
            if (item.isTodaySection) {
                tvHomeHeader.text = "오늘 일정"
                tvHomeHeaderSub.text = "지금 확인하면 좋은 일정"
                tvHomeHeader.alpha = 1.0f
                tvHomeHeaderSub.alpha = 0.95f
                viewHomeHeaderLine.alpha = 1.0f
            } else {
                tvHomeHeader.text = "다가오는 일정"
                tvHomeHeaderSub.text = "곧 예정된 일정"
                tvHomeHeader.alpha = 0.92f
                tvHomeHeaderSub.alpha = 0.82f
                viewHomeHeaderLine.alpha = 0.72f
            }
        }
    }

    class HomeFeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardHomeFeed: MaterialCardView = itemView.findViewById(R.id.cardHomeFeed)
        private val imgGroupProfile: ImageView = itemView.findViewById(R.id.imgGroupProfile)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val imgContent: ImageView = itemView.findViewById(R.id.imgContent)
        private val tvTag: TextView = itemView.findViewById(R.id.tvTag)

        fun bind(item: HomeFeedItem) {
            tvGroupName.text = item.groupName
            tvTime.text = item.time
            tvTime.alpha = 0.72f

            tvContent.text = item.content.ifBlank { "예정된 일정 정보를 확인해 보세요." }
            tvContent.maxLines = 2

            imgGroupProfile.setImageResource(R.drawable.person_24dp)

            if (item.imageResId != null) {
                imgContent.visibility = View.VISIBLE
                imgContent.setImageResource(item.imageResId)
            } else {
                imgContent.visibility = View.GONE
                imgContent.setImageDrawable(null)
            }

            tvTag.text = if (item.isToday) "오늘" else "예정"

            if (item.isToday) {
                tvTag.setTextColor(itemView.context.getColor(R.color.white))
                tvTag.setBackgroundResource(R.drawable.bg_home_tag_today)
                cardHomeFeed.strokeColor = itemView.context.getColor(R.color.lavender)
            } else {
                tvTag.setTextColor(itemView.context.getColor(R.color.lavender))
                tvTag.setBackgroundResource(R.drawable.bg_home_tag_upcoming)
                cardHomeFeed.strokeColor = itemView.context.getColor(R.color.very_light_lavender)
            }

            cardHomeFeed.strokeWidth = dpToPx(1)
        }

        private fun dpToPx(dp: Int): Int {
            val density = itemView.context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }
}