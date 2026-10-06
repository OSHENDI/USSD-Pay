package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.innerPillDepth
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.HistoryEntry
import com.example.MainViewModel
import com.example.Screen
import com.example.UiState
import com.example.ui.exportTransactions
import com.example.ui.formatDateCategory
import com.example.ui.parseDateAndTime
import com.example.ui.AutoFitText
import com.example.ui.ControlDensityScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    var selectedTab by remember { mutableStateOf("All") }
    var sortAscending by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredHistory = remember(state.history, selectedTab, sortAscending) {
        state.history.filter {
            when (selectedTab) {
                "Completed" -> it.status.uppercase() == "COMPLETED"
                "Rejected" -> it.status.uppercase() == "FAILED"
                else -> true
            }
        }.let {
            if (sortAscending) it.sortedBy { entry -> com.example.ui.parseDateToMillis(entry.timestamp) }
            else it.sortedByDescending { entry -> com.example.ui.parseDateToMillis(entry.timestamp) }
        }
    }

    val isDark = state.isDarkMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Smooth hero gradient at the top bar (adjusted to dark mode same as payment screen)
        val heroGradientColor = if (isDark) Color(0xFF0C5232) else Color(0xFF0FA968)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to heroGradientColor,
                            0.25f to heroGradientColor,
                            0.50f to heroGradientColor.copy(alpha = 0.65f),
                            0.75f to heroGradientColor.copy(alpha = 0.30f),
                            0.90f to heroGradientColor.copy(alpha = 0.10f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-contrast Header with exit arrow and dropdown
            val strings = com.example.ui.LocalAppStrings.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.historyTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isAr) (if (sortAscending) "ترتيب: الأحدث أولاً" else "ترتيب: الأقدم أولاً")
                                else (if (sortAscending) "Sort: Newest First" else "Sort: Oldest First")
                            )
                        },
                        onClick = { sortAscending = !sortAscending; showMenu = false },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isAr) "تصدير إلى CSV" else "Export CSV") },
                        onClick = {
                            showMenu = false
                            exportTransactions(context, filteredHistory, isAr, "csv")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Description, "CSV")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isAr) "تصدير إلى PDF" else "Export PDF") },
                        onClick = {
                            showMenu = false
                            exportTransactions(context, filteredHistory, isAr, "pdf")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PictureAsPdf, "PDF")
                        }
                    )
                }
            }
        }

        val isDark = state.isDarkMode

        // CSS-Inspired Segmented Control Container (width: 100%, max-width: 500px, border-radius: 999px, padding: 5px)
        val trackBg = if (isDark) Color(0xFF222924) else Color(0xFFF1F5F2)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(trackBg)
                .drawWithContent {
                    drawContent()
                    // box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.06);
                    val shadowAlpha = if (isDark) 0.25f else 0.06f
                    drawLine(
                        color = Color.Black.copy(alpha = shadowAlpha),
                        start = Offset(0f, 1.dp.toPx()),
                        end = Offset(size.width, 1.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                .padding(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf("All", "Completed", "Rejected")
                val arTabs = listOf("الكل", "مكتملة", "مرفوضة")
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == tab
                    val tabInteractionSource = remember { MutableInteractionSource() }
                    val isPressed by tabInteractionSource.collectIsPressedAsState()

                    val segmentShape = RoundedCornerShape(999.dp)

                    val segmentModifier = if (isSelected) {
                        val activeGradient = if (isDark) {
                            // Dark mode active state: subtle elevated contrast pill
                            Brush.verticalGradient(
                                listOf(
                                    if (isPressed) Color(0xFF333B36) else Color(0xFF3E4741),
                                    if (isPressed) Color(0xFF262C28) else Color(0xFF303833)
                                )
                            )
                        } else {
                            // CSS: background: linear-gradient(180deg, #ffffff 0%, #f2f2f2 100%);
                            Brush.verticalGradient(
                                listOf(
                                    if (isPressed) Color(0xFFEBEBEB) else Color(0xFFFFFFFF),
                                    if (isPressed) Color(0xFFDFDFDF) else Color(0xFFF2F2F2)
                                )
                            )
                        }
                        // CSS: box-shadow: inset 0 1px 0 rgba(255, 255, 255, 1)
                        val insetTopColor = if (isDark) Color(0x40FFFFFF) else Color(0xFFFFFFFF)

                        Modifier
                            .weight(1f)
                            // CSS: 0 2px 5px rgba(0, 0, 0, 0.08), 0 1px 1px rgba(0, 0, 0, 0.04);
                            .shadow(
                                elevation = 3.dp,
                                shape = segmentShape,
                                spotColor = if (isDark) Color(0x66000000) else Color(0x14000000),
                                ambientColor = if (isDark) Color(0x40000000) else Color(0x0A000000)
                            )
                            .clip(segmentShape)
                            .background(activeGradient)
                            .innerPillDepth(isDark, enabled = true)
                    } else {
                        val hoverOrPressedBg = if (isPressed) {
                            if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000)
                        } else {
                            Color.Transparent
                        }
                        Modifier
                            .weight(1f)
                            .clip(segmentShape)
                            .background(hoverOrPressedBg)
                    }

                    // CSS colors: color: #777 (inactive), color: #000000 (active); dark adjusted
                    val textColor = if (isSelected) {
                        if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
                    } else {
                        if (isDark) Color(0xFF9EABA3) else Color(0xFF777777)
                    }

                    Box(
                        modifier = segmentModifier
                            .height(40.dp)
                            .clickable(
                                interactionSource = tabInteractionSource,
                                indication = null
                            ) { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        ControlDensityScope {
                            AutoFitText(
                                text = if (isAr) arTabs[index] else tab,
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxFontSize = 15.sp,
                                minFontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Scrollable list content
        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isAr) "لا توجد معاملات مسجلة" else "No recent transactions found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Dynamic pagination loading
            var displayLimit by remember(selectedTab, filteredHistory.size) { mutableStateOf(15) }
            val visibleHistory = remember(filteredHistory, displayLimit) {
                filteredHistory.take(displayLimit)
            }
            val hasMore = visibleHistory.size < filteredHistory.size

            // Grouping by Today, Yesterday, Day Before Yesterday, and Formatted Date
            val groupedHistory = remember(visibleHistory, isAr) {
                val map = linkedMapOf<String, MutableList<HistoryEntry>>()
                for (item in visibleHistory) {
                    val cat = formatDateCategory(item.timestamp, isAr)
                    map.getOrPut(cat) { mutableListOf() }.add(item)
                }
                map
            }

            val listState = rememberLazyListState()

            val shouldLoadMore by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val totalItemsCount = layoutInfo.totalItemsCount
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore && displayLimit < filteredHistory.size) {
                    displayLimit = (displayLimit + 15).coerceAtMost(filteredHistory.size)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedHistory.forEach { (dateHeader, itemsInDate) ->
                    stickyHeader(key = "header_$dateHeader") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Floating frosted date badge sitting in the center
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isDark) Color(0xFF1A221C).copy(alpha = 0.95f) else Color(0xFFF2F7F4).copy(alpha = 0.96f),
                                shadowElevation = 1.5.dp,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                            ) {
                                AutoFitText(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxFontSize = 12.sp,
                                    minFontSize = 9.sp,
                                    color = if (isDark) Color(0xFFA5B8AB) else Color(0xFF475B4E),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = itemsInDate,
                        key = { index, item ->
                            if (item.id > 0) "tx_${item.id}" else "tx_${item.number}_${item.timestamp}_$index"
                        }
                    ) { _, item ->
                        TransactionItemCard(
                            item = item,
                            isAr = isAr,
                            isDark = isDark,
                            onItemClick = {
                                viewModel.refillForRetry(item.number, item.amount, item.type)
                            }
                        )
                    }
                }

                if (hasMore) {
                    item(key = "loading_more_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Compact, borderless transaction card with swipe-to-reveal extra info:
 * In Arabic: swiping to the right reveals extra info (time & SIM used).
 * In English: swiping to the left reveals extra info (time & SIM used).
 * Phone number stays where it is on the card.
 */
@Composable
private fun TransactionItemCard(
    item: HistoryEntry,
    isAr: Boolean,
    isDark: Boolean,
    onItemClick: () -> Unit
) {
    val timePart = remember(item.timestamp, isAr) {
        val (_, rawTimePart) = parseDateAndTime(item.timestamp)
        if (isAr) rawTimePart.replace("AM", "ص").replace("PM", "م") else rawTimePart
    }
    val isFailed = remember(item.status) { item.status.uppercase() == "FAILED" }

    val simLabel = remember(item.simName, isAr) {
        if (isAr && item.simName.startsWith("SIM ")) {
            "شريحة ${item.simName.substringAfter("SIM ")}"
        } else {
            item.simName
        }
    }
    val displayName = remember(item.name, item.number) {
        if (item.name.isNotBlank()) item.name else item.number
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealWidthPx = with(density) { 115.dp.toPx() }
    val swipeOffset = remember { Animatable(0f) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Consistent card background color across the entire application
    val baseCardColor = com.example.ui.appCardBackground(isDark)
    val cardBorderColor = com.example.ui.appCardBorder(isDark)
    val pressedHighlight = if (isDark) Color(0xFF263229) else Color(0xFFF1F5F2)
    val cardBg = if (isPressed) pressedHighlight else baseCardColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Behind the card: Revealed Extra Info Container (Time & SIM Used)
        // In Arabic, card moves to the RIGHT, revealing details on the LEFT side of the card.
        // In English, card moves to the LEFT, revealing details on the RIGHT side of the card.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFailed) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(115.dp)
                    .align(if (isAr) androidx.compose.ui.AbsoluteAlignment.CenterLeft else androidx.compose.ui.AbsoluteAlignment.CenterRight)
                    .padding(horizontal = 14.dp),
                contentAlignment = if (isAr) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = if (isAr) Alignment.Start else Alignment.End
                    ) {
                        // Line 1: Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = timePart,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, lineHeight = 13.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Line 2: SIM Used
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = "SIM",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            AutoFitText(
                                text = simLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxFontSize = 11.5.sp,
                                minFontSize = 7.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }

        // Foreground Card Surface: Swipes right (+translationX) in Arabic, left (-translationX) in English
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value }
                .pointerInput(isAr) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                if (isAr) {
                                    val next = (swipeOffset.value + dragAmount).coerceIn(0f, revealWidthPx)
                                    swipeOffset.snapTo(next)
                                } else {
                                    val next = (swipeOffset.value + dragAmount).coerceIn(-revealWidthPx, 0f)
                                    swipeOffset.snapTo(next)
                                }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (isAr) {
                                    val target = if (swipeOffset.value > revealWidthPx * 0.35f) revealWidthPx else 0f
                                    swipeOffset.animateTo(target, tween(200, easing = FastOutSlowInEasing))
                                } else {
                                    val target = if (swipeOffset.value < -revealWidthPx * 0.35f) -revealWidthPx else 0f
                                    swipeOffset.animateTo(target, tween(200, easing = FastOutSlowInEasing))
                                }
                            }
                        }
                    )
                }
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    onClick = {
                        if (kotlin.math.abs(swipeOffset.value) > 10f) {
                            coroutineScope.launch { swipeOffset.animateTo(0f, tween(180)) }
                        } else {
                            onItemClick()
                        }
                    }
                )
                .testTag("history_item_${item.number}"),
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Status circular icon + Recipient & Phone Number
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconBg = if (isFailed) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    }
                    val iconTint = if (isFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFailed) Icons.Default.Close else Icons.Filled.ArrowUpward,
                            contentDescription = if (isFailed) "Failed" else "Sent",
                            tint = iconTint,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (!isFailed) (if (isAr) -45f else 45f) else 0f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        // Contact name or fallback to formatted phone number
                        val displayName = if (item.name.isNotBlank()) item.name else item.number
                        AutoFitText(
                            text = displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxFontSize = 16.sp,
                            minFontSize = 8.sp,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Phone number stays where it is
                        Text(
                            text = item.number,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right: Amount and Status Pill
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    AutoFitText(
                        text = "- ${item.amount} ₪",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxFontSize = 16.sp,
                        minFontSize = 11.sp,
                        color = if (isFailed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val statusLabel = when (item.status.uppercase()) {
                        "FAILED" -> if (isAr) "مرفوضة" else "Failed"
                        else -> if (isAr) "مكتملة" else "Completed"
                    }
                    val statusColor = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                    AutoFitText(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxFontSize = 11.sp,
                        minFontSize = 8.5.sp,
                        color = statusColor,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
