package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.AutoFitText
import com.example.ui.ControlDensityScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Strips any trailing ".00" or redundant zeros from the amount display so that
 * pure integer amounts never display ".00", while decimal amounts (e.g. 125.5)
 * are cleanly displayed without extra trailing zeroes.
 */
fun formatCleanAmount(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "0"
    val num = trimmed.toDoubleOrNull() ?: return trimmed
    return if (num % 1.0 == 0.0) {
        num.toLong().toString()
    } else {
        val formatted = String.format(Locale.US, "%.2f", num)
        if (formatted.contains('.')) {
            formatted.trimEnd('0').trimEnd('.')
        } else {
            formatted
        }
    }
}

private fun shareReceipt(
    context: Context,
    amount: String,
    recipient: String,
    recipientName: String,
    network: String,
    isAr: Boolean
) {
    val cleanAmt = formatCleanAmount(amount)
    val text = if (isAr) {
        """
        🧾 إيصال تحويل USSD
        المبلغ: $cleanAmt ₪
        المستلم: ${if (recipientName.isNotBlank()) "$recipientName ($recipient)" else recipient}
        الشبكة: $network
        الحالة: مكتمل بنجاح
        """.trimIndent()
    } else {
        """
        🧾 USSD Payment Receipt
        Amount: $cleanAmt ₪
        Recipient: ${if (recipientName.isNotBlank()) "$recipientName ($recipient)" else recipient}
        Network: $network
        Status: Completed Successfully
        """.trimIndent()
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, if (isAr) "إيصال دفع" else "Payment Receipt")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, if (isAr) "مشاركة الإيصال" else "Share Receipt"))
}

@Composable
fun PaymentSuccessScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    val context = LocalContext.current
    val isDark = state.isDarkMode
    val scrollState = rememberScrollState()

    val cleanAmount = formatCleanAmount(state.amount)
    val activeSim = state.sims.find { it.subscriptionId == state.selectedSimId } ?: state.sims.firstOrNull()
    val simLabel = formatSimName(activeSim, 0, isAr).uppercase()

    val transactionTime = state.lastTransactionTimestamp.ifBlank {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date())
    }

    // Emerald theme colors
    val brandGreen = Color(0xFF0FA968)
    val headerGreen = Color(0xFF0C804E)
    val density = LocalDensity.current
    val gradientHeight = with(density) { 470.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                    Brush.verticalGradient(
                        0.0f to Color(0xFF0C3822),
                        0.35f to Color(0xFF102D1E),
                        0.65f to Color(0xFF122219),
                        0.85f to Color(0xFF111A15),
                        1.0f to Color(0xFF111713),
                        startY = 0f,
                        endY = gradientHeight
                    )
                } else {
                    Brush.verticalGradient(
                        0.0f to Color(0xFF0A6E43),
                        0.28f to Color(0xFF0FA968),
                        0.55f to Color(0xFF38BF81),
                        0.78f to Color(0xFFDBF2E6),
                        0.92f to Color(0xFFF1F9F4),
                        1.0f to Color(0xFFF8FAF9),
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

            // Hero Header Section: Concentric Rings & Checkmark Badge
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

                // Center solid badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x40000000))
                        .clip(CircleShape)
                        .background(Color(0xFF2FA36C))
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            val strings = com.example.ui.LocalAppStrings.current
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.paymentSuccess,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = strings.transferCompleted,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Ticket / Receipt Card
            val ticketShape = RoundedCornerShape(28.dp)
            val cardBg = if (isDark) Color(0xFF1F2722) else Color.White
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
                        spotColor = if (isDark) Color(0x60000000) else Color(0x1A0C804E),
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
                        color = if (isDark) Color(0xFF8E9E94) else Color(0xFF7A8C81),
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

                    // Metadata Details Rows: Only Recipient and Network
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                            valueColor = if (isDark) Color(0xFF5DD694) else brandGreen,
                            isDark = isDark
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Share Receipt Action Button (Exclusive to Success Screen)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                shareReceipt(
                                    context = context,
                                    amount = state.amount,
                                    recipient = state.recipient,
                                    recipientName = state.recipientName,
                                    network = simLabel,
                                    isAr = isAr
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Receipt",
                            tint = if (isDark) Color(0xFF5DD694) else brandGreen,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.shareReceipt,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF5DD694) else brandGreen,
                            letterSpacing = 0.8.sp
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
                // Primary Button: "Pay Again" with simple clear reload/renew icon
                RealisticPillButton(
                    text = strings.payAgain,
                    icon = Icons.Default.Autorenew,
                    onClick = { viewModel.resetToMain() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("success_another_pay_btn")
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
                        .testTag("success_return_home_btn")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isAr) "تم معالجة المعاملة بنجاح عبر بروتوكول USSD الآمن" else "Transaction processed securely via USSD protocol",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFF7A8C81) else Color(0xFF6B7E72),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ReceiptDetailRow(
    label: String,
    value: String,
    valueColor: Color? = null,
    isDark: Boolean = false
) {
    ControlDensityScope {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoFitText(
                text = label,
                maxFontSize = 12.sp,
                minFontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF8E9E94) else Color(0xFF7A8C81),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.width(8.dp))
            AutoFitText(
                text = value,
                maxFontSize = 13.sp,
                minFontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor ?: if (isDark) Color(0xFFE5EBE7) else Color(0xFF1F2937),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

