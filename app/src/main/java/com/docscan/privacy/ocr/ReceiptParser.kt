package com.docscan.privacy.ocr

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object ReceiptParser {

    data class ParsedReceipt(
        val title: String,
        val totalAmount: Double,
        val currency: String,
        val date: String,
        val category: String,
        val fullText: String
    )

    fun parse(ocrText: String): ParsedReceipt {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val currency = detectCurrency(ocrText)
        val totalAmount = extractTotalAmount(lines)
        val date = extractDate(ocrText)
        val title = extractTitle(lines)
        val category = classifyCategory(ocrText)

        return ParsedReceipt(
            title = title,
            totalAmount = totalAmount,
            currency = currency,
            date = date,
            category = category,
            fullText = ocrText
        )
    }

    private fun detectCurrency(text: String): String {
        return when {
            text.contains("₹") || text.contains("INR") || text.contains("Rs.") -> "₹"
            text.contains("€") || text.contains("EUR") -> "€"
            text.contains("£") || text.contains("GBP") -> "£"
            else -> "$"
        }
    }

    private fun extractTotalAmount(lines: List<String>): Double {
        val totalKeywords = listOf("GRAND TOTAL", "TOTAL DUE", "BALANCE DUE", "AMOUNT DUE", "NET AMOUNT", "FINAL TOTAL", "TOTAL")
        val amountPattern = Pattern.compile("""(?i)(?:total|amount|due|balance)?\s*[:$€£₹]?\s*([0-9]{1,4}(?:[.,][0-9]{2}))""")

        // 1. First pass: look specifically for lines with "TOTAL" keyword
        for (line in lines.reversed()) {
            val upper = line.uppercase()
            for (kw in totalKeywords) {
                if (upper.contains(kw)) {
                    val matcher = amountPattern.matcher(line)
                    if (matcher.find()) {
                        val numStr = matcher.group(1)?.replace(",", ".")
                        val parsed = numStr?.toDoubleOrNull()
                        if (parsed != null && parsed > 0) return parsed
                    }
                }
            }
        }

        // 2. Second pass: fallback to largest reasonable currency amount found on receipt
        var maxAmount = 0.0
        val genericNumberPattern = Pattern.compile("""\b([0-9]{1,4}\.[0-9]{2})\b""")
        for (line in lines) {
            val m = genericNumberPattern.matcher(line)
            while (m.find()) {
                val v = m.group(1)?.toDoubleOrNull() ?: 0.0
                if (v in 0.5..50000.0 && v > maxAmount) {
                    maxAmount = v
                }
            }
        }
        return maxAmount
    }

    private fun extractDate(text: String): String {
        val datePattern = Pattern.compile("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b|\b(\d{4}[/-]\d{1,2}[/-]\d{1,2})\b""")
        val matcher = datePattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(0) ?: getTodayDate()
        }
        return getTodayDate()
    }

    private fun extractTitle(lines: List<String>): String {
        for (line in lines) {
            val clean = line.replace(Regex("""[^a-zA-Z0-9\s&]"""), "").trim()
            if (clean.length in 3..30 && !clean.all { it.isDigit() }) {
                return clean
            }
        }
        return "Scanned Receipt"
    }

    private fun classifyCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.containsAny("grocer", "supermarket", "walmart", "costco", "kroger", "target", "market", "organic", "whole foods") -> "GROCERIES"
            lower.containsAny("cafe", "coffee", "restaurant", "starbucks", "mcdonald", "burger", "pizza", "bar", "kitchen", "diner", "grill") -> "DINING"
            lower.containsAny("fuel", "petrol", "diesel", "gas", "shell", "bp", "chevron", "exxon", "mobil", "station") -> "FUEL"
            lower.containsAny("apple", "google", "amazon", "electronics", "tech", "software", "computer", "hardware") -> "TECH"
            lower.containsAny("pharmacy", "medical", "rx", "chemist", "cvs", "walgreens", "health", "hospital") -> "PHARMACY"
            else -> "GENERAL"
        }
    }

    private fun String.containsAny(vararg words: String): Boolean {
        return words.any { this.contains(it) }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
