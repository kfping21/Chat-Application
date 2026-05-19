package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.network.WhisperWebSocket
import com.zjgsu.treehole.util.AvatarLoader
import com.zjgsu.treehole.util.TimeUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class MyHollowFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvStatSecrets: TextView
    private lateinit var tvStatEchoes: TextView
    private lateinit var tvStatSouls: TextView
    private lateinit var tvMyBio: TextView
    private lateinit var avatar: String
    private lateinit var nickname: String

    private var adapter: SecretAdapter? = null
    private val mySecrets = mutableListOf<com.zjgsu.treehole.model.Secret>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_hollow, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rv_my_secrets)
        ivAvatar = view.findViewById(R.id.iv_my_avatar)
        tvUsername = view.findViewById(R.id.tv_my_username)
        tvStatSecrets = view.findViewById(R.id.tv_stat_secrets)
        tvStatEchoes = view.findViewById(R.id.tv_stat_echoes)
        tvStatSouls = view.findViewById(R.id.tv_stat_souls)
        tvMyBio = view.findViewById(R.id.tv_my_bio)

        avatar = TokenManager.getAvatar() ?: ""
        nickname = TokenManager.getNickname() ?: ""

        // Load avatar and nickname
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

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = SecretAdapter(mySecrets) { postId, _ -> likePost(postId) }
        rv.adapter = adapter

        // Load my posts
        loadMyPosts()

        // Avatar click to change
        ivAvatar.setOnClickListener {
            val bottomSheet = AvatarPickerBottomSheet()
            bottomSheet.setOnAvatarSelectedListener { avatarUrl ->
                updateAvatar(avatarUrl, ivAvatar)
            }
            bottomSheet.show(childFragmentManager, AvatarPickerBottomSheet.TAG)
        }

        // Username click to edit
        tvUsername.setOnClickListener {
            showNicknameEditDialog(tvUsername)
        }

        // Bio click to edit
        tvMyBio.setOnClickListener {
            showBioEditDialog(tvMyBio)
        }

        // Add animations to settings buttons
        val buttons = listOf(
            view.findViewById<LinearLayout>(R.id.btn_privacy),
            view.findViewById<LinearLayout>(R.id.btn_theme),
            view.findViewById<LinearLayout>(R.id.btn_settings),
            view.findViewById<LinearLayout>(R.id.btn_logout)
        )

        buttons.forEach { btn ->
            ClickAnimations.addButtonPressAnimation(btn)
        }

        // Button click handlers
        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            showLogoutConfirmDialog()
        }

        view.findViewById<View>(R.id.btn_privacy).setOnClickListener {
            showPrivacyDialog()
        }

        view.findViewById<View>(R.id.btn_theme).setOnClickListener {
            showThemeDialog()
        }

        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            showSettingsDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadMyPosts()
    }

    private fun loadMyPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val postsResponse = RetrofitClient.postsApi.getMyPosts()
                val whispersResponse = RetrofitClient.whisperApi.getChatRooms()

                var totalLikes = 0
                var totalComments = 0
                var postCount = 0

                if (postsResponse.isSuccessful) {
                    val posts = postsResponse.body()?.posts ?: emptyList()
                    postCount = posts.size
                    posts.forEach { post ->
                        totalLikes += post.likes
                        totalComments += post.commentCount
                    }
                }

                val chatRoomCount = if (whispersResponse.isSuccessful) {
                    whispersResponse.body()?.rooms?.size ?: 0
                } else {
                    0
                }

                // Update stats
                activity?.runOnUiThread {
                    applyStatsPrivacy(
                        postCount = postCount,
                        echoesCount = totalLikes + totalComments,
                        soulsCount = chatRoomCount
                    )
                }

                // Load user profile for bio
                val currentUserId = TokenManager.getUserId()
                if (currentUserId != null) {
                    try {
                        val profileResponse = RetrofitClient.authApi.getUserProfile(currentUserId)
                        if (profileResponse.isSuccessful) {
                            val bio = profileResponse.body()?.bio ?: ""
                            activity?.runOnUiThread {
                                if (bio.isNotEmpty()) {
                                    tvMyBio.text = bio
                                    tvMyBio.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary))
                                    tvMyBio.setTypeface(null, android.graphics.Typeface.NORMAL)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore bio load error
                    }
                }

                // Update posts list
                mySecrets.clear()
                postsResponse.body()?.posts?.take(2)?.forEach { post ->
                    mySecrets.add(
                        com.zjgsu.treehole.model.Secret(
                            id = post.id,
                            content = post.content,
                            mood = post.mood,
                            timeAgo = TimeUtils.formatTimeAgo(post.createdAt),
                            likes = post.likes,
                            comments = post.commentCount,
                            avatar = avatar,
                            nickname = nickname,
                            isLiked = post.isLiked,
                            userId = post.user?.id ?: ""
                        )
                    )
                }
                adapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                android.util.Log.e("MyHollow", "Load error: ${e.message}")
            }
        }
    }

    private fun likePost(postId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.likePost(postId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        adapter?.updateLikeData(postId, body.likes, body.isLiked)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MyHollow", "Like post failed: ${e.message}")
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
        val cbHideStats = CheckBox(requireContext()).apply {
            text = "隐藏我的统计数据"
            isChecked = TokenManager.isHideMyStatsEnabled()
        }

        container.addView(cbIncognito)
        container.addView(cbHideStats)

        AlertDialog.Builder(requireContext())
            .setTitle("隐私保护")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                TokenManager.setIncognitoModeEnabled(cbIncognito.isChecked)
                TokenManager.setHideMyStatsEnabled(cbHideStats.isChecked)

                if (cbHideStats.isChecked) {
                    applyStatsPrivacy(postCount = 0, echoesCount = 0, soulsCount = 0)
                } else {
                    loadMyPosts()
                }
                Toast.makeText(requireContext(), "隐私设置已保存", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun applyStatsPrivacy(postCount: Int, echoesCount: Int, soulsCount: Int) {
        if (TokenManager.isHideMyStatsEnabled()) {
            tvStatSecrets.text = "***"
            tvStatEchoes.text = "***"
            tvStatSouls.text = "***"
        } else {
            tvStatSecrets.text = postCount.toString()
            tvStatEchoes.text = echoesCount.toString()
            tvStatSouls.text = soulsCount.toString()
        }
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
                AppCompatDelegate.setDefaultNightMode(selectedMode)
                dialog.dismiss()
                requireActivity().recreate()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("清理本地缓存", "重置隐私设置", "关于应用")
        AlertDialog.Builder(requireContext())
            .setTitle("设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        PostCacheManager.clearCache()
                        Toast.makeText(requireContext(), "缓存已清理", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        TokenManager.setIncognitoModeEnabled(false)
                        TokenManager.setHideMyStatsEnabled(false)
                        loadMyPosts()
                        Toast.makeText(requireContext(), "隐私设置已重置", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("关于应用")
                            .setMessage(
                                "树洞 v${resolveAppVersionName()}\n" +
                                    "实时后端: ${RetrofitClient.BASE_URL}\n" +
                                    "一个倾听与表达的匿名空间"
                            )
                            .setPositiveButton("我知道了", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun resolveAppVersionName(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
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
                findNavController().navigate(R.id.action_my_to_login)
            }
            .show()
    }

    private fun updateAvatar(avatarUrl: String, ivAvatar: ImageView) {
        viewLifecycleOwner.lifecycleScope.launch {
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
                android.util.Log.e("MyHollow", "Avatar update failed: ${e.message}")
            }

            val finalAvatarUrl = serverAvatarUrl ?: avatarUrl

            AvatarLoader.loadAvatar(requireContext(), finalAvatarUrl, ivAvatar)
            TokenManager.saveAvatar(finalAvatarUrl)
            avatar = finalAvatarUrl
            Toast.makeText(requireContext(), "头像已更新", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNicknameEditDialog(currentTextView: TextView) {
        val editText = EditText(requireContext()).apply {
            hint = "输入新昵称"
            setText(currentTextView.text)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("修改昵称")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newNickname = editText.text.toString().trim()
                if (newNickname.length >= 3) {
                    updateNickname(newNickname, currentTextView)
                } else {
                    Toast.makeText(requireContext(), "昵称至少3个字符", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateNickname(newNickname: String, tvUsername: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        nickname = user.nickname
                        Toast.makeText(requireContext(), "昵称已更新", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "更新失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBioEditDialog(currentTextView: TextView) {
        val editText = EditText(requireContext()).apply {
            hint = "输入个人介绍"
            val currentText = currentTextView.text.toString()
            setText(if (currentText == "点击添加介绍，让大家了解你...") "" else currentText)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
            minLines = 2
            maxLines = 4
        }

        AlertDialog.Builder(requireContext())
            .setTitle("编辑个人介绍")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newBio = editText.text.toString().trim()
                updateBio(newBio, currentTextView)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateBio(newBio: String, tvBio: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bioBody = newBio.toRequestBody("text/plain".toMediaTypeOrNull())
                val response = RetrofitClient.authApi.updateProfile(
                    null,
                    null,
                    bioBody,
                    null
                )
                if (response.isSuccessful) {
                    activity?.runOnUiThread {
                        if (newBio.isNotEmpty()) {
                            tvBio.text = newBio
                            tvBio.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary))
                            tvBio.setTypeface(null, android.graphics.Typeface.NORMAL)
                        } else {
                            tvBio.text = "点击添加介绍，让大家了解你..."
                            tvBio.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_muted))
                            tvBio.setTypeface(null, android.graphics.Typeface.ITALIC)
                        }
                        Toast.makeText(requireContext(), "个人介绍已更新", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "更新失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
