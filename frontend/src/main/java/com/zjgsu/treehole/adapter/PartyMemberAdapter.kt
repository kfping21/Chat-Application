package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.ExploreUserDto

class PartyMemberAdapter(
    private val creatorId: String
) : RecyclerView.Adapter<PartyMemberAdapter.ViewHolder>() {

    private val members = mutableListOf<ExploreUserDto>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_member_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_member_name)
        val tvRole: TextView = view.findViewById(R.id.tv_member_role)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_party_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = members[position]
        holder.tvName.text = member.nickname
        
        if (member.id == creatorId) {
            holder.tvRole.visibility = View.VISIBLE
        } else {
            holder.tvRole.visibility = View.GONE
        }

        if (member.avatar.isNotBlank()) {
            val avatarUrl = if (member.avatar.startsWith("http")) member.avatar else "${com.zjgsu.treehole.network.RetrofitClient.BASE_URL.trimEnd('/')}${member.avatar}"
            Glide.with(holder.itemView.context)
                .load(avatarUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.ivAvatar)
        }
    }

    override fun getItemCount(): Int = members.size

    fun replaceAll(newMembers: List<ExploreUserDto>) {
        members.clear()
        members.addAll(newMembers)
        notifyDataSetChanged()
    }
}
