package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R

data class TrendingItem(
    val title: String,
    val subtitle: String,
    val heat: String
)

class TrendingAdapter(private val items: List<TrendingItem>) :
    RecyclerView.Adapter<TrendingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tv_trending_rank)
        val tvTitle: TextView = view.findViewById(R.id.tv_trending_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_trending_subtitle)
        val tvHeat: TextView = view.findViewById(R.id.tv_trending_heat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val rank = position + 1
        holder.tvRank.text = rank.toString()
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvHeat.text = item.heat

        // Highlight top 3
        val context = holder.itemView.context
        when (rank) {
            1 -> {
                holder.tvRank.setTextColor(ContextCompat.getColor(context, R.color.gold_primary))
                holder.tvRank.backgroundTintList = ContextCompat.getColorStateList(context, R.color.gold_primary)
                holder.tvRank.background.alpha = 50
            }
            2 -> {
                holder.tvRank.setTextColor(android.graphics.Color.parseColor("#E2E8F0")) // Silver
                holder.tvRank.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E2E8F0"))
                holder.tvRank.background.alpha = 50
            }
            3 -> {
                holder.tvRank.setTextColor(android.graphics.Color.parseColor("#FDBA74")) // Bronze
                holder.tvRank.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FDBA74"))
                holder.tvRank.background.alpha = 50
            }
            else -> {
                holder.tvRank.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                holder.tvRank.backgroundTintList = ContextCompat.getColorStateList(context, R.color.bg_header)
                holder.tvRank.background.alpha = 100
            }
        }
    }

    override fun getItemCount() = items.size
}
