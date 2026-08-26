package com.example

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

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
    
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }
}
