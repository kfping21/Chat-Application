package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.PartyRoomDto

class PartyRoomAdapter(
    private val rooms: MutableList<PartyRoomDto>,
    private val onClick: (PartyRoomDto) -> Unit
) : RecyclerView.Adapter<PartyRoomAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_party_name)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_party_subtitle)
        val tvMeta: TextView = view.findViewById(R.id.tv_party_meta)
        
        val cvAvatar1: CardView = view.findViewById(R.id.cv_avatar_1)
        val ivAvatar1: ImageView = view.findViewById(R.id.iv_avatar_1)
        val cvAvatar2: CardView = view.findViewById(R.id.cv_avatar_2)
        val ivAvatar2: ImageView = view.findViewById(R.id.iv_avatar_2)
        val cvAvatar3: CardView = view.findViewById(R.id.cv_avatar_3)
        val ivAvatar3: ImageView = view.findViewById(R.id.iv_avatar_3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_party_room, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]
        holder.tvName.text = room.name
        holder.tvSubtitle.text = room.subtitle.ifBlank { "来都来了，说两句吧" }
        holder.tvMeta.text = "${room.onlineCount} 在线"
        holder.itemView.setOnClickListener { onClick(room) }

        val avatars = room.avatars ?: emptyList()
        val context = holder.itemView.context

        if (avatars.isNotEmpty()) {
            holder.cvAvatar1.visibility = View.VISIBLE
            Glide.with(context).load(avatars[0]).apply(RequestOptions.circleCropTransform()).into(holder.ivAvatar1)
        } else {
            holder.cvAvatar1.visibility = View.GONE
        }

        if (avatars.size >= 2) {
            holder.cvAvatar2.visibility = View.VISIBLE
            Glide.with(context).load(avatars[1]).apply(RequestOptions.circleCropTransform()).into(holder.ivAvatar2)
        } else {
            holder.cvAvatar2.visibility = View.GONE
        }

        if (avatars.size >= 3) {
            holder.cvAvatar3.visibility = View.VISIBLE
            Glide.with(context).load(avatars[2]).apply(RequestOptions.circleCropTransform()).into(holder.ivAvatar3)
        } else {
            holder.cvAvatar3.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = rooms.size

    fun replaceAll(newRooms: List<PartyRoomDto>) {
        rooms.clear()
        rooms.addAll(newRooms)
        notifyDataSetChanged()
    }

    fun addToTop(room: PartyRoomDto) {
        rooms.add(0, room)
        notifyItemInserted(0)
    }

    fun addToTop(room: com.zjgsu.treehole.network.WhisperWebSocket.PartyRoomDto) {
        val convertedRoom = PartyRoomDto(
            id = room.id,
            name = room.name,
            subtitle = room.subtitle,
            participantCount = room.participantCount,
            maxParticipants = room.maxParticipants,
            onlineCount = room.onlineCount,
            heat = room.heat,
            messageCount = room.messageCount,
            lastMessage = room.lastMessage,
            lastMessageAt = room.lastMessageAt,
            avatars = room.avatars
        )
        rooms.add(0, convertedRoom)
        notifyItemInserted(0)
    }
}