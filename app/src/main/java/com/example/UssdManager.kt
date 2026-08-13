package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager

object UssdManager {

    fun normalizePhone(phone: String): String {
        var digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        if (digits.startsWith("00970")) {
            digits = digits.substring(5)
        } else if (digits.startsWith("00972")) {
            digits = digits.substring(5)
        } else if (digits.startsWith("970")) {
            digits = digits.substring(3)
        } else if (digits.startsWith("972")) {
            digits = digits.substring(3)
        }

        if (digits.startsWith("5")) {
            digits = "0$digits"
        }

        return if (digits.length > 10) digits.take(10) else digits
    }

    fun isValidPhone(phone: String): Boolean {
        val normalized = normalizePhone(phone)
        return normalized.length == 10 && normalized.startsWith("05")
    }

    fun buildPaymentString(paymentType: PaymentType, pin: String, phone: String, amount: String): String {
        val normalized = normalizePhone(phone)
        val typeCode = if (paymentType == PaymentType.FRIEND) "1" else "2"
        return "*110*$typeCode*$pin*$normalized*$amount*1#"
    }

    fun buildBalanceString(): String {
        return "*110*3#"
    }

    // Carrier response translate maps
    private val ussdResponseMapArToEn = mapOf(
        "رمز PIN غير صحيح ، حاول مرة أخرى" to "Incorrect PIN, please try again",
        "الرجاء التأكد من رقم الصديق" to "Please verify the recipient's number",
        "الرجاء التأكد من رقم التاجر" to "Please verify the merchant's number",
        "الرجاء المحاولة مرة أخرى" to "Please try again",
        "الرصيد غير كافٍ" to "Insufficient balance",
        "عذراً، لا يمكن إتمام هذه العملية" to "Sorry, this transaction cannot be completed",
        "UKNOWN APPLICATION" to "Unknown application error",
        "تم تحويل المبلغ بنجاح" to "Transfer completed successfully"
    )

    fun translateResponse(raw: String, language: AppLanguage): TranslationResult {
        val trimmed = raw.trim()
        
        // Success match
        val isSuccess = trimmed == "تم تحويل المبلغ بنجاح" ||
                trimmed.contains("تم تحويل") ||
                trimmed.contains("بنجاح") ||
                trimmed.contains("تمت العملية")
        
        // Check if raw matches any known error message
        var isError = false
        if (trimmed.contains("تأكد") ||
            trimmed.contains("غير صحيح") ||
            trimmed.contains("خطأ") ||
            trimmed.contains("غير كاف") ||
            trimmed.contains("لا يمكن") ||
            trimmed.contains("فشل")
        ) {
            isError = true
        }

        val englishMapped = ussdResponseMapArToEn[trimmed]
        if (englishMapped != null && !isSuccess) {
            isError = true
        }

        val text = if (language == AppLanguage.AR) {
            trimmed
        } else {
            englishMapped ?: trimmed
        }

        return TranslationResult(text = text, isError = isError, isSuccess = isSuccess)
    }

    data class TranslationResult(
        val text: String,
        val isError: Boolean,
        val isSuccess: Boolean
    )

    fun extractBalanceAmount(raw: String): String {
        val regex = Regex("""(\d+(?:[.,]\d+)?)""")
        val match = regex.find(raw)
        return match?.value ?: ""
    }

    @SuppressLint("MissingPermission")
    fun sendPaymentUssd(
        context: Context,
        paymentType: PaymentType,
        pin: String,
        phone: String,
        amount: String,
        subscriptionId: Int?,
        onResponse: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val normalized = normalizePhone(phone)

        if (paymentType == PaymentType.FRIEND) {
            val ussdCode = "*110*1*$pin*$normalized*$amount*1#"
            sendUssd(context, ussdCode, subscriptionId, onResponse, onError)
            return
        }

        // MERCHANT FLOW: Since Jawwal/Ooredoo blocks background interactive USSD (returning -1)
        // and background Root Menu walking locks up the modem state, we must use the reliable
        // MERCHANT FLOW: Dynamic Interactive Session
        // Starting at the base merchant menu prevents carrier deep-link blocking
        val baseCode = "*110*2#"
        val mainHandler = Handler(Looper.getMainLooper())
        
        if (subscriptionId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val baseManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val manager = baseManager.createForSubscriptionId(subscriptionId)

                val callback = object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        tm: TelephonyManager?,
                        request: String?,
                        response: CharSequence?
                    ) {
                        val resp = response?.toString() ?: ""
                        val activeTm = tm ?: manager

                        // Determine the next step dynamically by reading the carrier's text
                        val nextInput = when {
                            resp.contains("الرقم السري") || resp.contains("PIN") -> pin
                            resp.contains("رقم التاجر") || resp.contains("التاجر") -> normalized
                            resp.contains("المبلغ") || resp.contains("ادخل") -> amount
                            resp.contains("تأكيد") || resp.contains("1.") -> "1"
                            resp.contains("بنجاح") || resp.contains("تم") -> {
                                onResponse(resp) // Finished!
                                return
                            }
                            resp.contains("تأكد") || resp.contains("خطأ") || resp.contains("فشل") -> {
                                onResponse(resp) // Known carrier error
                                return
                            }
                            else -> {
                                // Unknown state. Send '0' to gracefully cancel and free the modem.
                                try { activeTm.sendUssdRequest("0", this, mainHandler) } catch (e: Exception) {}
                                onError("Unexpected response: $resp")
                                return
                            }
                        }

                        // Add a small 500ms safety buffer to ensure modem is ready for the reply
                        mainHandler.postDelayed({
                            try {
                                activeTm.sendUssdRequest(nextInput, this, mainHandler)
                            } catch (e: Exception) {
                                onError("Failed to send input: $nextInput")
                            }
                        }, 500L) 
                    }

                    override fun onReceiveUssdResponseFailed(
                        tm: TelephonyManager?,
                        request: String?,
                        failureCode: Int
                    ) {
                        val errorMsg = when (failureCode) {
                            -1 -> "USSD Session dropped (-1). Check signal or merchant status."
                            else -> "USSD failed (code $failureCode)."
                        }
                        onError(errorMsg)
                    }
                }

                manager.sendUssdRequest(baseCode, callback, mainHandler)
            } catch (e: Exception) {
                onError("Failed to initiate USSD.")
            }
        } else {
            onError("Unsupported Android version for background USSD.")
        }
    }

    @SuppressLint("MissingPermission")
    fun sendUssd(
        context: Context,
        ussdCode: String,
        subscriptionId: Int?,
        onResponse: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        
        if (subscriptionId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val baseManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val manager = baseManager.createForSubscriptionId(subscriptionId)
                
                manager.sendUssdRequest(ussdCode, object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager?,
                        request: String?,
                        response: CharSequence?
                    ) {
                        onResponse(response?.toString() ?: "")
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager?,
                        request: String?,
                        failureCode: Int
                    ) {
                        val errorMsg = when (failureCode) {
                            -1 -> "USSD request failed. Please check the recipient number, PIN, and your balance, then try again."
                            else -> "USSD failed (code $failureCode). Check SIM and signal."
                        }
                        onError(errorMsg)
                    }
                }, mainHandler)
            } catch (e: Exception) {
                onError("carrier did not respond. check screen for carrier response.")
            }
        } else {
            // Legacy Call Fallback
            try {
                // Encode the '*' and '#' correctly
                val encodedUri = Uri.encode(ussdCode)
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$encodedUri")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                // Legacy dialing does not receive callback responses
                onResponse("Sent via legacy carrier launcher. Please view your carrier popup message.")
            } catch (e: Exception) {
                onError("Permission denied. grant CALL_PHONE permission.")
            }
        }
    }
}
