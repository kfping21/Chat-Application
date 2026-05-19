package com.zjgsu.treehole.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.Comment
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.ui.UserActionsBottomSheet
import com.zjgsu.treehole.util.AvatarLoader
import com.zjgsu.treehole.util.TimeUtils

class CommentAdapter(
    private val comments: List<Comment>,
    private val onAvatarClick: ((userId: String, nickname: String, avatar: String) -> Unit)? = null
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_comment_avatar)
        val tvUsername: TextView = view.findViewById(R.id.tv_comment_username)
        val tvAuthor: TextView = view.findViewById(R.id.tv_comment_author)
        val tvTime: TextView = view.findViewById(R.id.tv_comment_time)
        val tvContent: TextView = view.findViewById(R.id.tv_comment_content)
        val tvLikes: TextView = view.findViewById(R.id.tv_comment_likes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        val context = holder.itemView.context

        holder.tvUsername.text = comment.nickname.ifEmpty { "匿名用户" }
        holder.tvTime.text = TimeUtils.formatTimeAgo(comment.timeAgo)
        holder.tvContent.text = comment.content
        holder.tvLikes.text = comment.likes.toString()

        // Show author tag if this is the post author
        holder.tvAuthor.visibility = if (comment.isAuthor) View.VISIBLE else View.GONE

        // Load avatar with retry mechanism for mobile networks
        AvatarLoader.loadAvatar(context, comment.avatar, holder.ivAvatar)

        // Click avatar to navigate to user profile (only for other users)
        holder.ivAvatar.setOnClickListener {
            val currentUserId = TokenManager.getUserId() ?: ""
            if (!comment.userId.isNullOrEmpty() && comment.userId != currentUserId) {
                val bundle = Bundle().apply {
                    putString("userId", comment.userId)
                    putString("nickname", comment.nickname)
                    putString("avatar", comment.avatar)
                }
                holder.itemView.findNavController().navigate(R.id.userProfileFragment, bundle)
            }
        }

        // Click username to navigate to user profile (only for other users)
        holder.tvUsername.setOnClickListener {
            val currentUserId = TokenManager.getUserId() ?: ""
            if (!comment.userId.isNullOrEmpty() && comment.userId != currentUserId) {
                val bundle = Bundle().apply {
                    putString("userId", comment.userId)
                    putString("nickname", comment.nickname)
                    putString("avatar", comment.avatar)
                }
                holder.itemView.findNavController().navigate(R.id.userProfileFragment, bundle)
            }
        }
    }

    override fun getItemCount() = comments.size
}