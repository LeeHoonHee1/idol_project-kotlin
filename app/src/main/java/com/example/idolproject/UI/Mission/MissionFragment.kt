package com.example.idolproject.UI.Mission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.idolproject.R
import com.example.idolproject.databinding.FragmentMissionBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MissionFragment : Fragment() {

    private var _binding: FragmentMissionBinding? = null
    private val binding get() = _binding!!

    private lateinit var ivMissionBadge: ImageView
    private lateinit var tvMissionLevel: TextView
    private lateinit var tvMissionExp: TextView
    private lateinit var progressMissionExp: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMissionPager()
        bindGrowthViews()
        startMissionUserListener()
    }

    private fun setupMissionPager() {
        val adapter = MissionPagerAdapter(this)
        binding.vpMission.adapter = adapter

        TabLayoutMediator(binding.tabMission, binding.vpMission) { tab, position ->
            tab.text = when (position) {
                0 -> "일일 미션"
                1 -> "주간 미션"
                else -> ""
            }
        }.attach()
    }

    private fun bindGrowthViews() {
        ivMissionBadge = binding.ivMissionBadge
        tvMissionLevel = binding.tvMissionLevel
        tvMissionExp = binding.tvMissionExp
        progressMissionExp = binding.progressMissionExp
    }

    private fun startMissionUserListener() {
        val uid = auth.currentUser?.uid ?: return

        userListener?.remove()

        userListener = db.collection("users")
            .document(uid)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null || !snap.exists()) {
                    return@addSnapshotListener
                }

                val level = (snap.getLong("level") ?: 1L).toInt()
                val exp = (snap.getLong("exp") ?: 0L).toInt()

                val needExp = 100
                val percent = ((exp * 100) / needExp).coerceIn(0, 100)

                val badgeIdFromDb = snap.getString("badgeId").orEmpty()
                val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
                    getBadgeIdByLevel(level)
                } else {
                    badgeIdFromDb
                }

                tvMissionLevel.text = "Lv.$level"
                tvMissionExp.text = "EXP $exp / $needExp ($percent%)"

                progressMissionExp.max = 100
                progressMissionExp.progress = percent

                ivMissionBadge.setImageResource(mapBadgeRes(finalBadgeId))
            }
    }

    private fun mapBadgeRes(badgeId: String): Int {
        return when (badgeId) {
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

    override fun onDestroyView() {
        userListener?.remove()
        userListener = null
        _binding = null
        super.onDestroyView()
    }
}