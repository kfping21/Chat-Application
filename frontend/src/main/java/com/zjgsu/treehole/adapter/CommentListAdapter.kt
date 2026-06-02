package com.zjgsu.treehole.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.MyComment
import com.zjgsu.treehole.util.TimeUtils

class CommentListAdapter(
    private val comments: MutableList<MyComment>,
    private val onEdit: (String, String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CommentListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tv_comment_time)
        val tvContent: TextView = view.findViewById(R.id.tv_comment_content)
        val tvPostContent: TextView = view.findViewById(R.id.tv_post_content)
        val tvMood: TextView = view.findViewById(R.id.tv_comment_mood)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit_comment)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]

        holder.tvTime.text = TimeUtils.formatTimeAgo(comment.timeAgo)
        holder.tvContent.text = comment.content
        holder.tvPostContent.text = comment.postContent.take(50) + if (comment.postContent.length > 50) "..." else ""
        holder.tvMood.text = comment.mood

        holder.btnEdit.setOnClickListener {
            showEditDialog(holder, comment)
        }

        holder.btnDelete.setOnClickListener {
            showDeleteConfirmDialog(holder, comment.id)
        }
    }

    private fun showEditDialog(holder: ViewHolder, comment: MyComment) {
        val etContent = EditText(holder.itemView.context).apply {
            hint = "编辑你的评论..."
            setText(comment.content)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(holder.itemView.context)
            .setTitle("编辑评论")
            .setView(etContent)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newContent = etContent.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    onEdit(comment.id, newContent)
                }
            }
            .show()
    }

    private fun showDeleteConfirmDialog(holder: ViewHolder, commentId: String) {
        AlertDialog.Builder(holder.itemView.context)
            .setTitle("删除评论")
            .setMessage("确定要删除这条评论吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                onDelete(commentId)
            }
            .show()
    }

    override fun getItemCount() = comments.size
}
