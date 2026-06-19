package com.zjgsu.treehole.adapter

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.Comment
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.ui.SecretDetailFragment
import com.zjgsu.treehole.ui.UserActionsBottomSheet
import com.zjgsu.treehole.util.AvatarLoader
import com.zjgsu.treehole.util.TimeUtils

class CommentAdapter(
    private val comments: List<Comment>,
    private val detailFragment: SecretDetailFragment? = null,
    private val currentUserId: String = ""
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_comment_avatar)
        val tvUsername: TextView = view.findViewById(R.id.tv_comment_username)
        val tvAuthor: TextView = view.findViewById(R.id.tv_comment_author)
        val tvTime: TextView = view.findViewById(R.id.tv_comment_time)
        val tvContent: TextView = view.findViewById(R.id.tv_comment_content)
        val tvLikes: TextView = view.findViewById(R.id.tv_comment_likes)
        val btnMore: ImageButton = view.findViewById(R.id.btn_comment_more)
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

        holder.tvAuthor.visibility = if (comment.isAuthor) View.VISIBLE else View.GONE

        AvatarLoader.loadAvatar(context, comment.avatar, holder.ivAvatar)

        holder.btnMore.visibility = if (comment.userId == currentUserId) View.VISIBLE else View.GONE

        holder.btnMore.setOnClickListener {
            showCommentOptions(comment)
        }

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

    private fun showCommentOptions(comment: Comment) {
        val options = arrayOf("编辑", "删除")
        AlertDialog.Builder(detailFragment?.requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditCommentDialog(comment)
                    1 -> showDeleteCommentConfirmDialog(comment)
                }
            }
            .show()
    }

    private fun showEditCommentDialog(comment: Comment) {
        val etContent = EditText(detailFragment?.requireContext()).apply {
            hint = "编辑你的回响..."
            setText(comment.content)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(detailFragment?.requireContext())
            .setTitle("编辑回响")
            .setView(etContent)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newContent = etContent.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    detailFragment?.updateComment(comment.id, newContent)
                } else {
                    Toast.makeText(detailFragment?.requireContext(), "内容不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDeleteCommentConfirmDialog(comment: Comment) {
        AlertDialog.Builder(detailFragment?.requireContext())
            .setTitle("删除回响")
            .setMessage("确定要删除这个回响吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                detailFragment?.deleteComment(comment.id)
            }
            .show()
    }

    override fun getItemCount() = comments.size
}