package com.example

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.ContactsRepository
import com.example.data.HistoryRepository
import com.example.data.SettingsRepository
import com.example.data.TelephonyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainViewModelTest {

    private lateinit var app: Application
    private lateinit var fakeTelephonyRepo: FakeTelephonyRepository
    private lateinit var fakeContactsRepo: FakeContactsRepository
    private lateinit var fakeHistoryRepo: FakeHistoryRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        fakeTelephonyRepo = FakeTelephonyRepository()
        fakeContactsRepo = FakeContactsRepository()
        fakeHistoryRepo = FakeHistoryRepository()
        fakeSettingsRepo = FakeSettingsRepository()

        viewModel = MainViewModel(
            application = app,
            telephonyRepo = fakeTelephonyRepo,
            contactsRepo = fakeContactsRepo,
            historyRepo = fakeHistoryRepo,
            settingsRepo = fakeSettingsRepo
        )
    }

    @Test
    fun testInitializationLoadsSettings() {
        fakeSettingsRepo._lang = AppLanguage.AR
        fakeSettingsRepo._isDark = true
        fakeSettingsRepo._selfPhone = "0599123456"
        fakeSettingsRepo._balance = "50"
        fakeSettingsRepo._onboarded = true

        val vm = MainViewModel(
            application = app,
            telephonyRepo = fakeTelephonyRepo,
            contactsRepo = fakeContactsRepo,
            historyRepo = fakeHistoryRepo,
            settingsRepo = fakeSettingsRepo
        )

        assertEquals(Screen.MAIN, vm.uiState.value.currentScreen)
        assertEquals(AppLanguage.AR, vm.uiState.value.language)
        assertTrue(vm.uiState.value.isAr)
        assertTrue(vm.uiState.value.isDarkMode)
        assertEquals("0599123456", vm.uiState.value.selfPhone)
        assertEquals("50", vm.uiState.value.balanceResult)
    }

    @Test
    fun testToggleLanguage() {
        assertEquals(AppLanguage.EN, viewModel.uiState.value.language)
        assertFalse(viewModel.uiState.value.isAr)

        viewModel.toggleLanguage()

        assertEquals(AppLanguage.AR, viewModel.uiState.value.language)
        assertTrue(viewModel.uiState.value.isAr)
        assertEquals(AppLanguage.AR, fakeSettingsRepo._lang)

        viewModel.toggleLanguage()
        assertEquals(AppLanguage.EN, viewModel.uiState.value.language)
        assertEquals(AppLanguage.EN, fakeSettingsRepo._lang)
    }

    @Test
    fun testSetRememberPin() {
        viewModel.setSecretCode("1234")
        viewModel.setRememberPin(true)

        assertTrue(viewModel.uiState.value.rememberPin)
        assertTrue(fakeSettingsRepo._rememberPin)
        assertEquals("1234", fakeSettingsRepo._savedPin)

        viewModel.setRememberPin(false)
        assertFalse(viewModel.uiState.value.rememberPin)
        assertFalse(fakeSettingsRepo._rememberPin)
        assertEquals("", fakeSettingsRepo._savedPin)
    }

    @Test
    fun testUpdateAmountDigitsOnly() {
        viewModel.updateAmount("100abc20")
        assertEquals("10020", viewModel.uiState.value.amount)
    }

    @Test
    fun testResetToMain() {
        viewModel.navigateTo(Screen.SETTINGS)
        viewModel.updateAmount("50")
        viewModel.resetToMain()

        assertEquals(Screen.MAIN, viewModel.uiState.value.currentScreen)
        assertEquals("10", viewModel.uiState.value.amount)
    }

    @Test
    fun testRequestPayValidationFailure() {
        // Empty fields
        viewModel.requestPay()
        assertTrue(viewModel.uiState.value.errorMessage.isNotEmpty())
        assertFalse(viewModel.uiState.value.showConfirmDialog)

        // Invalid recipient
        viewModel.updateRecipient("123")
        viewModel.updateAmount("50")
        viewModel.setSecretCode("1234")
        viewModel.requestPay()
        assertTrue(viewModel.uiState.value.errorMessage.isNotEmpty())
        assertFalse(viewModel.uiState.value.showConfirmDialog)

        // Valid fields
        viewModel.updateRecipient("0599123456")
        viewModel.requestPay()
        assertEquals("", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.showConfirmDialog)
    }

    @Test
    fun testSettingsToggles() {
        viewModel.setDarkMode(true)
        assertTrue(viewModel.uiState.value.isDarkMode)
        assertTrue(fakeSettingsRepo._isDark)

        viewModel.setHideBalance(true)
        assertTrue(viewModel.uiState.value.hideBalance)
        assertTrue(fakeSettingsRepo._hideBal)

        viewModel.setSelfPhone("0599123456")
        assertEquals("0599123456", viewModel.uiState.value.selfPhone)
        assertEquals("0599123456", fakeSettingsRepo._selfPhone)
    }

    @Test
    fun testCompleteOnboarding() {
        fakeSettingsRepo._onboarded = false
        fakeSettingsRepo._selfPhone = ""
        viewModel.completeOnboarding()
        assertTrue(fakeSettingsRepo._onboarded)
        assertEquals(Screen.PHONE_SETUP, viewModel.uiState.value.currentScreen)

        viewModel.setSelfPhone("0599111222")
        viewModel.completeOnboarding()
        assertEquals(Screen.MAIN, viewModel.uiState.value.currentScreen)
    }

    private class FakeTelephonyRepository : TelephonyRepository {
        var sims: List<SimEntry> = emptyList()
        override fun getActiveSims(): List<SimEntry> = sims
        override suspend fun executeUssd(ussdCode: String, subscriptionId: Int?, isAr: Boolean): UssdExecutionResult {
            return UssdExecutionResult.Success("Success")
        }
    }

    private class FakeContactsRepository : ContactsRepository {
        override suspend fun getPhoneFromUri(uri: Uri): String? = null
        override suspend fun lookupName(normalizedNumber: String): String? = null
        override suspend fun getNameFromUri(uri: Uri): String? = null
        override suspend fun getNamesForNumbers(numbers: Set<String>): Map<String, String> = emptyMap()
    }

    private class FakeHistoryRepository : HistoryRepository {
        val entries = mutableListOf<HistoryEntry>()
        override fun observeHistory(): Flow<List<HistoryEntry>> = flowOf(entries)
        override suspend fun getHistory(): List<HistoryEntry> = entries
        override suspend fun addHistory(entry: HistoryEntry) {
            entries.add(entry)
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        var _lang = AppLanguage.EN
        var _hideBal = false
        var _isDark = false
        var _selfPhone = ""
        var _simId = -1
        var _balance = "0"
        var _balanceDiff = ""
        var _lastRefresh = 0L
        var _onboarded = false
        var _rememberPin = false
        var _savedPin = ""

        override fun getLanguage(): AppLanguage = _lang
        override fun setLanguage(lang: AppLanguage) { this._lang = lang }
        override fun getHideBalance(): Boolean = _hideBal
        override fun setHideBalance(hide: Boolean) { this._hideBal = hide }
        override fun getIsDarkMode(): Boolean = _isDark
        override fun setIsDarkMode(isDark: Boolean) { this._isDark = isDark }
        override fun getSelfPhone(): String = _selfPhone
        override fun setSelfPhone(phone: String) { this._selfPhone = phone }
        override fun getSelectedSimId(): Int = _simId
        override fun setSelectedSimId(simId: Int) { this._simId = simId }
        override fun getBalance(): String = _balance
        override fun setBalance(balance: String) { this._balance = balance }
        override fun getBalanceDifference(): String = _balanceDiff
        override fun setBalanceDifference(diff: String) { this._balanceDiff = diff }
        override fun getLastRefreshTime(): Long = _lastRefresh
        override fun setLastRefreshTime(time: Long) { this._lastRefresh = time }
        override fun isOnboardingCompleted(): Boolean = _onboarded
        override fun setOnboardingCompleted(completed: Boolean) { this._onboarded = completed }
        override fun getRememberPin(): Boolean = _rememberPin
        override fun setRememberPin(enabled: Boolean) { this._rememberPin = enabled }
        override fun getSavedPin(): String = _savedPin
        override fun savePin(pin: String) { this._savedPin = pin }
    }
}
