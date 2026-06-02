package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.CommentAdapter
import com.zjgsu.treehole.util.AvatarLoader
import com.zjgsu.treehole.util.TimeUtils
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.model.Comment
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SecretDetailFragment : Fragment() {

    private var postId: String = ""
    private var isLiked = false
    private var likeCount = 0
    private var isTogglingLike = false

    // Data passed from feed
    private var passedNickname = ""
    private var passedAvatar = ""
    private var passedContent = ""
    private var passedMood = ""
    private var passedTimeAgo = ""
    private var passedLikes = 0
    private var passedComments = 0
    private var passedIsLiked = false
    private var passedUserId = "" // Author's user ID for starting chat

    private lateinit var btnLike: LinearLayout
    private lateinit var ivLikeIcon: ImageView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_secret_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString("secretId") ?: ""

        // Get data passed from feed
        passedUserId = arguments?.getString("userId") ?: ""
        passedNickname = arguments?.getString("nickname") ?: ""
        passedAvatar = arguments?.getString("avatar") ?: ""
        passedContent = arguments?.getString("content") ?: ""
        passedMood = arguments?.getString("mood") ?: "平静"
        passedTimeAgo = arguments?.getString("timeAgo") ?: ""
        passedLikes = arguments?.getInt("likes") ?: 0
        passedComments = arguments?.getInt("comments") ?: 0
        passedIsLiked = arguments?.getBoolean("isLiked") ?: false

        isLiked = passedIsLiked
        likeCount = passedLikes

        btnLike = view.findViewById(R.id.btn_like_detail)
        ivLikeIcon = view.findViewById(R.id.iv_like_icon)
        progressBar = view.findViewById(R.id.progress_bar)
        val ivDetailAvatar = view.findViewById<ImageView>(R.id.iv_detail_avatar)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
        val btnShare = view.findViewById<ImageButton>(R.id.btn_share_detail)
        val etComment = view.findViewById<EditText>(R.id.et_comment)
        val btnSend = view.findViewById<ImageButton>(R.id.btn_send_comment)

        try {
            ClickAnimations.addButtonPressAnimation(btnLike)
            ClickAnimations.addButtonPressAnimation(btnBack)
            ClickAnimations.addButtonPressAnimation(btnShare)
        } catch (e: Exception) { /* Ignore animation errors */ }

        // Avatar click - navigate to user profile
        ivDetailAvatar.setOnClickListener {
            navigateToUserProfile()
        }

        // Username click - navigate to user profile
        val tvDetailUsername = view.findViewById<TextView>(R.id.tv_detail_username)
        tvDetailUsername.setOnClickListener {
            navigateToUserProfile()
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnShare.setOnClickListener {
            Toast.makeText(requireContext(), "分享功能即将上线 ✨", Toast.LENGTH_SHORT).show()
        }

        btnSend.setOnClickListener {
            val content = etComment.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "请输入你的回响", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendComment(content) {
                etComment.text.clear()
            }
        }

        btnLike.setOnClickListener {
            toggleLike()
        }

        // Step 1: Show passed data immediately (instant display)
        showPassedData()

        // Step 2: Try to load from cache (fast display)
        loadFromCache()

        // Step 3: Then load fresh data from server (update data)
        loadPost()
    }

    private fun showPassedData() {
        requireView().apply {
            findViewById<TextView>(R.id.tv_detail_mood).text = passedMood
            findViewById<TextView>(R.id.tv_detail_time).text = passedTimeAgo
            findViewById<TextView>(R.id.tv_detail_content).text = passedContent
            findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()
            findViewById<TextView>(R.id.tv_echoes_count).text = "回响 $passedComments"
            findViewById<TextView>(R.id.tv_detail_username).text = passedNickname.ifEmpty { "匿名用户" }
            findViewById<TextView>(R.id.tv_comment_count).text = passedComments.toString()

            val ivAvatar = findViewById<ImageView>(R.id.iv_detail_avatar)
            AvatarLoader.loadAvatar(requireContext(), passedAvatar, ivAvatar)

            ivLikeIcon.setColorFilter(
                if (isLiked) ContextCompat.getColor(requireContext(), R.color.unread_badge)
                else ContextCompat.getColor(requireContext(), R.color.text_muted)
            )
        }
    }

    private fun loadFromCache() {
        val cached = PostCacheManager.getPostDetail(postId)
        if (cached != null && PostCacheManager.isPostDetailCacheValid(postId)) {
            // We have valid cache - display it immediately
            requireView().apply {
                findViewById<TextView>(R.id.tv_like_count).text = cached.likes.toString()
                findViewById<TextView>(R.id.tv_echoes_count).text = "回响 ${cached.commentCount}"
                findViewById<TextView>(R.id.tv_comment_count).text = cached.commentCount.toString()

                isLiked = cached.isLiked
                likeCount = cached.likes

                ivLikeIcon.setColorFilter(
                    if (isLiked) ContextCompat.getColor(requireContext(), R.color.unread_badge)
                    else ContextCompat.getColor(requireContext(), R.color.text_muted)
                )

                // Show cached comments
                val comments = cached.comments.map { c ->
                    Comment(
                        id = c.id,
                        content = c.content,
                        timeAgo = c.timeAgo,
                        likes = 0,
                        floor = 0,
                        avatar = c.avatar,
                        nickname = c.nickname,
                        userId = c.userId
                    )
                }
                val adapter = CommentAdapter(comments)
                findViewById<RecyclerView>(R.id.rv_comments).adapter = adapter
                findViewById<RecyclerView>(R.id.rv_comments).layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }

    private fun loadPost() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.getPost(postId)
                }
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        val post = data.post
                        val comments = data.comments

                        // Don't update like state if user just toggled it
                        if (!isTogglingLike) {
                            isLiked = post.isLiked
                            likeCount = post.likes
                        }

                        requireView().apply {
                            findViewById<TextView>(R.id.tv_detail_mood).text = post.mood
                            findViewById<TextView>(R.id.tv_detail_time).text = parseTimeAgo(post.createdAt)
                            findViewById<TextView>(R.id.tv_detail_content).text = post.content
                            findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()
                            findViewById<TextView>(R.id.tv_echoes_count).text = "回响 ${comments.size}"
                            findViewById<TextView>(R.id.tv_detail_username).text = post.user?.nickname?.ifEmpty { "匿名用户" } ?: "匿名用户"
                            findViewById<TextView>(R.id.tv_comment_count).text = comments.size.toString()

                            val avatar = post.user?.avatar ?: ""
                            val ivAvatar = findViewById<ImageView>(R.id.iv_detail_avatar)
                            AvatarLoader.loadAvatar(requireContext(), avatar, ivAvatar)

                            ivLikeIcon.setColorFilter(
                                if (isLiked) ContextCompat.getColor(requireContext(), R.color.unread_badge)
                                else ContextCompat.getColor(requireContext(), R.color.text_muted)
                            )

                            val adapter = CommentAdapter(comments.map { c ->
                                Comment(
                                    id = c.id,
                                    content = c.content,
                                    timeAgo = parseTimeAgo(c.createdAt),
                                    likes = 0,
                                    floor = 0,
                                    avatar = c.user?.avatar ?: "",
                                    nickname = c.user?.nickname ?: "",
                                    userId = c.user?.id ?: "",
                                    isAuthor = c.user?.id == post.user?.id
                                )
                            })
                            findViewById<RecyclerView>(R.id.rv_comments).adapter = adapter
                            findViewById<RecyclerView>(R.id.rv_comments).layoutManager = LinearLayoutManager(requireContext())
                        }

                        // Cache the data for next time
                        PostCacheManager.cachePostDetail(
                            postId,
                            comments.map { c ->
                                PostCacheManager.CachedComment(
                                    id = c.id,
                                    content = c.content,
                                    timeAgo = parseTimeAgo(c.createdAt),
                                    avatar = c.user?.avatar ?: "",
                                    nickname = c.user?.nickname ?: "",
                                    userId = c.user?.id ?: ""
                                )
                            },
                            post.likes,
                            post.isLiked,
                            comments.size
                        )
                    }
                }
            } catch (e: Exception) {
                // Keep showing passed data on error - user already has something to see
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun toggleLike() {
        isTogglingLike = true
        val newIsLiked = !isLiked
        val newLikeCount = if (newIsLiked) likeCount + 1 else likeCount - 1

        // Optimistic update
        isLiked = newIsLiked
        likeCount = newLikeCount
        requireView().findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()
        ivLikeIcon.setColorFilter(
            if (isLiked) ContextCompat.getColor(requireContext(), R.color.unread_badge)
            else ContextCompat.getColor(requireContext(), R.color.text_muted)
        )
        if (isLiked) {
            try {
                ClickAnimations.animateLike(ivLikeIcon)
            } catch (e: Exception) { /* Ignore animation errors */ }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.likePost(postId)
                }
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        // Sync with server response
                        isLiked = body.isLiked
                        likeCount = body.likes
                        requireView().findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()
                        ivLikeIcon.setColorFilter(
                            if (isLiked) ContextCompat.getColor(requireContext(), R.color.unread_badge)
                            else ContextCompat.getColor(requireContext(), R.color.text_muted)
                        )
                        if (isLiked) {
                            Toast.makeText(requireContext(), "给予温暖 ✨", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Revert on failure
                    isLiked = !newIsLiked
                    likeCount = if (newIsLiked) newLikeCount - 1 else newLikeCount + 1
                    requireView().findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()
                    ivLikeIcon.setColorFilter(
                        if (isLiked) ContextCompat.getColor(requireContext(), R.color.unread_badge)
                        else ContextCompat.getColor(requireContext(), R.color.text_muted)
                    )
                }
            } catch (e: Exception) {
                // Network error - silently revert is already done by not updating
            } finally {
                isTogglingLike = false
            }
        }
    }

    private fun navigateToUserProfile() {
        val currentUserId = TokenManager.getUserId() ?: ""
        if (passedUserId.isNotEmpty() && passedUserId != currentUserId) {
            val bundle = Bundle().apply {
                putString("userId", passedUserId)
                putString("nickname", passedNickname)
                putString("avatar", passedAvatar)
            }
            findNavController().navigate(R.id.userProfileFragment, bundle)
        }
    }

    private fun sendComment(content: String, onSuccess: () -> Unit) {
        // Immediately close input and show success
        onSuccess()
        Toast.makeText(requireContext(), "回响已发送 ✨", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.addComment(postId, com.zjgsu.treehole.network.AddCommentRequest(content))
                }
                if (response.isSuccessful) {
                    // Refresh the comments
                    loadPost()
                }
            } catch (e: Exception) {
                // Silently fail - comment is already sent
            }
        }
    }

    private fun parseTimeAgo(dateString: String): String {
        return TimeUtils.formatTimeAgo(dateString)
    }
}
