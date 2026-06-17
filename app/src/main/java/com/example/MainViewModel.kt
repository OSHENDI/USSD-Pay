package com.example

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var payJob: Job? = null
    private var balanceJob: Job? = null
    private var prefsManager: PrefsManager? = null

    fun initialize(context: Context) {
        if (prefsManager == null) {
            prefsManager = PrefsManager(context)
            val lang = prefsManager!!.getLanguage()
            val isDark = prefsManager!!.getIsDarkMode()
            val selfPhone = prefsManager!!.getSelfPhone()
            val savedSimId = prefsManager!!.getSelectedSimId()
            val balance = prefsManager!!.getBalance()
            val onboarded = prefsManager!!.isOnboardingCompleted()
            
            val initialScreen = if (onboarded) Screen.MAIN else Screen.ONBOARDING

            _uiState.updateState { it.copy(
                currentScreen = initialScreen,
                language = lang,
                isAr = lang == AppLanguage.AR,
                isDarkMode = isDark,
                selfPhone = selfPhone,
                balanceResult = balance,
                selectedSimId = if (savedSimId != -1) savedSimId else null
            ) }
        }
    }

    private inline fun <T> MutableStateFlow<T>.updateState(newVal: (T) -> T) {
        val prev = this.value
        val updated = newVal(prev)
        if (prev != updated) {
            this.value = updated
        }
    }

    fun loadSims(context: Context) {
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
            if (sm == null) return
            val list = sm.activeSubscriptionInfoList
            val mapped = list?.map { info ->
                var phoneNumber: String? = null
                try {
                    @Suppress("DEPRECATION")
                    phoneNumber = info.number
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        try {
                            phoneNumber = sm.getPhoneNumber(info.subscriptionId)
                        } catch (e: Exception) {}
                    }
                    if (phoneNumber != null && (!phoneNumber.startsWith("05") || phoneNumber.length > 10)) {
                        phoneNumber = null
                    }
                } catch (e: Exception) { }

                SimEntry(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    phoneNumber = phoneNumber
                )
            } ?: emptyList()
            _uiState.updateState { state ->
                val newSelectedSim = if (state.selectedSimId != null && mapped.any { it.subscriptionId == state.selectedSimId }) {
                    state.selectedSimId
                } else {
                    val saved = prefsManager?.getSelectedSimId() ?: -1
                    if (saved != -1 && mapped.any { it.subscriptionId == saved }) saved else mapped.firstOrNull()?.subscriptionId
                }
                state.copy(
                    sims = mapped,
                    selectedSimId = newSelectedSim
                )
            }
        } catch (e: SecurityException) {
            // Permission not granted yet
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun selectSim(subscriptionId: Int) {
        prefsManager?.setSelectedSimId(subscriptionId)
        _uiState.updateState { it.copy(selectedSimId = subscriptionId) }
    }

    fun loadHistory(context: Context) {
        val resolverContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val items = HistoryManager(resolverContext).getHistory().map { entry ->
                if (entry.name.isBlank()) {
                    val contactName = ContactsHelper.lookupName(resolverContext, entry.number)
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

    fun updateRecipient(phone: String, context: Context) {
        val filtered = phone.filter { it.isDigit() }
        if (filtered.length > 10) return
        
        if (filtered.length == 10) {
            _uiState.updateState { it.copy(recipient = filtered) }
            val resolverContext = context.applicationContext
            viewModelScope.launch(Dispatchers.IO) {
                val displayName = ContactsHelper.lookupName(resolverContext, UssdManager.normalizePhone(filtered)) ?: ""
                withContext(Dispatchers.Main) {
                    _uiState.updateState { it.copy(recipientName = displayName) }
                }
            }
        } else {
            _uiState.updateState { it.copy(recipient = filtered, recipientName = "") }
        }
    }

    fun updateAmount(amountVal: String) {
        val filtered = amountVal.filter { it.isDigit() || it == '.' }
        if (filtered.startsWith("0")) return
        _uiState.updateState { it.copy(amount = filtered) }
    }

    fun updatePaymentType(type: PaymentType) {
        _uiState.updateState { it.copy(paymentType = type) }
    }

    fun setSecretCode(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _uiState.updateState { it.copy(secretCode = pin) }
        }
    }

    fun toggleLanguage() {
        _uiState.updateState { state ->
            val newLang = if (state.language == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
            prefsManager?.setLanguage(newLang)
            state.copy(
                language = newLang,
                isAr = newLang == AppLanguage.AR
            )
        }
    }

    fun setDarkMode(enabled: Boolean) {
        prefsManager?.setIsDarkMode(enabled)
        _uiState.updateState { it.copy(isDarkMode = enabled) }
    }

    fun setSelfPhone(phone: String) {
        val filtered = phone.filter { it.isDigit() }
        if (filtered.length <= 10) {
            prefsManager?.setSelfPhone(filtered)
            _uiState.updateState { it.copy(selfPhone = filtered) }
        }
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

    fun isAirplaneModeOn(context: Context): Boolean {
        return try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }

    fun requestPay(context: Context) {
        _uiState.updateState { it.copy(errorMessage = "", successMessage = "", payAttempted = true) }
        
        if (isAirplaneModeOn(context)) {
            showError(if (_uiState.value.isAr) "يرجى تعطيل وضع الطيران لإجراء العملية." else "Airplane mode is active. Please disable it to process transactions.")
            return
        }
        
        val state = _uiState.value
        
        if (state.sims.isEmpty() && state.selectedSimId == null) {
            // Give it one defensive scan reload
            loadSims(context)
        }
        
        val updatedState = _uiState.value
        
        if (updatedState.recipient.isBlank() || !UssdManager.isValidPhone(updatedState.recipient)) {
            showError(if (updatedState.isAr) "أدخل رقم هاتف صحيح" else "Please enter a valid phone number.")
            return
        }
        
        val amountNum = updatedState.amount.toDoubleOrNull() ?: 0.0
        if (updatedState.amount.isBlank() || amountNum < 1.0) {
            showError(if (updatedState.isAr) "أدخل مبلغ صحيح" else "Please enter a valid amount.")
            return
        }
        
        if (updatedState.secretCode.isBlank() || updatedState.secretCode.length != 4) {
            showError(if (updatedState.isAr) "أدخل الرمز السري من 4 أرقام" else "Please enter a 4-digit PIN.")
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
            amount = "",
            recipient = "",
            recipientName = "",
            secretCode = "",
            errorMessage = "",
            successMessage = "",
            failureReason = ""
        ) }
    }
    
    fun completeOnboarding() {
        prefsManager?.setOnboardingCompleted(true)
        if (_uiState.value.selfPhone.isEmpty()) {
            navigateTo(Screen.PHONE_SETUP)
        } else {
            navigateTo(Screen.MAIN)
        }
    }

    fun confirmPay(context: Context) {
        if (_uiState.value.isConfirmLoading) return
        
        _uiState.updateState { it.copy(isConfirmLoading = true, errorMessage = "", successMessage = "", failureReason = "") }
        
        if (_uiState.value.sims.isEmpty()) {
            loadSims(context)
        }
        
        val currentSims = _uiState.value.sims
        val resolvedSimId = _uiState.value.selectedSimId ?: currentSims.firstOrNull()?.subscriptionId
        
        val state = _uiState.value
        val ussdStr = UssdManager.buildPaymentString(state.paymentType, state.secretCode, state.recipient, state.amount)
        
        payJob = viewModelScope.launch {
            try {
                withTimeout(5000L) {
                    UssdManager.sendUssd(
                        context = context,
                        ussdCode = ussdStr,
                        subscriptionId = resolvedSimId,
                        onResponse = { response ->
                            payJob?.cancel()
                            val translation = UssdManager.translateResponse(response, _uiState.value.language)
                            if (translation.isSuccess) {
                                val simObj = state.sims.find { it.subscriptionId == resolvedSimId }
                                val simLabel = simObj?.displayName ?: "SIM 1"
                                val currentFormattedTime = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
                                
                                val historyEntry = HistoryEntry(
                                    number = UssdManager.normalizePhone(state.recipient),
                                    name = state.recipientName,
                                    amount = state.amount,
                                    status = "COMPLETED",
                                    timestamp = currentFormattedTime,
                                    simName = simLabel,
                                    type = state.paymentType.name
                                )
                                HistoryManager(context).addHistory(historyEntry)
                                loadHistory(context)
                                _uiState.updateState { it.copy(
                                    isConfirmLoading = false,
                                    showConfirmDialog = false,
                                    currentScreen = Screen.PAYMENT_SUCCESS,
                                    successMessage = translation.text,
                                    secretCode = "",
                                    showUpdateBadge = true
                                ) }
                            } else {
                                val simObj = state.sims.find { it.subscriptionId == resolvedSimId }
                                val simLabel = simObj?.displayName ?: "SIM 1"
                                val currentFormattedTime = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
                                
                                val historyEntry = HistoryEntry(
                                    number = UssdManager.normalizePhone(state.recipient),
                                    name = state.recipientName,
                                    amount = state.amount,
                                    status = "FAILED",
                                    timestamp = currentFormattedTime,
                                    simName = simLabel,
                                    type = state.paymentType.name
                                )
                                HistoryManager(context).addHistory(historyEntry)
                                loadHistory(context)
                                
                                _uiState.updateState { it.copy(
                                    isConfirmLoading = false, 
                                    showConfirmDialog = false,
                                    currentScreen = Screen.TRANSFER_FAILED,
                                    failureReason = translation.text,
                                    secretCode = ""
                                ) }
                            }
                        },
                        onError = { error ->
                            payJob?.cancel()
                            val simObj = state.sims.find { it.subscriptionId == resolvedSimId }
                            val simLabel = simObj?.displayName ?: "SIM 1"
                            val currentFormattedTime = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
                            
                            val historyEntry = HistoryEntry(
                                number = UssdManager.normalizePhone(state.recipient),
                                name = state.recipientName,
                                amount = state.amount,
                                status = "FAILED",
                                timestamp = currentFormattedTime,
                                simName = simLabel,
                                type = state.paymentType.name
                            )
                            HistoryManager(context).addHistory(historyEntry)
                            loadHistory(context)
                            
                            _uiState.updateState { it.copy(
                                isConfirmLoading = false, 
                                showConfirmDialog = false,
                                currentScreen = Screen.TRANSFER_FAILED,
                                failureReason = error,
                                secretCode = ""
                            ) }
                        }
                    )
                }
            } catch (e: TimeoutCancellationException) {
                val simObj = state.sims.find { it.subscriptionId == resolvedSimId }
                val simLabel = simObj?.displayName ?: "SIM 1"
                val currentFormattedTime = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
                
                val historyEntry = HistoryEntry(
                    number = UssdManager.normalizePhone(state.recipient),
                    name = state.recipientName.ifBlank { "Unknown" },
                    amount = state.amount,
                    status = "FAILED",
                    timestamp = currentFormattedTime,
                    simName = simLabel
                )
                HistoryManager(context).addHistory(historyEntry)
                loadHistory(context)
                
                _uiState.updateState { it.copy(
                    isConfirmLoading = false, 
                    showConfirmDialog = false
                ) }
                showError(if (_uiState.value.isAr) "لا يوجد استجابة من الشريحة. تأكد من تفعيلها." else "SIM not responding. Ensure it is active and has coverage.")
            } catch (e: Exception) {
                _uiState.updateState { it.copy(
                    isConfirmLoading = false, 
                    showConfirmDialog = false
                ) }
                showError(if (_uiState.value.isAr) "فشل الاتصال بالشريحة. تأكد من أنها مفعلة." else "Failed to connect to SIM. Ensure it is active.")
            }
        }
    }

    fun checkBalance(context: Context) {
        if (_uiState.value.isBalanceLoading) return
        
        _uiState.updateState { it.copy(isBalanceLoading = true, errorMessage = "", successMessage = "", showUpdateBadge = false) }
        
        if (_uiState.value.sims.isEmpty()) {
            loadSims(context)
        }
        
        val currentSims = _uiState.value.sims
        val resolvedSimId = _uiState.value.selectedSimId ?: currentSims.firstOrNull()?.subscriptionId
        
        val ussdStr = UssdManager.buildBalanceString()
        
        balanceJob = viewModelScope.launch {
            try {
                withTimeout(5000L) {
                    UssdManager.sendUssd(
                        context = context,
                        ussdCode = ussdStr,
                        subscriptionId = resolvedSimId,
                        onResponse = { response ->
                            balanceJob?.cancel()
                            val translation = UssdManager.translateResponse(response, _uiState.value.language)
                            if (translation.isError) {
                                _uiState.updateState { it.copy(isBalanceLoading = false) }
                                showError(translation.text)
                            } else {
                                val extracted = UssdManager.extractBalanceAmount(response)
                                prefsManager?.setBalance(extracted)
                                _uiState.updateState { it.copy(
                                    isBalanceLoading = false,
                                    balanceResult = extracted.ifBlank { "0.0" }
                                ) }
                            }
                        },
                        onError = { error ->
                            balanceJob?.cancel()
                            _uiState.updateState { it.copy(isBalanceLoading = false) }
                            showError(if (_uiState.value.isAr) "فشل رمز USSD. الشريحة غير مفعلة أو لا توجد تغطية." else "USSD code failed. SIM may be inactive or no coverage.")
                        }
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.updateState { it.copy(isBalanceLoading = false) }
                showError(if (_uiState.value.isAr) "لا يوجد استجابة من الشريحة. تأكد من تفعيلها." else "SIM not responding. Ensure it is active and has coverage.")
            } catch (e: Exception) {
                _uiState.updateState { it.copy(isBalanceLoading = false) }
                showError(if (_uiState.value.isAr) "فشل الاتصال بالشريحة. تأكد من أنها مفعلة." else "Failed to connect to SIM. Ensure it is active.")
            }
        }
    }

    fun resolveContact(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawPhone = ContactsHelper.getPhoneFromUri(context, uri)
                if (rawPhone != null) {
                    val normalized = UssdManager.normalizePhone(rawPhone)
                    val displayName = ContactsHelper.lookupName(context, normalized) ?: ContactsHelper.getNameFromUri(context, uri) ?: ""
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

    fun setRecipientFromQr(content: String, context: Context) {
        try {
            if (content.contains("|")) {
                val parts = content.split("|")
                val rawPhone = parts[0].trim()
                val resolvedType = if (parts.size > 1 && parts[1].trim().lowercase() == "merchant") {
                    PaymentType.MERCHANT
                } else {
                    PaymentType.FRIEND
                }
                val normalized = UssdManager.normalizePhone(rawPhone)
                val displayName = ContactsHelper.lookupName(context, normalized) ?: ""
                _uiState.updateState { it.copy(
                    recipient = normalized,
                    recipientName = displayName,
                    paymentType = resolvedType
                ) }
            } else {
                val normalized = UssdManager.normalizePhone(content)
                val displayName = ContactsHelper.lookupName(context, normalized) ?: ""
                _uiState.updateState { it.copy(
                    recipient = normalized,
                    recipientName = displayName,
                    paymentType = PaymentType.FRIEND
                ) }
            }
        } catch (e: Exception) {
            showError("Invalid QR content scanned.")
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
            secretCode = "",
            currentScreen = Screen.MAIN,
            errorMessage = "",
            successMessage = "",
            failureReason = ""
        ) }
    }
}
