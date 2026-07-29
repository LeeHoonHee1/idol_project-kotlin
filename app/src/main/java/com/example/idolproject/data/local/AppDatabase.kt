package com.example.idolproject.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.idolproject.data.local.dao.EventDao
import com.example.idolproject.data.local.entity.EventEntity

@Database(
    entities = [
        EventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}