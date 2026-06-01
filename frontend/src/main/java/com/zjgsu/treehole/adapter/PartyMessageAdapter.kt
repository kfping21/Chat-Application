package com.zjgsu.treehole.adapter

import android.view.Gravity
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    inner class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNickname: TextView = view.findViewById(R.id.tv_party_msg_nickname)
        val tvContent: TextView = view.findViewById(R.id.tv_party_msg_content)
        val tvTime: TextView = view.findViewById(R.id.tv_party_msg_time)
    }

    inner class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNickname: TextView = view.findViewById(R.id.tv_party_msg_nickname)
        val tvContent: TextView = view.findViewById(R.id.tv_party_msg_content)
        val tvTime: TextView = view.findViewById(R.id.tv_party_msg_time)
    }

    inner class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(R.id.tv_system_message)
    }

    override fun getItemViewType(position: Int): Int {
        val item = messages[position]
        return if (item.senderId == "system" || item.senderNickname == "系统") {
            VIEW_TYPE_SYSTEM
        } else if (item.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_party_message_sent, parent, false)
                SentViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_party_message_received, parent, false)
                ReceivedViewHolder(view)
            }
            VIEW_TYPE_SYSTEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_party_message_system, parent, false)
                SystemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = messages[position]
        val timeStr = item.createdAt.take(16).replace('T', ' ')

        when (holder) {
            is SentViewHolder -> {
                holder.tvNickname.text = "我"
                holder.tvNickname.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gold_primary))
                holder.tvContent.text = item.content
                holder.tvTime.text = timeStr
            }
            is ReceivedViewHolder -> {
                holder.tvNickname.text = item.senderNickname
                holder.tvNickname.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_secondary))
                holder.tvContent.text = item.content
                holder.tvTime.text = timeStr
            }
            is SystemViewHolder -> {
                holder.tvContent.text = item.content
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun replaceAll(newMessages: List<PartyMessageDto>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
