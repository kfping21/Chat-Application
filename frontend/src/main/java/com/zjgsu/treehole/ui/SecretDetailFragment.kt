package com.zjgsu.treehole.ui

import android.app.AlertDialog
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

    private var passedNickname = ""
    private var passedAvatar = ""
    private var passedContent = ""
    private var passedMood = ""
    private var passedTimeAgo = ""
    private var passedLikes = 0
    private var passedComments = 0
    private var passedIsLiked = false
    private var passedUserId = ""
    private var currentUserId: String = ""

    private lateinit var btnLike: LinearLayout
    private lateinit var ivLikeIcon: ImageView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_secret_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = TokenManager.getUserId() ?: ""
        postId = arguments?.getString("secretId") ?: ""

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
        val btnMore = view.findViewById<ImageButton>(R.id.btn_more_detail)
        val etComment = view.findViewById<EditText>(R.id.et_comment)
        val btnSend = view.findViewById<ImageButton>(R.id.btn_send_comment)

        try {
            ClickAnimations.addButtonPressAnimation(btnLike)
            ClickAnimations.addButtonPressAnimation(btnBack)
            ClickAnimations.addButtonPressAnimation(btnShare)
            ClickAnimations.addButtonPressAnimation(btnMore)
        } catch (e: Exception) { /* Ignore animation errors */ }

        ivDetailAvatar.setOnClickListener {
            navigateToUserProfile()
        }

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

        btnMore.setOnClickListener {
            showPostOptions()
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

        showPassedData()
        loadFromCache()
        loadPost()
    }

    private fun showPostOptions() {
        if (passedUserId != currentUserId) {
            return
        }

        val options = arrayOf("编辑", "删除")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPostDialog()
                    1 -> showDeletePostConfirmDialog()
                }
            }
            .show()
    }

    private fun showEditPostDialog() {
        val etContent = EditText(requireContext()).apply {
            hint = "编辑你的秘密..."
            setText(passedContent)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("编辑秘密")
            .setView(etContent)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newContent = etContent.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    updatePost(newContent)
                } else {
                    Toast.makeText(requireContext(), "内容不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun updatePost(content: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.updatePost(
                        postId,
                        com.zjgsu.treehole.network.UpdatePostRequest(content, passedMood)
                    )
                }
                if (response.isSuccessful) {
                    response.body()?.let {
                        passedContent = it.post.content
                        requireView().findViewById<TextView>(R.id.tv_detail_content).text = it.post.content
                        Toast.makeText(requireContext(), "已更新", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeletePostConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("删除秘密")
            .setMessage("确定要删除这个秘密吗？此操作无法撤销。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                deletePost()
            }
            .show()
    }

    private fun deletePost() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.deletePost(postId)
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateComment(commentId: String, content: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.updateComment(
                        commentId,
                        com.zjgsu.treehole.network.AddCommentRequest(content)
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "评论已更新", Toast.LENGTH_SHORT).show()
                    loadPost()
                } else {
                    Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.deleteComment(commentId)
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "评论已删除", Toast.LENGTH_SHORT).show()
                    loadPost()
                } else {
                    Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

            val btnMore = findViewById<ImageButton>(R.id.btn_more_detail)
            btnMore.visibility = if (passedUserId == currentUserId) View.VISIBLE else View.GONE
        }
    }

    private fun loadFromCache() {
        val cached = PostCacheManager.getPostDetail(postId)
        if (cached != null && PostCacheManager.isPostDetailCacheValid(postId)) {
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
                val adapter = CommentAdapter(comments, this@SecretDetailFragment, currentUserId)
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

                        if (!isTogglingLike) {
                            isLiked = post.isLiked
                            likeCount = post.likes
                        }

                        passedUserId = post.user?.id ?: ""
                        passedNickname = post.user?.nickname ?: ""
                        passedAvatar = post.user?.avatar ?: ""
                        passedContent = post.content
                        passedMood = post.mood

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

                            val btnMore = findViewById<ImageButton>(R.id.btn_more_detail)
                            btnMore.visibility = if (passedUserId == currentUserId) View.VISIBLE else View.GONE

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
                            }, this@SecretDetailFragment, currentUserId)
                            findViewById<RecyclerView>(R.id.rv_comments).adapter = adapter
                            findViewById<RecyclerView>(R.id.rv_comments).layoutManager = LinearLayoutManager(requireContext())
                        }

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
                // Keep showing passed data on error
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun toggleLike() {
        isTogglingLike = true
        val newIsLiked = !isLiked
        val newLikeCount = if (newIsLiked) likeCount + 1 else likeCount - 1

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
                    isLiked = !newIsLiked
                    likeCount = if (newIsLiked) newLikeCount - 1 else newLikeCount + 1
                    requireView().findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()
                    ivLikeIcon.setColorFilter(
                        if (isLiked) ContextCompat.getColor(requireContext(), R.color.unread_badge)
                        else ContextCompat.getColor(requireContext(), R.color.text_muted)
                    )
                }
            } catch (e: Exception) {
                // Network error
            } finally {
                isTogglingLike = false
            }
        }
    }

    private fun navigateToUserProfile() {
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
        onSuccess()
        Toast.makeText(requireContext(), "回响已发送 ✨", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.addComment(postId, com.zjgsu.treehole.network.AddCommentRequest(content))
                }
                if (response.isSuccessful) {
                    loadPost()
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun parseTimeAgo(dateString: String): String {
        return TimeUtils.formatTimeAgo(dateString)
    }
}
