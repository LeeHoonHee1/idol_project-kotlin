package com.example.idolproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val reward: String,
    val status: String
)