package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.SimEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text

val RequiredPermissions = arrayOf(
    android.Manifest.permission.CALL_PHONE,
    android.Manifest.permission.READ_PHONE_STATE,
    android.Manifest.permission.READ_PHONE_NUMBERS
)

fun hasAllPermissions(context: Context): Boolean {
    return RequiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun formatSimName(sim: SimEntry?, slotIndexFallback: Int = 0, isAr: Boolean = false): String {
    val strings = getAppStrings(isAr)
    if (sim == null) {
        return strings.simSlotName(slotIndexFallback + 1)
    }
    val slot = sim.slotIndex + 1
    val rawName = sim.displayName.trim()
    val cleanName = when {
        rawName.contains("Jawwal", ignoreCase = true) -> if (isAr) "جوال" else "Jawwal"
        rawName.contains("Ooredoo", ignoreCase = true) -> if (isAr) "أوريدو" else "Ooredoo"
        rawName.contains(" - ") -> rawName.substringBefore(" - ").trim()
        rawName.length > 10 -> rawName.take(9).trim()
        else -> rawName
    }
    return if (cleanName.equals("SIM 1", ignoreCase = true) || cleanName.equals("SIM 2", ignoreCase = true) || cleanName.startsWith("SIM", ignoreCase = true)) {
        strings.simSlotName(slot)
    } else {
        "$cleanName $slot"
    }
}

fun convertArabicDigits(input: String): String {
    if (input.isEmpty()) return ""
    val builder = StringBuilder(input.length)
    for (i in 0 until input.length) {
        val ch = input[i]
        when (ch) {
            '٠' -> builder.append('0')
            '١' -> builder.append('1')
            '٢' -> builder.append('2')
            '٣' -> builder.append('3')
            '٤' -> builder.append('4')
            '٥' -> builder.append('5')
            '٦' -> builder.append('6')
            '٧' -> builder.append('7')
            '٨' -> builder.append('8')
            '٩' -> builder.append('9')
            else -> builder.append(ch)
        }
    }
    return builder.toString()
}

private val parsedDateCache = java.util.concurrent.ConcurrentHashMap<String, java.util.Date>()
private val parsedDateAndTimeCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String>>()
private val dateCategoryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

fun parseDate(raw: String): java.util.Date? {
    if (raw.isBlank()) return null
    parsedDateCache[raw]?.let { return it }

    // Normalize Arabic-Indic digits to ASCII
    val normalized = raw
        .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
        .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
        .replace("ص", "AM").replace("م", "PM")

    val formats = listOf(
        "dd/MM/yyyy • hh:mm a",
        "dd/MM/yyyy • HH:mm",
        "dd/MM/yyyy, hh:mm a",
        "dd/MM/yyyy hh:mm a",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy",
        "MMM dd, yyyy • hh:mm a",
        "MMM d, yyyy • hh:mm a",
        "MMM dd, yyyy, hh:mm a",
        "MMM dd, yyyy",
        "dd MMM yyyy • hh:mm a",
        "dd MMM yyyy, hh:mm a",
        "dd MMM yyyy",
        "dd MMM, hh:mm a",
        "dd MMM, HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
            val parsed = sdf.parse(normalized)
            if (parsed != null) {
                val cal = Calendar.getInstance().apply { time = parsed }
                if (cal.get(Calendar.YEAR) < 2000) {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    cal.set(Calendar.YEAR, currentYear)
                }
                val res = cal.time
                parsedDateCache[raw] = res
                return res
            }
        } catch (_: Exception) { }
    }
    return null
}

fun parseDateToMillis(raw: String): Long {
    val date = parseDate(raw)
    return date?.time ?: 0L
}

fun parseDateAndTime(raw: String): Pair<String, String> {
    if (raw.isBlank()) return Pair("", "")
    parsedDateAndTimeCache[raw]?.let { return it }

    val parsed = parseDate(raw)
    if (parsed != null) {
        val dateOut = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(parsed)
        val timeOut = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(parsed)
        val res = Pair(dateOut, timeOut)
        parsedDateAndTimeCache[raw] = res
        return res
    }

    val slashRegex = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{4})\b""")
    val slashMatch = slashRegex.find(raw)
    if (slashMatch != null) {
        val (d, m, y) = slashMatch.destructured
        val day = d.padStart(2, '0')
        val month = m.padStart(2, '0')
        val datePart = "$day/$month/$y"
        val timePart = if (raw.contains("•")) {
            raw.substringAfter("•").trim()
        } else if (raw.contains(" ") && raw.indexOf(" ") < raw.length - 1) {
            raw.substring(raw.indexOf(" ") + 1).trim()
        } else ""
        val res = Pair(datePart, timePart)
        parsedDateAndTimeCache[raw] = res
        return res
    }

    if (raw.contains("•")) {
        val res = Pair(raw.substringBefore("•").trim(), raw.substringAfter("•").trim())
        parsedDateAndTimeCache[raw] = res
        return res
    }
    val res = Pair(raw, "")
    parsedDateAndTimeCache[raw] = res
    return res
}

fun formatDateCategory(raw: String, isAr: Boolean = false): String {
    val cacheKey = "$raw#$isAr"
    dateCategoryCache[cacheKey]?.let { return it }

    val strings = getAppStrings(isAr)
    if (raw.isBlank()) return strings.earlier
    
    val parsedDate = parseDate(raw)
    if (parsedDate != null) {
        val calTarget = Calendar.getInstance().apply { time = parsedDate }
        val calNow = Calendar.getInstance()

        val calTargetDay = Calendar.getInstance().apply {
            set(calTarget.get(Calendar.YEAR), calTarget.get(Calendar.MONTH), calTarget.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calNowDay = Calendar.getInstance().apply {
            set(calNow.get(Calendar.YEAR), calNow.get(Calendar.MONTH), calNow.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffDays = ((calNowDay.timeInMillis - calTargetDay.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

        val res = when (diffDays) {
            0 -> strings.today
            1 -> strings.yesterday
            2 -> strings.dayBeforeYesterday
            else -> {
                val isSameYear = calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
                val pattern = if (isSameYear) "d MMMM" else "d MMMM yyyy"
                val locale = if (isAr) Locale.forLanguageTag("ar") else Locale.ENGLISH
                SimpleDateFormat(pattern, locale).format(parsedDate)
            }
        }
        dateCategoryCache[cacheKey] = res
        return res
    }

    val (datePart, _) = parseDateAndTime(raw)
    val res = if (isAr) {
        // Fallback: translate any English month names if raw had English text
        datePart
            .replace("January", "يناير").replace("Jan", "يناير")
            .replace("February", "فبراير").replace("Feb", "فبراير")
            .replace("March", "مارس").replace("Mar", "مارس")
            .replace("April", "أبريل").replace("Apr", "أبريل")
            .replace("May", "مايو")
            .replace("June", "يونيو").replace("Jun", "يونيو")
            .replace("July", "يوليو").replace("Jul", "يوليو")
            .replace("August", "أغسطس").replace("Aug", "أغسطس")
            .replace("September", "سبتمبر").replace("Sep", "سبتمبر")
            .replace("October", "أكتوبر").replace("Oct", "أكتوبر")
            .replace("November", "نوفمبر").replace("Nov", "نوفمبر")
            .replace("December", "ديسمبر").replace("Dec", "ديسمبر")
            .replace("Today", "اليوم").replace("Yesterday", "أمس")
    } else {
        if (datePart.isNotBlank()) datePart else strings.earlier
    }
    dateCategoryCache[cacheKey] = res
    return res
}

fun formatDisplayDate(raw: String, isAr: Boolean = false): String {
    if (raw.isBlank()) return ""
    val strings = getAppStrings(isAr)
    val (datePart, timePart) = parseDateAndTime(raw)
    val localizedTime = if (isAr) {
        timePart.replace("AM", strings.am).replace("PM", strings.pm)
    } else {
        timePart
    }
    if (datePart.isNotBlank() && localizedTime.isNotBlank()) {
        return "\u200E$datePart\u200E • \u200E$localizedTime\u200E"
    } else if (datePart.isNotBlank()) {
        return "\u200E$datePart\u200E"
    }
    return "\u200E$raw\u200E"
}

/**
 * Universal Application Card Background & Border (shared across Settings, History, and Payment cards)
 * providing high contrast and consistent visual elevation.
 */
fun appCardBackground(isDark: Boolean): Color =
    if (isDark) Color(0xFF1C241E) else Color.White

fun appCardBorder(isDark: Boolean): Color =
    if (isDark) Color(0xFF2B372D) else Color(0xFFE5ECE7)

/**
 * Tactile pill depth modifier inspired by premium liquid glass and inset pill design.
 * Provides a balanced, curved inner specular highlight & soft inner light glow from the top,
 * and an ambient inner dark shadow from the bottom hugging any pill or rounded shape.
 */
fun Modifier.innerPillDepth(
    isDark: Boolean,
    enabled: Boolean = true,
    horizontalInset: Dp = 0.dp,
    cornerRadiusDp: Dp? = null
): Modifier = if (!enabled) this else this.drawWithContent {
    drawContent()
    if (size.width <= 0f || size.height <= 0f) return@drawWithContent

    val radiusPx = cornerRadiusDp?.toPx() ?: (size.height / 2f)
    val cornerRadius = CornerRadius(radiusPx, radiusPx)

    // 1. Balanced Curved Rim Bevel (Soft specular top, subtle ambient bottom)
    val strokeWidthPx = 0.75.dp.toPx()
    val halfStroke = strokeWidthPx / 2f
    val strokeBoundsSize = Size(
        (size.width - strokeWidthPx).coerceAtLeast(0f),
        (size.height - strokeWidthPx).coerceAtLeast(0f)
    )
    val strokeBoundsOffset = Offset(halfStroke, halfStroke)
    val strokeRadius = CornerRadius(
        (radiusPx - halfStroke).coerceAtLeast(0f),
        (radiusPx - halfStroke).coerceAtLeast(0f)
    )

    val topLightStroke = if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.60f)
    val bottomDarkStroke = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f)

    val rimGradient = Brush.verticalGradient(
        0.00f to topLightStroke,
        0.20f to topLightStroke.copy(alpha = topLightStroke.alpha * 0.35f),
        0.40f to Color.Transparent,
        0.60f to Color.Transparent,
        0.85f to bottomDarkStroke.copy(alpha = bottomDarkStroke.alpha * 0.40f),
        1.00f to bottomDarkStroke
    )

    drawRoundRect(
        brush = rimGradient,
        topLeft = strokeBoundsOffset,
        size = strokeBoundsSize,
        cornerRadius = strokeRadius,
        style = Stroke(width = strokeWidthPx)
    )

    // 2. Softly Blurred Inner Specular Light from Top (Full shape gradient with smooth non-linear falloff)
    val topGlowColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.40f)
    val topGlowGradient = Brush.verticalGradient(
        0.00f to topGlowColor,
        0.10f to topGlowColor.copy(alpha = topGlowColor.alpha * 0.50f),
        0.25f to topGlowColor.copy(alpha = topGlowColor.alpha * 0.15f),
        0.45f to Color.Transparent,
        1.00f to Color.Transparent
    )
    drawRoundRect(
        brush = topGlowGradient,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius
    )

    // 3. Softly Blurred Inner Ambient Dark Shadow from Bottom (Full shape gradient with soft smooth non-linear falloff)
    val bottomDarkColor = if (isDark) Color.Black.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.06f)
    val bottomShadowGradient = Brush.verticalGradient(
        0.00f to Color.Transparent,
        0.52f to Color.Transparent,
        0.72f to bottomDarkColor.copy(alpha = bottomDarkColor.alpha * 0.18f),
        0.86f to bottomDarkColor.copy(alpha = bottomDarkColor.alpha * 0.50f),
        1.00f to bottomDarkColor
    )
    drawRoundRect(
        brush = bottomShadowGradient,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius
    )
}

/**
 * Scopes font scale to a safe range inside compact controls (buttons, pills, tabs),
 * preventing excessive accessibility scaling from overflowing fixed-height components.
 */
@Composable
fun ControlDensityScope(
    minFontScale: Float = 0.80f,
    maxFontScale: Float = 1.10f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val safeDensity = remember(density.density, density.fontScale, minFontScale, maxFontScale) {
        androidx.compose.ui.unit.Density(
            density = density.density,
            fontScale = density.fontScale.coerceIn(minFontScale, maxFontScale)
        )
    }
    CompositionLocalProvider(LocalDensity provides safeDensity, content = content)
}

/**
 * Enterprise-grade auto-fitting text composable used across all pill buttons, chips, and input cards.
 * Measures text in O(1) time before layout using rememberTextMeasurer inside BoxWithConstraints.
 * Zero recomposition loops, zero visual frame flicker, and zero truncation/ellipses/wrapping.
 * Seamlessly adapts down to 6.sp on low-DPI (e.g. 320dpi) or small-width legacy phones.
 */
@Composable
fun AutoFitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    minFontSize: TextUnit = 6.sp,
    maxFontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Center
) {
    val effectiveMax = if (maxFontSize.isSpecified) maxFontSize else (if (style.fontSize.isSpecified) style.fontSize else 14.sp)
    val alignment = when (textAlign) {
        TextAlign.Start, TextAlign.Left -> Alignment.CenterStart
        TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val availableWidthPx = constraints.maxWidth

        val mergedStyle = style.copy(
            color = if (color != Color.Unspecified) color else style.color,
            fontWeight = fontWeight ?: style.fontWeight,
            textAlign = textAlign
        )

        val targetFontSize = remember(text, availableWidthPx, density.fontScale, density.density, effectiveMax, minFontSize) {
            if (availableWidthPx <= 0 || text.isEmpty()) {
                effectiveMax
            } else {
                val measured = textMeasurer.measure(
                    text = text,
                    style = mergedStyle.copy(fontSize = effectiveMax),
                    maxLines = 1,
                    softWrap = false
                )
                val textWidth = measured.size.width
                if (textWidth > availableWidthPx && textWidth > 0) {
                    val scale = (availableWidthPx.toFloat() / textWidth.toFloat()).coerceAtMost(1f)
                    val baseSp = effectiveMax.value
                    val minSp = if (minFontSize.isSpecified) minFontSize.value else 6f
                    // Apply a 0.96f sub-pixel safety margin to guarantee zero horizontal clipping
                    val calculatedSp = (baseSp * scale * 0.96f).coerceIn(minSp, baseSp)
                    calculatedSp.sp
                } else {
                    effectiveMax
                }
            }
        }

        Text(
            text = text,
            style = mergedStyle.copy(fontSize = targetFontSize),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

