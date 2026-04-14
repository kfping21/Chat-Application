package com.zjgsu.treehole.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.model.MoodItem

class MoodAdapter(
    private val moods: List<MoodItem>,
    private val onMoodSelected: (MoodItem) -> Unit
) : RecyclerView.Adapter<MoodAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tv_mood_emoji)
        val tvName: TextView = view.findViewById(R.id.tv_mood_name)
        val container: View = view.findViewById(R.id.mood_item_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mood = moods[position]
        holder.tvEmoji.text = mood.emoji
        holder.tvName.text = mood.name

        val isSelected = position == selectedPosition
        holder.container.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
        )
        holder.tvName.setTextColor(
            holder.itemView.context.getColor(
                if (isSelected) R.color.gold_primary else R.color.text_muted
            )
        )

        holder.container.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onMoodSelected(mood)
        }
    }

    override fun getItemCount() = moods.size
}
