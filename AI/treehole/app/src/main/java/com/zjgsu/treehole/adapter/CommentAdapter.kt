package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.Comment

class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFloor: TextView = view.findViewById(R.id.tv_comment_floor)
        val tvTime: TextView = view.findViewById(R.id.tv_comment_time)
        val tvContent: TextView = view.findViewById(R.id.tv_comment_content)
        val tvLikes: TextView = view.findViewById(R.id.tv_comment_likes)
        val ivAvatar: android.widget.ImageView = view.findViewById(R.id.iv_comment_avatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvFloor.text = "${comment.floor}楼"
        holder.tvTime.text = comment.timeAgo
        holder.tvContent.text = comment.content
        holder.tvLikes.text = comment.likes.toString()

        // Load random landscape avatar based on comment ID/floor
        com.bumptech.glide.Glide.with(holder.itemView.context)
            .load("https://picsum.photos/seed/comment_${comment.id}/200")
            .circleCrop()
            .into(holder.ivAvatar)
    }

    override fun getItemCount() = comments.size
}
