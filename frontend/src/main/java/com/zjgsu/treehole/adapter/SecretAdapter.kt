package com.zjgsu.treehole.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zjgsu.treehole.R
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.service.PostPreloadService
import com.zjgsu.treehole.ui.ClickAnimations
import com.zjgsu.treehole.ui.FullScreenImageDialog

class SecretAdapter(
    private val secrets: List<Secret>,
    private val onLikeClick: ((String, Boolean) -> Unit)? = null,
    private val onDeleteClick: ((String) -> Unit)? = null,
    private val canDelete: Boolean = false
) : RecyclerView.Adapter<SecretAdapter.ViewHolder>() {

    private val likedState = mutableMapOf<String, Boolean>()
    private val likesCount = mutableMapOf<String, Int>()
    private val commentsCount = mutableMapOf<String, Int>()

    private var onItemClickListener: ((Secret) -> Unit)? = null

    fun setOnItemClickListener(listener: (Secret) -> Unit) {
        onItemClickListener = listener
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        val tvUsername: TextView = view.findViewById(R.id.tv_username)
        val tvMood: TextView = view.findViewById(R.id.tv_card_mood)
        val tvTime: TextView = view.findViewById(R.id.tv_card_time)
        val tvContent: TextView = view.findViewById(R.id.tv_card_content)
        val tvLikes: TextView = view.findViewById(R.id.tv_card_likes)
        val tvComments: TextView = view.findViewById(R.id.tv_card_comments)
        val btnLike: LinearLayout = view.findViewById(R.id.btn_card_like)
        val ivLikeIcon: ImageView = view.findViewById(R.id.iv_like_icon)
        val llCardImages: View = view.findViewById(R.id.ll_card_images)
        val ivCardImage1: ImageView = view.findViewById(R.id.iv_card_image_1)
        val ivCardImage2: ImageView = view.findViewById(R.id.iv_card_image_2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_secret_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val secret = secrets[position]
        val context = holder.itemView.context

        // Cache the post for quick access
        PostCacheManager.cacheFeedPost(secret)

        // Use real user data
        val displayNickname = secret.nickname.ifEmpty { "匿名用户" }
        holder.tvUsername.text = displayNickname

        // Load avatar - with cache busting for updated avatars
        if (secret.avatar.isNotEmpty()) {
            if (secret.avatar.startsWith("preset_")) {
                val presetId = context.resources.getIdentifier(secret.avatar, "drawable", context.packageName)
                if (presetId != 0) holder.ivAvatar.setImageResource(presetId)
            } else {
                Glide.with(context)
                    .load(secret.avatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_nav_my)
                    .error(R.drawable.ic_nav_my)
                    // Don't cache avatars to ensure we always get the latest
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.ivAvatar)
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_nav_my)
        }

        holder.tvMood.text = secret.mood
        holder.tvTime.text = secret.timeAgo
        holder.tvContent.text = secret.content

        // Load post image
        if (secret.imageUrls.isNotEmpty()) {
            holder.llCardImages.visibility = View.VISIBLE
            
            holder.ivCardImage1.visibility = View.VISIBLE
            Glide.with(context)
                .load(secret.imageUrls[0])
                .into(holder.ivCardImage1)
                
            holder.ivCardImage1.setOnClickListener {
                FullScreenImageDialog.show(context, secret.imageUrls[0])
            }
                
            if (secret.imageUrls.size > 1) {
                // If 2 images, force aspect ratio to square (or just use layout weights properly)
                // They both have layout_weight="1", height="wrap_content", and scaleType="centerCrop".
                // Let's set a fixed height programmatically to make them square based on width? Or max height is 200dp.
                // In xml they have maxHeight 200dp and adjustViewBounds true.
                holder.ivCardImage2.visibility = View.VISIBLE
                Glide.with(context)
                    .load(secret.imageUrls[1])
                    .into(holder.ivCardImage2)
                    
                holder.ivCardImage2.setOnClickListener {
                    FullScreenImageDialog.show(context, secret.imageUrls[1])
                }
            } else {
                holder.ivCardImage2.visibility = View.GONE
            }
        } else {
            holder.llCardImages.visibility = View.GONE
        }

        // Get counts - never display negative values
        val currentLiked = likedState[secret.id] ?: secret.isLiked
        val serverLikes = likesCount[secret.id] ?: secret.likes
        val serverComments = commentsCount[secret.id] ?: secret.comments

        // Ensure non-negative display values
        val displayLikes = maxOf(0, serverLikes)
        val displayComments = maxOf(0, serverComments)

        holder.tvLikes.text = displayLikes.toString()
        holder.tvComments.text = displayComments.toString()

        val likeColor = if (currentLiked) {
            androidx.core.content.ContextCompat.getColor(context, R.color.unread_badge)
        } else {
            androidx.core.content.ContextCompat.getColor(context, R.color.text_muted)
        }
        holder.ivLikeIcon.setColorFilter(likeColor)

        val moodColorRes = when (secret.mood) {
            "孤独" -> R.color.mood_lonely
            "开心" -> R.color.mood_happy
            "后悔" -> R.color.mood_regret
            "焦虑" -> R.color.mood_anxious
            "平静" -> R.color.mood_calm
            "迷茫" -> R.color.mood_lost
            "感动" -> R.color.mood_touched
            "释然" -> R.color.mood_release
            else -> R.color.text_muted
        }
        val moodColor = androidx.core.content.ContextCompat.getColor(context, moodColorRes)
        holder.tvMood.setTextColor(moodColor)

        val gd = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            val r = android.graphics.Color.red(moodColor)
            val g = android.graphics.Color.green(moodColor)
            val b = android.graphics.Color.blue(moodColor)
            cornerRadius = 50f * context.resources.displayMetrics.density
            setStroke((0.5f * context.resources.displayMetrics.density).coerceAtLeast(1f).toInt(), android.graphics.Color.argb(60, r, g, b))
            setColor(android.graphics.Color.argb(25, r, g, b))
        }
        holder.tvMood.background = gd

        // Card click - navigate to detail
        holder.itemView.setOnClickListener {
            if (onItemClickListener != null) {
                onItemClickListener?.invoke(secret)
                return@setOnClickListener
            }

            // Cancel any pending preload for this post
            PostPreloadService.clearPreloaded()

            val args = Bundle().apply {
                putString("secretId", secret.id)
                putString("userId", secret.userId)
                putString("nickname", secret.nickname)
                putString("avatar", secret.avatar)
                putString("content", secret.content)
                putString("mood", secret.mood)
                putString("timeAgo", secret.timeAgo)
                putInt("likes", displayLikes)
                putInt("comments", displayComments)
                putBoolean("isLiked", currentLiked)
                putStringArray("imageUrls", secret.imageUrls.toTypedArray())
            }
            it.findNavController().navigate(R.id.secretDetailFragment, args)
        }

        // Long press - delete (only in MyHollow where canDelete is true)
        if (canDelete) {
            holder.itemView.setOnLongClickListener {
                onDeleteClick?.invoke(secret.id)
                true
            }
        }

        // Like button click
        holder.btnLike.setOnClickListener {
            val currentLikes = likesCount[secret.id] ?: secret.likes
            val newLikedState = !currentLiked
            likedState[secret.id] = newLikedState
            likesCount[secret.id] = if (newLikedState) currentLikes + 1 else maxOf(0, currentLikes - 1)

            holder.tvLikes.text = (likesCount[secret.id] ?: 0).toString()

            if (newLikedState) {
                holder.ivLikeIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.unread_badge))
                ClickAnimations.animateLike(holder.ivLikeIcon)
            } else {
                holder.ivLikeIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.text_muted))
            }

            // Call API to like/unlike
            onLikeClick?.invoke(secret.id, newLikedState)
        }
    }

    override fun getItemCount() = secrets.size

    /**
     * Update like and comment counts for all posts
     */
    fun updateCounts(postId: String, likes: Int, comments: Int) {
        if (likes >= 0) {
            likesCount[postId] = likes
        }
        if (comments >= 0) {
            commentsCount[postId] = comments
        }
        val position = secrets.indexOfFirst { it.id == postId }
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }

    fun updateLikeState(postId: String, isLiked: Boolean) {
        likedState[postId] = isLiked
        val position = secrets.indexOfFirst { it.id == postId }
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }

    fun updateLikeData(postId: String, likes: Int, isLiked: Boolean) {
        val oldLikes = likesCount[postId]
        val oldLiked = likedState[postId]
        if (likes >= 0) {
            likesCount[postId] = likes
        }
        likedState[postId] = isLiked
        val likesChanged = likes >= 0 && oldLikes != likes
        val likedChanged = oldLiked != isLiked
        if (!likesChanged && !likedChanged) return
        val position = secrets.indexOfFirst { it.id == postId }
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }

    /**
     * Update all counts from feed refresh
     */
    fun refreshCounts() {
        secrets.forEachIndexed { index, secret ->
            val oldLikes = likesCount[secret.id]
            val oldComments = commentsCount[secret.id]
            if (oldLikes != secret.likes || oldComments != secret.comments) {
                likesCount[secret.id] = secret.likes
                commentsCount[secret.id] = secret.comments
            }
        }
        notifyDataSetChanged()
    }

    /**
     * Call this when the list is scrolled to preload upcoming posts
     */
    fun onVisiblePositionsChanged(firstVisible: Int, lastVisible: Int) {
        // Preload posts that are likely to be clicked next
        // Preload next 3 posts after the last visible one
        val preloadIds = mutableListOf<String>()

        for (i in (lastVisible + 1)..minOf(lastVisible + 3, secrets.size - 1)) {
            if (i >= 0 && i < secrets.size) {
                preloadIds.add(secrets[i].id)
            }
        }

        // Also preload some posts before in case user scrolls up
        for (i in (firstVisible - 1) downTo maxOf(0, firstVisible - 2)) {
            if (i < secrets.size && !preloadIds.contains(secrets[i].id)) {
                preloadIds.add(secrets[i].id)
            }
        }

        PostPreloadService.preloadMultiple(preloadIds)
    }
}
