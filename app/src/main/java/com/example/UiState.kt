package com.example

enum class Screen {
    ONBOARDING,
    PHONE_SETUP,
    MAIN,
    HISTORY,
    SETTINGS,
    PAYMENT_SUCCESS,
    TRANSFER_FAILED,
    PRIVACY_POLICY
}

data class UiState(
    val currentScreen: Screen = Screen.MAIN,
    val language: AppLanguage = AppLanguage.AR,
    val paymentType: PaymentType = PaymentType.FRIEND,
    val recipient: String = "",
    val recipientName: String = "",
    val amount: String = "10",
    val secretCode: String = "",
    val balanceResult: String = "",
    val selfPhone: String = "",
    val payAttempted: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val showQrDialog: Boolean = false,
    val errorMessage: String = "",
    val successMessage: String = "",
    val failureReason: String = "",
    val sims: List<SimEntry> = emptyList(),
    val selectedSimId: Int? = null,
    val history: List<HistoryEntry> = emptyList(),
    val isDebounced: Boolean = false,
    val isAr: Boolean = true,
    val isBalanceLoading: Boolean = false,
    val isConfirmLoading: Boolean = false,
    val showUpdateBadge: Boolean = false,
    val isDarkMode: Boolean = false,
    val hideBalance: Boolean = false,
    val rememberPin: Boolean = true,
    val lastRefreshTime: Long = 0L,
    val historySortAscending: Boolean = false,
    val lastTransactionId: String = "",
    val lastTransactionTimestamp: String = "",
    val balanceDifference: String = ""
)

