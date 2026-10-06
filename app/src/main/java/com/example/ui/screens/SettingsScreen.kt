package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

import androidx.compose.runtime.*

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppLanguage
import com.example.BiometricHelper
import com.example.MainViewModel
import com.example.Screen
import com.example.UiState
import com.example.ui.components.ProfileCard
import com.example.ui.components.SegmentedToggle

@Composable
fun SettingsScreenContent(
    state: UiState,
    viewModel: MainViewModel,
    isAr: Boolean
) {
    val isDark = state.isDarkMode
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 10 } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Smooth hero gradient at the top (adjusted to dark mode same as payment screen)
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
            modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 62.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile visually layered card
        val simPhoneFallback = state.sims.find { it.subscriptionId == state.selectedSimId }?.phoneNumber ?: state.sims.firstOrNull()?.phoneNumber ?: ""
        ProfileCard(
            selfPhone = state.selfPhone,
            simPhoneFallback = simPhoneFallback,
            isAr = isAr,
            onSavePhone = { viewModel.setSelfPhone(it) },
            isDark = isDark,
            modifier = Modifier.padding(bottom = 20.dp)
        )

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
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.appCardBackground(isDark)),
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

        // PREFERENCES
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAr) "التفضيلات" else "Preferences",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.appCardBackground(isDark)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Item 1: Theme
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness4,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isAr) "المظهر" else "Theme",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        SegmentedToggle(
                            isLeftSelected = !state.isDarkMode,
                            leftText = if (isAr) "فاتح" else "Light",
                            rightText = if (isAr) "داكن" else "Dark",
                            onLeftClick = { viewModel.setDarkMode(false) },
                            onRightClick = { viewModel.setDarkMode(true) },
                            isDark = state.isDarkMode,
                            modifier = Modifier.width(148.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Item 2: Language
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isAr) "اللغة" else "Language",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        SegmentedToggle(
                            isLeftSelected = isAr,
                            leftText = if (isAr) "العربية" else "Arabic",
                            rightText = if (isAr) "English" else "English",
                            onLeftClick = { if (!isAr) viewModel.toggleLanguage() },
                            onRightClick = { if (isAr) viewModel.toggleLanguage() },
                            isDark = state.isDarkMode,
                            modifier = Modifier.width(148.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Item 3: PIN Protection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isAr) "حفظ الرمز" else "Save PIN",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAr) "البصمة للدفع السريع" else "Fingerprint authorization",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        val context = androidx.compose.ui.platform.LocalContext.current
                        SegmentedToggle(
                            isLeftSelected = !state.rememberPin,
                            leftText = if (isAr) "تعطيل" else "Off",
                            rightText = if (isAr) "تفعيل" else "On",
                            isDark = state.isDarkMode,
                            modifier = Modifier.width(148.dp),
                            onLeftClick = { 
                                if (state.rememberPin) {
                                    val activity = context as? androidx.fragment.app.FragmentActivity
                                    if (activity != null) {
                                        BiometricHelper.showPrompt(
                                            activity = activity,
                                            title = if (isAr) "تأكيد الهوية" else "Verify Identity",
                                            subtitle = if (isAr) "لتعطيل هذه الميزة" else "To disable this feature",
                                            onSuccess = { viewModel.setRememberPin(false) },
                                            onError = {}
                                        )
                                    } else {
                                        viewModel.setRememberPin(false)
                                    }
                                }
                            },
                            onRightClick = { 
                                if (!state.rememberPin) {
                                    val activity = context as? androidx.fragment.app.FragmentActivity
                                    if (activity != null) {
                                        BiometricHelper.showPrompt(
                                            activity = activity,
                                            title = if (isAr) "تأكيد الهوية" else "Verify Identity",
                                            subtitle = if (isAr) "لفتح هذه الميزة" else "To unlock this feature",
                                            onSuccess = { viewModel.setRememberPin(true) },
                                            onError = {}
                                        )
                                    } else {
                                        viewModel.setRememberPin(true)
                                    }
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Item 4: Balance Visibility
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = if (state.hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isAr) "عرض الرصيد" else "Balance",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        SegmentedToggle(
                            isLeftSelected = !state.hideBalance,
                            leftText = if (isAr) "إظهار" else "Show",
                            rightText = if (isAr) "إخفاء" else "Hide",
                            isDark = state.isDarkMode,
                            modifier = Modifier.width(148.dp),
                            onLeftClick = { viewModel.setHideBalance(false) },
                            onRightClick = { viewModel.setHideBalance(true) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SUPPORT & LEGAL SECTION (Separated into its own dedicated category with matching design)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAr) "الدعم والمساعدة" else "Support & Legal",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.appCardBackground(isDark)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Privacy Policy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { viewModel.navigateTo(Screen.PRIVACY_POLICY) },
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
                            Text(
                                text = if (isAr) "سياسة الخصوصية" else "Privacy Policy",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Help & Support link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { uriHandler.openUri("https://utiapps.netlify.app/support.html") },
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
                            Text(
                                text = if (isAr) "المساعدة والدعم" else "Help & Support",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
            text = if (isAr) "نسخة: v${com.example.BuildConfig.VERSION_NAME} • التطبيق غير تابع لشركة الاتصالات." else "Version: v${com.example.BuildConfig.VERSION_NAME} • not affiliated with any mobile carrier.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

        // Sticky fixed header bar at the top containing settings label and self QR button
    val strings = com.example.ui.LocalAppStrings.current
    val stickyHeaderBrush = if (isScrolled) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to heroGradientColor,
                0.25f to heroGradientColor,
                0.50f to heroGradientColor.copy(alpha = 0.65f),
                0.75f to heroGradientColor.copy(alpha = 0.30f),
                0.90f to heroGradientColor.copy(alpha = 0.10f),
                1.00f to Color.Transparent
            )
        )
    } else {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(brush = stickyHeaderBrush),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = { viewModel.setQrDialogVisible(true) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                modifier = Modifier.testTag("settings_qr_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "My QR Code",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
}
