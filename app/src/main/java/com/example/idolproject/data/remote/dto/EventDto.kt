package com.example.idolproject.data.remote.dto

data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val reward: String,
    val status: String
)