package com.example

data class HistoryEntry(
    val id: Int = 0,
    val number: String,
    val name: String = "",
    val amount: String = "150.00",
    val status: String = "COMPLETED", // COMPLETED, PENDING, FAILED
    val timestamp: String = "Mar 12, 2024 • 10:45 AM",
    val simName: String = "SIM 1",
    val type: String = "FRIEND" // FRIEND or MERCHANT
)

