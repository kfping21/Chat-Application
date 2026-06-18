package com.zjgsu.treehole

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.zjgsu.treehole.network.TokenManager

class TreeHoleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.init(this)
        AppCompatDelegate.setDefaultNightMode(TokenManager.getThemeMode())
    }
}
