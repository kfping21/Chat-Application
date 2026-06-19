package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.ui.FollowListFragment
import com.zjgsu.treehole.util.AvatarLoader

class FollowUserAdapter(
    private val users: List<FollowListFragment.FollowUser>,
    private val showUnfollow: Boolean,
    private val onUnfollow: (String) -> Unit,
    private val onItemClick: (FollowListFragment.FollowUser) -> Unit
) : RecyclerView.Adapter<FollowUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        val viewOnline: View = view.findViewById(R.id.view_online)
        val tvNickname: TextView = view.findViewById(R.id.tv_nickname)
        val tvBio: TextView = view.findViewById(R.id.tv_bio)
        val btnUnfollow: TextView = view.findViewById(R.id.btn_unfollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        holder.tvNickname.text = user.nickname
        holder.tvBio.text = user.bio.ifEmpty { "这个人很懒，什么都没写" }
        holder.viewOnline.visibility = if (user.isOnline) View.VISIBLE else View.GONE

        // Load avatar
        if (user.avatar.isNotEmpty()) {
            AvatarLoader.loadAvatar(holder.itemView.context, user.avatar, holder.ivAvatar)
        }

        // Unfollow button
        if (showUnfollow) {
            holder.btnUnfollow.visibility = View.VISIBLE
            holder.btnUnfollow.setOnClickListener {
                onUnfollow(user.id)
            }
        } else {
            holder.btnUnfollow.visibility = View.GONE
        }

        // Item click to view profile
        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }

    override fun getItemCount() = users.size
}