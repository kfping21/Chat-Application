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
import com.zjgsu.treehole.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar

    private var adapter: SecretAdapter? = null
    private val secrets = mutableListOf<Secret>()
    private var isLoading = false
    private var currentPage = 1
    private var hasMore = true
    private val pageSize = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_feed, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rv_feed)
        tvEmpty = view.findViewById(R.id.tv_empty)
        progressBar = view.findViewById(R.id.progress_bar)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = SecretAdapter(secrets) { postId, _ -> likePost(postId) }
        rv.adapter = adapter

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
                    RetrofitClient.postsApi.getFeed(page = 1, limit = pageSize)
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
                                userId = post.user?.id ?: ""
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
                    RetrofitClient.postsApi.getFeed(page = 1, limit = pageSize)
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
                                userId = post.user?.id ?: ""
                            )
                            secrets.add(secret)
                            adapter?.updateCounts(post.id, post.likes, post.commentCount)
                        }
                    }
                    tvEmpty.visibility = if (secrets.isEmpty()) View.VISIBLE else View.GONE
                    if (secrets.isEmpty()) {
                        tvEmpty.text = "暂无帖子"
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
                    RetrofitClient.postsApi.getFeed(page = currentPage + 1, limit = pageSize)
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
                                userId = post.user?.id ?: ""
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

    private fun fastFormatTime(dateString: String): String {
        try {
            val len = dateString.length
            if (len < 10) return dateString
            val year = dateString.substring(0, 4).toInt()
            val month = dateString.substring(5, 7).toInt()
            val day = dateString.substring(8, 10).toInt()
            val hour = if (len > 11) dateString.substring(11, 13).toInt() else 0
            val minute = if (len > 14) dateString.substring(14, 16).toInt() else 0
            val now = java.util.Calendar.getInstance()
            val then = java.util.Calendar.getInstance().apply { set(year, month - 1, day, hour, minute) }
            val diffMin = (now.timeInMillis - then.timeInMillis) / 60000
            val diffHour = diffMin / 60
            val diffDay = diffHour / 24
            return when {
                diffDay > 0 -> "${diffDay}天前"
                diffHour > 0 -> "${diffHour}小时前"
                diffMin > 0 -> "${diffMin}分钟前"
                else -> "刚刚"
            }
        } catch (e: Exception) {
            return dateString
        }
    }
}
