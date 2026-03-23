package com.zjgsu.treehole.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.Secret

class SecretAdapter(private val secrets: List<Secret>) :
    RecyclerView.Adapter<SecretAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMood: TextView = view.findViewById(R.id.tv_card_mood)
        val tvTime: TextView = view.findViewById(R.id.tv_card_time)
        val tvContent: TextView = view.findViewById(R.id.tv_card_content)
        val tvLikes: TextView = view.findViewById(R.id.tv_card_likes)
        val tvComments: TextView = view.findViewById(R.id.tv_card_comments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_secret_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val secret = secrets[position]
        holder.tvMood.text = secret.mood
        holder.tvTime.text = secret.timeAgo
        holder.tvContent.text = secret.content
        holder.tvLikes.text = secret.likes.toString()
        holder.tvComments.text = secret.comments.toString()

        // Navigate to detail on card click
        holder.itemView.setOnClickListener {
            val args = Bundle().apply { putString("secretId", secret.id) }
            it.findNavController().navigate(R.id.secretDetailFragment, args)
        }
    }

    override fun getItemCount() = secrets.size
}
