package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.util.AvatarLoader
import com.zjgsu.treehole.util.TimeUtils
import kotlinx.coroutines.launch


class UserProfileFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvStatFollowing: TextView
    private lateinit var tvStatFollowers: TextView
    private lateinit var tvStatSecrets: TextView
    private lateinit var tvStatEchoes: TextView
    private lateinit var tvStatSouls: TextView
    private lateinit var tvFollow: TextView
    private lateinit var btnFollow: LinearLayout
    private lateinit var btnChat: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var rvSecrets: RecyclerView
    private lateinit var tvEmpty: TextView

    private var userId: String = ""
    private var isFollowing = false
    private var followRequesting = false
    private lateinit var adapter: SecretAdapter
    private val secrets = mutableListOf<Secret>()

    companion object {
        private const val ARG_USER_ID = "userId"
        private const val ARG_NICKNAME = "nickname"
        private const val ARG_AVATAR = "avatar"

        fun newInstance(userId: String, nickname: String, avatar: String): UserProfileFragment {
            return UserProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putString(ARG_NICKNAME, nickname)
                    putString(ARG_AVATAR, avatar)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getString(ARG_USER_ID) ?: ""

        // Init views
        ivAvatar = view.findViewById(R.id.iv_user_avatar)
        tvNickname = view.findViewById(R.id.tv_user_nickname)
        tvStatFollowing = view.findViewById(R.id.tv_stat_following)
        tvStatFollowers = view.findViewById(R.id.tv_stat_followers)
        tvStatSecrets = view.findViewById(R.id.tv_stat_secrets)
        tvStatEchoes = view.findViewById(R.id.tv_stat_echoes)
        tvStatSouls = view.findViewById(R.id.tv_stat_souls)
        tvFollow = view.findViewById(R.id.tv_follow)
        btnFollow = view.findViewById(R.id.btn_follow)
        btnChat = view.findViewById(R.id.btn_chat)
        btnBack = view.findViewById(R.id.btn_back)
        rvSecrets = view.findViewById(R.id.rv_user_secrets)
        tvEmpty = view.findViewById(R.id.tv_empty)

        // Setup RecyclerView
        adapter = SecretAdapter(secrets)
        rvSecrets.layoutManager = LinearLayoutManager(requireContext())
        rvSecrets.adapter = adapter

        // Load user profile
        loadUserProfile()
        loadUserPosts()

        btnFollow.setOnClickListener {
            toggleFollow()
        }

        // Chat button - start chat
        btnChat.setOnClickListener {
            startChat()
        }

        // Back button
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.getUserProfile(userId)
                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        activity?.runOnUiThread {
                            tvNickname.text = profile.nickname.ifEmpty { "匿名灵魂" }
                            tvStatFollowing.text = profile.followingCount.toString()
                            tvStatFollowers.text = profile.followersCount.toString()
                            tvStatSecrets.text = profile.postsCount.toString()
                            tvStatEchoes.text = profile.commentsCount.toString()
                            tvStatSouls.text = profile.chatRoomsCount.toString()

                            // Load avatar
                            AvatarLoader.loadAvatar(requireContext(), profile.avatar, ivAvatar)

                            // Update follow button
                            isFollowing = profile.isFollowing
                            updateFollowButton()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "获取用户信息失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            tvFollow.text = "已关注"
            btnFollow.setBackgroundResource(R.drawable.bg_chip_default)
            tvFollow.setTextColor(resources.getColor(R.color.text_muted, null))
        } else {
            tvFollow.text = "关注"
            btnFollow.setBackgroundResource(R.drawable.bg_ai_notice_chip)
            tvFollow.setTextColor(resources.getColor(R.color.ai_accent, null))
        }
    }

    private fun toggleFollow() {
        if (followRequesting) return
        if (userId.isBlank()) return
        if (userId == TokenManager.getUserId()) {
            Toast.makeText(requireContext(), "不能关注自己", Toast.LENGTH_SHORT).show()
            return
        }

        followRequesting = true
        btnFollow.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.whisperApi.followUser(userId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        isFollowing = body.isFollowing
                        updateFollowButton()
                        updateFollowersCount(isFollowing)
                        Toast.makeText(requireContext(), body.message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                followRequesting = false
                btnFollow.isEnabled = true
            }
        }
    }

    private fun updateFollowersCount(followingNow: Boolean) {
        val current = tvStatFollowers.text?.toString()?.toIntOrNull() ?: 0
        val next = if (followingNow) current + 1 else (current - 1).coerceAtLeast(0)
        tvStatFollowers.text = next.toString()
    }

    private fun loadUserPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.getUserPosts(userId)
                if (response.isSuccessful) {
                    secrets.clear()
                    response.body()?.posts?.forEach { post ->
                        secrets.add(
                            Secret(
                                id = post.id,
                                content = post.content,
                                mood = post.mood,
                                timeAgo = TimeUtils.formatTimeAgo(post.createdAt),
                                likes = post.likes,
                                comments = post.commentCount,
                                avatar = post.user?.avatar ?: "",
                                nickname = post.user?.nickname ?: "",
                                isLiked = post.isLiked,
                                userId = userId
                            )
                        )
                    }
                    activity?.runOnUiThread {
                        adapter.notifyDataSetChanged()
                        tvEmpty.visibility = if (secrets.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun startChat() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.whisperApi.startChat(userId)
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        val bundle = Bundle().apply {
                            putString("roomId", data.roomId)
                            putString("participantId", data.participantId)
                            putString("nickname", data.nickname)
                            putString("avatar", data.avatar)
                        }
                        findNavController().navigate(R.id.whisperChatFragment, bundle)
                    }
                } else {
                    Toast.makeText(requireContext(), "发起聊天失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "发起聊天失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
