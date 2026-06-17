package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle

import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val appFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Google Sans Text"), fontProvider = provider),
    Font(googleFont = GoogleFont("Google Sans"), fontProvider = provider),
    Font(googleFont = GoogleFont("Noto Sans Arabic"), fontProvider = provider)
)

val defaultPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)
val defaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

fun customTextStyle(
    fontWeight: FontWeight,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit
) = TextStyle(
    fontFamily = appFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    platformStyle = defaultPlatformTextStyle,
    lineHeightStyle = defaultLineHeightStyle
)

val Typography = Typography(
    displayLarge = customTextStyle(FontWeight.Normal, 57.sp, 64.sp, (-0.25).sp),
    displayMedium = customTextStyle(FontWeight.Bold, 45.sp, 52.sp, 0.sp),
    displaySmall = customTextStyle(FontWeight.Normal, 36.sp, 44.sp, 0.sp),
    headlineLarge = customTextStyle(FontWeight.Normal, 32.sp, 40.sp, 0.sp),
    headlineMedium = customTextStyle(FontWeight.Normal, 28.sp, 36.sp, 0.sp),
    headlineSmall = customTextStyle(FontWeight.Normal, 24.sp, 32.sp, 0.sp),
    titleLarge = customTextStyle(FontWeight.Medium, 22.sp, 28.sp, 0.sp),
    titleMedium = customTextStyle(FontWeight.Medium, 16.sp, 24.sp, 0.15.sp),
    titleSmall = customTextStyle(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    bodyLarge = customTextStyle(FontWeight.Normal, 16.sp, 24.sp, 0.5.sp),
    bodyMedium = customTextStyle(FontWeight.Normal, 14.sp, 20.sp, 0.25.sp),
    bodySmall = customTextStyle(FontWeight.Normal, 12.sp, 16.sp, 0.4.sp),
    labelLarge = customTextStyle(FontWeight.Medium, 14.sp, 20.sp, 0.1.sp),
    labelMedium = customTextStyle(FontWeight.Medium, 12.sp, 16.sp, 0.5.sp),
    labelSmall = customTextStyle(FontWeight.Normal, 11.sp, 16.sp, 0.5.sp)
)
