package com.example.idolproject.UI.Friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.idolproject.R
import com.google.android.material.button.MaterialButton

data class FriendRequestItem(
    val senderUid: String,
    val senderNickname: String,
    val statusMessage: String = "상태메시지 없음",
    val level: Int = 1,
    val badgeId: String = "bronze",
    val favoriteGroupId: String = "",
    val favoriteGroupName: String = "-",
    val photoUrl: String? = null,
    val isSameFavorite: Boolean = false
)

class FriendRequestsAdapter(
    private val onAccept: (FriendRequestItem) -> Unit,
    private val onReject: (FriendRequestItem) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.VH>() {

    private val items = mutableListOf<FriendRequestItem>()

    fun submitList(newItems: List<FriendRequestItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.iv_request_profile)
        val tvSender: TextView = itemView.findViewById(R.id.tv_sender)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_request_status)
        val ivBadge: ImageView = itemView.findViewById(R.id.iv_request_badge)
        val tvLevel: TextView = itemView.findViewById(R.id.tv_request_level)
        val tvFavorite: TextView = itemView.findViewById(R.id.tv_request_favorite)
        val tvSameFavorite: TextView = itemView.findViewById(R.id.tv_request_same_favorite)
        val btnAccept: MaterialButton = itemView.findViewById(R.id.btn_accept)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)

        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvSender.text = item.senderNickname
        holder.tvStatus.text = item.statusMessage.ifBlank { "상태메시지 없음" }
        holder.tvLevel.text = "Lv.${item.level}"
        holder.tvFavorite.text = if (item.favoriteGroupName.isBlank() || item.favoriteGroupName == "-") {
            "최애: -"
        } else {
            "최애: ${item.favoriteGroupName}"
        }

        holder.ivBadge.setImageResource(mapBadgeRes(item.badgeId, item.level))

        if (item.photoUrl.isNullOrBlank()) {
            holder.ivProfile.setImageResource(R.drawable.person_24dp)
        } else {
            holder.ivProfile.load(item.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
            }
        }

        holder.tvSameFavorite.visibility = if (item.isSameFavorite) {
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.btnAccept.setOnClickListener {
            onAccept(item)
        }

        holder.btnReject.setOnClickListener {
            onReject(item)
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
}