@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.PaymentType
import com.example.PmaQrManager
import com.example.UssdManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@Composable
fun SelfQrSheetContent(
    isAr: Boolean,
    myExtractedNumber: String,
    isDark: Boolean = false,
    onPhoneChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val strings = com.example.ui.LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    var myNumber by remember(myExtractedNumber) {
        mutableStateOf(UssdManager.normalizePhone(myExtractedNumber))
    }
    var qrType by remember { mutableStateOf(PaymentType.FRIEND) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(myNumber, qrType) {
        val normalized = UssdManager.normalizePhone(myNumber)
        if (normalized.length == 10 && normalized.startsWith("05")) {
            val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    val payload = PmaQrManager.generatePmaQrPayload(normalized, qrType)
                    generateQrCode(payload)
                } catch (e: Exception) {
                    null
                }
            }
            qrBitmap = bmp
        } else {
            qrBitmap = null
        }
    }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    var isPhoneFocused by remember { mutableStateOf(false) }
    val isExpandedMode = isPhoneFocused || isKeyboardOpen

    val qrCardSize by animateDpAsState(
        targetValue = if (isExpandedMode) 140.dp else 224.dp,
        label = "qr_card_size"
    )
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.selfQrTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isExpandedMode) 8.dp else 16.dp))

        // Bigger, rounded QR Container fitting properly without breaking alignment
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1B241E) else Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 1.dp else 3.dp),
            modifier = Modifier
                .size(qrCardSize)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF2B3A30) else Color(0xFFE2EEE6),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isExpandedMode) 10.dp else 14.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmapScaled = qrBitmap
                if (bitmapScaled != null) {
                    Image(
                        bitmap = bitmapScaled.asImageBitmap(),
                        contentDescription = "My QR Code Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isExpandedMode) 52.dp else 76.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.enterPhoneForQr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isExpandedMode) 12.dp else 20.dp))

        OutlinedTextField(
            value = myNumber,
            onValueChange = { input ->
                val digits = com.example.ui.convertArabicDigits(input).filter { it.isDigit() }.take(10)
                myNumber = digits
                onPhoneChange(digits)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .onFocusChanged {
                    isPhoneFocused = it.isFocused
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                textDirection = TextDirection.Ltr
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (myNumber.isNotEmpty()) onPhoneChange(myNumber)
            }),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            placeholder = {
                Text(
                    text = "059 123 4567",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reusable Segmented Friend / Merchant Toggle matching the payment screen
        UniversalSegmentedFriendToggle(
            paymentType = qrType,
            onSelect = { qrType = it },
            isAr = isAr,
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.showQrHelper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun generateQrCode(content: String): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val size = 512
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        null
    }
}
