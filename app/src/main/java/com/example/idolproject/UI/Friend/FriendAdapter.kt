package com.example.idolproject.UI.Friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R

class FriendAdapter(
    private val items: MutableList<Friend> = mutableListOf(),
    private val onItemClick: ((Friend) -> Unit)? = null,
    private val onDeleteClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    fun submitList(newItems: List<Friend>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.iv_profile)
        val tvNickname: TextView = itemView.findViewById(R.id.tv_nickname)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val ivBadge: ImageView = itemView.findViewById(R.id.iv_badge)
        val tvLevel: TextView = itemView.findViewById(R.id.tv_level)
        val tvFavorite: TextView = itemView.findViewById(R.id.tv_favorite)
        val tvSameFavorite: TextView = itemView.findViewById(R.id.tv_same_favorite)
        val btnDeleteFriend: ImageButton = itemView.findViewById(R.id.btn_delete_friend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)

        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val item = items[position]

        holder.tvNickname.text = item.nickname
        holder.tvStatus.text = item.statusMessage.ifBlank { "상태메시지 없음" }
        holder.tvLevel.text = "Lv.${item.level}"
        holder.tvFavorite.text = if (item.favoriteGroupName.isBlank()) {
            "최애: -"
        } else {
            "최애: ${item.favoriteGroupName}"
        }

        holder.ivProfile.setImageResource(R.drawable.person_24dp)
        holder.ivBadge.setImageResource(mapBadgeRes(item.badgeId, item.level))

        holder.tvSameFavorite.visibility = if (item.isSameFavorite) {
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }

        holder.btnDeleteFriend.setOnClickListener { anchor ->
            val menu = PopupMenu(anchor.context, anchor)

            menu.menu.add(0, MENU_DELETE, 0, "친구 삭제")

            menu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    MENU_DELETE -> {
                        onDeleteClick(item)
                        true
                    }

                    else -> false
                }
            }

            menu.show()
        }
    }

    override fun getItemCount(): Int = items.size

    private fun mapBadgeRes(badgeId: String, level: Int): Int {
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

    private fun getBadgeIdByLevel(level: Int): String {
        return when (level) {
            in 1..4 -> "bronze"
            in 5..9 -> "silver"
            in 10..14 -> "gold"
            in 15..19 -> "platinum"
            in 20..29 -> "master"
            in 30..39 -> "grandmaster"
            else -> "challenger"
        }
    }

    companion object {
        private const val MENU_DELETE = 1
    }
}