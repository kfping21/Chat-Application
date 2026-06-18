package com.zjgsu.treehole.network

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREFS_NAME = "treehole_auth"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_INCOGNITO_MODE = "incognito_mode"
    private const val KEY_HIDE_MY_STATS = "hide_my_stats"
    private const val KEY_THEME_MODE = "theme_mode"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUser(id: String, username: String, nickname: String, avatar: String = "") {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_AVATAR, avatar)
            .apply()
    }

    fun saveAvatar(avatar: String) {
        prefs.edit().putString(KEY_AVATAR, avatar).apply()
    }

    fun saveNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getNickname(): String? = prefs.getString(KEY_NICKNAME, null)
    fun getAvatar(): String? = prefs.getString(KEY_AVATAR, null)

    fun setIncognitoModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INCOGNITO_MODE, enabled).apply()
    }

    fun isIncognitoModeEnabled(): Boolean = prefs.getBoolean(KEY_INCOGNITO_MODE, false)

    fun setHideMyStatsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_MY_STATS, enabled).apply()
    }

    fun isHideMyStatsEnabled(): Boolean = prefs.getBoolean(KEY_HIDE_MY_STATS, false)

    fun setThemeMode(themeMode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply()
    }

    fun getThemeMode(defaultMode: Int = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES): Int =
        prefs.getInt(KEY_THEME_MODE, defaultMode)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }
}
