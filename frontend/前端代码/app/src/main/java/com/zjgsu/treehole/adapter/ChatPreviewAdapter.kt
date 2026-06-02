package com.zjgsu.treehole.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.ChatPreview
import com.zjgsu.treehole.util.AvatarLoader

class ChatPreviewAdapter(
    private val chats: List<ChatPreview>,
    private val onClick: ((ChatPreview) -> Unit)? = null
) : RecyclerView.Adapter<ChatPreviewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_chat_name)
        val tvLastMsg: TextView = view.findViewById(R.id.tv_last_message)
        val tvTime: TextView = view.findViewById(R.id.tv_chat_time)
        val tvUnread: TextView = view.findViewById(R.id.tv_unread_count)
        val ivAvatar: ImageView = view.findViewById(R.id.iv_chat_avatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        holder.tvName.text = chat.nickname.ifEmpty { "匿名用户" }
        holder.tvLastMsg.text = chat.lastMessage.ifEmpty { "暂无消息" }
        holder.tvTime.text = chat.timeAgo

        // Show unread badge only if > 0, display "9+" for numbers > 9
        if (chat.unread > 0) {
            holder.tvUnread.visibility = View.VISIBLE
            holder.tvUnread.text = if (chat.unread > 9) "9+" else chat.unread.toString()
        } else {
            holder.tvUnread.visibility = View.GONE
        }

        // Load real avatar
        if (chat.avatar.isNotEmpty()) {
            AvatarLoader.loadAvatar(holder.itemView.context, chat.avatar, holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_nav_my)
        }

        holder.itemView.setOnClickListener {
            val args = Bundle().apply {
                putString("roomId", chat.id)
                putString("participantId", chat.participantId)
                putString("nickname", chat.nickname)
                putString("avatar", chat.avatar)
            }
            it.findNavController().navigate(R.id.whisperChatFragment, args)
        }
    }

    override fun getItemCount() = chats.size
}
