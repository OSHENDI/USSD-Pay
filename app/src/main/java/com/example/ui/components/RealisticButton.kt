package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.innerPillDepth
import com.example.ui.AutoFitText
import com.example.ui.ControlDensityScope

/**
 * Universal CTA button with realistic tactile lighting, balanced inner depth,
 * animated press depth, dark outer bevel border, and loading state.
 * Uses the proper emerald gradient theme (#0FA968 -> #0C804E).
 */
@Composable
fun RealisticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    contentColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()

    // Balanced emerald gradient with rich contrast: crisp white text readability, vibrant yet not harsh or dim
    val emeraldNormalGradient = if (isDark) {
        listOf(Color(0xFF1CB574), Color(0xFF108752))
    } else {
        listOf(Color(0xFF15A866), Color(0xFF0D824D))
    }
    val emeraldPressedGradient = listOf(Color(0xFF0F7D4A), Color(0xFF0A633A))

    val bgGradient = Brush.verticalGradient(
        if (isPressed) emeraldPressedGradient else emeraldNormalGradient
    )

    val borderColor = if (enabled) {
        if (isDark) Color(0xFF23C47E) else Color(0xFF14965C)
    } else {
        if (isDark) Color(0xFF2B3A31) else Color(0xFFB0C9BC)
    }

    val elevation by animateDpAsState(
        targetValue = if (isPressed || !enabled) 0.dp else 4.dp,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "btn_elevation"
    )

    // 69dp pill radius matching CSS border-radius: 69px
    val buttonShape = RoundedCornerShape(69.dp)

    Box(
        modifier = modifier
            // Outer Drop Shadow
            .shadow(
                elevation = elevation,
                shape = buttonShape,
                spotColor = Color(0x260C804E),
                ambientColor = Color(0x260C804E)
            )
            // Border: 1px solid bevel
            .border(width = 1.dp, color = borderColor, shape = buttonShape)
            .clip(buttonShape)
            .background(if (enabled) bgGradient else Brush.verticalGradient(listOf(Color(0xFF384D40), Color(0xFF2D3E33))))
            .innerPillDepth(isDark = isDark, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Tactile visuals handled dynamically
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            ControlDensityScope {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    AutoFitText(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxFontSize = 17.sp,
                        minFontSize = 8.sp,
                        color = contentColor,
                        modifier = Modifier.weight(1f, fill = false)
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
}

/**
 * Fully rounded (999.dp) primary CTA button adhering to the Clean UI emerald tactile aesthetic.
 * Can also render in a rich crimson red state for error/retry workflows.
 */
@Composable
fun RealisticPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isErrorState: Boolean = false,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pillShape = RoundedCornerShape(999.dp)

    val elevation by animateDpAsState(
        targetValue = if (isPressed || !enabled) 1.dp else 4.dp,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "pill_elevation"
    )

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bgGradient = if (isErrorState) {
        if (isPressed) {
            Brush.verticalGradient(listOf(Color(0xFF9E1B1B), Color(0xFF7A1313)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFFA81A1A)))
        }
    } else {
        if (isPressed) {
            Brush.verticalGradient(listOf(Color(0xFF0F7D4A), Color(0xFF0A633A)))
        } else {
            if (isSystemDark) {
                Brush.verticalGradient(listOf(Color(0xFF1CB574), Color(0xFF108752)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF15A866), Color(0xFF0D824D)))
            }
        }
    }

    val borderColor = if (isErrorState) {
        if (isPressed) Color(0xFF7A1313) else Color(0xFF9E1B1B)
    } else {
        if (isPressed) Color(0xFF097A48) else (if (isSystemDark) Color(0xFF23C47E) else Color(0xFF14965C))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = pillShape,
                spotColor = if (isErrorState) Color(0x33D32F2F) else Color(0x330FA968),
                ambientColor = if (isErrorState) Color(0x1AD32F2F) else Color(0x1A0FA968)
            )
            .border(width = 1.dp, color = borderColor, shape = pillShape)
            .clip(pillShape)
            .background(bgGradient)
            .innerPillDepth(isDark = isSystemDark, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            ControlDensityScope {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    AutoFitText(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxFontSize = 16.sp,
                        minFontSize = 8.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

/**
 * Fully rounded (999.dp) tactile secondary button matching the CSS specification:
 * background: linear-gradient(180deg, #ffffff 0%, #ebebeb 100%);
 * border: 1px solid rgba(0, 0, 0, 0.12);
 * box-shadow: inset 0 1px 0 rgba(255, 255, 255, 1), 0 3px 6px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.05);
 * color: #000000;
 * font-weight: 600;
 */
@Composable
fun ReturnHomePillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAr: Boolean = false,
    isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    icon: ImageVector? = Icons.Filled.Home,
    text: String = if (isAr) "العودة للرئيسية" else "Return Home"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pillShape = RoundedCornerShape(999.dp)

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 3.dp,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "return_home_elevation"
    )

    val bgGradient = if (isDark) {
        if (isPressed) {
            Brush.verticalGradient(listOf(Color(0xFF222824), Color(0xFF1B201D)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF2E3730), Color(0xFF222924)))
        }
    } else {
        if (isPressed) {
            Brush.verticalGradient(listOf(Color(0xFFEBEBEB), Color(0xFFDFDFDF)))
        } else {
            // linear-gradient(180deg, #ffffff 0%, #ebebeb 100%)
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEBEBEB)))
        }
    }

    val borderColor = if (isDark) {
        Color(0xFF3F4D43)
    } else {
        // border: 1px solid rgba(0, 0, 0, 0.12)
        Color(0x1F000000)
    }

    val textColor = if (isDark) Color.White else Color(0xFF000000)

    Box(
        modifier = modifier
            // 0 3px 6px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.05)
            .shadow(
                elevation = elevation,
                shape = pillShape,
                spotColor = if (isDark) Color(0x4D000000) else Color(0x1A000000),
                ambientColor = if (isDark) Color(0x26000000) else Color(0x0D000000)
            )
            .border(width = 1.dp, color = borderColor, shape = pillShape)
            .clip(pillShape)
            .background(bgGradient)
            .innerPillDepth(isDark = isDark, enabled = true)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        ControlDensityScope {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                AutoFitText(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxFontSize = 16.sp,
                    minFontSize = 8.sp,
                    color = textColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

/**
 * Universal tactile secondary button for secondary actions (e.g. Skip, Cancel)
 */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()
) {
    ReturnHomePillButton(
        onClick = onClick,
        modifier = modifier,
        isAr = false,
        isDark = isDark,
        icon = icon,
        text = text
    )
}
