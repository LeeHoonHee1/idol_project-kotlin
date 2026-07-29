package com.example.idolproject.Drawer.Event

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EventFragment : Fragment(R.layout.fragment_event) {

    private val viewModel: EventViewModel by viewModels()

    private lateinit var eventAdapter: EventAdapter

    private lateinit var rvEvents: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var btnAll: MaterialButton
    private lateinit var btnActive: MaterialButton
    private lateinit var btnFinished: MaterialButton

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }

    private fun bindViews(view: View) {
        rvEvents = view.findViewById(R.id.rv_events)
        tvEmpty = view.findViewById(R.id.tv_event_empty)

        btnAll = view.findViewById(R.id.btn_event_all)
        btnActive = view.findViewById(R.id.btn_event_active)
        btnFinished = view.findViewById(R.id.btn_event_finished)
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter()

        rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupClickListeners() {
        btnAll.setOnClickListener {
            viewModel.selectFilter(EventFilter.ALL)
        }

        btnActive.setOnClickListener {
            viewModel.selectFilter(EventFilter.ACTIVE)
        }

        btnFinished.setOnClickListener {
            viewModel.selectFilter(EventFilter.FINISHED)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindUiState(uiState)
                }
            }
        }
    }

    private fun bindUiState(uiState: EventUiState) {
        when (uiState) {
            EventUiState.Loading -> {
                rvEvents.visibility = View.GONE
                tvEmpty.visibility = View.GONE
            }

            is EventUiState.Success -> {
                eventAdapter.submitList(uiState.events)
                rvEvents.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
            }

            is EventUiState.Empty -> {
                eventAdapter.submitList(emptyList())
                rvEvents.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = uiState.message
            }

            is EventUiState.Error -> {
                eventAdapter.submitList(emptyList())
                rvEvents.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "이벤트를 불러오지 못했어요"
                Toast.makeText(requireContext(), uiState.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}