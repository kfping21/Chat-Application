package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjgsu.treehole.R
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.network.ChangePasswordRequest
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SettingsBottomSheet : BottomSheetDialogFragment(), CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    companion object {
        const val TAG = "SettingsBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_settings_bottom_sheet, container, false)
        
        view.findViewById<LinearLayout>(R.id.btn_change_password).setOnClickListener {
            showChangePasswordDialog()
        }
        
        view.findViewById<LinearLayout>(R.id.btn_privacy).setOnClickListener {
            showPrivacyDialog()
        }
        
        view.findViewById<LinearLayout>(R.id.btn_storage).setOnClickListener {
            showStorageInfoDialog()
        }
        
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun showChangePasswordDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val etOldPassword = EditText(requireContext()).apply {
            hint = "旧密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        etOldPassword.setPadding(0, 16, 0, 16)

        val etNewPassword = EditText(requireContext()).apply {
            hint = "新密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        etNewPassword.setPadding(0, 16, 0, 16)

        val etConfirmPassword = EditText(requireContext()).apply {
            hint = "确认新密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        etConfirmPassword.setPadding(0, 16, 0, 16)

        container.addView(etOldPassword)
        container.addView(etNewPassword)
        container.addView(etConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("修改密码")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                val oldPassword = etOldPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (oldPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入旧密码", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入新密码", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(requireContext(), "密码至少6个字符", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(requireContext(), "两次密码不一致", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(oldPassword, newPassword)
            }
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        launch {
            try {
                val response = RetrofitClient.authApi.changePassword(
                    ChangePasswordRequest(oldPassword, newPassword)
                )
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "密码修改成功", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "修改失败"
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPrivacyDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val cbIncognito = CheckBox(requireContext()).apply {
            text = "隐身在线状态（不显示在线）"
            isChecked = TokenManager.isIncognitoModeEnabled()
        }

        container.addView(cbIncognito)

        AlertDialog.Builder(requireContext())
            .setTitle("隐私保护")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                TokenManager.setIncognitoModeEnabled(cbIncognito.isChecked)
                Toast.makeText(requireContext(), "隐私设置已保存", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showStorageInfoDialog() {
        val externalDir = Environment.getExternalStorageDirectory()
        val totalSpace = externalDir.totalSpace
        val freeSpace = externalDir.freeSpace

        AlertDialog.Builder(requireContext())
            .setTitle("存储空间")
            .setMessage(
                "设备存储:\n" +
                        "  总容量: ${(totalSpace / 1024 / 1024 / 1024)} GB\n" +
                        "  可用空间: ${(freeSpace / 1024 / 1024 / 1024)} GB"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("清理缓存") { _, _ ->
                PostCacheManager.clearCache()
                Toast.makeText(requireContext(), "缓存已清理", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
