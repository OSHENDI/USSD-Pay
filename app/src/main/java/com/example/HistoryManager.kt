package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("ussdpay_history", Context.MODE_PRIVATE)

    fun getHistory(): List<HistoryEntry> {
        val jsonStr = prefs.getString("recent_numbers", "[]") ?: "[]"
        val list = mutableListOf<HistoryEntry>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(HistoryEntry(
                    number = obj.optString("number", ""),
                    name = obj.optString("name", ""),
                    amount = obj.optString("amount", "150.00"),
                    status = obj.optString("status", "COMPLETED"),
                    timestamp = obj.optString("timestamp", "Mar 12, 2024 • 10:45 AM"),
                    simName = obj.optString("simName", "SIM 1"),
                    type = obj.optString("type", "FRIEND")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Clean slate: return empty list if no dynamic history saved
        return list
    }

    fun addHistory(entry: HistoryEntry) {
        if (entry.number.isEmpty()) return
        val current = getHistory().toMutableList()
        // Deduplication removed so we store every transaction instance
        current.add(0, entry)
        
        val limited = current.take(20)
        
        val arr = JSONArray()
        for (item in limited) {
            val obj = JSONObject()
            obj.put("number", item.number)
            obj.put("name", item.name)
            obj.put("amount", item.amount)
            obj.put("status", item.status)
            obj.put("timestamp", item.timestamp)
            obj.put("simName", item.simName)
            obj.put("type", item.type)
            arr.put(obj)
        }
        
        prefs.edit().putString("recent_numbers", arr.toString()).apply()
    }
}
