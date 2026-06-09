package com.example.idolproject.UI.Ranking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.idolproject.R

class UserRankingAdapter(
    private var items: List<UserRank>,
    private val myUid: String? = null
) : RecyclerView.Adapter<UserRankingAdapter.UserRankViewHolder>() {

    inner class UserRankViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRankNumber: TextView = itemView.findViewById(R.id.tv_rank_number)
        val imgProfile: ImageView = itemView.findViewById(R.id.img_user_profile)
        val tvNickname: TextView = itemView.findViewById(R.id.tv_user_nickname)
        val tvSubInfo: TextView = itemView.findViewById(R.id.tv_user_sub_info)
        val imgUserBadge: ImageView = itemView.findViewById(R.id.img_user_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRankViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking_user, parent, false)
        return UserRankViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: UserRankViewHolder, position: Int) {
        val item = items[position]
        val realRank = position + 4

        holder.tvRankNumber.text = realRank.toString()

        val isMe = item.uid == myUid
        holder.tvNickname.text = if (isMe) {
            "${item.nickname}  · 나"
        } else {
            item.nickname
        }

        holder.tvSubInfo.text = "Lv.${item.level} · EXP ${item.exp}"

        if (item.profileImageUrl.isBlank()) {
            holder.imgProfile.setImageResource(R.drawable.person_24dp)
        } else {
            holder.imgProfile.load(item.profileImageUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
            }
        }

        holder.imgUserBadge.setImageResource(
            mapBadgeRes(
                badgeId = item.badgeId,
                level = item.level
            )
        )
    }

    fun updateList(newItems: List<UserRank>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun mapBadgeRes(badgeId: String, level: Long): Int {
        val finalBadgeId = if (badgeId.isBlank() || badgeId == "default") {
            getBadgeIdByLevel(level)
        } else {
            badgeId
        }

        return when (finalBadgeId) {
            "bronze" -> R.drawable.ic_badge_bronze
            "silver" -> R.drawable.ic_badge_silver
            "gold" -> R.drawable.ic_badge_gold
            "platinum" -> R.drawable.ic_badge_platinum
            "master" -> R.drawable.ic_badge_master
            "grandmaster" -> R.drawable.ic_badge_grandmaster
            "challenger" -> R.drawable.ic_badge_challenger
            else -> R.drawable.ic_badge_bronze
        }
    }

    private fun getBadgeIdByLevel(level: Long): String {
        return when (level) {
            in 1L..4L -> "bronze"
            in 5L..9L -> "silver"
            in 10L..14L -> "gold"
            in 15L..19L -> "platinum"
            in 20L..29L -> "master"
            in 30L..39L -> "grandmaster"
            else -> "challenger"
        }
    }
}