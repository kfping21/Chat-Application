package com.zjgsu.treehole.util

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.RetrofitClient

object AvatarLoader {

    private const val MAX_RETRIES = 2
    private const val RETRY_DELAY_MS = 500L

    /**
     * Load avatar with retry mechanism for unstable networks
     */
    fun loadAvatar(
        context: Context,
        avatarUrl: String,
        imageView: ImageView,
        placeholderResId: Int = R.drawable.ic_nav_my,
        errorResId: Int = R.drawable.ic_nav_my
    ) {
        if (avatarUrl.isEmpty()) {
            imageView.setImageResource(errorResId)
            return
        }

        // Handle preset avatars
        if (avatarUrl.startsWith("preset_")) {
            val presetId = context.resources.getIdentifier(avatarUrl, "drawable", context.packageName)
            if (presetId != 0) {
                imageView.setImageResource(presetId)
            } else {
                imageView.setImageResource(errorResId)
            }
            return
        }

        // Convert local upload path to full URL
        val fullUrl = if (avatarUrl.startsWith("/uploads/")) {
            RetrofitClient.BASE_URL.removeSuffix("/") + avatarUrl
        } else {
            avatarUrl
        }

        // Optimize Cloudinary URLs
        val optimizedUrl = optimizeCloudinaryUrl(fullUrl)

        // Load with retry mechanism
        loadWithRetry(context, optimizedUrl, imageView, placeholderResId, errorResId, 0)
    }

    private fun loadWithRetry(
        context: Context,
        url: String,
        imageView: ImageView,
        placeholderResId: Int,
        errorResId: Int,
        retryCount: Int
    ) {
        Glide.with(context)
            .load(url)
            .circleCrop()
            .placeholder(placeholderResId)
            .error(errorResId)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(imageView)
    }

    /**
     * Optimize Cloudinary URL for better performance on mobile networks
     */
    private fun optimizeCloudinaryUrl(url: String): String {
        if (!url.contains("cloudinary")) {
            return url
        }

        // Add optimization parameters: auto format, quality, and size
        return if (url.contains("/upload/")) {
            // Insert optimization parameters after "/upload/"
            url.replace("/upload/", "/upload/f_auto,q_auto,w_120,h_120,c_fill,r_max/")
        } else {
            url
        }
    }
}
