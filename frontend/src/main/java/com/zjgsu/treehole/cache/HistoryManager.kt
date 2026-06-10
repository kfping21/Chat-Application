package com.zjgsu.treehole.cache

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class HistoryItem(
    val id: String,
    val content: String,
    val mood: String,
    val timeAgo: String,
    val avatar: String,
    val nickname: String,
    val viewedAt: Long = System.currentTimeMillis()
)

object HistoryManager {
    private const val PREFS_NAME = "history_prefs"
    private const val KEY_HISTORY_LIST = "history_list"
    private const val MAX_HISTORY = 50

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addHistory(context: Context, item: HistoryItem) {
        val prefs = getPrefs(context)
        val historyList = getHistory(context).toMutableList()

        // Remove if already exists to push it to the top
        historyList.removeAll { it.id == item.id }

        // Add to top
        historyList.add(0, item)

        // Trim
        if (historyList.size > MAX_HISTORY) {
            historyList.removeAt(historyList.size - 1)
        }

        prefs.edit().putString(KEY_HISTORY_LIST, gson.toJson(historyList)).apply()
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val json = getPrefs(context).getString(KEY_HISTORY_LIST, "[]") ?: "[]"
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY_LIST).apply()
    }
}
