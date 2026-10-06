package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.MainViewModel
import com.example.R
import com.example.ui.components.RealisticButton

@Composable
fun OnboardingScreenContent(viewModel: MainViewModel, isAr: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .height(72.dp)
                .padding(horizontal = 16.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isAr) "تطبيق USSD Pay" else "USSD Pay",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAr) "يوفر هذا التطبيق واجهة سريعة وغير متصلة بالإنترنت للوصول لنظام الدفع عبر USSD." else "This application provides a fast, offline interface to your mobile carrier's USSD system.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAr) "يتطلب التطبيق أذونات الهاتف القياسية وجهات الاتصال والكاميرا للوصول إلى شبكة الاتصالات وإدارة جهات التحويل." else "The app requires Telephony, Contacts, and Camera permissions to securely initiate USSD codes, read SIM state, scan QR, and select payees.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        RealisticButton(
            text = if (isAr) "موافق ومتابعة" else "Agree and Continue",
            onClick = { viewModel.completeOnboarding() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}
