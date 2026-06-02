package com.zjgsu.treehole.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 网络状态管理器 - 处理网络切换时的用户体验
 */
object NetworkToastManager {

    private var previousOnlineState: Boolean? = null
    private var hideJob: Job? = null

    fun observeNetworkState(context: Context, scope: CoroutineScope) {
        val networkMonitor = NetworkMonitor.getInstance(context)

        scope.launch(Dispatchers.Main) {
            networkMonitor.isOnline.collectLatest { isOnline ->
                handleNetworkChange(context, isOnline)
            }
        }
    }

    private fun handleNetworkChange(context: Context, isOnline: Boolean) {
        val previousState = previousOnlineState
        previousOnlineState = isOnline

        when {
            // 刚从离线变为在线
            previousState == false && isOnline -> {
                showToast(context, "网络已连接，正在刷新...", short = true)
            }
            // 刚从在线变为离线
            previousState == true && !isOnline -> {
                showToast(context, "网络已断开，请在网络恢复后重试", short = false)
            }
            // 持续离线状态，不重复提示
            previousState == false && !isOnline -> {
                // 不做任何提示
            }
            // 首次检测
            previousState == null -> {
                if (!isOnline) {
                    showToast(context, "当前无网络连接", short = false)
                }
            }
        }
    }

    private fun showToast(context: Context, message: String, short: Boolean) {
        hideJob?.cancel()

        Toast.makeText(context, message, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()

        if (short) {
            hideJob = CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
            }
        }
    }
}
