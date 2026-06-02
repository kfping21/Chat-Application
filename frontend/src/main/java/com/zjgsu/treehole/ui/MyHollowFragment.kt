package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.CommentListAdapter
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.model.MyComment
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.network.MyCommentDto
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.util.AvatarLoader
import com.zjgsu.treehole.util.TimeUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

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

    private lateinit var tvEmptySecrets: TextView
    private lateinit var tvEmptyLiked: TextView
    private lateinit var tvEmptyComments: TextView
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

        tvEmptySecrets = view.findViewById(R.id.tv_empty_secrets)
        tvEmptyLiked = view.findViewById(R.id.tv_empty_liked)
        tvEmptyComments = view.findViewById(R.id.tv_empty_comments)

        tabSecrets = view.findViewById(R.id.tab_secrets)
        tabLiked = view.findViewById(R.id.tab_liked)
        tabComments = view.findViewById(R.id.tab_comments)

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
            sidebar.show(childFragmentManager, "SidebarFragment")
        }

        ivAvatar.setOnClickListener {
            val bottomSheet = AvatarPickerBottomSheet()
            bottomSheet.setOnAvatarSelectedListener { avatarUrl ->
                updateAvatar(avatarUrl, ivAvatar)
            }
            bottomSheet.show(childFragmentManager, AvatarPickerBottomSheet.TAG)
        }

        tvUsername.setOnClickListener {
            showNicknameEditDialog(tvUsername)
        }

        tvStatFollowing.setOnClickListener {
            val userId = TokenManager.getUserId() ?: return@setOnClickListener
            navigateToFollowList(userId, "following", "我的关注")
        }

        tvStatFollowers.setOnClickListener {
            val userId = TokenManager.getUserId() ?: return@setOnClickListener
            navigateToFollowList(userId, "followers", "我的粉丝")
        }
    }

    private fun setupRecyclerViews() {
        rvMySecrets.layoutManager = LinearLayoutManager(requireContext())
        secretsAdapter = SecretAdapter(mySecrets, { postId, _ -> likePost(postId) }, { postId -> showDeleteConfirmDialog(postId) }, canDelete = true)
        rvMySecrets.adapter = secretsAdapter

        rvMyLiked.layoutManager = LinearLayoutManager(requireContext())
        likedAdapter = SecretAdapter(myLikedPosts, { postId, _ -> likePost(postId) }, null, canDelete = false)
        rvMyLiked.adapter = likedAdapter

        rvMyComments.layoutManager = LinearLayoutManager(requireContext())
        commentsAdapter = CommentListAdapter(myComments, { commentId, content -> editComment(commentId, content) }, { commentId -> deleteComment(commentId) })
        rvMyComments.adapter = commentsAdapter
    }

    private fun switchTab(tabIndex: Int) {
        rvMySecrets.visibility = View.GONE
        rvMyLiked.visibility = View.GONE
        rvMyComments.visibility = View.GONE
        tvEmptySecrets.visibility = View.GONE
        tvEmptyLiked.visibility = View.GONE
        tvEmptyComments.visibility = View.GONE

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
                    tvEmptySecrets.visibility = View.VISIBLE
                } else {
                    rvMySecrets.visibility = View.VISIBLE
                }
            }
            1 -> {
                tabLiked.setBackgroundResource(R.drawable.bg_button_golden)
                tabLiked.setTextColor(resources.getColor(R.color.bg_deep))
                if (myLikedPosts.isEmpty()) {
                    tvEmptyLiked.visibility = View.VISIBLE
                } else {
                    rvMyLiked.visibility = View.VISIBLE
                }
            }
            2 -> {
                tabComments.setBackgroundResource(R.drawable.bg_button_golden)
                tabComments.setTextColor(resources.getColor(R.color.bg_deep))
                if (myComments.isEmpty()) {
                    tvEmptyComments.visibility = View.VISIBLE
                } else {
                    rvMyComments.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadMyProfile()
        loadAllData()
    }

    private fun loadMyProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUserId = TokenManager.getUserId()
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
        }
    }

    private fun loadAllData() {
        Log.d("MyHollow", "Loading all data...")
        loadMyPosts()
        loadMyLikedPosts()
        loadMyComments()
    }

    private fun loadMyPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("MyHollow", "Loading my posts...")
                val postsResponse = RetrofitClient.postsApi.getMyPosts()
                Log.d("MyHollow", "My posts response code: ${postsResponse.code()}")
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
                            userId = post.user?.id ?: ""
                        )
                    )
                }
                Log.d("MyHollow", "Loaded ${mySecrets.size} posts")
                secretsAdapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("MyHollow", "Load posts error: ${e.message}")
            }
        }
    }

    private fun loadMyLikedPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("MyHollow", "Loading liked posts...")
                val response = RetrofitClient.postsApi.getMyLikedPosts()
                Log.d("MyHollow", "Liked posts response code: ${response.code()}")
                Log.d("MyHollow", "Liked posts response body: ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    myLikedPosts.clear()
                    val posts = response.body()?.posts ?: emptyList()
                    Log.d("MyHollow", "Number of liked posts from API: ${posts.size}")
                    
                    if (posts.isEmpty()) {
                        Toast.makeText(requireContext(), "API返回0个点赞帖子", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "API返回${posts.size}个点赞帖子", Toast.LENGTH_SHORT).show()
                    }
                    
                    posts.forEach { post ->
                        Log.d("MyHollow", "Found liked post: ${post.id} - ${post.content.take(20)}")
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
                                isLiked = true,
                                userId = post.user?.id ?: ""
                            )
                        )
                    }
                    Log.d("MyHollow", "Loaded ${myLikedPosts.size} liked posts")
                    likedAdapter?.notifyDataSetChanged()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("MyHollow", "Failed to load liked posts. Code: ${response.code()}, Error: $errorBody")
                    Toast.makeText(requireContext(), "加载点赞失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MyHollow", "Load liked posts error: ${e.message}", e)
                Toast.makeText(requireContext(), "加载点赞异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMyComments() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("MyHollow", "Loading my comments...")
                val response = RetrofitClient.postsApi.getMyComments()
                Log.d("MyHollow", "Comments response code: ${response.code()}")
                Log.d("MyHollow", "Comments response body: ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    myComments.clear()
                    val comments = response.body()?.comments ?: emptyList()
                    Log.d("MyHollow", "Number of comments from API: ${comments.size}")
                    
                    if (comments.isEmpty()) {
                        Toast.makeText(requireContext(), "API返回0条评论", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "API返回${comments.size}条评论", Toast.LENGTH_SHORT).show()
                    }
                    
                    comments.forEach { comment: MyCommentDto ->
                        Log.d("MyHollow", "Found comment: ${comment.id} - ${comment.content.take(20)}")
                        myComments.add(
                            MyComment(
                                id = comment.id,
                                content = comment.content,
                                timeAgo = TimeUtils.formatTimeAgo(comment.createdAt),
                                postId = comment.postId,
                                postContent = comment.post?.content ?: "",
                                mood = comment.post?.mood ?: ""
                            )
                        )
                    }
                    Log.d("MyHollow", "Loaded ${myComments.size} comments")
                    commentsAdapter?.notifyDataSetChanged()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("MyHollow", "Failed to load comments. Code: ${response.code()}, Error: $errorBody")
                    Toast.makeText(requireContext(), "加载评论失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MyHollow", "Load comments error: ${e.message}", e)
                Toast.makeText(requireContext(), "加载评论异常: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun editComment(commentId: String, content: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.updateComment(commentId, com.zjgsu.treehole.network.AddCommentRequest(content))
                if (response.isSuccessful) {
                    val index = myComments.indexOfFirst { it.id == commentId }
                    if (index >= 0) {
                        myComments[index] = myComments[index].copy(content = content)
                        commentsAdapter?.notifyItemChanged(index)
                    }
                    Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
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
}
