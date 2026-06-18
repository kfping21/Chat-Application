package com.zjgsu.treehole.cache

import android.content.Context
import android.content.SharedPreferences

object DraftManager {
    private const val PREFS_NAME = "drafts_prefs"
    private const val KEY_SECRET_DRAFT = "secret_draft"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDraft(context: Context, content: String) {
        getPrefs(context).edit().putString(KEY_SECRET_DRAFT, content).apply()
    }

    fun getDraft(context: Context): String {
        return getPrefs(context).getString(KEY_SECRET_DRAFT, "") ?: ""
    }

    fun clearDraft(context: Context) {
        getPrefs(context).edit().remove(KEY_SECRET_DRAFT).apply()
    }
}
