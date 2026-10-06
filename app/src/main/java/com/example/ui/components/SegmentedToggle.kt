package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PaymentType
import com.example.ui.innerPillDepth
import com.example.ui.AutoFitText
import com.example.ui.ControlDensityScope

@Composable
fun SegmentedToggle(
    isLeftSelected: Boolean,
    leftText: String,
    rightText: String,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val trackBg = if (isDark) Color(0xFF222924) else Color(0xFFF1F5F2)

    Box(
        modifier = modifier
            .widthIn(min = 144.dp, max = 152.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackBg)
            .drawWithContent {
                drawContent()
                val shadowAlpha = if (isDark) 0.25f else 0.06f
                drawLine(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    start = Offset(0f, 1.dp.toPx()),
                    end = Offset(size.width, 1.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .padding(3.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val segmentShape = RoundedCornerShape(999.dp)

            // Left Segment
            val leftInteractionSource = remember { MutableInteractionSource() }
            val isLeftPressed by leftInteractionSource.collectIsPressedAsState()
            SegmentItem(
                text = leftText,
                isSelected = isLeftSelected,
                isPressed = isLeftPressed,
                isDark = isDark,
                segmentShape = segmentShape,
                interactionSource = leftInteractionSource,
                modifier = Modifier.weight(1f),
                onClick = onLeftClick
            )

            // Right Segment
            val rightInteractionSource = remember { MutableInteractionSource() }
            val isRightPressed by rightInteractionSource.collectIsPressedAsState()
            SegmentItem(
                text = rightText,
                isSelected = !isLeftSelected,
                isPressed = isRightPressed,
                isDark = isDark,
                segmentShape = segmentShape,
                interactionSource = rightInteractionSource,
                modifier = Modifier.weight(1f),
                onClick = onRightClick
            )
        }
    }
}

@Composable
private fun SegmentItem(
    text: String,
    isSelected: Boolean,
    isPressed: Boolean,
    isDark: Boolean,
    segmentShape: RoundedCornerShape,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val segmentModifier = if (isSelected) {
        val activeGradient = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    if (isPressed) Color(0xFF333B36) else Color(0xFF3E4741),
                    if (isPressed) Color(0xFF262C28) else Color(0xFF303833)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    if (isPressed) Color(0xFFEBEBEB) else Color(0xFFFFFFFF),
                    if (isPressed) Color(0xFFDFDFDF) else Color(0xFFF2F2F2)
                )
            )
        }

        Modifier
            .shadow(
                elevation = 2.dp,
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
            .clip(segmentShape)
            .background(hoverOrPressedBg)
    }

    val textColor = if (isSelected) {
        if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    } else {
        if (isDark) Color(0xFF9EABA3) else Color(0xFF777777)
    }

    Box(
        modifier = modifier
            .then(segmentModifier)
            .height(36.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        ControlDensityScope {
            AutoFitText(
                text = text,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxFontSize = 13.5.sp,
                minFontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Universal tactile Segmented Friend / Merchant Toggle used in both Payment form and Self QR.
 */
@Composable
fun UniversalSegmentedFriendToggle(
    paymentType: PaymentType,
    onSelect: (PaymentType) -> Unit,
    isAr: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val trackBg = if (isDark) Color(0xFF222924) else Color(0xFFF1F5F2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(trackBg)
            .drawWithContent {
                drawContent()
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
            val strings = com.example.ui.LocalAppStrings.current
            val options = listOf(
                Pair(PaymentType.FRIEND, strings.friend),
                Pair(PaymentType.MERCHANT, strings.merchant)
            )

            options.forEach { (type, label) ->
                val isSelected = paymentType == type
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val segmentShape = RoundedCornerShape(999.dp)

                val segmentModifier = if (isSelected) {
                    val activeGradient = if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                if (isPressed) Color(0xFF333B36) else Color(0xFF3E4741),
                                if (isPressed) Color(0xFF262C28) else Color(0xFF303833)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                if (isPressed) Color(0xFFEBEBEB) else Color(0xFFFFFFFF),
                                if (isPressed) Color(0xFFDFDFDF) else Color(0xFFF2F2F2)
                            )
                        )
                    }

                    Modifier
                        .weight(1f)
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

                val textColor = if (isSelected) {
                    if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
                } else {
                    if (isDark) Color(0xFF9EABA3) else Color(0xFF777777)
                }

                Box(
                    modifier = segmentModifier
                        .height(40.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onSelect(type) }
                        .testTag(if (type == PaymentType.FRIEND) "type_friend_tab" else "type_merchant_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    ControlDensityScope {
                        AutoFitText(
                            text = label,
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
}
