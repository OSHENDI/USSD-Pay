package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.Screen

@Composable
fun PrivacyPolicyScreenContent(viewModel: MainViewModel, isAr: Boolean) {
    val uriHandler = LocalUriHandler.current

    CompositionLocalProvider(LocalLayoutDirection provides if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.SETTINGS) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (isAr) "سياسة الخصوصية" else "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val introText = if (isAr) "تحكم سياسة الخصوصية هذه الطريقة التي يقوم بها التطبيق بجمع واستخدام والحفاظ على المعلومات والإفصاح عنها."
            else "This Privacy Policy governs the manner in which the App collects, uses, maintains, and discloses information."

            val sections = if (isAr) listOf(
                "١. جمع المعلومات وأذونات الهاتف القياسية" to "• يطلب التطبيق أذونات الهاتف القياسية بصورة صارمة، مثل CALL_PHONE و READ_PHONE_STATE.\n• هذه الأذونات مطلوبة بشكل أساسي لقراءة حالة شريحة الاتصال (SIM) ولتنفيذ أوامر شبكة USSD المحلية للتواصل مع مزود الخدمة الخلوية.",
                "٢. خصوصية البيانات والتخزين المحلي" to "• نحن نلتزم بأعلى معايير الخصوصية. لا يتم جمع أو نقل أي بيانات للمكالمات الهاتفية، السجلات، تفاصيل الشريحة، أو معلومات تعريفية إلى أطراف خارجية.\n• يعمل التطبيق بشكل مستقل كواجهة رسومية دون اتصال بالإنترنت، ويتفاعل مباشرةً مع البنية التحتية الآمنة لشركة الاتصالات الخاصة بك.\n• يتم الاحتفاظ بسجل المعاملات المحفوظ محلياً فقط على جهازك وبطريقة آمنة.",
                "٣. سياسة المشاركة مع الأطراف الثالثة" to "• نحن لا نبيع أو نتاجر أو نؤجر أو نشارك أي بيانات شخصية، مالية، هاتفية، أو أرقام سرية (PIN) مع أي جهات خارجية.\n• جميع العمليات تنفذ بشكل آمن بالكامل داخل بيئة جهاز المستخدم وفيزيائياً على جهازه فقط.",
                "٤. التعديلات على سياسة الخصوصية" to "• نحتفظ بالحق في إجراء تحديثات على هذه السياسة في أي وقت. وننصح بالاطلاع عليها دورياً لمتابعة أي تغييرات."
            ) else listOf(
                "1. Information Collection and Telephony Permissions" to "• The App strictly requests specific standard telephony permissions, such as CALL_PHONE and READ_PHONE_STATE.\n• These are unconditionally required to read your SIM card state and execute local USSD network codes for mobile carrier communications.",
                "2. Data Privacy and Local Storage" to "• We uphold strict privacy standards. No phone call data, logs, SIM details, or personally identifiable information is collected or transmitted to external servers.\n• The App operates independently as an offline graphical interface connecting directly to your carrier's secure telecommunications infrastructure.\n• Any transaction history saved is logged locally and securely on your device.",
                "3. Sharing and Third-Party Policy" to "• We do not sell, trade, rent, or share any personal, financial, telephony data, or PIN numbers with third parties.\n• All operational processes execute securely within the device environment.",
                "4. Changes to this Privacy Policy" to "• We retain the discretion to update this privacy policy at any time. We encourage Users to check this policy for any changes."
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text(
                        text = introText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    sections.forEach { (heading, body) ->
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(start = if (isAr) 0.dp else 12.dp, end = if (isAr) 12.dp else 0.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Text(
                        text = if (isAr) "٥. معلومات التواصل" else "5. Contact Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "لمزيد من الاستفسارات أو للحصول على مراجعة شاملة للسياسات، قم بزيارة موقعنا:" else "For further inquiries or a comprehensive review of our policies, visit our website:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextButton(
                        onClick = { uriHandler.openUri("https://utiapps.netlify.app/privacy-policy") },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "https://utiapps.netlify.app/privacy-policy",
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
