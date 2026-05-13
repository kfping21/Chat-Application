package com.zjgsu.treehole.service

import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.cache.PostCacheManager.CachedComment
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.network.RetrofitClient
import kotlinx.coroutines.*

/**
 * 帖子预加载服务
 * 在后台预加载帖子详情，加快用户查看时的响应速度
 */
object PostPreloadService {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preloadedIds = mutableSetOf<String>()
    private val pendingPreloads = mutableSetOf<String>()

    // 正在预加载的job
    private val preloadJobs = mutableMapOf<String, Job>()

    /**
     * 预加载帖子详情
     * @param postId 帖子ID
     * @param priority 优先级，0最高
     */
    fun preload(postId: String, priority: Int = 0) {
        if (preloadedIds.contains(postId) && PostCacheManager.isPostDetailCacheValid(postId)) {
            return // Already cached and valid
        }

        if (pendingPreloads.contains(postId)) {
            return // Already being preloaded
        }

        pendingPreloads.add(postId)

        // 根据优先级决定延迟时间
        val delayMs = (priority * 100L).coerceAtMost(2000L)

        scope.launch {
            try {
                delay(delayMs)

                if (!PostCacheManager.isPostDetailCacheValid(postId)) {
                    val cache = fetchAndCache(postId)
                    if (cache != null) {
                        preloadedIds.add(postId)
                    }
                } else {
                    preloadedIds.add(postId)
                }
            } catch (e: CancellationException) {
                // Ignored
            } catch (e: Exception) {
                // Silently fail
            } finally {
                pendingPreloads.remove(postId)
            }
        }
    }

    /**
     * 批量预加载多个帖子
     */
    fun preloadMultiple(postIds: List<String>) {
        postIds.forEachIndexed { index, id ->
            preload(id, priority = index)
        }
    }

    /**
     * 预加载当前Feed中的热门帖子（用于探索页等）
     */
    fun preloadFeedPosts(secrets: List<Secret>) {
        // 只预加载前5个
        secrets.take(5).forEach { secret ->
            preload(secret.id, priority = secrets.indexOf(secret))
        }
    }

    private suspend fun fetchAndCache(postId: String): PostCacheManager.PostDetailCache? {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.postsApi.getPost(postId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val cachedComments = body.comments.map { comment ->
                            CachedComment(
                                id = comment.id,
                                content = comment.content,
                                timeAgo = formatTimeAgo(comment.createdAt),
                                avatar = comment.user?.avatar ?: "",
                                nickname = comment.user?.nickname ?: ""
                            )
                        }

                        val cache = PostCacheManager.PostDetailCache(
                            comments = cachedComments,
                            likes = body.post.likes,
                            isLiked = body.post.isLiked,
                            commentCount = body.comments.size
                        )

                        PostCacheManager.cachePostDetail(postId, cachedComments, body.post.likes, body.post.isLiked, body.comments.size)
                        cache
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun formatTimeAgo(dateString: String): String {
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss"
            )
            var date: java.util.Date? = null
            for (formatStr in formats) {
                try {
                    val format = java.text.SimpleDateFormat(formatStr, java.util.Locale.getDefault())
                    date = format.parse(dateString)
                    if (date != null) break
                } catch (e: Exception) {
                    // Try next format
                }
            }
            if (date == null) return dateString

            val now = java.util.Date()
            val diff = now.time - date.time
            val minutes = diff / 60000
            val hours = minutes / 60
            val days = hours / 24

            when {
                days > 0 -> "${days}天前"
                hours > 0 -> "${hours}小时前"
                minutes > 0 -> "${minutes}分钟前"
                else -> "刚刚"
            }
        } catch (e: Exception) {
            dateString
        }
    }

    fun clearPreloaded() {
        preloadedIds.clear()
        pendingPreloads.clear()
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
    }

    fun onDestroy() {
        clearPreloaded()
        scope.cancel()
    }
}
