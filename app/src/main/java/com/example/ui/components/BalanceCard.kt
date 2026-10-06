package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BalanceCard(
    balanceResult: String,
    hideBalance: Boolean,
    isLoading: Boolean,
    showUpdateBadge: Boolean,
    lastRefreshTime: Long,
    isAr: Boolean,
    balanceDifference: String = "",
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRefreshClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("balance_card"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val strings = com.example.ui.LocalAppStrings.current
            // Centered "Your Balance" label
            Text(
                text = strings.yourBalance,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Centered Large Balance Number: e.g. "2,035.75 ₪"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val displayAmount = if (hideBalance || balanceResult.isEmpty()) "••••" else balanceResult
                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "₪",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Centered "Last Updated"
            val lastUpdatedText = remember(lastRefreshTime, strings) {
                if (lastRefreshTime > 0L) {
                    val cal = Calendar.getInstance().apply { timeInMillis = lastRefreshTime }
                    val datePart = SimpleDateFormat("dd/MM", Locale.ENGLISH).format(cal.time)
                    val timePart = SimpleDateFormat("hh:mm", Locale.ENGLISH).format(cal.time)
                    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) strings.am else strings.pm
                    "${strings.lastUpdatedPrefix}$datePart $timePart $amPm"
                } else {
                    strings.lastUpdatedJustNow
                }
            }

            Text(
                text = lastUpdatedText,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
