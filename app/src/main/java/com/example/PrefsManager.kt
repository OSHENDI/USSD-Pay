package com.example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences

    init {
        var tempPrefs: SharedPreferences? = null
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            tempPrefs = EncryptedSharedPreferences.create(
                context,
                "app_prefs_enc",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("PrefsManager", "Failed to init EncryptedSharedPreferences", e)
            try {
                // If Keystore is corrupted, delete the old encrypted file and try standard
                context.deleteSharedPreferences("app_prefs_enc")
            } catch (ex: Exception) {}
            tempPrefs = context.getSharedPreferences("app_prefs_fallback", Context.MODE_PRIVATE)
        }
        prefs = tempPrefs!!

        try {
            // Silent Migration from old plain-text prefs
            val oldPrefsFile = java.io.File(context.applicationInfo.dataDir, "shared_prefs/app_prefs.xml")
            if (oldPrefsFile.exists()) {
                Log.d("PrefsManager", "Found legacy plain-text preferences. Migrating...")
                val oldPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                
                // Start migration
                val editor = prefs.edit()
                for ((key, value) in oldPrefs.all) {
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Int -> editor.putInt(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Long -> editor.putLong(key, value)
                    }
                }
                editor.apply()
                
                // Delete old prefs to secure data
                oldPrefs.edit().clear().commit()
                oldPrefsFile.delete()
            }
        } catch (e: Exception) {
            Log.e("PrefsManager", "Migration failed", e)
        }
    }

    fun getLanguage(): AppLanguage {
        if (!prefs.contains("app_language")) {
            val defaultLang = if (java.util.Locale.getDefault().language.startsWith("ar")) AppLanguage.AR else AppLanguage.EN
            setLanguage(defaultLang)
            return defaultLang
        }
        val langStr = prefs.getString("app_language", AppLanguage.AR.name) ?: AppLanguage.AR.name
        return try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.AR
        }
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.name).apply()
    }

    fun getHideBalance(): Boolean {
        return prefs.getBoolean("hide_balance", false)
    }

    fun setHideBalance(hide: Boolean) {
        prefs.edit().putBoolean("hide_balance", hide).apply()
    }

    fun getIsDarkMode(): Boolean {
        return prefs.getBoolean("is_dark_mode", false)
    }

    fun setIsDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", isDark).apply()
    }

    fun getSelfPhone(): String {
        return prefs.getString("self_phone", "") ?: ""
    }

    fun setSelfPhone(phone: String) {
        prefs.edit().putString("self_phone", phone).apply()
    }

    fun getSelectedSimId(): Int {
        return prefs.getInt("selected_sim_id", -1)
    }

    fun setSelectedSimId(simId: Int) {
        prefs.edit().putInt("selected_sim_id", simId).apply()
    }

    fun getBalance(): String {
        return prefs.getString("cached_balance", "") ?: ""
    }

    fun setBalance(balance: String) {
        prefs.edit().putString("cached_balance", balance).apply()
    }

    fun getBalanceDifference(): String {
        return prefs.getString("balance_difference", "") ?: ""
    }

    fun setBalanceDifference(diff: String) {
        prefs.edit().putString("balance_difference", diff).apply()
    }
    
    fun getLastRefreshTime(): Long {
        return prefs.getLong("last_refresh_time", 0L)
    }
    
    fun setLastRefreshTime(time: Long) {
        prefs.edit().putLong("last_refresh_time", time).apply()
    }
    
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun getRememberPin(): Boolean {
        return prefs.getBoolean("remember_pin_enabled", false)
    }

    fun setRememberPin(enabled: Boolean) {
        prefs.edit().putBoolean("remember_pin_enabled", enabled).apply()
    }

    fun getSavedPin(): String {
        return prefs.getString("saved_pin", "") ?: ""
    }

    fun savePin(pin: String) {
        if (pin.isEmpty()) {
            prefs.edit().remove("saved_pin").apply()
        } else {
            prefs.edit().putString("saved_pin", pin).apply()
        }
    }
}
