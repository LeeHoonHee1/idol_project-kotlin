package com.example.idolproject.UI.Ranking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.idolproject.R

class GroupRankingAdapter(
    private var items: List<GroupRank>
) : RecyclerView.Adapter<GroupRankingAdapter.GroupRankViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupRankViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking_group, parent, false)
        return GroupRankViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupRankViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<GroupRank>) {
        items = newItems
        notifyDataSetChanged()
    }

    class GroupRankViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRank: TextView = itemView.findViewById(R.id.tv_rank)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tv_group_name)
        private val tvGroupSub: TextView = itemView.findViewById(R.id.tv_group_sub)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tv_like_count)
        private val ivGroup: ImageView = itemView.findViewById(R.id.iv_group)

        fun bind(item: GroupRank, position: Int) {
            val realRank = position + 4

            tvRank.text = realRank.toString()
            tvGroupName.text = item.groupName.ifBlank { "이름 없음" }
            tvGroupSub.text = "팬들이 선택한 그룹"
            tvLikeCount.text = item.likeCount.toString()

            if (item.imageUrl.isBlank()) {
                ivGroup.setImageResource(R.drawable.person_24dp)
            } else {
                ivGroup.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.person_24dp)
                    error(R.drawable.person_24dp)
                }
            }
        }
    }
}