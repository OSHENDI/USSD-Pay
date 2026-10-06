package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import com.example.BiometricHelper
import com.example.MainViewModel
import com.example.Screen
import com.example.ui.components.ConfirmPaymentSheetContent
import com.example.ui.components.SelfQrSheetContent
import com.example.ui.screens.HistoryScreenContent
import com.example.ui.screens.MainScreenContent
import com.example.ui.screens.OnboardingScreenContent
import com.example.ui.screens.PaymentSuccessScreenContent
import com.example.ui.screens.PhoneSetupScreenContent
import com.example.ui.screens.PrivacyPolicyScreenContent
import com.example.ui.screens.SettingsScreenContent
import com.example.ui.screens.TransferFailedScreenContent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: MainViewModel,
    onOpenScanner: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var pendingPayment by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasAllPermissions(context)) {
            viewModel.loadSims()
            if (pendingPayment) {
                viewModel.confirmPay()
            }
        }
        pendingPayment = false
    }

    LaunchedEffect(Unit) {
        viewModel.loadSims()
        viewModel.loadHistory()
    }

    val isRtl = state.isAr
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val currentStrings = remember(state.isAr) {
        if (state.isAr) ArabicStrings else EnglishStrings
    }

    // Back button press handling: go back to main screen if not already there
    BackHandler(enabled = state.currentScreen != Screen.MAIN) {
        viewModel.navigateTo(Screen.MAIN)
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides currentStrings
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Show bottom navigation bar on main-tabbed screens and payment result screens
                if (state.currentScreen == Screen.MAIN || state.currentScreen == Screen.HISTORY || state.currentScreen == Screen.SETTINGS || state.currentScreen == Screen.PAYMENT_SUCCESS || state.currentScreen == Screen.TRANSFER_FAILED) {
                    val navSelectedColor = if (state.isDarkMode) Color(0xFF28BA76) else Color(0xFF0B7D4B)
                    val navIndicatorColor = if (state.isDarkMode) Color(0xFF163E2B) else Color(0xFFE2F6EC)
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = navSelectedColor,
                        selectedTextColor = navSelectedColor,
                        indicatorColor = navIndicatorColor,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("app_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = state.currentScreen == Screen.MAIN || state.currentScreen == Screen.PAYMENT_SUCCESS || state.currentScreen == Screen.TRANSFER_FAILED,
                            onClick = { viewModel.resetToMain() },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = currentStrings.tabPay,
                                    modifier = Modifier.size(24.dp).let { if (isRtl) it.graphicsLayer { scaleX = -1f } else it }
                                )
                            },
                            label = { Text(currentStrings.tabPay) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_tab_pay")
                        )
                        NavigationBarItem(
                            selected = state.currentScreen == Screen.HISTORY,
                            onClick = { viewModel.navigateTo(Screen.HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = currentStrings.tabHistory,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(currentStrings.tabHistory) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_tab_history")
                        )
                        NavigationBarItem(
                            selected = state.currentScreen == Screen.SETTINGS,
                            onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = currentStrings.tabSettings,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(currentStrings.tabSettings) },
                            colors = navItemColors,
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
                    AnimatedVisibility(
                        visible = state.errorMessage.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        if (state.errorMessage.isNotEmpty()) {
                            LaunchedEffect(state.errorMessage) {
                                delay(4000)
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
                                        style = MaterialTheme.typography.bodyMedium
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

                val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                val sheetBorderBrush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (state.isDarkMode) 0.16f else 0.38f),
                        Color.Transparent
                    )
                )

                // --- 1. Confirm Payment Bottom Sheet ---
                if (state.showConfirmDialog) {
                    val selectedSim = state.sims.find { it.subscriptionId == state.selectedSimId } ?: state.sims.firstOrNull()
                    val formattedSim = formatSimName(selectedSim, 0, isRtl)
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.dismissConfirm() },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 8.dp)
                                    .width(48.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                            )
                        },
                        modifier = Modifier.border(width = 0.7.dp, brush = sheetBorderBrush, shape = sheetShape),
                        shape = sheetShape
                    ) {
                        ConfirmPaymentSheetContent(
                            recipient = state.recipient,
                            amount = state.amount,
                            simName = formattedSim,
                            isLoading = state.isConfirmLoading,
                            isAr = isRtl,
                            isDark = state.isDarkMode,
                            onConfirm = {
                                val triggerPay = {
                                    if (hasAllPermissions(context)) {
                                        viewModel.confirmPay()
                                    } else {
                                        pendingPayment = true
                                        permissionLauncher.launch(RequiredPermissions)
                                    }
                                }

                                if (state.rememberPin) {
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        BiometricHelper.showPrompt(
                                            activity = activity,
                                            title = currentStrings.biometricTitle,
                                            subtitle = currentStrings.biometricSubtitle,
                                            onSuccess = triggerPay,
                                            onError = { /* Do nothing on error, let user try again */ }
                                        )
                                    } else {
                                        triggerPay()
                                    }
                                } else {
                                    triggerPay()
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
                        scrimColor = Color.Black.copy(alpha = 0.50f),
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 8.dp)
                                    .width(48.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                            )
                        },
                        shape = sheetShape,
                        modifier = Modifier.border(width = 0.7.dp, brush = sheetBorderBrush, shape = sheetShape)
                    ) {
                        val derivedNumber = state.selfPhone.ifBlank {
                            state.sims.find { it.subscriptionId == state.selectedSimId }?.phoneNumber
                                ?: state.sims.firstOrNull()?.phoneNumber ?: ""
                        }
                        SelfQrSheetContent(
                            isAr = isRtl,
                            myExtractedNumber = derivedNumber,
                            isDark = state.isDarkMode,
                            onPhoneChange = { viewModel.setSelfPhone(it) },
                            onClose = { viewModel.setQrDialogVisible(false) }
                        )
                    }
                }
            }
        }
    }
}
