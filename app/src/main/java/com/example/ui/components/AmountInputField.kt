package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay

@Composable
fun AmountInputField(
    amount: String,
    payAttempted: Boolean,
    onAmountChange: (String) -> Unit
) {
    var localAmount by rememberSaveable { mutableStateOf(amount) }

    LaunchedEffect(amount) {
        if (localAmount != amount) {
            localAmount = amount
        }
    }

    LaunchedEffect(localAmount) {
        if (localAmount != amount) {
            delay(300)
            onAmountChange(localAmount)
        }
    }

    val isAmountError = payAttempted && (localAmount.isBlank() || localAmount.toDoubleOrNull() == null || localAmount.toDouble() <= 0)
    val focusManager = LocalFocusManager.current

    TextField(
        value = localAmount,
        onValueChange = {
            localAmount = com.example.ui.convertArabicDigits(it).filter { ch -> ch.isDigit() }
        },
        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
            textAlign = androidx.compose.ui.text.style.TextAlign.Start
        ),
        modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
        placeholder = {
            Text(
                text = "0",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = if (isAmountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        singleLine = true,
        isError = isAmountError,
        shape = RectangleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )
}
