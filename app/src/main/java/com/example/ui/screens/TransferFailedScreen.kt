package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.Screen
import com.example.UiState
import com.example.ui.components.RealisticPillButton
import com.example.ui.components.ReturnHomePillButton
import com.example.ui.formatSimName

@Composable
fun TransferFailedScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    val isDark = state.isDarkMode
    val scrollState = rememberScrollState()

    val cleanAmount = formatCleanAmount(state.amount)
    val activeSim = state.sims.find { it.subscriptionId == state.selectedSimId } ?: state.sims.firstOrNull()
    val simLabel = formatSimName(activeSim, 0, isAr).uppercase()

    val crimsonRed = Color(0xFFD32F2F)
    val density = LocalDensity.current
    val gradientHeight = with(density) { 470.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    Brush.verticalGradient(
                        0.0f to Color(0xFF3E1414),
                        0.35f to Color(0xFF2B1515),
                        0.65f to Color(0xFF201414),
                        0.85f to Color(0xFF191313),
                        1.0f to Color(0xFF171212),
                        startY = 0f,
                        endY = gradientHeight
                    )
                } else {
                    Brush.verticalGradient(
                        0.0f to Color(0xFF9E1F1F),
                        0.28f to Color(0xFFC62828),
                        0.55f to Color(0xFFE55353),
                        0.78f to Color(0xFFF9DEDE),
                        0.92f to Color(0xFFFCF3F3),
                        1.0f to Color(0xFFFAF7F7),
                        startY = 0f,
                        endY = gradientHeight
                    )
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero Header Section: Concentric Rings & Exclamation Badge
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer decorative concentric circle
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                )

                // Middle decorative concentric circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                )

                // Center solid badge with exclamation mark
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x40000000))
                        .clip(CircleShape)
                        .background(crimsonRed)
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = "Failed",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            val strings = com.example.ui.LocalAppStrings.current
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.paymentFailed,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = strings.transferFailedSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Ticket / Status Card
            val ticketShape = RoundedCornerShape(28.dp)
            val cardBg = if (isDark) Color(0xFF241C1C) else Color.White
            val ticketBorderBrush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.16f else 0.38f),
                    Color.Transparent
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 4.dp else 8.dp,
                        shape = ticketShape,
                        spotColor = if (isDark) Color(0x60000000) else Color(0x1AD32F2F),
                        ambientColor = if (isDark) Color(0x30000000) else Color(0x0D000000)
                    )
                    .border(width = 0.7.dp, brush = ticketBorderBrush, shape = ticketShape)
                    .clip(ticketShape)
                    .background(cardBg)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Label: AMOUNT
                    Text(
                        text = strings.amountHeader,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA69090) else Color(0xFF947E7E),
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Amount value without trailing .00
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = cleanAmount,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₪",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF111827)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Solid separator line with faded edges (gradient)
                    val lineSolidColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                0.15f to lineSolidColor,
                                0.50f to lineSolidColor,
                                0.85f to lineSolidColor,
                                1.0f to Color.Transparent
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Metadata Details Rows: Only Reason, Recipient, and Network
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Reason Row: Spacious top alignment with padding so text wraps naturally without cramping
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = strings.reasonLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA69090) else Color(0xFF947E7E),
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = state.failureReason.ifBlank { strings.insufficientFunds },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = crimsonRed,
                                textAlign = TextAlign.End,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                        }

                        // Recipient Row
                        ReceiptDetailRow(
                            label = strings.recipientHeader,
                            value = if (state.recipientName.isNotBlank()) "${state.recipientName} • ${state.recipient}" else state.recipient,
                            isDark = isDark
                        )

                        // Network Row
                        ReceiptDetailRow(
                            label = strings.networkHeader,
                            value = simLabel,
                            isDark = isDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons Section: Both buttons are FULLY ROUNDED
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Button: "Retry Payment" stays GREEN as requested (isErrorState = false)
                RealisticPillButton(
                    text = strings.retryPayment,
                    icon = Icons.Default.Refresh,
                    isErrorState = false,
                    onClick = { viewModel.refillForRetry(state.recipient, state.amount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("fail_retry_pay_btn")
                )

                // Secondary Button: "Return Home" matching user's CSS specification
                ReturnHomePillButton(
                    onClick = { viewModel.resetToMain() },
                    isAr = isAr,
                    isDark = isDark,
                    text = strings.returnHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("fail_back_to_home_btn")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.noDeduction,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFFA69090) else Color(0xFF8C7373),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

