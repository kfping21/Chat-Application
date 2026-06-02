package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
        holder.tvMeta.text = "${room.participantCount}/${room.maxParticipants}人 · 热度 ${room.heat}"
        holder.itemView.setOnClickListener { onClick(room) }
    }

    override fun getItemCount(): Int = rooms.size

    fun replaceAll(newRooms: List<PartyRoomDto>) {
        rooms.clear()
        rooms.addAll(newRooms)
        notifyDataSetChanged()
    }
}
