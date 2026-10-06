package com.example

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("ussdpay_history", Context.MODE_PRIVATE)
    private val database = HistoryDatabase.getDatabase(context)
    private val historyDao = database.historyDao()

    fun observeHistory(): Flow<List<HistoryEntry>> {
        return historyDao.observeAllHistory().map { entities ->
            entities.map {
                HistoryEntry(
                    id = it.id,
                    number = it.number,
                    name = it.name,
                    amount = it.amount,
                    status = it.status,
                    timestamp = it.timestamp,
                    simName = it.simName,
                    type = it.type
                )
            }
        }
    }

    suspend fun getHistory(): List<HistoryEntry> {
        migrateIfNeeded()
        
        // Load latest 500 records
        val entities = historyDao.getAllHistory()
        return entities.map {
            HistoryEntry(
                id = it.id,
                number = it.number,
                name = it.name,
                amount = it.amount,
                status = it.status,
                timestamp = it.timestamp,
                simName = it.simName,
                type = it.type
            )
        }
    }

    suspend fun addHistory(entry: HistoryEntry) {
        if (entry.number.isEmpty()) return
        migrateIfNeeded()
        
        val entity = HistoryEntity(
            number = entry.number,
            name = entry.name,
            amount = entry.amount,
            status = entry.status,
            timestamp = entry.timestamp,
            simName = entry.simName,
            type = entry.type
        )
        historyDao.insert(entity)
    }

    private suspend fun migrateIfNeeded() {
        if (prefs.contains("recent_numbers")) {
            val jsonStr = prefs.getString("recent_numbers", "[]") ?: "[]"
            val list = mutableListOf<HistoryEntity>()
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        HistoryEntity(
                            number = obj.optString("number", ""),
                            name = obj.optString("name", ""),
                            amount = obj.optString("amount", "150.00"),
                            status = obj.optString("status", "COMPLETED"),
                            timestamp = obj.optString("timestamp", "Mar 12, 2024 • 10:45 AM"),
                            simName = obj.optString("simName", "SIM 1"),
                            type = obj.optString("type", "FRIEND")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    // Reverse list to insert oldest first so that primary key autogenerate preserves order
                    historyDao.insertAll(list.reversed())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Clear the old shared preferences to prevent migrating again
            prefs.edit().remove("recent_numbers").apply()
        }
    }
}
