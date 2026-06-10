package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.zjgsu.treehole.R
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.network.AuthApi
import com.zjgsu.treehole.network.ChangePasswordRequest
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.network.WhisperWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.CoroutineContext

class SidebarFragment : androidx.fragment.app.DialogFragment(), CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvFollowing: TextView
    private lateinit var tvFollowers: TextView
    private var onDismissListener: (() -> Unit)? = null
    private var onLogoutListener: (() -> Unit)? = null

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

    fun setOnLogoutListener(listener: () -> Unit) {
        onLogoutListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.SidebarDialog)
        dialog.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            it.setBackgroundDrawableResource(android.R.color.transparent)
            it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.attributes?.dimAmount = 0.5f
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_sidebar_container, container, false)
        
        val sidebarContainer = view.findViewById<ViewGroup>(R.id.sidebar_content)
        sidebarContainer.addView(inflater.inflate(R.layout.layout_sidebar, sidebarContainer, false))
        
        view.setOnClickListener {
            if (isResumed) {
                dismiss()
            }
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivAvatar = view.findViewById(R.id.iv_sidebar_avatar)
        tvUsername = view.findViewById(R.id.tv_sidebar_username)
        tvFollowing = view.findViewById(R.id.tv_sidebar_following)
        tvFollowers = view.findViewById(R.id.tv_sidebar_followers)

        loadUserInfo()

        // Edit Profile
        view.findViewById<LinearLayout>(R.id.sidebar_edit_profile).setOnClickListener {
            showEditProfileDialog()
        }

        // Browsing History
        view.findViewById<LinearLayout>(R.id.sidebar_history).setOnClickListener {
            val bottomSheet = HistoryBottomSheet()
            bottomSheet.show(childFragmentManager, "HistoryBottomSheet")
        }

        // Drafts
        view.findViewById<LinearLayout>(R.id.sidebar_drafts).setOnClickListener {
            val bottomSheet = DraftsBottomSheet()
            bottomSheet.show(childFragmentManager, "DraftsBottomSheet")
        }

        // Theme
        view.findViewById<LinearLayout>(R.id.sidebar_theme).setOnClickListener {
            showThemeDialog()
        }

        // Settings Center (opens BottomSheet)
        view.findViewById<LinearLayout>(R.id.sidebar_settings_center).setOnClickListener {
            val bottomSheet = SettingsBottomSheet()
            bottomSheet.show(childFragmentManager, SettingsBottomSheet.TAG)
        }

        // Help
        view.findViewById<LinearLayout>(R.id.sidebar_help).setOnClickListener {
            val bottomSheet = FeedbackBottomSheet()
            bottomSheet.show(childFragmentManager, "FeedbackBottomSheet")
        }

        // Logout
        view.findViewById<LinearLayout>(R.id.sidebar_logout).setOnClickListener {
            showLogoutConfirmDialog()
        }

        ivAvatar.setOnClickListener {
            val bottomSheet = AvatarPickerBottomSheet()
            bottomSheet.setOnAvatarSelectedListener { avatarUrl ->
                updateAvatar(avatarUrl, ivAvatar)
            }
            bottomSheet.show(childFragmentManager, AvatarPickerBottomSheet.TAG)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        onDismissListener?.invoke()
    }

    private fun loadUserInfo() {
        val avatar = TokenManager.getAvatar() ?: ""
        val nickname = TokenManager.getNickname() ?: ""

        if (avatar.isNotEmpty()) {
            if (avatar.startsWith("preset_")) {
                val presetId = resources.getIdentifier(avatar, "drawable", requireContext().packageName)
                if (presetId != 0) ivAvatar.setImageResource(presetId)
            } else {
                Glide.with(this)
                    .load(avatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_nav_my)
                    .error(R.drawable.ic_nav_my)
                    .into(ivAvatar)
            }
        }

        if (nickname.isNotEmpty()) {
            tvUsername.text = nickname
        }

        launch {
            val currentUserId = TokenManager.getUserId()
            if (currentUserId != null) {
                try {
                    val profileResponse = RetrofitClient.authApi.getUserProfile(currentUserId)
                    if (profileResponse.isSuccessful) {
                        val profile = profileResponse.body()
                        tvFollowing.text = profile?.followingCount?.toString() ?: "0"
                        tvFollowers.text = profile?.followersCount?.toString() ?: "0"
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun showEditProfileDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val etNickname = EditText(requireContext()).apply {
            hint = "输入新昵称"
            setText(tvUsername.text)
            setSelection(text.length)
        }
        etNickname.setPadding(0, 32, 0, 32)

        container.addView(etNickname)

        AlertDialog.Builder(requireContext())
            .setTitle("编辑资料")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newNickname = etNickname.text.toString().trim()
                if (newNickname.length >= 3) {
                    updateNickname(newNickname)
                } else {
                    Toast.makeText(requireContext(), "昵称至少3个字符", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun updateNickname(newNickname: String) {
        launch {
            try {
                val nicknameBody = newNickname.toRequestBody("text/plain".toMediaTypeOrNull())
                val response = RetrofitClient.authApi.updateProfile(
                    nicknameBody,
                    null,
                    null,
                    null
                )
                if (response.isSuccessful) {
                    response.body()?.user?.let { user ->
                        TokenManager.saveNickname(user.nickname)
                        tvUsername.text = user.nickname
                        Toast.makeText(requireContext(), "昵称已更新", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun showThemeDialog() {
        val themeOptions = arrayOf("深色模式", "浅色模式", "跟随系统")
        val modeValues = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        val currentMode = TokenManager.getThemeMode()
        val currentIndex = modeValues.indexOf(currentMode).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle("主题选择")
            .setSingleChoiceItems(themeOptions, currentIndex) { dialog, which ->
                val selectedMode = modeValues[which]
                TokenManager.setThemeMode(selectedMode)
                // This automatically triggers recreation if the uiMode changes
                AppCompatDelegate.setDefaultNightMode(selectedMode)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确认退出当前账号吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("退出") { _, _ ->
                WhisperWebSocket.disconnect()
                PostCacheManager.clearCache()
                TokenManager.clear()
                Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onLogoutListener?.invoke()
                }, 500)
            }
            .show()
    }

    private fun updateAvatar(avatarUrl: String, ivAvatar: ImageView) {
        launch {
            var serverAvatarUrl: String? = null
            try {
                val avatarBody = avatarUrl.toRequestBody("text/plain".toMediaTypeOrNull())
                val response = RetrofitClient.authApi.updateProfile(
                    null,
                    avatarBody,
                    null,
                    null
                )
                if (response.isSuccessful) {
                    serverAvatarUrl = response.body()?.user?.avatar
                }
            } catch (e: Exception) {
                android.util.Log.e("Sidebar", "Avatar update failed: ${e.message}")
            }

            val finalAvatarUrl = serverAvatarUrl ?: avatarUrl

            Glide.with(this@SidebarFragment)
                .load(finalAvatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_nav_my)
                .error(R.drawable.ic_nav_my)
                .into(ivAvatar)
            TokenManager.saveAvatar(finalAvatarUrl)
            Toast.makeText(requireContext(), "头像已更新", Toast.LENGTH_SHORT).show()
        }
    }
}
