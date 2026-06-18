package com.zjgsu.treehole.cache

import android.content.Context
import android.util.LruCache
import com.zjgsu.treehole.model.Secret
import kotlinx.coroutines.*

/**
 * 帖子数据缓存管理器
 * 使用 LRU 内存缓存，加快帖子详情加载速度
 */
object PostCacheManager {

    // LRU Cache for post details - cache comments and full post data (increased to 30 for more cached detail pages)
    private val postDetailCache = object : LruCache<String, PostDetailCache>(30) {
        override fun sizeOf(key: String, value: PostDetailCache): Int {
            return 1 // Each entry counts as 1
        }
    }

    // Cache for feed posts - quick access to post data
    private val feedCache = object : LruCache<String, Secret>(50) {
        override fun sizeOf(key: String, value: Secret): Int {
            return 1
        }
    }

    // Cache for entire feed list to preserve navigation state
    private val feedListCache = object : LruCache<String, List<Secret>>(3) {
        override fun sizeOf(key: String, value: List<Secret>): Int {
            return value.size
        }
    }

    // Preload job for background preloading
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preloadJobs = mutableMapOf<String, Job>()

    data class PostDetailCache(
        val comments: List<CachedComment>,
        val likes: Int,
        val isLiked: Boolean,
        val commentCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class CachedComment(
        val id: String,
        val content: String,
        val timeAgo: String,
        val avatar: String,
        val nickname: String,
        val userId: String = ""
    )

    fun cacheFeedPost(post: Secret) {
        feedCache.put(post.id, post)
    }

    fun getFeedPost(postId: String): Secret? {
        return feedCache.get(postId)
    }

    // Feed list caching - preserves data when navigating away
    fun cacheFeedList(posts: List<Secret>) {
        feedListCache.put("main_feed", posts)
    }

    fun getFeedList(): List<Secret>? {
        return feedListCache.get("main_feed")
    }

    fun cachePostDetail(postId: String, comments: List<CachedComment>, likes: Int, isLiked: Boolean, commentCount: Int) {
        postDetailCache.put(postId, PostDetailCache(comments, likes, isLiked, commentCount))
    }

    fun getPostDetail(postId: String): PostDetailCache? {
        return postDetailCache.get(postId)
    }

    fun isPostDetailCacheValid(postId: String): Boolean {
        val cache = postDetailCache.get(postId) ?: return false
        // Cache valid for 5 minutes
        return System.currentTimeMillis() - cache.timestamp < 5 * 60 * 1000
    }

    fun preloadPostDetail(postId: String, loader: suspend () -> PostDetailCache?) {
        // Cancel existing preload job for this post
        preloadJobs[postId]?.cancel()

        // Start new preload job
        preloadJobs[postId] = preloadScope.launch {
            try {
                // Small delay to avoid overwhelming the network
                delay(500)
                if (!isPostDetailCacheValid(postId)) {
                    val result = loader()
                    result?.let { cachePostDetail(postId, it.comments, it.likes, it.isLiked, it.commentCount) }
                }
            } catch (e: CancellationException) {
                // Ignored - job was cancelled
            } catch (e: Exception) {
                // Preload failed silently
            }
        }
    }

    fun cancelPreload(postId: String) {
        preloadJobs[postId]?.cancel()
        preloadJobs.remove(postId)
    }

    fun clearCache() {
        postDetailCache.evictAll()
        feedCache.evictAll()
        feedListCache.evictAll()
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
    }

    fun onDestroy() {
        clearCache()
        preloadScope.cancel()
    }
}
