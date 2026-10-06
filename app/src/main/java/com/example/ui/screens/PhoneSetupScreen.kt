package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.MainViewModel
import com.example.Screen
import com.example.UiState
import com.example.ui.components.RealisticButton
import com.example.ui.components.SecondaryPillButton

@Composable
fun PhoneSetupScreenContent(state: UiState, viewModel: MainViewModel, isAr: Boolean) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        val strings = com.example.ui.LocalAppStrings.current
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = strings.welcome,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.phoneSetupSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.selfPhone,
            onValueChange = { input ->
                val converted = com.example.ui.convertArabicDigits(input).filter { it.isDigit() }.take(10)
                viewModel.setSelfPhone(converted)
            },
            label = { Text(strings.phoneNumber) },
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                textAlign = if (isAr) TextAlign.End else TextAlign.Start,
                textDirection = TextDirection.Ltr
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Call,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            shape = RoundedCornerShape(999.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        RealisticButton(
            text = strings.continueText,
            onClick = { viewModel.navigateTo(Screen.MAIN) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryPillButton(
            text = strings.skip,
            onClick = { viewModel.navigateTo(Screen.MAIN) },
            isDark = state.isDarkMode,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}
