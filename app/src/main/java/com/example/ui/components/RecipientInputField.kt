package com.example.ui.components

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.example.UssdManager
import kotlinx.coroutines.delay

@Composable
fun RecipientInputField(
    recipient: String,
    payAttempted: Boolean,
    isAr: Boolean,
    onRecipientChange: (String) -> Unit,
    onOpenScanner: () -> Unit,
    contactPickerLauncher: ActivityResultLauncher<Void?>
) {
    var recipientFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = recipient, selection = TextRange(recipient.length)))
    }

    LaunchedEffect(recipient) {
        if (recipientFieldValue.text != recipient) {
            recipientFieldValue = TextFieldValue(
                text = recipient,
                selection = TextRange(recipient.length)
            )
        }
    }

    val currentText = recipientFieldValue.text
    val isRecipientError = payAttempted && (currentText.isBlank() || currentText.length != 10 || !currentText.startsWith("05"))
    val focusManager = LocalFocusManager.current

    TextField(
        value = recipientFieldValue,
        onValueChange = { newVal ->
            val converted = com.example.ui.convertArabicDigits(newVal.text)
            val isPaste = converted.length > recipientFieldValue.text.length + 1
            val cleanDigits = if (isPaste && (converted.contains("+") || converted.startsWith("00") || converted.startsWith("97"))) {
                UssdManager.normalizePhone(converted)
            } else {
                converted.filter { it.isDigit() }.take(10)
            }
            val newCursor = cleanDigits.length.coerceAtMost(newVal.selection.end)
            recipientFieldValue = newVal.copy(text = cleanDigits, selection = TextRange(newCursor))
            onRecipientChange(cleanDigits)
        },
        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
            textAlign = if (isAr) TextAlign.End else TextAlign.Start,
            textDirection = TextDirection.Ltr
        ),
        modifier = Modifier.fillMaxWidth().testTag("recipient_phone_input"),
        placeholder = {
            Text(
                text = "059 123 4567",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.Ltr
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (isAr) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isRecipientError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { contactPickerLauncher.launch(null) },
                    modifier = Modifier.testTag("contact_picker_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PermContactCalendar,
                        contentDescription = "Pick Contact",
                        tint = if (isRecipientError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onOpenScanner,
                    modifier = Modifier.testTag("scan_qr_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = if (isRecipientError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        singleLine = true,
        isError = isRecipientError,
        shape = RectangleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )
}
