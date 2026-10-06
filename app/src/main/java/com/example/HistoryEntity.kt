package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String,
    val amount: String,
    val status: String,
    val timestamp: String,
    val simName: String,
    val type: String
)
