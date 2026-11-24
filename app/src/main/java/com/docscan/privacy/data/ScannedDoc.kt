package com.docscan.privacy.data

data class ScannedDoc(
    val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val currency: String,
    val date: String,
    val category: String, // GROCERIES, DINING, FUEL, TECH, PHARMACY, GENERAL
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)
