package com.zjgsu.treehole.cache

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SearchHistoryManager {
    private const val PREFS_NAME = "search_history_prefs"
    private const val KEY_HISTORY_LIST = "search_history_list"
    private const val MAX_HISTORY = 20

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addHistory(context: Context, keyword: String) {
        if (keyword.isBlank()) return

        val prefs = getPrefs(context)
        val historyList = getHistory(context).toMutableList()

        // Remove if already exists to push it to the top
        historyList.remove(keyword)

        // Add to top
        historyList.add(0, keyword)

        // Trim
        if (historyList.size > MAX_HISTORY) {
            historyList.removeAt(historyList.size - 1)
        }

        prefs.edit().putString(KEY_HISTORY_LIST, gson.toJson(historyList)).apply()
    }

    fun getHistory(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_HISTORY_LIST, "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
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
