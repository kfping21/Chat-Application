package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tabRecommend: TextView
    private lateinit var tabFollowing: TextView

    private var adapter: SecretAdapter? = null
    private val secrets = mutableListOf<Secret>()
    private var isLoading = false
    private var currentPage = 1
    private var hasMore = true
    private val pageSize = 10
    private var currentFeedType = "all" // "all" or "following"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rv_feed)
        tvEmpty = view.findViewById(R.id.tv_empty)
        progressBar = view.findViewById(R.id.progress_bar)
        tabRecommend = view.findViewById(R.id.tab_recommend)
        tabFollowing = view.findViewById(R.id.tab_following)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = SecretAdapter(secrets, { postId, _ -> likePost(postId) }, null, false)
        rv.adapter = adapter

        // Tab click listeners
        tabRecommend.setOnClickListener {
            if (currentFeedType != "all") {
                currentFeedType = "all"
                updateTabStyle()
                refreshFeed()
            }
        }

        tabFollowing.setOnClickListener {
            if (currentFeedType != "following") {
                currentFeedType = "following"
                updateTabStyle()
                refreshFeed()
            }
        }

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                // Trigger preload for upcoming posts
                adapter?.onVisiblePositionsChanged(firstVisible, lastVisibleItem)

                if (!isLoading && hasMore && lastVisibleItem >= totalItemCount - 3 && dy > 0) {
                    loadMore()
                }
            }
        })

        // Only load if we truly have no data anywhere
        val cachedFeed = PostCacheManager.getFeedList()
        if (cachedFeed != null && cachedFeed.isNotEmpty()) {
            // Restore from cache - no network needed
            if (secrets.isEmpty()) {
                secrets.addAll(cachedFeed)
                adapter?.notifyDataSetChanged()
            }
            tvEmpty.visibility = View.GONE
        } else if (secrets.isEmpty()) {
            // No cache, no data - must load from network
            loadFeed()
        }
        // If secrets is not empty, data is already there - just show it
    }

    private fun updateTabStyle() {
        if (currentFeedType == "all") {
            tabRecommend.setTextColor(resources.getColor(R.color.gold_primary, null))
            tabFollowing.setTextColor(resources.getColor(R.color.text_muted, null))
        } else {
            tabRecommend.setTextColor(resources.getColor(R.color.text_muted, null))
            tabFollowing.setTextColor(resources.getColor(R.color.gold_primary, null))
        }
    }

    override fun onResume() {
        super.onResume()
        // Keep feed fresh when returning (new posts + latest counts)
        refreshCountsIfNeeded()
    }

    private fun refreshCountsIfNeeded() {
        if (isLoading) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.getFeed(page = 1, limit = pageSize, type = currentFeedType)
                }
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        hasMore = body.pagination?.hasMore ?: false
                        currentPage = 1

                        val latest = body.posts.map { post ->
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
                        }

                        secrets.clear()
                        secrets.addAll(latest)
                        adapter?.notifyDataSetChanged()
                        PostCacheManager.cacheFeedList(secrets.toList())
                        tvEmpty.visibility = if (secrets.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                // Silently fail - keep current list
            }
        }
    }

    private fun refreshFeed() {
        secrets.clear()
        currentPage = 1
        hasMore = true
        loadFeed()
    }

    private fun loadFeed() {
        if (isLoading) return
        isLoading = true
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.getFeed(page = 1, limit = pageSize, type = currentFeedType)
                }
                if (response.isSuccessful) {
                    secrets.clear()
                    response.body()?.let { body ->
                        hasMore = body.pagination?.hasMore ?: false
                        currentPage = 1
                        body.posts.forEach { post ->
                            val secret = Secret(
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
                            secrets.add(secret)
                            adapter?.updateCounts(post.id, post.likes, post.commentCount)
                        }
                    }
                    tvEmpty.visibility = if (secrets.isEmpty()) View.VISIBLE else View.GONE
                    if (secrets.isEmpty()) {
                        tvEmpty.text = if (currentFeedType == "following") "关注一些用户，查看他们的秘密" else "暂无帖子"
                    }
                    adapter?.notifyDataSetChanged()
                    // Cache for fast return
                    PostCacheManager.cacheFeedList(secrets.toList())
                    // Refresh the counts from server
                    adapter?.refreshCounts()
                } else if (response.code() in 500..599) {
                    Toast.makeText(requireContext(), "服务器维护中，请稍后刷新", Toast.LENGTH_SHORT).show()
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "服务器维护中\n请稍后再试"
                } else {
                    Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "加载失败\n点击重试"
                    tvEmpty.setOnClickListener { refreshFeed() }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "网络异常", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "网络异常\n点击重试"
                tvEmpty.setOnClickListener { refreshFeed() }
            } finally {
                progressBar.visibility = View.GONE
                isLoading = false
            }
        }
    }

    private fun loadMore() {
        if (isLoading || !hasMore) return
        isLoading = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.getFeed(page = currentPage + 1, limit = pageSize, type = currentFeedType)
                }
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val startSize = secrets.size
                        hasMore = body.pagination?.hasMore ?: false
                        currentPage++
                        body.posts.forEach { post ->
                            val secret = Secret(
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
                            secrets.add(secret)
                            adapter?.updateCounts(post.id, post.likes, post.commentCount)
                        }
                        adapter?.notifyItemRangeInserted(startSize, body.posts.size)
                        // Update cache
                        PostCacheManager.cacheFeedList(secrets.toList())
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "加载更多失败", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    private fun likePost(postId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.likePost(postId)
                }
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        adapter?.updateLikeData(postId, body.likes, body.isLiked)
                    }
                }
            } catch (e: Exception) {
                // 点赞失败静默处理
            }
        }
    }
}