package com.zjgsu.treehole.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.ui.ClickAnimations

class SecretAdapter(private val secrets: List<Secret>) :
    RecyclerView.Adapter<SecretAdapter.ViewHolder>() {

    private val likedSet = mutableSetOf<Int>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMood: TextView = view.findViewById(R.id.tv_card_mood)
        val tvTime: TextView = view.findViewById(R.id.tv_card_time)
        val tvContent: TextView = view.findViewById(R.id.tv_card_content)
        val tvLikes: TextView = view.findViewById(R.id.tv_card_likes)
        val tvComments: TextView = view.findViewById(R.id.tv_card_comments)
        val btnLike: LinearLayout = view.findViewById(R.id.btn_card_like)
        val ivLikeIcon: ImageView = view.findViewById(R.id.iv_like_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_secret_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val secret = secrets[position]
        val context = holder.itemView.context
        val isLiked = likedSet.contains(position)

        holder.tvMood.text = secret.mood
        holder.tvTime.text = secret.timeAgo
        holder.tvContent.text = secret.content
        holder.tvLikes.text = if (isLiked) (secret.likes + 1).toString() else secret.likes.toString()
        holder.tvComments.text = secret.comments.toString()

        // 更新点赞图标颜色
        val likeColor = if (isLiked) {
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

        // 卡片点击 - 导航到详情
        holder.itemView.setOnClickListener {
            val args = Bundle().apply { putString("secretId", secret.id) }
            it.findNavController().navigate(R.id.secretDetailFragment, args)
        }

        // 点赞按钮点击 - 带动画效果
        holder.btnLike.setOnClickListener {
            if (likedSet.contains(position)) {
                // 取消点赞
                likedSet.remove(position)
                holder.tvLikes.text = secret.likes.toString()
                holder.ivLikeIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.text_muted))
            } else {
                // 点赞动画
                likedSet.add(position)
                holder.tvLikes.text = (secret.likes + 1).toString()
                holder.ivLikeIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.unread_badge))
                ClickAnimations.animateLike(holder.ivLikeIcon)
            }
        }
    }

    override fun getItemCount() = secrets.size
}
