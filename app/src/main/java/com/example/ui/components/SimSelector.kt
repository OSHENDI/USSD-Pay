package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SimEntry
import com.example.ui.AutoFitText
import com.example.ui.ControlDensityScope
import com.example.ui.formatSimName

@Composable
fun SimSelector(
    sims: List<SimEntry>,
    selectedSimId: Int?,
    isAr: Boolean,
    onSelectSim: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().testTag("sim_selector")) {
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
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeSims = if (sims.isEmpty()) {
                listOf(
                    SimEntry(1, 0, if (isAr) "الشريحة 1" else "SIM 1"),
                    SimEntry(2, 1, if (isAr) "الشريحة 2" else "SIM 2")
                )
            } else sims

            activeSims.forEachIndexed { idx, sim ->
                val isSelected = (selectedSimId == sim.subscriptionId) || (selectedSimId == null && idx == 0)
                val simLabel = formatSimName(sim, idx, isAr)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onSelectSim(sim.subscriptionId) }
                        .testTag("sim_tab_${sim.subscriptionId}"),
                    contentAlignment = Alignment.Center
                ) {
                    AutoFitText(
                        text = simLabel,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        maxFontSize = 13.sp,
                        minFontSize = 7.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
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
}
