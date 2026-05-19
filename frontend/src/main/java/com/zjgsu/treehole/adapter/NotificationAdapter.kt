package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.Notification

class NotificationAdapter(
    notifications: List<Notification>,
    private val onItemClick: ((Notification) -> Unit)? = null
) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    private val notifications = notifications.toMutableList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.iv_notif_icon)
        val tvMessage: TextView = view.findViewById(R.id.tv_notif_message)
        val tvTime: TextView = view.findViewById(R.id.tv_notif_time)
        val vDot: View = view.findViewById(R.id.v_unread_dot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifications[position]

        // Set icon based on type
        val iconRes = when (notif.type) {
            "like" -> android.R.drawable.btn_star_big_on
            "comment" -> android.R.drawable.ic_dialog_email
            else -> android.R.drawable.ic_dialog_info
        }
        holder.ivIcon.setImageResource(iconRes)

        // Build message with post content preview if available
        val fullMessage = if (notif.postContent?.isNotEmpty() == true) {
            "${notif.message}: ${notif.postContent}..."
        } else {
            notif.message
        }
        holder.tvMessage.text = fullMessage
        holder.tvTime.text = notif.createdAt
        holder.vDot.isVisible = !notif.read
        holder.itemView.setOnClickListener { onItemClick?.invoke(notif) }
    }

    override fun getItemCount() = notifications.size

    fun markAsRead(notificationId: String) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index == -1) return
        val notification = notifications[index]
        if (notification.read) return
        notifications[index] = notification.copy(read = true)
        notifyItemChanged(index)
    }
}
