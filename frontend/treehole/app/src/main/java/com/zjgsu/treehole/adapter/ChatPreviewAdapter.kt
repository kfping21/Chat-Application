package com.zjgsu.treehole.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.ChatPreview

class ChatPreviewAdapter(private val chats: List<ChatPreview>) :
    RecyclerView.Adapter<ChatPreviewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_chat_name)
        val tvLastMsg: TextView = view.findViewById(R.id.tv_last_message)
        val tvTime: TextView = view.findViewById(R.id.tv_chat_time)
        val tvUnread: TextView = view.findViewById(R.id.tv_unread_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        holder.tvName.text = chat.pseudonym
        holder.tvLastMsg.text = chat.lastMessage
        holder.tvTime.text = chat.timeAgo
        holder.tvUnread.isVisible = chat.unread > 0
        holder.tvUnread.text = chat.unread.toString()

        holder.itemView.setOnClickListener {
            val args = Bundle().apply { putString("chatId", chat.id) }
            it.findNavController().navigate(R.id.whisperChatFragment, args)
        }
    }

    override fun getItemCount() = chats.size
}
