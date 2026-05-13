package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.PartyMessageDto

class PartyMessageAdapter(
    private val currentUserId: String,
    private val messages: MutableList<PartyMessageDto>
) : RecyclerView.Adapter<PartyMessageAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNickname: TextView = view.findViewById(R.id.tv_party_msg_nickname)
        val tvContent: TextView = view.findViewById(R.id.tv_party_msg_content)
        val tvTime: TextView = view.findViewById(R.id.tv_party_msg_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_party_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = messages[position]
        val isMe = item.senderId == currentUserId
        holder.tvNickname.text = if (isMe) "我" else item.senderNickname
        holder.tvContent.text = item.content
        holder.tvTime.text = item.createdAt.take(16).replace('T', ' ')
        holder.tvNickname.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isMe) R.color.gold_primary else R.color.text_secondary
            )
        )
    }

    override fun getItemCount(): Int = messages.size

    fun replaceAll(newMessages: List<PartyMessageDto>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
