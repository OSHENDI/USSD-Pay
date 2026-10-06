package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.UssdManager

@Composable
fun ProfileCard(
    selfPhone: String,
    simPhoneFallback: String,
    isAr: Boolean,
    onSavePhone: (String) -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.appCardBackground(isDark)),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.20f else 0.45f),
                    Color.Transparent
                )
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large styled picture avatar
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val focusManager = LocalFocusManager.current
            var isEditingPhone by remember { mutableStateOf(false) }
            val myNumber = selfPhone.ifBlank { simPhoneFallback }
            var localPhone by remember(myNumber) { mutableStateOf(myNumber) }
            var phoneValue by remember(localPhone) {
                mutableStateOf(
                    TextFieldValue(
                        text = localPhone,
                        selection = TextRange(localPhone.length)
                    )
                )
            }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(isEditingPhone) {
                if (isEditingPhone) {
                    phoneValue = TextFieldValue(
                        text = localPhone,
                        selection = TextRange(localPhone.length)
                    )
                    focusRequester.requestFocus()
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isEditingPhone) {
                    var hasGainedFocus by remember { mutableStateOf(false) }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        BasicTextField(
                            value = phoneValue,
                            onValueChange = { newVal ->
                                val converted = com.example.ui.convertArabicDigits(newVal.text).filter { it.isDigit() }
                                val capped = converted.take(10)
                                val cursor = newVal.selection.start.coerceIn(0, capped.length)
                                localPhone = capped
                                phoneValue = TextFieldValue(text = capped, selection = TextRange(cursor))
                                onSavePhone(capped)
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                textDirection = TextDirection.Ltr
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                isEditingPhone = false
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .width(IntrinsicSize.Min)
                                .defaultMinSize(minWidth = 160.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        hasGainedFocus = true
                                    } else if (hasGainedFocus) {
                                        isEditingPhone = false
                                    }
                                }
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val y = size.height - strokeWidth / 2
                                    drawLine(
                                        color = Color.Gray,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (localPhone.isEmpty()) {
                                        Text(
                                            "05X XXXXXXX",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            fontSize = 20.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                } else {
                    val isWalletValid = myNumber.length == 10 && myNumber.startsWith("05")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = myNumber.ifBlank { "05X XXXXXXX" },
                            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            maxLines = 1,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isEditingPhone = true
                            }
                        )
                        if (!isWalletValid && myNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = "Invalid",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.weight(1f), contentAlignment = if (isAr) Alignment.CenterStart else Alignment.CenterEnd) {
                            IconButton(
                                onClick = { isEditingPhone = true },
                                modifier = Modifier.size(24.dp).padding(
                                    if (isAr) PaddingValues(end = 24.dp) else PaddingValues(start = 24.dp)
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Phone", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Text(
                text = if (isAr) "رقم محفظتك" else "Your Wallet Number",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
