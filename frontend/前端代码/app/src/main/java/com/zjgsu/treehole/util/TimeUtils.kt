package com.zjgsu.treehole.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object TimeUtils {
    private val isoFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    fun formatTimeAgo(isoDateString: String): String {
        return try {
            var date: Date? = null
            for (formatStr in isoFormats) {
                try {
                    val format = SimpleDateFormat(formatStr, Locale.getDefault())
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    date = format.parse(isoDateString)
                    if (date != null) break
                } catch (e: Exception) {
                    // Try next format
                }
            }
            if (date == null) return isoDateString

            val now = Date()
            val diff = now.time - date.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            when {
                days > 0 -> "${days}天前"
                hours > 0 -> "${hours}小时前"
                minutes > 0 -> "${minutes}分钟前"
                else -> "刚刚"
            }
        } catch (e: Exception) {
            isoDateString
        }
    }
}