package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactsRepository
import com.example.data.DefaultContactsRepository
import com.example.data.DefaultHistoryRepository
import com.example.data.DefaultSettingsRepository
import com.example.data.DefaultTelephonyRepository
import com.example.data.HistoryRepository
import com.example.data.SettingsRepository
import com.example.data.TelephonyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.example.ui.getAppStrings

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val telephonyRepo: TelephonyRepository = DefaultTelephonyRepository(application),
    private val contactsRepo: ContactsRepository = DefaultContactsRepository(application),
    private val historyRepo: HistoryRepository = DefaultHistoryRepository(application),
    private val settingsRepo: SettingsRepository = DefaultSettingsRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var payJob: Job? = null
    private var balanceJob: Job? = null
    private var historyJob: Job? = null
    private var coldStartBalanceChecked = false

    init {
        val lang = settingsRepo.getLanguage()
        val isDark = settingsRepo.getIsDarkMode()
        val selfPhone = settingsRepo.getSelfPhone()
        val savedSimId = settingsRepo.getSelectedSimId()
        val balance = settingsRepo.getBalance()
        val onboarded = settingsRepo.isOnboardingCompleted()
        val hideBal = settingsRepo.getHideBalance()
        val savedPin = settingsRepo.getSavedPin()
        val hasSavedPin = settingsRepo.getRememberPin()
        val lastRefresh = settingsRepo.getLastRefreshTime()
        val balanceDiff = settingsRepo.getBalanceDifference()

        val initialScreen = if (onboarded) Screen.MAIN else Screen.ONBOARDING

        _uiState.updateState { it.copy(
            currentScreen = initialScreen,
            language = lang,
            isAr = lang == AppLanguage.AR,
            isDarkMode = isDark,
            selfPhone = selfPhone,
            balanceResult = balance,
            balanceDifference = balanceDiff,
            selectedSimId = if (savedSimId != -1) savedSimId else null,
            hideBalance = hideBal,
            secretCode = savedPin,
            rememberPin = hasSavedPin,
            lastRefreshTime = lastRefresh
        ) }

        observeHistoryInternal()
    }

    private fun observeHistoryInternal() {
        if (historyJob == null) {
            historyJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    historyRepo.observeHistory().collect { rawItems ->
                        val missingNameNumbers = rawItems.filter { it.name.isBlank() }.map { it.number }.toSet()
                        val resolvedNames = if (missingNameNumbers.isNotEmpty()) {
                            contactsRepo.getNamesForNumbers(missingNameNumbers)
                        } else {
                            emptyMap()
                        }
                        val items = rawItems.map { entry ->
                            if (entry.name.isBlank()) {
                                val contactName = resolvedNames[entry.number]
                                if (contactName != null) entry.copy(name = contactName) else entry
                            } else {
                                entry
                            }
                        }
                        withContext(Dispatchers.Main) {
                            _uiState.updateState { it.copy(history = items) }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error observing history", e)
                }
            }
        }
    }

    fun initialize(context: Context? = null) {
        // Handled automatically in init block; safe no-op for backward compatibility
    }

    private inline fun <T> MutableStateFlow<T>.updateState(newVal: (T) -> T) {
        val prev = this.value
        val updated = newVal(prev)
        if (prev != updated) {
            this.value = updated
        }
    }

    fun loadSims(context: Context? = null) {
        try {
            val mapped = telephonyRepo.getActiveSims()
            _uiState.updateState { state ->
                val newSelectedSim = if (state.selectedSimId != null && mapped.any { it.subscriptionId == state.selectedSimId }) {
                    state.selectedSimId
                } else {
                    val saved = settingsRepo.getSelectedSimId()
                    if (saved != -1 && mapped.any { it.subscriptionId == saved }) saved else mapped.firstOrNull()?.subscriptionId
                }
                val autoSelfPhone = if (state.selfPhone.isEmpty()) {
                    val detected = mapped.firstOrNull { it.phoneNumber != null }?.phoneNumber
                    if (detected != null) {
                        settingsRepo.setSelfPhone(detected)
                        detected
                    } else state.selfPhone
                } else state.selfPhone

                state.copy(
                    sims = mapped,
                    selectedSimId = newSelectedSim,
                    selfPhone = autoSelfPhone
                )
            }
        } catch (_: Exception) {}
    }

    fun selectSim(simId: Int) {
        settingsRepo.setSelectedSimId(simId)
        _uiState.updateState { it.copy(selectedSimId = simId) }
    }

    fun loadHistory(context: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val rawItems = historyRepo.getHistory()
            val missingNameNumbers = rawItems.filter { it.name.isBlank() }.map { it.number }.toSet()
            val resolvedNames = if (missingNameNumbers.isNotEmpty()) {
                contactsRepo.getNamesForNumbers(missingNameNumbers)
            } else {
                emptyMap()
            }

            val items = rawItems.map { entry ->
                if (entry.name.isBlank()) {
                    val contactName = resolvedNames[entry.number]
                    if (contactName != null) {
                        entry.copy(name = contactName)
                    } else {
                        entry
                    }
                } else {
                    entry
                }
            }
            withContext(Dispatchers.Main) {
                _uiState.updateState { it.copy(history = items) }
            }
        }
    }

    fun updateRecipient(phone: String, context: Context? = null) {
        val normalized = UssdManager.normalizePhone(phone)

        if (normalized.length == 10) {
            _uiState.updateState { it.copy(recipient = normalized) }
            viewModelScope.launch(Dispatchers.IO) {
                val displayName = contactsRepo.lookupName(normalized) ?: ""
                withContext(Dispatchers.Main) {
                    _uiState.updateState { it.copy(recipientName = displayName) }
                }
            }
        } else {
            _uiState.updateState { it.copy(recipient = normalized, recipientName = "") }
        }
    }

    fun checkBalanceOnColdStart(context: Context? = null) {
        if (coldStartBalanceChecked) return
        coldStartBalanceChecked = true
        checkBalance()
    }

    fun updateAmount(amountVal: String) {
        val digits = amountVal.filter { it.isDigit() }
        val num = digits.toLongOrNull() ?: 0L
        val capped = if (num > 12000L) "12000" else digits
        _uiState.updateState { it.copy(amount = capped) }
    }

    fun updatePaymentType(type: PaymentType) {
        _uiState.updateState { it.copy(paymentType = type) }
    }

    fun setSecretCode(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _uiState.updateState { it.copy(secretCode = pin) }
        }
    }

    fun getSavedPin(): String = settingsRepo.getSavedPin()

    fun applySavedPin(): Boolean {
        val saved = settingsRepo.getSavedPin()
        if (saved.length == 4) {
            setSecretCode(saved)
            return true
        }
        return false
    }

    fun setRememberPin(remember: Boolean) {
        _uiState.updateState { it.copy(rememberPin = remember) }
        settingsRepo.setRememberPin(remember)
        if (!remember) {
            settingsRepo.savePin("")
        } else if (_uiState.value.secretCode.length == 4) {
            settingsRepo.savePin(_uiState.value.secretCode)
        }
    }

    fun toggleLanguage() {
        _uiState.updateState { state ->
            val newLang = if (state.language == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
            settingsRepo.setLanguage(newLang)
            state.copy(
                language = newLang,
                isAr = newLang == AppLanguage.AR
            )
        }
    }

    fun setDarkMode(enabled: Boolean) {
        settingsRepo.setIsDarkMode(enabled)
        _uiState.updateState { it.copy(isDarkMode = enabled) }
    }

    fun setHideBalance(hide: Boolean) {
        settingsRepo.setHideBalance(hide)
        _uiState.updateState { it.copy(hideBalance = hide) }
    }

    fun setSelfPhone(phone: String) {
        val normalized = UssdManager.normalizePhone(phone)
        settingsRepo.setSelfPhone(normalized)
        _uiState.updateState { it.copy(selfPhone = normalized) }
    }

    fun showError(message: String) {
        _uiState.updateState { it.copy(errorMessage = message, successMessage = "") }
    }

    fun dismissError() {
        _uiState.updateState { it.copy(errorMessage = "") }
    }

    fun dismissSuccess() {
        _uiState.updateState { it.copy(successMessage = "") }
    }

    fun showConfirm() {
        _uiState.updateState { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirm() {
        _uiState.updateState { it.copy(showConfirmDialog = false) }
    }

    fun isAirplaneModeOn(context: Context? = null): Boolean {
        return try {
            Settings.Global.getInt(
                getApplication<Application>().contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }

    fun requestPay(context: Context? = null) {
        _uiState.updateState { it.copy(errorMessage = "", successMessage = "", payAttempted = true) }

        val strings = getAppStrings(_uiState.value.isAr)

        if (isAirplaneModeOn()) {
            showError(strings.errAirplaneMode)
            return
        }

        val state = _uiState.value

        if (state.sims.isEmpty() && state.selectedSimId == null) {
            loadSims()
        }

        val updatedState = _uiState.value
        val updatedStrings = getAppStrings(updatedState.isAr)

        if (updatedState.recipient.isBlank() || !UssdManager.isValidPhone(updatedState.recipient)) {
            showError(updatedStrings.errInvalidPhone)
            return
        }

        val amountNum = updatedState.amount.toDoubleOrNull() ?: 0.0
        if (updatedState.amount.isBlank() || amountNum < 1.0) {
            showError(updatedStrings.errMinAmount)
            return
        }

        if (updatedState.secretCode.isBlank() || updatedState.secretCode.length != 4) {
            showError(updatedStrings.errPinRequired)
            return
        }

        _uiState.updateState { it.copy(showConfirmDialog = true, payAttempted = false) }
    }

    fun navigateTo(screen: Screen) {
        _uiState.updateState { it.copy(currentScreen = screen, errorMessage = "") }
    }

    fun resetToMain() {
        _uiState.updateState { it.copy(
            currentScreen = Screen.MAIN,
            amount = "10",
            recipient = "",
            recipientName = "",
            secretCode = if (it.rememberPin) it.secretCode else "",
            errorMessage = "",
            successMessage = "",
            failureReason = ""
        ) }
    }

    fun completeOnboarding() {
        settingsRepo.setOnboardingCompleted(true)
        if (_uiState.value.selfPhone.isEmpty()) {
            navigateTo(Screen.PHONE_SETUP)
        } else {
            navigateTo(Screen.MAIN)
        }
    }

    fun confirmPay(context: Context? = null) {
        if (_uiState.value.isConfirmLoading) return

        _uiState.updateState { it.copy(isConfirmLoading = true, errorMessage = "", successMessage = "", failureReason = "") }

        if (_uiState.value.rememberPin) {
            settingsRepo.savePin(_uiState.value.secretCode)
        } else {
            settingsRepo.savePin("")
        }

        if (_uiState.value.sims.isEmpty()) {
            loadSims()
        }

        val currentSims = _uiState.value.sims
        val resolvedSimId = _uiState.value.selectedSimId ?: currentSims.firstOrNull()?.subscriptionId

        val state = _uiState.value
        val ussdStr = UssdManager.buildPaymentString(state.paymentType, state.secretCode, state.recipient, state.amount)

        payJob = viewModelScope.launch {
            try {
                val result = withTimeout(15000L) {
                    telephonyRepo.executeUssd(
                        ussdCode = ussdStr,
                        subscriptionId = resolvedSimId,
                        isAr = _uiState.value.isAr
                    )
                }

                val simObj = state.sims.find { it.subscriptionId == resolvedSimId }
                val simLabel = simObj?.displayName ?: "SIM 1"
                val currentFormattedTime = java.text.SimpleDateFormat("dd/MM/yyyy • hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())

                when (result) {
                    is UssdExecutionResult.Success -> {
                        val response = result.rawResponse
                        val translation = UssdManager.translateResponse(response, _uiState.value.language)
                        if (translation.isSuccess) {
                            val currentBal = _uiState.value.balanceResult.toDoubleOrNull() ?: settingsRepo.getBalance().toDoubleOrNull() ?: 0.0
                            val paidAmt = state.amount.toDoubleOrNull() ?: 0.0
                            val newBal = (currentBal - paidAmt).coerceAtLeast(0.0)
                            val formattedBal = if (newBal % 1.0 == 0.0) {
                                String.format(java.util.Locale.ENGLISH, "%.0f", newBal)
                            } else {
                                String.format(java.util.Locale.ENGLISH, "%.2f", newBal).trimEnd('0').trimEnd('.')
                            }
                            settingsRepo.setBalance(formattedBal)
                            settingsRepo.setLastRefreshTime(System.currentTimeMillis())
                            _uiState.updateState { it.copy(lastRefreshTime = System.currentTimeMillis()) }

                            val historyEntry = HistoryEntry(
                                number = UssdManager.normalizePhone(state.recipient),
                                name = state.recipientName,
                                amount = state.amount,
                                status = "COMPLETED",
                                timestamp = currentFormattedTime,
                                simName = simLabel,
                                type = state.paymentType.name
                            )
                            historyRepo.addHistory(historyEntry)

                            _uiState.updateState { it.copy(
                                balanceResult = formattedBal,
                                isConfirmLoading = false,
                                showConfirmDialog = false,
                                currentScreen = Screen.PAYMENT_SUCCESS,
                                successMessage = translation.text,
                                secretCode = if (it.rememberPin) it.secretCode else "",
                                showUpdateBadge = false,
                                lastTransactionId = "",
                                lastTransactionTimestamp = currentFormattedTime
                            ) }
                        } else {
                            val historyEntry = HistoryEntry(
                                number = UssdManager.normalizePhone(state.recipient),
                                name = state.recipientName,
                                amount = state.amount,
                                status = "FAILED",
                                timestamp = currentFormattedTime,
                                simName = simLabel,
                                type = state.paymentType.name
                            )
                            historyRepo.addHistory(historyEntry)

                            _uiState.updateState { it.copy(
                                isConfirmLoading = false,
                                showConfirmDialog = false,
                                currentScreen = Screen.TRANSFER_FAILED,
                                failureReason = translation.text,
                                secretCode = if (it.rememberPin) it.secretCode else "",
                                lastTransactionId = "",
                                lastTransactionTimestamp = currentFormattedTime
                            ) }
                        }
                    }
                    is UssdExecutionResult.Failure -> {
                        val historyEntry = HistoryEntry(
                            number = UssdManager.normalizePhone(state.recipient),
                            name = state.recipientName,
                            amount = state.amount,
                            status = "FAILED",
                            timestamp = currentFormattedTime,
                            simName = simLabel,
                            type = state.paymentType.name
                        )
                        historyRepo.addHistory(historyEntry)

                        _uiState.updateState { it.copy(
                            isConfirmLoading = false,
                            showConfirmDialog = false,
                            currentScreen = Screen.TRANSFER_FAILED,
                            failureReason = result.message,
                            secretCode = if (it.rememberPin) it.secretCode else "",
                            lastTransactionId = "",
                            lastTransactionTimestamp = currentFormattedTime
                        ) }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val simObj = state.sims.find { it.subscriptionId == resolvedSimId }
                val simLabel = simObj?.displayName ?: "SIM 1"
                val currentFormattedTime = java.text.SimpleDateFormat("dd/MM/yyyy • hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())

                val historyEntry = HistoryEntry(
                    number = UssdManager.normalizePhone(state.recipient),
                    name = state.recipientName.ifBlank { "Unknown" },
                    amount = state.amount,
                    status = "FAILED",
                    timestamp = currentFormattedTime,
                    simName = simLabel
                )
                historyRepo.addHistory(historyEntry)

                _uiState.updateState { it.copy(
                    isConfirmLoading = false,
                    showConfirmDialog = false
                ) }
                showError(getAppStrings(_uiState.value.isAr).errSimNoResponse)
            } catch (e: Exception) {
                _uiState.updateState { it.copy(
                    isConfirmLoading = false,
                    showConfirmDialog = false
                ) }
                showError(getAppStrings(_uiState.value.isAr).errSimConnectFailed)
            }
        }
    }

    fun checkBalance(context: Context? = null) {
        if (_uiState.value.isBalanceLoading) return

        _uiState.updateState { it.copy(isBalanceLoading = true, errorMessage = "", successMessage = "", showUpdateBadge = false) }

        if (_uiState.value.sims.isEmpty()) {
            loadSims()
        }

        val currentSims = _uiState.value.sims
        val resolvedSimId = _uiState.value.selectedSimId ?: currentSims.firstOrNull()?.subscriptionId

        val ussdStr = UssdManager.buildBalanceString()

        balanceJob = viewModelScope.launch {
            try {
                val result = withTimeout(15000L) {
                    telephonyRepo.executeUssd(
                        ussdCode = ussdStr,
                        subscriptionId = resolvedSimId,
                        isAr = _uiState.value.isAr
                    )
                }
                when (result) {
                    is UssdExecutionResult.Success -> {
                        val response = result.rawResponse
                        val translation = UssdManager.translateResponse(response, _uiState.value.language)
                        if (translation.isError) {
                            _uiState.updateState { it.copy(isBalanceLoading = false) }
                            showError(translation.text)
                        } else {
                            val extracted = UssdManager.extractBalanceAmount(response)
                            if (extracted.isNotBlank()) {
                                val prevBal = settingsRepo.getBalance().toDoubleOrNull()
                                val newBal = extracted.toDoubleOrNull()
                                if (prevBal != null && newBal != null && prevBal != newBal) {
                                    val diff = newBal - prevBal
                                    val formattedDiff = if (diff % 1.0 == 0.0) {
                                        String.format(java.util.Locale.ENGLISH, "%+.0f", diff)
                                    } else {
                                        String.format(java.util.Locale.ENGLISH, "%+.2f", diff).trimEnd('0').trimEnd('.')
                                    }
                                    settingsRepo.setBalanceDifference(formattedDiff)
                                    _uiState.updateState { it.copy(balanceDifference = formattedDiff) }
                                }
                                settingsRepo.setBalance(extracted)
                                settingsRepo.setLastRefreshTime(System.currentTimeMillis())
                                _uiState.updateState { it.copy(lastRefreshTime = System.currentTimeMillis()) }
                                _uiState.updateState { it.copy(
                                    isBalanceLoading = false,
                                    balanceResult = extracted
                                ) }
                            } else {
                                _uiState.updateState { it.copy(isBalanceLoading = false) }
                            }
                        }
                    }
                    is UssdExecutionResult.Failure -> {
                        _uiState.updateState { it.copy(isBalanceLoading = false) }
                        showError(getAppStrings(_uiState.value.isAr).errUssdFailed)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.updateState { it.copy(isBalanceLoading = false) }
                showError(getAppStrings(_uiState.value.isAr).errSimNoResponse)
            } catch (e: Exception) {
                _uiState.updateState { it.copy(isBalanceLoading = false) }
                showError(getAppStrings(_uiState.value.isAr).errSimConnectFailed)
            }
        }
    }

    fun resolveContact(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawPhone = contactsRepo.getPhoneFromUri(uri)
                if (rawPhone != null) {
                    val normalized = UssdManager.normalizePhone(rawPhone)
                    val displayName = contactsRepo.lookupName(normalized) ?: contactsRepo.getNameFromUri(uri) ?: ""
                    withContext(Dispatchers.Main) {
                        _uiState.updateState { it.copy(
                            recipient = normalized,
                            recipientName = displayName
                        ) }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Could not retrieve a valid phone number for this contact.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error resolving contact: ${e.message}")
                }
            }
        }
    }

    fun resolveContact(context: Context, uri: Uri) = resolveContact(uri)

    fun setRecipientFromQr(content: String, context: Context? = null) {
        try {
            val parsed = PmaQrManager.parseQrCode(content)
            if (parsed != null) {
                val (normalizedPhone, resolvedType) = parsed
                if (normalizedPhone.startsWith("050") || (resolvedType == PaymentType.MERCHANT && normalizedPhone.startsWith("050"))) {
                    _uiState.updateState { it.copy(
                        recipient = "",
                        recipientName = "",
                        paymentType = PaymentType.MERCHANT
                    ) }
                    showError(
                        if (_uiState.value.isAr)
                            "رمز QR للتجار متوقف مؤقتاً.\nيرجى إدخال رقم التاجر مبدوءاً بـ 059"
                        else
                            "Merchant QR is temporarily disabled.\nPlease enter the merchant number starting with 059"
                    )
                    return
                }

                viewModelScope.launch(Dispatchers.IO) {
                    val displayName = contactsRepo.lookupName(normalizedPhone) ?: ""
                    withContext(Dispatchers.Main) {
                        _uiState.updateState { it.copy(
                            recipient = normalizedPhone,
                            recipientName = displayName,
                            paymentType = resolvedType,
                            errorMessage = ""
                        ) }
                    }
                }
            } else {
                showError(getAppStrings(_uiState.value.isAr).errInvalidQr)
            }
        } catch (e: Exception) {
            showError(getAppStrings(_uiState.value.isAr).errInvalidQr)
        }
    }

    fun setQrDialogVisible(visible: Boolean) {
        _uiState.updateState { it.copy(showQrDialog = visible) }
    }

    fun refillForRetry(number: String, amount: String, typeName: String? = null) {
        val paymentType = if (typeName != null) {
            try { PaymentType.valueOf(typeName) } catch (e: Exception) { PaymentType.FRIEND }
        } else {
            _uiState.value.paymentType
        }

        _uiState.updateState { it.copy(
            recipient = number,
            amount = amount,
            paymentType = paymentType,
            secretCode = if (it.rememberPin) it.secretCode else "",
            currentScreen = Screen.MAIN,
            errorMessage = "",
            successMessage = "",
            failureReason = ""
        ) }
    }
}
