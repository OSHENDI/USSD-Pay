package com.example

data class SimEntry(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val phoneNumber: String? = null
)
