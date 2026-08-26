package com.example.ui

import android.graphics.Bitmap
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

val RequiredPermissions = arrayOf(
    android.Manifest.permission.CALL_PHONE,
    android.Manifest.permission.READ_PHONE_STATE,
    android.Manifest.permission.READ_PHONE_NUMBERS
)

fun hasAllPermissions(context: android.content.Context): Boolean {
    return RequiredPermissions.all {
        androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

fun formatSimName(sim: SimEntry?, slotIndexFallback: Int = 0, isAr: Boolean): String {
    if (sim == null) {
        return if (isAr) "الشريحة ${slotIndexFallback + 1}" else "SIM ${slotIndexFallback + 1}"
    }
    val slot = sim.slotIndex + 1
    val dName = sim.displayName.trim()
    return if (dName.equals("SIM 1", ignoreCase = true) || dName.equals("SIM 2", ignoreCase = true) || dName.startsWith("SIM", ignoreCase = true)) {
        if (isAr) "الشريحة $slot" else "SIM $slot"
    } else {
        "$dName $slot"
    }
}

fun formatDisplayDate(raw: String, isAr: Boolean = false): String {
    if (raw.isBlank()) return ""
    val slashRegex = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{4})\b""")
    val slashMatch = slashRegex.find(raw)
    if (slashMatch != null) {
        val (d, m, y) = slashMatch.destructured
        val day = d.padStart(2, '0')
        val month = m.padStart(2, '0')
        val timePart = if (raw.contains("•")) {
            val rawTime = raw.substringAfter("•").trim()
            // Format time cleanly in LTR so AM/PM doesn't flip before the clock digits in RTL
            "\u200E • \u200E$rawTime\u200E"
        } else ""
        return "\u200E$day/$month/$y$timePart\u200E"
    }

    val formats = listOf(
        "MMM dd, yyyy • hh:mm a",
        "MMM d, yyyy • hh:mm a",
        "MMM dd, yyyy • HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    for (fmt in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
            val parsed = sdf.parse(raw)
            if (parsed != null) {
                val outSdf = java.text.SimpleDateFormat("dd/MM/yyyy • hh:mm a", java.util.Locale.ENGLISH)
                val formatted = outSdf.format(parsed)
                val dPart = formatted.substringBefore("•").trim()
                val tPart = formatted.substringAfter("•").trim()
                return "\u200E$dPart\u200E • \u200E$tPart\u200E"
            }
        } catch (e: Exception) { }
    }
    return "\u200E$raw\u200E"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: MainViewModel,
    onOpenScanner: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var pendingPayment by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (hasAllPermissions(context)) {
            viewModel.loadSims(context)
            if (pendingPayment) {
                viewModel.confirmPay(context)
            }
        }
        pendingPayment = false
    }

    LaunchedEffect(Unit) {
        viewModel.loadSims(context)
        viewModel.loadHistory(context)
    }

    val isRtl = state.isAr
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Back button press handling: go back to main screen if not already there
    BackHandler(enabled = state.currentScreen != Screen.MAIN) {
        viewModel.navigateTo(Screen.MAIN)
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Show bottom navigation bar only on main-tabbed screens (Main, History, Settings)
                if (state.currentScreen == Screen.MAIN || state.currentScreen == Screen.HISTORY || state.currentScreen == Screen.SETTINGS) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("app_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = state.currentScreen == Screen.MAIN,
                            onClick = { viewModel.navigateTo(Screen.MAIN) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "Pay",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(if (isRtl) "تحويل" else "Pay") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_pay")
                        )
                        NavigationBarItem(
                            selected = state.currentScreen == Screen.HISTORY,
                            onClick = { viewModel.navigateTo(Screen.HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Transactions",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(if (isRtl) "الحوالات" else "Transactions") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_history")
                        )
                        NavigationBarItem(
                            selected = state.currentScreen == Screen.SETTINGS,
                            onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(if (isRtl) "الضبط" else "Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Switchboard Router
                when (state.currentScreen) {
                    Screen.ONBOARDING -> OnboardingScreenContent(viewModel, isRtl)
                    Screen.PHONE_SETUP -> PhoneSetupScreenContent(state, viewModel, isRtl)
                    Screen.MAIN -> MainScreenContent(state, viewModel, isRtl, onOpenScanner)
                    Screen.HISTORY -> HistoryScreenContent(state, viewModel, isRtl)
                    Screen.SETTINGS -> SettingsScreenContent(state, viewModel, isRtl)
                    Screen.PAYMENT_SUCCESS -> PaymentSuccessScreenContent(state, viewModel, isRtl)
                    Screen.TRANSFER_FAILED -> TransferFailedScreenContent(state, viewModel, isRtl)
                    Screen.PRIVACY_POLICY -> PrivacyPolicyScreenContent(viewModel, isRtl)
                }

                // Global Error Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .zIndex(5f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.errorMessage.isNotEmpty(),
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut()
                    ) {
                        if (state.errorMessage.isNotEmpty()) {
                            androidx.compose.runtime.LaunchedEffect(state.errorMessage) {
                                kotlinx.coroutines.delay(4000)
                                viewModel.dismissError()
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("error_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = state.errorMessage,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(onClick = { viewModel.dismissError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 1. Confirm Payment Bottom Sheet ---
                if (state.showConfirmDialog) {
                    val selectedSim = state.sims.find { it.subscriptionId == state.selectedSimId } ?: state.sims.firstOrNull()
                    val formattedSim = formatSimName(selectedSim, 0, isRtl)
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.dismissConfirm() },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = null,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        ConfirmPaymentSheetContent(
                            recipient = state.recipient,
                            amount = state.amount,
                            simName = formattedSim,
                            isLoading = state.isConfirmLoading,
                            isAr = isRtl,
                            onConfirm = { 
                                if (hasAllPermissions(context)) {
                                    viewModel.confirmPay(context)
                                } else {
                                    pendingPayment = true
                                    permissionLauncher.launch(RequiredPermissions)
                                }
                            },
                            onCancel = { viewModel.dismissConfirm() }
                        )
                    }
                }

                // --- 2. Self QR Bottom Sheet ---
                if (state.showQrDialog) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.setQrDialogVisible(false) },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = null,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        val derivedNumber = state.selfPhone.ifBlank { state.sims.find { it.subscriptionId == state.selectedSimId }?.phoneNumber ?: state.sims.firstOrNull()?.phoneNumber ?: "" }
                        SelfQrSheetContent(
                            isAr = isRtl,
                            myExtractedNumber = derivedNumber,
                            onPhoneChange = { viewModel.setSelfPhone(it) },
                            onClose = { viewModel.setQrDialogVisible(false) }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// REALISTIC 3D TACTILE BUTTON (Nikolai Lehbrink physical lighting & shadow model)
// ============================================================================
@Composable
fun RealisticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    baseColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = Color(0xFF1B5E20),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Precise animations with 150ms FastOutSlowInEasing matching the CSS transition
    val colorAnimSpec = tween<Color>(durationMillis = 150, easing = FastOutSlowInEasing)

    // Background color shifts darker on press
    val currentBgColor by animateColorAsState(
        targetValue = if (isPressed) baseColor.copy(alpha = 0.88f).compositeOver(Color.Black) else baseColor,
        animationSpec = colorAnimSpec,
        label = "realistic_btn_bg"
    )

    // Inset top highlight line shifts from bright to subtle on press
    val currentInsetTop by animateColorAsState(
        targetValue = if (isPressed) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.45f),
        animationSpec = colorAnimSpec,
        label = "realistic_btn_inset"
    )

    // Outer elevation drops flat on press (4dp -> 0dp)
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 4.dp,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "realistic_btn_elevation"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) elevation else 0.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.3f),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            )
            .border(width = 1.dp, color = if (enabled) borderColor else Color.Transparent, shape = shape)
            .clip(shape)
            .background(if (enabled) currentBgColor else baseColor.copy(alpha = 0.38f))
            .drawWithContent {
                drawContent()
                if (enabled) {
                    // Simulated overhead diffuse lighting (white 18% to transparent)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )

                    // Simulated CSS inset shadow (1px highlight on the top edge)
                    drawLine(
                        color = currentInsetTop,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 1. MAIN SCREEN CONTENT
// ============================================================================
@Composable
fun MainScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    var pendingRefresh by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (hasAllPermissions(context)) {
            viewModel.loadSims(context)
            if (pendingRefresh) {
                viewModel.checkBalance(context)
            }
        }
        pendingRefresh = false
    }

    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Header Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "USSD Pay",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isAr) "جوال باي" else "Jawwal Pay",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // QR Generator Trigger
                IconButton(
                    onClick = { viewModel.setQrDialogVisible(true) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("main_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "My QR Code",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Current Balance Textured Card Overlay
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
        val dotsColor = onPrimaryColor.copy(alpha = 0.08f)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), // Vibrant green representation
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .testTag("balance_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height * 0.7f)
                            quadraticTo(
                                size.width * 0.4f, size.height * 0.5f,
                                size.width, size.height * 0.9f
                            )
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = onPrimaryColor.copy(alpha = 0.05f))
                        
                        val secondPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height * 0.4f)
                            quadraticTo(
                                size.width * 0.5f, size.height * 0.8f,
                                size.width, size.height * 0.3f
                            )
                            lineTo(size.width, 0f)
                            lineTo(0f, 0f)
                            close()
                        }
                        drawPath(secondPath, color = onPrimaryColor.copy(alpha = 0.03f))
                    }
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top row: Label and Refresh button aligned perfectly on center vertical axis
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "الرصيد الحالي" else "Current Balance",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        // Refresh Button wrapped in Box for corner badge
                        Box {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .clickable {
                                        if (hasAllPermissions(context)) {
                                            viewModel.checkBalance(context)
                                        } else {
                                            pendingRefresh = true
                                            permissionLauncher.launch(RequiredPermissions)
                                        }
                                    }
                                    .testTag("refresh_balance_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (state.isBalanceLoading) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isAr) "تحديث" else "Refresh",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (state.showUpdateBadge) {
                                val badgeAlignment = if (isAr) Alignment.TopStart else Alignment.TopEnd
                                Box(
                                    modifier = Modifier
                                        .align(badgeAlignment)
                                        .offset(
                                            x = if (isAr) (-4).dp else 4.dp,
                                            y = (-4).dp
                                        )
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFBC02D)), // High contrast yellow-gold
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "!",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Amount display (respects hideBalance toggle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayAmount = if (state.hideBalance || state.balanceResult.isEmpty()) "••••" else state.balanceResult
                        Text(
                            text = displayAmount,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₪",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SIM Selector Title & Outline Segmented Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAr) "اختر الشريحة" else "Select SIM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), RoundedCornerShape(16.dp))
                    .background(Color.Transparent), // Neutral inactive background
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If sims list is empty, display fallback dual buttons
                val activeSims = if (state.sims.isEmpty()) {
                    listOf(
                        SimEntry(1, 0, if (isAr) "الشريحة 1" else "SIM 1"),
                        SimEntry(2, 1, if (isAr) "الشريحة 2" else "SIM 2")
                    )
                } else state.sims

                activeSims.forEachIndexed { idx, sim ->
                    val isSelected = (state.selectedSimId == sim.subscriptionId) || (state.selectedSimId == null && idx == 0)
                    val simLabel = formatSimName(sim, idx, isAr)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { viewModel.selectSim(sim.subscriptionId) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = simLabel,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (idx < activeSims.lastIndex) {
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(0.6f),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Structured Input Card representing the main system form (Carrier design card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            // --- ROW 1: RECIPIENT PHONE ---
            val contactPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickContact()
            ) { uri ->
                if (uri != null) {
                    viewModel.resolveContact(context, uri)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAr) "رقم هاتف المستلم" else "Recipient Phone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Inline Toggle Pill Friend / Merchant
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(0.dp))
                            .background(if (state.paymentType == PaymentType.FRIEND) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { viewModel.updatePaymentType(PaymentType.FRIEND) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAr) "صديق" else "Friend",
                            color = if (state.paymentType == PaymentType.FRIEND) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(0.dp))
                            .background(if (state.paymentType == PaymentType.MERCHANT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { viewModel.updatePaymentType(PaymentType.MERCHANT) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAr) "تاجر" else "Merchant",
                            color = if (state.paymentType == PaymentType.MERCHANT) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text field for phone input
            val isRecipientError = state.payAttempted && (state.recipient.isBlank() || state.recipient.length != 10 || !state.recipient.startsWith("05"))
            TextField(
                value = state.recipient,
                onValueChange = { viewModel.updateRecipient(it, context) },
                modifier = Modifier.fillMaxWidth().testTag("recipient_phone_input"),
                placeholder = {
                    Text(
                        text = "05X XXXXXXX",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isRecipientError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { contactPickerLauncher.launch(null) },
                            modifier = Modifier.testTag("contact_picker_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PermContactCalendar,
                                contentDescription = "Pick Contact",
                                tint = if (isRecipientError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onOpenScanner,
                            modifier = Modifier.testTag("scan_qr_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = if (isRecipientError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                isError = isRecipientError,
                shape = androidx.compose.ui.graphics.RectangleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )

            if (state.recipientName.isNotEmpty()) {
                Text(
                    text = "${state.recipientName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                )
            }


            Spacer(modifier = Modifier.height(16.dp))
            // --- ROW 2: TRANSFER AMOUNT ---
            val amountNum = state.amount.toDoubleOrNull() ?: 0.0
            val isAmountError = state.payAttempted && (state.amount.isBlank() || amountNum < 1.0)
            Text(
                text = if (isAr) "مبلغ التحويل" else "Transfer Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextField(
                value = state.amount,
                onValueChange = { viewModel.updateAmount(it) },
                modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = if (isAmountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Text(
                        text = if (isAr) "شيكل" else "₪",
                        color = if (isAmountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                singleLine = true,
                isError = isAmountError,
                shape = androidx.compose.ui.graphics.RectangleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- ROW 3: SECRET PIN (UNDERLINE SPACES LOOKUP)
            Text(
                text = if (isAr) "الرمز السري (PIN)" else "Secret PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            val isPinError = state.payAttempted && (state.secretCode.isBlank() || state.secretCode.length != 4)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isPinError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in 0 until 4) {
                            val dotText = if (state.secretCode.length > i) "•" else ""
                            val isFocusedNow = state.secretCode.length == i
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = dotText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (isPinError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.height(30.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(
                                            when {
                                                isPinError -> MaterialTheme.colorScheme.error
                                                isFocusedNow -> MaterialTheme.colorScheme.primary
                                                else -> Color(0xFFB0BEC5)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                // Invisible overlay capture
                BasicTextField(
                    value = state.secretCode,
                    onValueChange = { viewModel.setSecretCode(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("secret_pin_field"),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Pay Now Button Action (Realistic Tactile Button)
        RealisticButton(
            text = if (isAr) "ادفع الآن" else "Pay now",
            icon = Icons.AutoMirrored.Filled.Send,
            onClick = { viewModel.requestPay(context) },
            isLoading = state.isConfirmLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("pay_now_button"),
            baseColor = MaterialTheme.colorScheme.primary,
            borderColor = Color(0xFF1B5E20),
            contentColor = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Small Brand Footer Description
        Text(
            text = if (isAr) "تتم المعاملات بأمان عبر بروتوكول USSD" else "Transactions processed securely via USSD protocol.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        // Spacing at bottom of form
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================================
// 2. HISTORY SCREEN CONTENT
// ============================================================================
@Composable
fun HistoryScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    var selectedTab by remember { mutableStateOf("All") }
    val filteredHistory = state.history.filter {
        when (selectedTab) {
            "Completed" -> it.status.uppercase() == "COMPLETED"
            "Rejected" -> it.status.uppercase() == "FAILED"
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-contrast Header with exit arrow
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.MAIN) },
                modifier = Modifier.testTag("history_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to home",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAr) "الحوالات" else "Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Horizontal filter bar (Tabs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("All", "Completed", "Rejected")
            val arTabs = listOf("الكل", "مكتملة", "مرفوضة")
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).clickable(interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { selectedTab = tab }
                ) {
                    Text(
                        text = if (isAr) arTabs[index] else tab,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(top = 0.dp, bottom = 12.dp))

        // Scrollable list content
        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) "لا توجد معاملات بعد" else "No recent transactions found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHistory) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { viewModel.refillForRetry(item.number, item.amount, item.type) }.testTag("history_item_${item.number}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            val isFailed = item.status.uppercase() == "FAILED"
                            val greenContainer = MaterialTheme.colorScheme.secondaryContainer
                            val greenText = MaterialTheme.colorScheme.primary

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val iconBgColor = if (isFailed) MaterialTheme.colorScheme.errorContainer else greenContainer
                                    val iconColor = if (isFailed) MaterialTheme.colorScheme.error else greenText

                                    // Circular icon representing transaction status
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(iconBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowOutward,
                                            contentDescription = "Outgoing",
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        if (item.name.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = if (isAr) "(غير مسجل)" else "(not registered)",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        Text(
                                            text = item.number,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                // Money representation and badge status
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "- ${item.amount} ₪",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Dynamic Badge status rendering
                                    val (badgeBg, badgeText, label) = when (item.status.uppercase()) {
                                        "FAILED" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, if (isAr) "مرفوضة" else "REJECTED")
                                        else -> Triple(greenContainer, greenText, if (isAr) "مكتملة" else "COMPLETED")
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = badgeText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDisplayDate(item.timestamp, isAr),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val displayedSim = if (isAr && item.simName.startsWith("SIM ")) {
                                    "الشريحة ${item.simName.substringAfter("SIM ")}"
                                } else {
                                    item.simName
                                }
                                Text(
                                    text = displayedSim,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. SETTINGS SCREEN CONTENT
// ============================================================================
@Composable
fun SettingsScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.MAIN) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAr) "الضبط" else "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Profile visually layered card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large styled picture avatar
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val myNumber = state.selfPhone.ifBlank { state.sims.find { it.subscriptionId == state.selectedSimId }?.phoneNumber ?: state.sims.firstOrNull()?.phoneNumber ?: "05X XXXXXXX" }
                Text(
                    text = myNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isAr) "رقم محفظتك" else "Your Wallet Number",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SIM MANAGEMENT SECTION
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAr) "إدارة الشرائح" else "SIM Management",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.sims.isEmpty()) {
                        // Fallback empty view or permission pending view
                        Row(modifier = Modifier.fillMaxWidth().alpha(0.6f)) {
                            Icon(imageVector = Icons.Default.SimCard, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (isAr) "لم يتم اكتشاف شرائح أو ننتظر الإذن..." else "No SIMs detected or waiting permission...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        state.sims.forEachIndexed { index, sim ->
                            val isActive = state.selectedSimId == sim.subscriptionId
                            val statusLabel = if (isActive) (if (isAr) "نشطة" else "Active") else (if (isAr) "متاحة" else "Ready")
                            val simTitle = if (isAr) "الشريحة ${index + 1}" else "SIM ${index + 1}"
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.selectSim(sim.subscriptionId) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = null,
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(text = simTitle, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${sim.displayName} • $statusLabel", 
                                                style = MaterialTheme.typography.bodySmall, 
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            if (!sim.phoneNumber.isNullOrBlank()) {
                                                Text(
                                                    text = " | ${sim.phoneNumber}", 
                                                    style = MaterialTheme.typography.bodySmall, 
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isActive) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = if (isAr) "افتراضي" else "Default", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Switch(
                                            checked = true,
                                            onCheckedChange = { },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                } else {
                                    Switch(
                                        checked = false,
                                        onCheckedChange = { viewModel.selectSim(sim.subscriptionId) },
                                    )
                                }
                            }

                            if (index < state.sims.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PREFERENCES & PRIVACY
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAr) "التفضيلات والخصوصية" else "Preferences & Privacy",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Item 1: App Theme
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Brightness4,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (isAr) "مظهر التطبيق" else "App Theme", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // App Theme Segmented Slider light / dark (fully rounded and reactive)
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .height(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(0.dp))
                                    .background(if (!state.isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { viewModel.setDarkMode(false) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) "فاتح" else "Light",
                                    color = if (!state.isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(0.dp))
                                    .background(if (state.isDarkMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { viewModel.setDarkMode(true) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) "داكن" else "Dark",
                                    color = if (state.isDarkMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Item 2: App Language
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { viewModel.toggleLanguage() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (isAr) "لغة التطبيق" else "App Language", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .height(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(0.dp))
                                    .background(if (!isAr) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { if (isAr) viewModel.toggleLanguage() }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "English",
                                    color = if (!isAr) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(0.dp))
                                    .background(if (isAr) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { if (!isAr) viewModel.toggleLanguage() }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "العربية",
                                    color = if (isAr) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Item 3: Balance Status (Show / Hide)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isAr) "حالة الرصيد" else "Balance Status",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .height(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(0.dp))
                                    .background(if (!state.hideBalance) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { viewModel.setHideBalance(false) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) "إظهار" else "Show",
                                    color = if (!state.hideBalance) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(0.dp))
                                    .background(if (state.hideBalance) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { viewModel.setHideBalance(true) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) "إخفاء" else "Hide",
                                    color = if (state.hideBalance) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { viewModel.navigateTo(Screen.PRIVACY_POLICY) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (isAr) "سياسة الخصوصية" else "Privacy Policy", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Item 3: Help & Support link
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { uriHandler.openUri("https://utiapps.netlify.app/support.html") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (isAr) "المساعدة والدعم" else "Help & Support", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic footer signatures
        Text(
            text = "USSD Pay",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isAr) "نسخة: v1.0.0 • التطبيق غير تابع لشركة الاتصالات." else "Version: v1.0.0 • not affiliated with any mobile carrier.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// 4. PAYMENT SUCCESSFUL SCREEN CONTENT
// ============================================================================
@Composable
fun PaymentSuccessScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Content layout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
            // Success circular symbol with glowing ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success tick",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isAr) "تم الدفع بنجاح" else "Payment Successful",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isAr) "اكتملت المعاملة بنجاح" else "Transaction Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rounded receipt parameters
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Row 1: Recipient phone/name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "المستلم" else "Recipient",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.recipient,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Row 2: Amount parameter in custom color
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "المبلغ" else "Amount",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${state.amount} ILS",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Row 3: SIM allocation badge selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "منفذ الشريحة" else "SIM Slot",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SIM 1",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Action Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.resetToMain() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("success_another_pay_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "إجراء عملية دفع أخرى" else "Make Another Payment",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.HISTORY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("success_view_history_btn"),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "عرض السجل" else "View History",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isAr) "تم معالجة المعاملة بنجاح عبر بروتوكول USSD الآمن" else "Transaction processed via secure USSD protocol",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}

// ============================================================================
// 5. TRANSFER FAILED SCREEN CONTENT
// ============================================================================
@Composable
fun TransferFailedScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.MAIN) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAr) "حالة التحويل" else "Transfer Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Center visual indication
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            // Red exclamation warn sign
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isAr) "فشل التحويل" else "Transfer Failed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isAr) "لم نتمكن من معالجة دفعتك. يرجى التحقق من التفاصيل والمحاولة مرة أخرى." else "We couldn't process your payment. Please check the details and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error details
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Reason row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "سبب الحالة" else "Status Reason",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = state.failureReason.ifBlank { "Insufficient Funds" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error, // Red failure indication
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Recipient Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "المستلم" else "Recipient",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (state.recipientName.isNotEmpty()) "${state.recipientName}\n${state.recipient}" else state.recipient,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Amount Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "المبلغ" else "Amount",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₪ ${state.amount}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // SIM card Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "شبكة الشريحة" else "SIM Network",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "SIM 1 (Primary)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Bottom Action Section Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.refillForRetry(state.recipient, state.amount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("fail_retry_pay_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "إعادة المحاولة" else "Retry Payment",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.resetToMain() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("fail_back_to_home_btn"),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isAr) "العودة للرئيسية" else "Back to Home",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Error Code: CORE_USSD_TIMEOUT_102",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// --- 6. CONFIRM PAYMENT BOTTOM SHEET CONTENT ---
// ============================================================================
@Composable
fun ConfirmPaymentSheetContent(
    recipient: String,
    amount: String,
    simName: String,
    isLoading: Boolean,
    isAr: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isAr) "تأكيد عملية الدفع" else "Confirm Payment",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Parameters visual border box
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "المستلم" else "Recipient",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = recipient,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("confirm_recipient")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "المبلغ" else "Amount",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$amount ₪",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("confirm_amount")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "الشريحة" else "SIM",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = simName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary solid button
        Button(
            onClick = onConfirm,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("confirm_action_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = if (isAr) "تأكيد" else "Confirm",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary outline button cancel (full width, rounded corners matching confirm)
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("confirm_cancel_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = if (isAr) "إلغاء" else "Cancel",
                style = MaterialTheme.typography.labelLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// --- 7. MY QR CODE BOTTOM SHEET CONTENT ---
// ============================================================================
@Composable
fun SelfQrSheetContent(
    isAr: Boolean,
    myExtractedNumber: String,
    onPhoneChange: (String) -> Unit,
    onClose: () -> Unit
) {
    var myNumber by remember(myExtractedNumber) { mutableStateOf(myExtractedNumber) }
    var qrType by remember { mutableStateOf(PaymentType.FRIEND) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(myNumber, qrType) {
        val normalized = UssdManager.normalizePhone(myNumber)
        if (normalized.length == 10) {
            val payload = PmaQrManager.generatePmaQrPayload(normalized, qrType)
            qrBitmap = generateQrCode(payload)
        } else {
            qrBitmap = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAr) "رمز QR الشخصي" else "My QR Code",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // QR display card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .size(230.dp)
                .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val bitmapScaled = qrBitmap
                if (bitmapScaled != null) {
                    Image(
                        bitmap = bitmapScaled.asImageBitmap(),
                        contentDescription = "My QR Code Image",
                        modifier = Modifier.fillMaxSize(0.9f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAr) "أدخل رقم هاتفك لإنشاء الرمز" else "Enter phone to generate QR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Telephone outline
        OutlinedTextField(
            value = myNumber,
            onValueChange = {
                if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                    myNumber = it
                    onPhoneChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            placeholder = {
                Text(
                    text = "05X XXXXXXX",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Buttons friend / merchant
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), CircleShape)
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(0.dp))
                    .background(if (qrType == PaymentType.FRIEND) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { qrType = PaymentType.FRIEND },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) "صديق" else "Friend",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (qrType == PaymentType.MERCHANT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { qrType = PaymentType.MERCHANT },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) "تاجر" else "Merchant",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isAr) "أظهر هذا الرمز للمرسل لمسحه والدفع" else "Show this QR to the sender to scan and pay",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Helper to generate a crisp Barcode bitmap
private fun generateQrCode(content: String): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val size = 512
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun OnboardingScreenContent(viewModel: MainViewModel, isAr: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ussd_pay_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isAr) "تطبيق USSD Pay" else "USSD Pay",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAr) "يوفر هذا التطبيق واجهة سريعة وغير متصلة بالإنترنت للوصول لنظام الدفع عبر USSD." else "This application provides a fast, offline interface to your mobile carrier's USSD system.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAr) "يتطلب التطبيق أذونات الهاتف القياسية وجهات الاتصال والكاميرا للوصول إلى شبكة الاتصالات وإدارة جهات التحويل." else "The app requires Telephony, Contacts, and Camera permissions to securely initiate USSD codes, read SIM state, scan QR, and select payees.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        RealisticButton(
            text = if (isAr) "موافق ومتابعة" else "Agree and Continue",
            onClick = { viewModel.completeOnboarding() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}

@Composable
fun PrivacyPolicyScreenContent(viewModel: MainViewModel, isAr: Boolean) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides if (isAr) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.SETTINGS) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isAr) "سياسة الخصوصية" else "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val introText = if (isAr) "تحكم سياسة الخصوصية هذه الطريقة التي يقوم بها التطبيق بجمع واستخدام والحفاظ على المعلومات والإفصاح عنها." 
                        else "This Privacy Policy governs the manner in which the App collects, uses, maintains, and discloses information."
            
            val sections = if (isAr) listOf(
                "١. جمع المعلومات وأذونات الهاتف القياسية" to "• يطلب التطبيق أذونات الهاتف القياسية بصورة صارمة، مثل CALL_PHONE و READ_PHONE_STATE.\n• هذه الأذونات مطلوبة بشكل أساسي لقراءة حالة شريحة الاتصال (SIM) ولتنفيذ أوامر شبكة USSD المحلية للتواصل مع مزود الخدمة الخلوية.",
                "٢. خصوصية البيانات والتخزين المحلي" to "• نحن نلتزم بأعلى معايير الخصوصية. لا يتم جمع أو نقل أي بيانات للمكالمات الهاتفية، السجلات، تفاصيل الشريحة، أو معلومات تعريفية إلى أطراف خارجية.\n• يعمل التطبيق بشكل مستقل كواجهة رسومية دون اتصال بالإنترنت، ويتفاعل مباشرةً مع البنية التحتية الآمنة لشركة الاتصالات الخاصة بك.\n• يتم الاحتفاظ بسجل المعاملات المحفوظ محلياً فقط على جهازك وبطريقة آمنة.",
                "٣. سياسة المشاركة مع الأطراف الثالثة" to "• نحن لا نبيع أو نتاجر أو نؤجر أو نشارك أي بيانات شخصية، مالية، هاتفية، أو أرقام سرية (PIN) مع أي جهات خارجية.\n• جميع العمليات تنفذ بشكل آمن بالكامل داخل بيئة جهاز المستخدم وفيزيائياً على جهازه فقط.",
                "٤. التعديلات على سياسة الخصوصية" to "• نحتفظ بالحق في إجراء تحديثات على هذه السياسة في أي وقت. وننصح بالاطلاع عليها دورياً لمتابعة أي تغييرات."
            ) else listOf(
                "1. Information Collection and Telephony Permissions" to "• The App strictly requests specific standard telephony permissions, such as CALL_PHONE and READ_PHONE_STATE.\n• These are unconditionally required to read your SIM card state and execute local USSD network codes for mobile carrier communications.",
                "2. Data Privacy and Local Storage" to "• We uphold strict privacy standards. No phone call data, logs, SIM details, or personally identifiable information is collected or transmitted to external servers.\n• The App operates independently as an offline graphical interface connecting directly to your carrier's secure telecommunications infrastructure.\n• Any transaction history saved is logged locally and securely on your device.",
                "3. Sharing and Third-Party Policy" to "• We do not sell, trade, rent, or share any personal, financial, telephony data, or PIN numbers with third parties.\n• All operational processes execute securely within the device environment.",
                "4. Changes to this Privacy Policy" to "• We retain the discretion to update this privacy policy at any time. We encourage Users to check this policy for any changes."
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text(
                        text = introText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    sections.forEach { (heading, body) ->
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(start = if (isAr) 0.dp else 12.dp, end = if (isAr) 12.dp else 0.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Text(
                        text = if (isAr) "٥. معلومات التواصل" else "5. Contact Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "لمزيد من الاستفسارات أو للحصول على مراجعة شاملة للسياسات، قم بزيارة موقعنا:" else "For further inquiries or a comprehensive review of our policies, visit our website:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextButton(
                        onClick = { uriHandler.openUri("https://utiapps.netlify.app/privacy-policy") },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "https://utiapps.netlify.app/privacy-policy",
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneSetupScreenContent(state: UiState, viewModel: MainViewModel, isAr: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isAr) "إعداد رقم هاتفك" else "Set up your phone number",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAr) "أدخل رقم هاتفك لتسهيل إنشاء رمز QR خاص بك. يمكنك تخطي هذه الخطوة." else "Enter your phone number to generate your personal QR code easily. You can skip this step anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = state.selfPhone,
            onValueChange = { viewModel.setSelfPhone(it) },
            label = { Text(if (isAr) "رقم هاتفك" else "Your Phone Number") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            placeholder = {
                Text(
                    text = "05X XXXXXXX",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        RealisticButton(
            text = if (isAr) "متابعة" else "Continue",
            onClick = { viewModel.navigateTo(Screen.MAIN) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { viewModel.navigateTo(Screen.MAIN) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isAr) "تخطي" else "Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


