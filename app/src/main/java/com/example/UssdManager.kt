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
        if (digits.startsWith("05") && digits.length >= 10) {
            return digits.take(10)
        }
        if (digits.startsWith("972")) {
            digits = digits.substring(3)
        } else if (digits.startsWith("970")) {
            digits = digits.substring(3)
        }
        if (!digits.startsWith("0")) {
            digits = "0$digits"
        }
        return digits.take(10)
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
        
        // Exact success match
        val isSuccess = trimmed == "تم تحويل المبلغ بنجاح"
        
        // Check if raw matches any known error message (which we want to flag)
        var isError = false
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
