package com.example.idolproject.UI.Ranking

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.idolproject.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupRankingFragment : Fragment(R.layout.fragment_ranking_group) {

    private val viewModel: RankingViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var rankingAdapter: GroupRankingAdapter

    private lateinit var ivFirstGroup: ImageView
    private lateinit var ivSecondGroup: ImageView
    private lateinit var ivThirdGroup: ImageView

    private lateinit var tvFirstName: TextView
    private lateinit var tvSecondName: TextView
    private lateinit var tvThirdName: TextView

    private lateinit var tvFirstLike: TextView
    private lateinit var tvSecondLike: TextView
    private lateinit var tvThirdLike: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupRecyclerView()
        observeGroupRanking()
    }

    private fun bindViews(view: View) {
        ivFirstGroup = view.findViewById(R.id.iv_first_group)
        ivSecondGroup = view.findViewById(R.id.iv_second_group)
        ivThirdGroup = view.findViewById(R.id.iv_third_group)

        tvFirstName = view.findViewById(R.id.tv_first_name)
        tvSecondName = view.findViewById(R.id.tv_second_name)
        tvThirdName = view.findViewById(R.id.tv_third_name)

        tvFirstLike = view.findViewById(R.id.tv_first_like)
        tvSecondLike = view.findViewById(R.id.tv_second_like)
        tvThirdLike = view.findViewById(R.id.tv_third_like)

        recyclerView = view.findViewById(R.id.recycler_group_rank)
    }

    private fun setupRecyclerView() {
        rankingAdapter = GroupRankingAdapter(emptyList())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = rankingAdapter
    }

    private fun observeGroupRanking() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.groupRankingUiState.collect { uiState ->
                    when (uiState) {
                        GroupRankingUiState.Loading -> {
                            bindPodium(emptyList())
                            rankingAdapter.updateList(emptyList())
                        }

                        is GroupRankingUiState.Success -> {
                            bindPodium(uiState.top3)
                            rankingAdapter.updateList(uiState.others)
                        }

                        is GroupRankingUiState.Error -> {
                            bindPodium(emptyList())
                            rankingAdapter.updateList(emptyList())
                        }
                    }
                }
            }
        }
    }

    private fun bindPodium(fullList: List<GroupRank>) {
        bindPodiumItem(
            item = fullList.getOrNull(0),
            nameView = tvFirstName,
            likeView = tvFirstLike,
            imageView = ivFirstGroup,
            emptyName = "1등 대기"
        )

        bindPodiumItem(
            item = fullList.getOrNull(1),
            nameView = tvSecondName,
            likeView = tvSecondLike,
            imageView = ivSecondGroup,
            emptyName = "2등 대기"
        )

        bindPodiumItem(
            item = fullList.getOrNull(2),
            nameView = tvThirdName,
            likeView = tvThirdLike,
            imageView = ivThirdGroup,
            emptyName = "3등 대기"
        )
    }

    private fun bindPodiumItem(
        item: GroupRank?,
        nameView: TextView,
        likeView: TextView,
        imageView: ImageView,
        emptyName: String
    ) {
        if (item == null) {
            nameView.text = emptyName
            likeView.text = "0"
            imageView.setImageResource(R.drawable.person_24dp)
            return
        }

        nameView.text = item.groupName.ifBlank { "이름 없음" }
        likeView.text = item.likeCount.toString()

        if (item.imageUrl.isBlank()) {
            imageView.setImageResource(R.drawable.person_24dp)
        } else {
            imageView.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
            }
        }
    }
}