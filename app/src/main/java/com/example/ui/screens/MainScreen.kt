@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.fragment.app.FragmentActivity
import com.example.BiometricHelper
import com.example.ui.components.UniversalSegmentedFriendToggle
import com.example.MainViewModel
import com.example.PaymentType
import com.example.SimEntry
import com.example.UiState
import com.example.UssdManager
import com.example.ui.RequiredPermissions
import com.example.ui.LocalAppStrings
import com.example.ui.components.BalanceCard
import com.example.ui.components.RealisticButton
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.ui.formatSimName
import com.example.ui.hasAllPermissions
import com.example.ui.innerPillDepth
import com.example.ui.AutoFitText
import com.example.ui.ControlDensityScope

@Composable
fun MainScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean,
    onOpenScanner: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingRefresh by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasAllPermissions(context)) {
            viewModel.loadSims()
            if (pendingRefresh) {
                viewModel.checkBalance()
            }
        }
        pendingRefresh = false
    }

    val triggerRefreshAction: () -> Unit = {
        if (hasAllPermissions(context)) {
            viewModel.checkBalance()
        } else {
            pendingRefresh = true
            permissionLauncher.launch(RequiredPermissions)
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            viewModel.resolveContact(uri)
        }
    }

    val pullAnim = remember { Animatable(0f) }
    val maxDragPx = 180f
    val triggerThresholdPx = 100f

    // When balance loading state changes, smoothly snap back if not loading
    LaunchedEffect(state.isBalanceLoading) {
        if (!state.isBalanceLoading && pullAnim.value > 0f) {
            pullAnim.animateTo(0f, animationSpec = tween(300))
        }
    }

    val scrollState = rememberScrollState()

    // NestedScrollConnection intercepts pull-down gestures directly from the scroll hierarchy
    val nestedScrollConnection = remember(state.isBalanceLoading) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If already pulled down, handle upward drags first to retract the offset
                if (pullAnim.value > 0f && available.y < 0) {
                    val consumed = available.y.coerceAtLeast(-pullAnim.value)
                    coroutineScope.launch {
                        pullAnim.snapTo((pullAnim.value + consumed).coerceIn(0f, maxDragPx))
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // When content is at the very top (scrollState.value == 0) and user drags downwards
                if (scrollState.value == 0 && available.y > 0 && !state.isBalanceLoading) {
                    val dragAmount = available.y * 0.5f
                    val newTarget = (pullAnim.value + dragAmount).coerceIn(0f, maxDragPx)
                    coroutineScope.launch {
                        pullAnim.snapTo(newTarget)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullAnim.value >= triggerThresholdPx && !state.isBalanceLoading) {
                    pullAnim.animateTo(64f, animationSpec = tween(200))
                    triggerRefreshAction()
                } else if (pullAnim.value > 0f) {
                    pullAnim.animateTo(0f, animationSpec = tween(250))
                }
                return Velocity.Zero
            }
        }
    }

    val isDark = state.isDarkMode

    // Root Container with theme-aware canvas color and hero gradient at the top
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFFAFCFA))
            .nestedScroll(nestedScrollConnection)
    ) {
        // Centered Top Pill Refresh Indicator (Subtle, semi-transparent frosted background)
        if (state.isBalanceLoading || pullAnim.value > 8f) {
            val pullFraction = (pullAnim.value / triggerThresholdPx).coerceIn(0f, 1f)
            val pillOffsetY by animateDpAsState(
                targetValue = if (state.isBalanceLoading) 16.dp else (6f + pullFraction * 14f).dp,
                animationSpec = tween(150, easing = FastOutSlowInEasing),
                label = "pill_offset_y"
            )
            val pillAlpha by animateFloatAsState(
                targetValue = if (state.isBalanceLoading) 1f else (pullAnim.value / 30f).coerceIn(0f, 1f),
                animationSpec = tween(150),
                label = "pill_alpha"
            )
            val pillScale by animateFloatAsState(
                targetValue = if (state.isBalanceLoading) 1f else (0.85f + pullFraction * 0.15f),
                animationSpec = tween(150),
                label = "pill_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(30f)
                    .offset(y = pillOffsetY)
                    .graphicsLayer {
                        alpha = pillAlpha
                        scaleX = pillScale
                        scaleY = pillScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xCC063F25), // Semi-transparent deep rich emerald
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .testTag("refresh_loading_status")
                        .innerPillDepth(isDark, enabled = true, horizontalInset = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.isBalanceLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { pullFraction },
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.20f),
                                strokeWidth = 2.dp
                            )
                        }
                        val strings = com.example.ui.LocalAppStrings.current
                        Text(
                            text = if (state.isBalanceLoading) {
                                strings.updatingBalance
                            } else if (pullAnim.value >= triggerThresholdPx) {
                                strings.releaseToUpdate
                            } else {
                                strings.pullToUpdate
                            },
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Hero Gradient: 380px (380.dp) height, linear-gradient(180deg, #0FA968 0%, #0FA968 35%, rgba(250, 252, 250, 0) 100%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to (if (isDark) Color(0xFF0C5232) else Color(0xFF0FA968)),
                            0.35f to (if (isDark) Color(0xFF0C5232) else Color(0xFF0FA968)),
                            1.00f to (if (isDark) Color(0x00111411) else Color(0x00FAFCFA))
                        )
                    )
                )
                .drawBehind {
                    // Flowing subtle organic wave curves across the upper hero area
                    val wave1 = Path().apply {
                        moveTo(0f, size.height * 0.28f)
                        quadraticTo(
                            size.width * 0.40f, size.height * 0.18f,
                            size.width, size.height * 0.32f
                        )
                        lineTo(size.width, size.height * 0.58f)
                        quadraticTo(
                            size.width * 0.60f, size.height * 0.48f,
                            0f, size.height * 0.56f
                        )
                        close()
                    }
                    drawPath(wave1, color = Color.White.copy(alpha = 0.06f))

                    val wave2 = Path().apply {
                        moveTo(0f, size.height * 0.42f)
                        quadraticTo(
                            size.width * 0.55f, size.height * 0.32f,
                            size.width, size.height * 0.46f
                        )
                        lineTo(size.width, size.height * 0.70f)
                        quadraticTo(
                            size.width * 0.45f, size.height * 0.64f,
                            0f, size.height * 0.72f
                        )
                        close()
                    }
                    drawPath(wave2, color = Color.White.copy(alpha = 0.04f))
                }
        )

        val formBringIntoViewRequester = remember { BringIntoViewRequester() }
        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        val imeBottomPx = imeInsets.getBottom(density)
        val imeBottomDp = with(density) { imeBottomPx.toDp() }
        val isKeyboardOpen = imeBottomPx > 0
        var showDiffTooltip by remember { mutableStateOf(false) }

        LaunchedEffect(showDiffTooltip) {
            if (showDiffTooltip) {
                kotlinx.coroutines.delay(3200)
                showDiffTooltip = false
            }
        }

        // Dynamic extra bottom clearance: when keyboard is open, add generous headroom (at least 280dp)
        // so that the scrollable column expands dynamically, allowing the entire form card
        // (including PIN, SIM, and Pay button) to smoothly scroll completely into view above the keyboard.
        val targetExtraBottom = if (isKeyboardOpen) maxOf(imeBottomDp, 280.dp) else 0.dp

        val animatedExtraBottom by animateDpAsState(
            targetValue = targetExtraBottom,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
            label = "keyboard_bottom_clearance"
        )

        LaunchedEffect(isKeyboardOpen) {
            if (isKeyboardOpen) {
                kotlinx.coroutines.delay(160)
                scrollState.animateScrollTo(
                    value = scrollState.maxValue,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
        }

        // Main Scrollable Column without rigid imePadding; height expands dynamically at the bottom
        // so the entire form card remains fully visible and ready for input without clipping.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Top App Bar: "Hello there! Ready to share some joy?" ---
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "USSD Pay",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 26.sp,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Jawwal Pay",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                    }

                    // Balance Difference +- indicator positioned elegantly in the header
                    if (state.balanceDifference.isNotBlank() && !state.hideBalance) {
                        val isPositive = state.balanceDifference.startsWith("+")
                        Box(contentAlignment = Alignment.TopEnd) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isPositive) Color(0x402E7D32) else Color(0x40C62828),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .innerPillDepth(isDark, enabled = true)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color.White)
                                    ) { showDiffTooltip = !showDiffTooltip }
                                    .testTag("header_balance_difference")
                            ) {
                                ControlDensityScope {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AutoFitText(
                                            text = "${state.balanceDifference} ₪",
                                            color = Color.White,
                                            maxFontSize = 13.sp,
                                            minFontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (showDiffTooltip) {
                                val strings = com.example.ui.LocalAppStrings.current
                                Popup(
                                    alignment = Alignment.BottomCenter,
                                    offset = IntOffset(0, with(density) { 8.dp.roundToPx() }),
                                    onDismissRequest = { showDiffTooltip = false }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xF018241C) else Color(0xF0122216),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showDiffTooltip = false }
                                            .padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = strings.diffTooltip,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Centered Balance Section (Seamless on gradient, with swipe-down refresh) ---
            BalanceCard(
                balanceResult = state.balanceResult,
                hideBalance = state.hideBalance,
                isLoading = state.isBalanceLoading,
                showUpdateBadge = state.showUpdateBadge,
                lastRefreshTime = state.lastRefreshTime,
                isAr = isAr,
                balanceDifference = state.balanceDifference,
                onRefreshClick = {
                    if (hasAllPermissions(context)) {
                        viewModel.checkBalance()
                    } else {
                        pendingRefresh = true
                        permissionLauncher.launch(RequiredPermissions)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- Amount Card: Frosted Glass with backdrop blur simulation, shadow-sm, and dynamic slider ---
            AmountGlassCard(
                amount = state.amount,
                onAmountChange = { viewModel.updateAmount(it) },
                isAr = isAr,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- Lower Controls (Form Container with theme-aware styling) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(formBringIntoViewRequester)
                    .clip(RoundedCornerShape(26.dp))
                    .background(com.example.ui.appCardBackground(isDark))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Friend / Merchant Universal Segmented Pill Toggle
                UniversalSegmentedFriendToggle(
                    paymentType = state.paymentType,
                    onSelect = { viewModel.updatePaymentType(it) },
                    isAr = isAr,
                    isDark = isDark
                )

                // 2. Recipient Phone Card with LTR number entry & Arabic end alignment
                RecipientPillCard(
                    recipient = state.recipient,
                    recipientName = state.recipientName,
                    payAttempted = state.payAttempted,
                    isAr = isAr,
                    isDark = isDark,
                    onRecipientChange = { viewModel.updateRecipient(it) },
                    onFocus = {
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(140)
                            scrollState.animateScrollTo(
                                value = scrollState.maxValue,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    onOpenContactPicker = { contactPickerLauncher.launch(null) },
                    onOpenScanner = onOpenScanner
                )

                // 3. Dual Pills: PIN THEN SIM in Left-to-Right layout for both English and Arabic
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PIN Inline Entry Pill (First / Left)
                        PinPillSelector(
                            secretCode = state.secretCode,
                            isAr = isAr,
                            isDark = isDark,
                            onPinChange = { viewModel.setSecretCode(it) },
                            onFocus = {
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(140)
                                    scrollState.animateScrollTo(
                                        value = scrollState.maxValue,
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // SIM Selector Pill (Second / Right)
                        SimPillSelector(
                            sims = state.sims,
                            selectedSimId = state.selectedSimId,
                            isAr = isAr,
                            isDark = isDark,
                            onCycleSim = {
                                if (state.sims.isNotEmpty()) {
                                    val currentIdx = state.sims.indexOfFirst { it.subscriptionId == state.selectedSimId }
                                    val nextIdx = if (currentIdx >= 0 && currentIdx < state.sims.size - 1) currentIdx + 1 else 0
                                    viewModel.selectSim(state.sims[nextIdx].subscriptionId)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val strings = LocalAppStrings.current

                // 4. Primary Pay Button (Respectable emerald gradient CTA)
                RealisticButton(
                    text = strings.payButton,
                    icon = Icons.AutoMirrored.Filled.Send,
                    isLoading = state.isConfirmLoading,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.requestPay()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pay_now_button")
                )

                // 5. Concise Helper Text (Sentence case, concise)
                Text(
                    text = strings.ussdHelper,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp + animatedExtraBottom))
        }
    }
}

/**
 * Frosted Glass Amount Card with continuous slider, editable text field with cursor indicator,
 * dynamic label width, and dynamic tick expansion based on digit count after unfocus or release.
 */
@Composable
private fun AmountGlassCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    isAr: Boolean,
    isDark: Boolean
) {
    val focusManager = LocalFocusManager.current
    var isAmountFocused by remember { mutableStateOf(false) }

    var amountFieldValue by remember(amount) {
        mutableStateOf(
            TextFieldValue(
                text = amount,
                selection = TextRange(amount.length)
            )
        )
    }

    val amountBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.16f else 0.38f),
            Color.Transparent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x0D000000),
                spotColor = Color(0x0D000000)
            )
            .border(width = 0.7.dp, brush = amountBorderBrush, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(com.example.ui.appCardBackground(isDark))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            val strings = LocalAppStrings.current
            // Clean "Amount" / "المبلغ" label
            Text(
                text = strings.amountLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFA2B2A7) else Color(0xFF425649),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Centered editable Amount Field with clean responsive centering across all screen sizes (e.g. 360 dpi)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    BasicTextField(
                        value = amountFieldValue,
                        onValueChange = { newVal ->
                            val converted = com.example.ui.convertArabicDigits(newVal.text).filter { it.isDigit() }
                            val num = converted.toLongOrNull() ?: 0L
                            val capped = if (num > 12000L) "12000" else converted
                            val trimmed = if (capped.length > 1 && capped.startsWith("0")) capped.trimStart('0') else capped
                            val cursor = trimmed.length
                            amountFieldValue = TextFieldValue(text = trimmed, selection = TextRange(cursor))
                            onAmountChange(trimmed)
                        },
                        textStyle = TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1B2E22),
                            textAlign = TextAlign.Center,
                            textDirection = TextDirection.Ltr
                        ),
                        cursorBrush = SolidColor(if (isDark) Color(0xFF68DE9F) else Color(0xFF0FA968)),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (amountFieldValue.text.isEmpty() || (amountFieldValue.text.toDoubleOrNull() ?: 0.0) < 1.0) {
                                    amountFieldValue = TextFieldValue("10", selection = TextRange(2))
                                    onAmountChange("10")
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 56.dp)
                                    .padding(horizontal = 4.dp)
                            ) {
                                // Invisible measuring text ensures exact dynamic width and prevents digit/cursor clipping across all screen sizes and DPIs
                                Text(
                                    text = (if (amountFieldValue.text.isEmpty()) "10" else amountFieldValue.text) + " ",
                                    style = TextStyle(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    color = Color.Transparent,
                                    maxLines = 1
                                )
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .widthIn(min = 52.dp)
                            .defaultMinSize(minHeight = 56.dp)
                            .onFocusChanged { focusState ->
                                isAmountFocused = focusState.isFocused
                                if (!focusState.isFocused) {
                                    if (amountFieldValue.text.isEmpty() || (amountFieldValue.text.toDoubleOrNull() ?: 0.0) < 1.0) {
                                        amountFieldValue = TextFieldValue("10", selection = TextRange(2))
                                        onAmountChange("10")
                                    }
                                }
                            }
                            .testTag("transfer_amount_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "₪",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF68DE9F) else Color(0xFF1B2E22)
                    )
                }

                // Dynamic bottom border that changes color to active emerald state upon input focus and scales responsively
                val activeUnderlineColor = if (isDark) Color(0xFF68DE9F) else Color(0xFF0FA968)
                val inactiveUnderlineColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.14f)
                val underlineColor by animateColorAsState(
                    targetValue = if (isAmountFocused) activeUnderlineColor else inactiveUnderlineColor,
                    label = "amount_underline_color"
                )
                val underlineWidthFraction = if (isAmountFocused) 0.50f else 0.38f

                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(underlineWidthFraction)
                        .height(if (isAmountFocused) 2.5.dp else 1.5.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    underlineColor,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Responsive Quick Amount Pills: 5, 10, 20, 50 (no text wrap two lines)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickAmounts = listOf("5", "10", "20", "50")
                quickAmounts.forEach { pillAmount ->
                    val isSelected = amount == pillAmount
                    val pillBg = if (isSelected) {
                        Color(0xFF0FA968)
                    } else {
                        if (isDark) Color(0xFF222B24) else Color(0xFFF0F5F2)
                    }
                    val pillTextColor = if (isSelected) {
                        Color.White
                    } else {
                        if (isDark) Color(0xFFBDCFC4) else Color(0xFF2B4734)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(pillBg)
                            .innerPillDepth(isDark, enabled = true)
                            .clickable {
                                onAmountChange(pillAmount)
                                amountFieldValue = TextFieldValue(text = pillAmount, selection = TextRange(pillAmount.length))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ControlDensityScope {
                            AutoFitText(
                                text = "$pillAmount ₪",
                                color = pillTextColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                maxFontSize = 13.5.sp,
                                minFontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recipient Phone Card with Avatar, Contact Picker, and Scanner.
 * Uses LTR digit entry and End alignment for Arabic.
 */
@Composable
private fun RecipientPillCard(
    recipient: String,
    recipientName: String,
    payAttempted: Boolean,
    isAr: Boolean,
    isDark: Boolean,
    onRecipientChange: (String) -> Unit,
    onFocus: () -> Unit = {},
    onOpenContactPicker: () -> Unit,
    onOpenScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isError = payAttempted && (recipient.isBlank() || recipient.length != 10 || !recipient.startsWith("05"))

    var phoneValue by remember(recipient) {
        mutableStateOf(
            TextFieldValue(
                text = recipient,
                selection = TextRange(recipient.length)
            )
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ControlDensityScope {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) Color(0xFF222924) else Color(0xFFF1F5F2)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar badge on the left
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF2E3831) else Color(0xFFDFE6E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isError) MaterialTheme.colorScheme.error else (if (isDark) Color(0xFF9EABA3) else Color(0xFF526659)),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Text Input with exact mathematical ratio font sizing: strictly single-line, zero wrap or clip
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (it.isFocused) onFocus() }
                        ) {
                            val textMeasurer = rememberTextMeasurer()
                            val density = LocalDensity.current
                            val availableWidthPx = constraints.maxWidth
                            val baseStyle = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1B2E22),
                                textDirection = TextDirection.Ltr
                            )

                            // Measure worst-case 10 digit number "059 123 4567" or full text so font size stays stable during typing
                            val textToMeasure = if (phoneValue.text.length >= 10) phoneValue.text else "059 123 4567"
                            val dynamicFontSize = remember(textToMeasure, availableWidthPx, density.fontScale, density.density) {
                                if (availableWidthPx <= 0) {
                                    16.sp
                                } else {
                                    val measured = textMeasurer.measure(
                                        text = textToMeasure,
                                        style = baseStyle.copy(fontSize = 16.sp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (measured.size.width > availableWidthPx && measured.size.width > 0) {
                                        val scale = (availableWidthPx.toFloat() / measured.size.width.toFloat()).coerceAtMost(1f)
                                        (16f * scale * 0.96f).coerceIn(8.5f, 16f).sp
                                    } else {
                                        16.sp
                                    }
                                }
                            }

                            BasicTextField(
                                value = phoneValue,
                                onValueChange = { newVal ->
                                    val converted = com.example.ui.convertArabicDigits(newVal.text).filter { it.isDigit() }
                                    val capped = converted.take(10)
                                    val cursor = newVal.selection.start.coerceIn(0, capped.length)
                                    phoneValue = TextFieldValue(text = capped, selection = TextRange(cursor))
                                    onRecipientChange(capped)
                                },
                                textStyle = baseStyle.copy(fontSize = dynamicFontSize),
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                cursorBrush = SolidColor(if (isDark) Color(0xFF68DE9F) else Color(0xFF0FA968)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recipient_phone_input"),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (recipient.isEmpty()) {
                                            Text(
                                                text = "059 123 4567",
                                                style = baseStyle.copy(
                                                    fontSize = dynamicFontSize,
                                                    color = if (isDark) Color(0xFF6B7970) else Color(0xFF8A9A90),
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        // Trailing Action Icons on the right (compact touch targets preserving maximum input space)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onOpenContactPicker,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .testTag("contact_picker_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PermContactCalendar,
                                    contentDescription = "Pick contact",
                                    tint = if (isDark) Color(0xFF9EABA3) else Color(0xFF526659),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onOpenScanner,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .testTag("scanner_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = if (isDark) Color(0xFF9EABA3) else Color(0xFF526659),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (recipientName.isNotBlank()) {
                        AutoFitText(
                            text = recipientName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF68DE9F) else Color(0xFF00A859),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            maxFontSize = 11.5.sp,
                            minFontSize = 7.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 38.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * SIM Selector Pill
 */
@Composable
private fun SimPillSelector(
    sims: List<SimEntry>,
    selectedSimId: Int?,
    isAr: Boolean,
    isDark: Boolean,
    onCycleSim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSim = sims.find { it.subscriptionId == selectedSimId } ?: sims.firstOrNull()
    val simLabel = if (activeSim != null) {
        val idx = sims.indexOf(activeSim)
        formatSimName(activeSim, if (idx >= 0) idx else 0, isAr)
    } else {
        if (isAr) "شريحة 1" else "SIM 1"
    }

    ControlDensityScope {
        Surface(
            modifier = modifier
                .height(48.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable { onCycleSim() }
                .testTag("sim_selector"),
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF222924) else Color(0xFFF1F5F2)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = "SIM",
                    tint = if (isDark) Color(0xFF68DE9F) else Color(0xFF00A859),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                AutoFitText(
                    text = simLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF68DE9F) else Color(0xFF008945),
                    maxFontSize = 13.sp,
                    minFontSize = 6.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Switch SIM",
                    tint = if (isDark) Color(0xFF9EABA3) else Color(0xFF6A7B70),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Straightforward Inline PIN Entry Pill.
 * No popup dialog: entered directly into the pill.
 */
@Composable
private fun PinPillSelector(
    secretCode: String,
    isAr: Boolean,
    isDark: Boolean,
    onPinChange: (String) -> Unit,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pinValue by remember(secretCode) {
        mutableStateOf(
            TextFieldValue(
                text = secretCode,
                selection = TextRange(secretCode.length)
            )
        )
    }

    ControlDensityScope {
        Surface(
            modifier = modifier
                .height(48.dp)
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF222924) else Color(0xFFF1F5F2)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "PIN",
                    tint = if (isDark) Color(0xFF9EABA3) else Color(0xFF6A7B70),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))

                BasicTextField(
                    value = pinValue,
                    onValueChange = { newVal ->
                        val converted = com.example.ui.convertArabicDigits(newVal.text).filter { it.isDigit() }
                        val capped = converted.take(4)
                        val cursor = capped.length
                        pinValue = TextFieldValue(text = capped, selection = TextRange(cursor))
                        onPinChange(capped)
                    },
                    visualTransformation = PasswordVisualTransformation('•'),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    maxLines = 1,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1B2E22),
                        textDirection = TextDirection.Ltr
                    ),
                    cursorBrush = SolidColor(if (isDark) Color(0xFF68DE9F) else Color(0xFF0FA968)),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { if (it.isFocused) onFocus() }
                        .testTag("secret_pin_field"),
                    decorationBox = { innerTextField ->
                        val strings = LocalAppStrings.current
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (secretCode.isEmpty()) {
                                AutoFitText(
                                    text = strings.pinPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    color = if (isDark) Color(0xFF6B7970) else Color(0xFF8A9A90),
                                    maxFontSize = 13.sp,
                                    minFontSize = 7.sp,
                                    textAlign = TextAlign.Start
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}
