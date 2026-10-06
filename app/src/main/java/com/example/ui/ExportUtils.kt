package com.example.ui

import android.content.Context
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.HistoryEntry
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun exportTransactions(context: Context, data: List<HistoryEntry>, isAr: Boolean, type: String) {
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "USSD-Pay")
    if (!dir.exists()) dir.mkdirs()

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())

    if (type == "csv") {
        val file = File(dir, "transactions_$timeStamp.csv")
        try {
            val writer = FileWriter(file)
            val header = "Date,Amount,Recipient,Phone,Status,Type"
            writer.append(header + "\n")
            data.forEach { entry ->
                val date = formatDisplayDate(entry.timestamp, isAr).replace(",", " ")
                writer.append("$date,${entry.amount},${entry.name},${entry.number},${entry.status},${entry.type}\n")
            }
            writer.flush()
            writer.close()
            Toast.makeText(context, "Saved in Documents/USSD-Pay", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else if (type == "pdf") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val webView = WebView(context)
        val html = buildHtmlReport(data, isAr)
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printAdapter = webView.createPrintDocumentAdapter("USSD_Pay_Report_$timeStamp")
                printManager.print("Transactions Report", printAdapter, PrintAttributes.Builder().build())
            }
        }
    }
}

fun buildHtmlReport(data: List<HistoryEntry>, isAr: Boolean): String {
    val dir = if (isAr) "rtl" else "ltr"
    val align = if (isAr) "right" else "left"
    val title = "Transactions Report"
    val hDate = "Date"
    val hAmount = "Amount"
    val hRecipient = "Recipient"
    val hPhone = "Phone"
    val hStatus = "Status"
    val hType = "Type"

    val rows = data.joinToString("\n") { entry ->
        val date = formatDisplayDate(entry.timestamp, isAr)
        "<tr><td>$date</td><td>${entry.amount}</td><td>${entry.name}</td><td>${entry.number}</td><td>${entry.status}</td><td>${entry.type}</td></tr>"
    }

    return """
        <!DOCTYPE html>
        <html dir="$dir">
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: sans-serif; padding: 20px; color: #333; }
                h2 { text-align: center; color: #1565C0; margin-bottom: 20px; }
                table { width: 100%; border-collapse: collapse; margin-top: 20px; font-size: 14px; }
                th { background-color: #f4f4f4; border-bottom: 2px solid #ddd; padding: 12px; text-align: $align; color: #555; }
                td { border-bottom: 1px solid #eee; padding: 12px; text-align: $align; }
                tr:nth-child(even) { background-color: #fafafa; }
            </style>
        </head>
        <body>
            <h2>$title</h2>
            <table>
                <thead>
                    <tr><th>$hDate</th><th>$hAmount</th><th>$hRecipient</th><th>$hPhone</th><th>$hStatus</th><th>$hType</th></tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
        </body>
        </html>
    """.trimIndent()
}
