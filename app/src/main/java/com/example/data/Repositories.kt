package com.example.data

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.telephony.SubscriptionManager
import com.example.AppLanguage
import com.example.ContactsHelper
import com.example.HistoryEntry
import com.example.HistoryManager
import com.example.PaymentType
import com.example.PrefsManager
import com.example.SimEntry
import com.example.UssdExecutionResult
import com.example.UssdManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface TelephonyRepository {
    fun getActiveSims(): List<SimEntry>
    suspend fun executeUssd(ussdCode: String, subscriptionId: Int?, isAr: Boolean): UssdExecutionResult
}

class DefaultTelephonyRepository(private val application: Application) : TelephonyRepository {
    @SuppressLint("MissingPermission")
    override fun getActiveSims(): List<SimEntry> {
        return try {
            val sm = application.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                ?: return emptyList()
            val list = sm.activeSubscriptionInfoList ?: return emptyList()
            var detectedPhoneForSelf: String? = null
            list.map { info ->
                var phoneNumber: String? = null
                try {
                    @Suppress("DEPRECATION")
                    phoneNumber = info.number
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            phoneNumber = sm.getPhoneNumber(info.subscriptionId)
                        } catch (_: Exception) {}
                    }
                    if (phoneNumber != null) {
                        val norm = UssdManager.normalizePhone(phoneNumber)
                        if (norm.length == 10 && norm.startsWith("05")) {
                            phoneNumber = norm
                            if (detectedPhoneForSelf == null) {
                                detectedPhoneForSelf = norm
                            }
                        } else {
                            phoneNumber = null
                        }
                    }
                } catch (_: Exception) {}

                SimEntry(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    phoneNumber = phoneNumber
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    override suspend fun executeUssd(
        ussdCode: String,
        subscriptionId: Int?,
        isAr: Boolean
    ): UssdExecutionResult {
        return UssdManager.executeUssd(
            context = application,
            ussdCode = ussdCode,
            subscriptionId = subscriptionId,
            isAr = isAr
        )
    }
}

interface ContactsRepository {
    suspend fun getPhoneFromUri(uri: Uri): String?
    suspend fun lookupName(normalizedNumber: String): String?
    suspend fun getNameFromUri(uri: Uri): String?
    suspend fun getNamesForNumbers(numbers: Set<String>): Map<String, String>
}

class DefaultContactsRepository(private val application: Application) : ContactsRepository {
    override suspend fun getPhoneFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        ContactsHelper.getPhoneFromUri(application, uri)
    }

    override suspend fun lookupName(normalizedNumber: String): String? = withContext(Dispatchers.IO) {
        ContactsHelper.lookupName(application, normalizedNumber)
    }

    override suspend fun getNameFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        ContactsHelper.getNameFromUri(application, uri)
    }

    override suspend fun getNamesForNumbers(numbers: Set<String>): Map<String, String> = withContext(Dispatchers.IO) {
        ContactsHelper.getNamesForNumbers(application, numbers)
    }
}

interface HistoryRepository {
    fun observeHistory(): Flow<List<HistoryEntry>>
    suspend fun getHistory(): List<HistoryEntry>
    suspend fun addHistory(entry: HistoryEntry)
}

class DefaultHistoryRepository(private val application: Application) : HistoryRepository {
    private val historyManager = HistoryManager(application)

    override fun observeHistory(): Flow<List<HistoryEntry>> = historyManager.observeHistory()

    override suspend fun getHistory(): List<HistoryEntry> = withContext(Dispatchers.IO) {
        historyManager.getHistory()
    }

    override suspend fun addHistory(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        historyManager.addHistory(entry)
    }
}

interface SettingsRepository {
    fun getLanguage(): AppLanguage
    fun setLanguage(lang: AppLanguage)
    fun getHideBalance(): Boolean
    fun setHideBalance(hide: Boolean)
    fun getIsDarkMode(): Boolean
    fun setIsDarkMode(isDark: Boolean)
    fun getSelfPhone(): String
    fun setSelfPhone(phone: String)
    fun getSelectedSimId(): Int
    fun setSelectedSimId(simId: Int)
    fun getBalance(): String
    fun setBalance(balance: String)
    fun getBalanceDifference(): String
    fun setBalanceDifference(diff: String)
    fun getLastRefreshTime(): Long
    fun setLastRefreshTime(time: Long)
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
    fun getRememberPin(): Boolean
    fun setRememberPin(enabled: Boolean)
    fun getSavedPin(): String
    fun savePin(pin: String)
}

class DefaultSettingsRepository(private val application: Application) : SettingsRepository {
    private val prefsManager = PrefsManager(application)

    override fun getLanguage(): AppLanguage = prefsManager.getLanguage()
    override fun setLanguage(lang: AppLanguage) = prefsManager.setLanguage(lang)
    override fun getHideBalance(): Boolean = prefsManager.getHideBalance()
    override fun setHideBalance(hide: Boolean) = prefsManager.setHideBalance(hide)
    override fun getIsDarkMode(): Boolean = prefsManager.getIsDarkMode()
    override fun setIsDarkMode(isDark: Boolean) = prefsManager.setIsDarkMode(isDark)
    override fun getSelfPhone(): String = prefsManager.getSelfPhone()
    override fun setSelfPhone(phone: String) = prefsManager.setSelfPhone(phone)
    override fun getSelectedSimId(): Int = prefsManager.getSelectedSimId()
    override fun setSelectedSimId(simId: Int) = prefsManager.setSelectedSimId(simId)
    override fun getBalance(): String = prefsManager.getBalance()
    override fun setBalance(balance: String) = prefsManager.setBalance(balance)
    override fun getBalanceDifference(): String = prefsManager.getBalanceDifference()
    override fun setBalanceDifference(diff: String) = prefsManager.setBalanceDifference(diff)
    override fun getLastRefreshTime(): Long = prefsManager.getLastRefreshTime()
    override fun setLastRefreshTime(time: Long) = prefsManager.setLastRefreshTime(time)
    override fun isOnboardingCompleted(): Boolean = prefsManager.isOnboardingCompleted()
    override fun setOnboardingCompleted(completed: Boolean) = prefsManager.setOnboardingCompleted(completed)
    override fun getRememberPin(): Boolean = prefsManager.getRememberPin()
    override fun setRememberPin(enabled: Boolean) = prefsManager.setRememberPin(enabled)
    override fun getSavedPin(): String = prefsManager.getSavedPin()
    override fun savePin(pin: String) = prefsManager.savePin(pin)
}
