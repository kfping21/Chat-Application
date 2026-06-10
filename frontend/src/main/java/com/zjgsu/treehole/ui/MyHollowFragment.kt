package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.CommentListAdapter
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.model.MyComment
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.network.MyCommentDto
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

    private lateinit var rvMySecrets: RecyclerView
    private lateinit var rvMyLiked: RecyclerView
    private lateinit var rvMyComments: RecyclerView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvStatFollowing: TextView
    private lateinit var tvStatFollowers: TextView
    private lateinit var tvStatSecrets: TextView
    private lateinit var tvStatEchoes: TextView
    private lateinit var tvStatSouls: TextView
    private lateinit var avatar: String
    private lateinit var nickname: String

    private lateinit var tabSecrets: TextView
    private lateinit var tabLiked: TextView
    private lateinit var tabComments: TextView

    private var secretsAdapter: SecretAdapter? = null
    private var likedAdapter: SecretAdapter? = null
    private var commentsAdapter: CommentListAdapter? = null

    private val mySecrets = mutableListOf<Secret>()
    private val myLikedPosts = mutableListOf<Secret>()
    private val myComments = mutableListOf<MyComment>()

    private var tvEmptySecrets: View? = null
    private var tvEmptyLiked: View? = null
    private var tvEmptyComments: View? = null
    private lateinit var btnMenu: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_hollow, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvMySecrets = view.findViewById(R.id.rv_my_secrets)
        rvMyLiked = view.findViewById(R.id.rv_my_liked)
        rvMyComments = view.findViewById(R.id.rv_my_comments)
        ivAvatar = view.findViewById(R.id.iv_my_avatar)
        tvUsername = view.findViewById(R.id.tv_my_username)
        tvStatFollowing = view.findViewById(R.id.tv_stat_following)
        tvStatFollowers = view.findViewById(R.id.tv_stat_followers)
        tvStatSecrets = view.findViewById(R.id.tv_stat_secrets)
        tvStatEchoes = view.findViewById(R.id.tv_stat_echoes)
        tvStatSouls = view.findViewById(R.id.tv_stat_souls)

        btnMenu = view.findViewById(R.id.btn_menu)

        tvEmptySecrets = view.findViewById<View>(R.id.tv_empty_secrets)
        tvEmptySecrets?.findViewById<TextView>(R.id.tv_empty_text)?.text = "暂无秘密"
        tvEmptySecrets?.findViewById<TextView>(R.id.tv_empty_emoji)?.text = "🍃"
        
        tvEmptyLiked = view.findViewById<View>(R.id.tv_empty_liked)
        tvEmptyLiked?.findViewById<TextView>(R.id.tv_empty_text)?.text = "暂无点赞"
        tvEmptyLiked?.findViewById<TextView>(R.id.tv_empty_emoji)?.text = "💔"
        
        tvEmptyComments = view.findViewById<View>(R.id.tv_empty_comments)
        tvEmptyComments?.findViewById<TextView>(R.id.tv_empty_text)?.text = "暂无评论"
        tvEmptyComments?.findViewById<TextView>(R.id.tv_empty_emoji)?.text = "💬"

        tabSecrets = view.findViewById(R.id.tab_secrets)
        tabLiked = view.findViewById(R.id.tab_liked)
        tabComments = view.findViewById(R.id.tab_comments)

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

        setupRecyclerViews()

        tabSecrets.setOnClickListener { switchTab(0) }
        tabLiked.setOnClickListener { switchTab(1) }
        tabComments.setOnClickListener { switchTab(2) }

        loadMyProfile()
        loadAllData()

        btnMenu.setOnClickListener {
            val sidebar = SidebarFragment()
            sidebar.setOnDismissListener {
                loadMyProfile()
                loadAllData()
            }
            sidebar.setOnLogoutListener {
                val activity = requireActivity()
                val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.action_my_to_login, null, androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_my_hollow, true)
                    .build())
            }
            sidebar.show(childFragmentManager, "SidebarFragment")
        }

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

        // Click listeners for following/followers stats
        tvStatFollowing.setOnClickListener {
            val userId = TokenManager.getUserId() ?: return@setOnClickListener
            navigateToFollowList(userId, "following", "我的关注")
        }

        tvStatFollowers.setOnClickListener {
            val userId = TokenManager.getUserId() ?: return@setOnClickListener
            navigateToFollowList(userId, "followers", "我的粉丝")
        }
    }

    private fun navigateToFollowList(userId: String, type: String, title: String) {
        findNavController().navigate(
            R.id.action_my_hollow_to_follow_list,
            Bundle().apply {
                putString("userId", userId)
                putString("type", type)
                putString("title", title)
            }
        )
    }

    private fun setupRecyclerViews() {
        rvMySecrets.layoutManager = LinearLayoutManager(requireContext())
        secretsAdapter = SecretAdapter(mySecrets, { postId, _ -> likePost(postId) }, { postId -> showDeleteConfirmDialog(postId) }, canDelete = true)
        rvMySecrets.adapter = secretsAdapter

        rvMyLiked.layoutManager = LinearLayoutManager(requireContext())
        likedAdapter = SecretAdapter(myLikedPosts, { postId, _ -> likePost(postId) }, null, canDelete = false)
        rvMyLiked.adapter = likedAdapter

        rvMyComments.layoutManager = LinearLayoutManager(requireContext())
        commentsAdapter = CommentListAdapter(
            myComments,
            { commentId, content -> editComment(commentId, content) },
            { commentId -> deleteComment(commentId) },
            { postId ->
                findNavController().navigate(R.id.secretDetailFragment, bundleOf("secretId" to postId))
            }
        )
        rvMyComments.adapter = commentsAdapter
    }

    private fun switchTab(tabIndex: Int) {
        rvMySecrets.visibility = View.GONE
        rvMyLiked.visibility = View.GONE
        rvMyComments.visibility = View.GONE
        tvEmptySecrets?.visibility = View.GONE
        tvEmptyLiked?.visibility = View.GONE
        tvEmptyComments?.visibility = View.GONE

        tabSecrets.setBackgroundResource(R.drawable.bg_button_ghost)
        tabSecrets.setTextColor(resources.getColor(R.color.text_muted))
        tabLiked.setBackgroundResource(R.drawable.bg_button_ghost)
        tabLiked.setTextColor(resources.getColor(R.color.text_muted))
        tabComments.setBackgroundResource(R.drawable.bg_button_ghost)
        tabComments.setTextColor(resources.getColor(R.color.text_muted))

        when (tabIndex) {
            0 -> {
                tabSecrets.setBackgroundResource(R.drawable.bg_button_golden)
                tabSecrets.setTextColor(resources.getColor(R.color.bg_deep))
                if (mySecrets.isEmpty()) {
                    tvEmptySecrets?.visibility = View.VISIBLE
                } else {
                    rvMySecrets.visibility = View.VISIBLE
                }
            }
            1 -> {
                tabLiked.setBackgroundResource(R.drawable.bg_button_golden)
                tabLiked.setTextColor(resources.getColor(R.color.bg_deep))
                if (myLikedPosts.isEmpty()) {
                    tvEmptyLiked?.visibility = View.VISIBLE
                } else {
                    rvMyLiked.visibility = View.VISIBLE
                }
            }
            2 -> {
                tabComments.setBackgroundResource(R.drawable.bg_button_golden)
                tabComments.setTextColor(resources.getColor(R.color.bg_deep))
                if (myComments.isEmpty()) {
                    tvEmptyComments?.visibility = View.VISIBLE
                } else {
                    rvMyComments.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAllData()
    }

    private fun loadMyProfile() {
        avatar = TokenManager.getAvatar() ?: ""
        nickname = TokenManager.getNickname() ?: ""

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
    }

    private fun loadAllData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentUserId = TokenManager.getUserId()

                // Load user profile for follow stats
                if (currentUserId != null) {
                    try {
                        val profileResponse = RetrofitClient.authApi.getUserProfile(currentUserId)
                        if (profileResponse.isSuccessful) {
                            val profile = profileResponse.body()
                            activity?.runOnUiThread {
                                tvStatFollowing.text = profile?.followingCount?.toString() ?: "0"
                                tvStatFollowers.text = profile?.followersCount?.toString() ?: "0"
                                tvStatSecrets.text = profile?.postsCount?.toString() ?: "0"
                                tvStatEchoes.text = profile?.commentsCount?.toString() ?: "0"
                                tvStatSouls.text = profile?.chatRoomsCount?.toString() ?: "0"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MyHollow", "Profile load error: ${e.message}")
                    }
                }

                // Load my posts
                loadMyPosts()
                // Load liked posts
                loadLikedPosts()
                // Load my comments
                loadMyComments()

                // Default to secrets tab
                switchTab(0)
            } catch (e: Exception) {
                Log.e("MyHollow", "Load error: ${e.message}")
            }
        }
    }

    private fun loadMyPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val postsResponse = RetrofitClient.postsApi.getMyPosts()

                mySecrets.clear()
                postsResponse.body()?.posts?.forEach { post ->
                    mySecrets.add(
                        Secret(
                            id = post.id,
                            content = post.content,
                            mood = post.mood,
                            timeAgo = TimeUtils.formatTimeAgo(post.createdAt),
                            likes = post.likes,
                            comments = post.commentCount,
                            avatar = avatar,
                            nickname = nickname,
                            isLiked = post.isLiked,
                            userId = post.user?.id ?: "",
                            imageUrls = post.imageUrls
                        )
                    )
                }
                secretsAdapter?.notifyDataSetChanged()
                
                // If secrets tab is active, update visibility
                if (tabSecrets.currentTextColor == resources.getColor(R.color.bg_deep)) {
                    if (mySecrets.isEmpty()) {
                        tvEmptySecrets?.visibility = View.VISIBLE
                        rvMySecrets.visibility = View.GONE
                    } else {
                        tvEmptySecrets?.visibility = View.GONE
                        rvMySecrets.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("MyHollow", "Load my posts error: ${e.message}")
            }
        }
    }

    private fun loadLikedPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val likedResponse = RetrofitClient.postsApi.getLikedPosts()

                myLikedPosts.clear()
                likedResponse.body()?.posts?.forEach { post ->
                    myLikedPosts.add(
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
                            userId = post.user?.id ?: "",
                            imageUrls = post.imageUrls
                        )
                    )
                }
                likedAdapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("MyHollow", "Load liked posts error: ${e.message}")
            }
        }
    }

    private fun loadMyComments() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val commentsResponse = RetrofitClient.postsApi.getMyComments()

                myComments.clear()
                commentsResponse.body()?.comments?.forEach { dto ->
                    myComments.add(
                        MyComment(
                            id = dto.id,
                            content = dto.content,
                            timeAgo = TimeUtils.formatTimeAgo(dto.createdAt),
                            postId = dto.postId,
                            postContent = dto.postContent,
                            mood = dto.mood
                        )
                    )
                }
                commentsAdapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("MyHollow", "Load my comments error: ${e.message}")
            }
        }
    }

    private fun likePost(postId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.likePost(postId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        secretsAdapter?.updateLikeData(postId, body.likes, body.isLiked)
                        likedAdapter?.updateLikeData(postId, body.likes, body.isLiked)
                    }
                }
            } catch (e: Exception) {
                Log.e("MyHollow", "Like post failed: ${e.message}")
            }
        }
    }

    private fun editComment(commentId: String, newContent: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.updateComment(commentId, com.zjgsu.treehole.network.AddCommentRequest(newContent))
                if (response.isSuccessful) {
                    // Update local data
                    val index = myComments.indexOfFirst { it.id == commentId }
                    if (index >= 0) {
                        myComments[index] = myComments[index].copy(content = newContent)
                        commentsAdapter?.notifyItemChanged(index)
                    }
                    Toast.makeText(requireContext(), "评论已更新", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteComment(commentId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.deleteComment(commentId)
                if (response.isSuccessful) {
                    val index = myComments.indexOfFirst { it.id == commentId }
                    if (index >= 0) {
                        myComments.removeAt(index)
                        commentsAdapter?.notifyItemRemoved(index)
                    }
                    Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(postId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除秘密")
            .setMessage("确定要删除这条秘密吗？删除后无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                deletePost(postId)
            }
            .show()
    }

    private fun deletePost(postId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.deletePost(postId)
                if (response.isSuccessful) {
                    // Remove from local list and update adapter
                    val index = mySecrets.indexOfFirst { it.id == postId }
                    if (index >= 0) {
                        mySecrets.removeAt(index)
                        secretsAdapter?.notifyItemRemoved(index)
                    }
                    Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                Log.e("MyHollow", "Avatar update failed: ${e.message}")
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
}