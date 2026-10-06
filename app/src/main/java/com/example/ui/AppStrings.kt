package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Type-safe, compile-time verified localization contract for USSD Pay.
 * Eliminates scattered inline `if (isAr)` conditionals across the codebase.
 */
interface AppStrings {
    val tabPay: String
    val tabHistory: String
    val tabSettings: String
    val appTitle: String
    val appSubtitle: String

    val yourBalance: String
    val lastUpdatedJustNow: String
    val lastUpdatedPrefix: String
    val updatingBalance: String
    val releaseToUpdate: String
    val pullToUpdate: String
    val diffTooltip: String
    val am: String
    val pm: String
    val amountLabel: String
    val friend: String
    val merchant: String
    val pinPlaceholder: String
    val payButton: String
    val ussdHelper: String

    val confirmTitle: String
    val transferAmount: String
    val recipient: String
    val simCard: String
    val confirm: String
    val cancel: String
    val biometricTitle: String
    val biometricSubtitle: String

    val selfQrTitle: String
    val enterPhoneForQr: String
    val showQrHelper: String

    val welcome: String
    val phoneSetupSubtitle: String
    val phoneNumber: String
    val continueText: String
    val skip: String

    val historyTitle: String
    val searchPlaceholder: String
    val exportCsv: String
    val exportPdf: String
    val clearHistory: String
    val noTransactions: String
    val noResults: String
    val retry: String
    val success: String
    val failed: String
    val today: String
    val yesterday: String
    val dayBeforeYesterday: String
    val earlier: String
    val sortNewestFirst: String
    val sortOldestFirst: String

    val settingsTitle: String
    val profile: String
    val language: String
    val arabic: String
    val english: String
    val darkMode: String
    val biometricAuth: String
    val privacyPolicy: String
    val aboutApp: String

    val paymentSent: String
    val paymentFailed: String
    val returnHome: String
    val paymentSuccess: String
    val transferCompleted: String
    val transferFailedSubtitle: String
    val shareReceipt: String
    val payAgain: String
    val retryPayment: String
    val noDeduction: String
    val insufficientFunds: String
    val networkLabel: String
    val reasonLabel: String

    val amountHeader: String get() = amountLabel
    val recipientHeader: String get() = recipient
    val networkHeader: String get() = networkLabel

    // Standardized error messages
    val errAirplaneMode: String
    val errInvalidPhone: String
    val errMinAmount: String
    val errPinRequired: String
    val errSimNoResponse: String
    val errSimConnectFailed: String
    val errUssdFailed: String
    val errInvalidQr: String

    fun simSlotName(slot: Int): String
}

object EnglishStrings : AppStrings {
    override val tabPay = "Pay"
    override val tabHistory = "History"
    override val tabSettings = "Settings"
    override val appTitle = "USSD Pay"
    override val appSubtitle = "Jawwal Pay"

    override val yourBalance = "Your Balance"
    override val lastUpdatedJustNow = "Last Updated: Just now"
    override val lastUpdatedPrefix = "Last Updated: "
    override val updatingBalance = "Updating balance..."
    override val releaseToUpdate = "Release to update"
    override val pullToUpdate = "Pull to update"
    override val diffTooltip = "Difference from last refresh"
    override val am = "AM"
    override val pm = "PM"
    override val amountLabel = "Amount"
    override val friend = "Friend"
    override val merchant = "Merchant"
    override val pinPlaceholder = "PIN"
    override val payButton = "Pay"
    override val ussdHelper = "Processed securely via USSD protocol"

    override val confirmTitle = "Confirm Payment"
    override val transferAmount = "Transfer Amount"
    override val recipient = "Recipient"
    override val simCard = "SIM Card"
    override val confirm = "Confirm"
    override val cancel = "Cancel"
    override val biometricTitle = "Confirm Payment"
    override val biometricSubtitle = "Use fingerprint to authorize"

    override val selfQrTitle = "My QR Code"
    override val enterPhoneForQr = "Enter phone to generate QR"
    override val showQrHelper = "Show this QR to the sender to scan and pay"

    override val welcome = "Welcome"
    override val phoneSetupSubtitle = "Enter your 10-digit Jawwal phone number to start"
    override val phoneNumber = "Your Phone Number"
    override val continueText = "Continue"
    override val skip = "Skip"

    override val historyTitle = "Transaction History"
    override val searchPlaceholder = "Search by number or amount..."
    override val exportCsv = "Export CSV"
    override val exportPdf = "Export PDF"
    override val clearHistory = "Clear History"
    override val noTransactions = "No transactions yet"
    override val noResults = "No results matching your search"
    override val retry = "Retry"
    override val success = "Success"
    override val failed = "Failed"
    override val today = "Today"
    override val yesterday = "Yesterday"
    override val dayBeforeYesterday = "Day Before Yesterday"
    override val earlier = "Earlier"
    override val sortNewestFirst = "Sort: Newest First"
    override val sortOldestFirst = "Sort: Oldest First"

    override val settingsTitle = "Settings"
    override val profile = "Profile"
    override val language = "Language"
    override val arabic = "العربية"
    override val english = "English"
    override val darkMode = "Dark Mode"
    override val biometricAuth = "Biometric Authentication"
    override val privacyPolicy = "Privacy Policy"
    override val aboutApp = "About App"

    override val paymentSent = "Payment Sent Successfully"
    override val paymentFailed = "Payment Failed"
    override val returnHome = "Return to Home"
    override val paymentSuccess = "Payment Success"
    override val transferCompleted = "Transfer completed successfully"
    override val transferFailedSubtitle = "Could not process transfer"
    override val shareReceipt = "Share Receipt"
    override val payAgain = "Pay Again"
    override val retryPayment = "Retry Payment"
    override val noDeduction = "No funds were deducted from your account"
    override val insufficientFunds = "Insufficient Funds"
    override val networkLabel = "Network"
    override val reasonLabel = "Reason"

    override val errAirplaneMode = "Airplane mode is active. Please disable it to process transactions."
    override val errInvalidPhone = "Please enter a valid phone number."
    override val errMinAmount = "Minimum amount is 1."
    override val errPinRequired = "Please enter a 4-digit PIN."
    override val errSimNoResponse = "SIM not responding. Ensure it is active and has coverage."
    override val errSimConnectFailed = "Failed to connect to SIM. Ensure it is active."
    override val errUssdFailed = "USSD code failed. SIM may be inactive or no coverage."
    override val errInvalidQr = "Invalid QR content scanned."

    override fun simSlotName(slot: Int): String = "SIM $slot"
}

object ArabicStrings : AppStrings {
    override val tabPay = "تحويل"
    override val tabHistory = "السجل"
    override val tabSettings = "الضبط"
    override val appTitle = "USSD Pay"
    override val appSubtitle = "Jawwal Pay"

    override val yourBalance = "رصيد محفظتك"
    override val lastUpdatedJustNow = "تم التحديث للتو"
    override val lastUpdatedPrefix = "آخر تحديث: "
    override val updatingBalance = "جارٍ تحديث الرصيد..."
    override val releaseToUpdate = "أفلت للتحديث"
    override val pullToUpdate = "اسحب للتحديث"
    override val diffTooltip = "الفرق عن آخر تحديث"
    override val am = "ص"
    override val pm = "م"
    override val amountLabel = "المبلغ"
    override val friend = "صديق"
    override val merchant = "تاجر"
    override val pinPlaceholder = "PIN رمز"
    override val payButton = "ادفع"
    override val ussdHelper = "معالجة آمنة ومباشرة عبر بروتوكول USSD"

    override val confirmTitle = "تأكيد عملية الدفع"
    override val transferAmount = "مبلغ التحويل"
    override val recipient = "المستلم"
    override val simCard = "الشريحة المستخدمة"
    override val confirm = "تأكيد"
    override val cancel = "إلغاء"
    override val biometricTitle = "تأكيد الدفع"
    override val biometricSubtitle = "استخدم البصمة لتأكيد العملية"

    override val selfQrTitle = "رمز QR الشخصي"
    override val enterPhoneForQr = "أدخل رقم هاتفك لإنشاء الرمز"
    override val showQrHelper = "أظهر هذا الرمز للمرسل لمسحه والدفع"

    override val welcome = "مرحباً بك"
    override val phoneSetupSubtitle = "أدخل رقم هاتف جوال المكون من 10 أرقام للبدء"
    override val phoneNumber = "رقم الهاتف الخاص بك"
    override val continueText = "متابعة"
    override val skip = "تخطي"

    override val historyTitle = "سجل العمليات"
    override val searchPlaceholder = "بحث بالرقم أو المبلغ..."
    override val exportCsv = "تصدير CSV"
    override val exportPdf = "تصدير PDF"
    override val clearHistory = "مسح السجل"
    override val noTransactions = "لا توجد حركات بعد"
    override val noResults = "لا توجد نتائج مطابقة لبحثك"
    override val retry = "إعادة المحاولة"
    override val success = "ناجحة"
    override val failed = "فاشلة"
    override val today = "اليوم"
    override val yesterday = "أمس"
    override val dayBeforeYesterday = "أول أمس"
    override val earlier = "سابقاً"
    override val sortNewestFirst = "ترتيب: الأحدث أولاً"
    override val sortOldestFirst = "ترتيب: الأقدم أولاً"

    override val settingsTitle = "الضبط"
    override val profile = "الملف الشخصي"
    override val language = "اللغة"
    override val arabic = "العربية"
    override val english = "English"
    override val darkMode = "الوضع الداكن"
    override val biometricAuth = "المصادقة بالبصمة"
    override val privacyPolicy = "سياسة الخصوصية"
    override val aboutApp = "عن التطبيق"

    override val paymentSent = "تم إرسال الدفعة بنجاح"
    override val paymentFailed = "فشلت عملية التحويل"
    override val returnHome = "العودة للرئيسية"
    override val paymentSuccess = "تم الدفع بنجاح"
    override val transferCompleted = "اكتملت المعاملة بنجاح"
    override val transferFailedSubtitle = "تعذر إتمام العملية"
    override val shareReceipt = "مشاركة الإيصال"
    override val payAgain = "دفع مجدداً"
    override val retryPayment = "إعادة المحاولة"
    override val noDeduction = "لم يتم خصم أي مبلغ من رصيدك"
    override val insufficientFunds = "رصيد غير كافٍ"
    override val networkLabel = "الشبكة"
    override val reasonLabel = "السبب"

    override val errAirplaneMode = "يرجى تعطيل وضع الطيران لإجراء العملية."
    override val errInvalidPhone = "أدخل رقم هاتف صحيح"
    override val errMinAmount = "الحد الأدنى للمبلغ هو 1"
    override val errPinRequired = "أدخل الرمز السري من 4 أرقام"
    override val errSimNoResponse = "لا يوجد استجابة من الشريحة. تأكد من تفعيلها."
    override val errSimConnectFailed = "فشل الاتصال بالشريحة. تأكد من أنها مفعلة."
    override val errUssdFailed = "فشل رمز USSD. الشريحة غير مفعلة أو لا توجد تغطية."
    override val errInvalidQr = "رمز QR غير صالح"

    override fun simSlotName(slot: Int): String = "شريحة $slot"
}

fun getAppStrings(isAr: Boolean): AppStrings = if (isAr) ArabicStrings else EnglishStrings

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }

/**
 * Access the current localized AppStrings inside any Composable.
 */
val appStrings: AppStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalAppStrings.current
