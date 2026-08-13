package com.example

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PmaQrManager {

    /**
     * Parses a QR code string (either EMVCo PMA IPS format, pipe-delimited, or raw phone)
     * and extracts the normalized phone number and PaymentType (FRIEND or MERCHANT).
     */
    fun parseQrCode(content: String): Pair<String, PaymentType>? {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return null

        // 1. Check if it is EMVCo / PMA IPS format (starts with 000201 or contains ps.pma.ips or Tag 26)
        if (trimmed.startsWith("000201") || trimmed.contains("ps.pma.ips") || trimmed.contains("0410")) {
            // Determine PaymentType
            // Differentiating Pattern:
            // Tag 52 (MCC):
            // Friend: "52044444" (P2P / Personal transfer MCC 4444)
            // Merchant: "52045411" (Merchant category MCC 5411)
            val isMerchant = if (trimmed.contains("52044444")) {
                false
            } else if (trimmed.contains("52045411") || trimmed.contains("520454")) {
                true
            } else {
                false
            }
            
            val paymentType = if (isMerchant) PaymentType.MERCHANT else PaymentType.FRIEND

            // Extract Phone Number
            // Subtag 04 format inside Tag 26: "04" + 2-digit length + phone digits
            var extractedPhone = ""
            val subtag04Regex = Regex("""04(\d{2})(\d+)""")
            val subtagMatches = subtag04Regex.findAll(trimmed)
            for (m in subtagMatches) {
                val len = m.groupValues[1].toIntOrNull() ?: 0
                val rawVal = m.groupValues[2]
                if (rawVal.length >= len && len > 0) {
                    val candidate = rawVal.substring(0, len)
                    val normalizedCandidate = UssdManager.normalizePhone(candidate)
                    if (normalizedCandidate.length == 10 && normalizedCandidate.startsWith("05")) {
                        extractedPhone = normalizedCandidate
                        break
                    }
                }
            }

            if (extractedPhone.isEmpty()) {
                // Fallback: search for any phone number with international or local prefix in the QR string
                val phoneRegex = Regex("""(?:00970|00972|970|972|\+970|\+972|0)?5[0-9]{8}""")
                val phoneMatch = phoneRegex.find(trimmed)
                if (phoneMatch != null) {
                    extractedPhone = UssdManager.normalizePhone(phoneMatch.value)
                }
            }

            val normalized = if (extractedPhone.isNotEmpty()) extractedPhone else UssdManager.normalizePhone(trimmed)
            return Pair(normalized, paymentType)
        }

        // 2. Check if simple pipe-delimited format (e.g., "0591000001|MERCHANT")
        if (trimmed.contains("|")) {
            val parts = trimmed.split("|")
            val rawPhone = parts[0].trim()
            val type = if (parts.size > 1 && parts[1].trim().lowercase() == "merchant") {
                PaymentType.MERCHANT
            } else {
                PaymentType.FRIEND
            }
            return Pair(UssdManager.normalizePhone(rawPhone), type)
        }

        // 3. Raw phone number string
        return Pair(UssdManager.normalizePhone(trimmed), PaymentType.FRIEND)
    }

    /**
     * Generates a valid PMA IPS EMVCo QR payload string for the given phone number and PaymentType.
     */
    fun generatePmaQrPayload(phone: String, type: PaymentType): String {
        val normalizedPhone = UssdManager.normalizePhone(phone)
        val formattedPhone = if (normalizedPhone.length == 10) normalizedPhone else "0590000000"

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date())

        val basePayload = if (type == PaymentType.FRIEND) {
            val tag26Value = "0010ps.pma.ips0108JPAYPS220410${formattedPhone}1004EPAY"
            val tag26Len = String.format(Locale.ENGLISH, "%02d", tag26Value.length)

            "00020101021126${tag26Len}${tag26Value}5204444453033765802PS5904User6004Gaza80370010ps.pma.ips0119${currentTime}6304"
        } else {
            val tag26Value = "0010ps.pma.ips0108JPAYPS220410${formattedPhone}1004EPAY"
            val tag26Len = String.format(Locale.ENGLISH, "%02d", tag26Value.length)

            "00020101021126${tag26Len}${tag26Value}5204541153033765802PS5904User6008Ramallah80370010ps.pma.ips0119${currentTime}6304"
        }

        val crc = calculateCrc16(basePayload)
        return "$basePayload$crc"
    }

    /**
     * Computes EMVCo CRC16 (CCITT, polynomial 0x1021, initial value 0xFFFF).
     */
    private fun calculateCrc16(data: String): String {
        val bytes = data.toByteArray(Charsets.UTF_8)
        var crc = 0xFFFF
        val polynomial = 0x1021
        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.ENGLISH, "%04X", crc)
    }
}
