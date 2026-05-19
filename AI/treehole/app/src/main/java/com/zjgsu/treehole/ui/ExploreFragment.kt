package com.zjgsu.treehole.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R

class ExploreFragment : Fragment() {

    private val popularTags = listOf(
        "#深夜话题", "#分享喜悦", "#倾诉烦恼", "#人生困惑",
        "#温暖瞬间", "#失眠夜晚", "#工作压力", "#感情故事",
        "#校园生活", "#职场成长", "#情感治愈", "#心灵共鸣"
    )

    private val tagColors = listOf(
        0xFF60A5FA.toInt(), // Blue
        0xFFFBBF24.toInt(), // Yellow
        0xFF34D399.toInt(), // Green
        0xFFF472B6.toInt(), // Pink
        0xFFA78BFA.toInt(), // Purple
        0xFF8B5CF6.toInt(), // Deep Purple
        0xFFEC4899.toInt(), // Rose
        0xFFFB923C.toInt()  // Orange
    )

    private var selectedPosition: Int = -1
    private lateinit var tagAdapter: TagAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_explore, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup tags RecyclerView
        val rvTags = view.findViewById<RecyclerView>(R.id.rv_tags)
        val spacing = (4 * resources.displayMetrics.density).toInt()
        rvTags.layoutManager = GridLayoutManager(requireContext(), 4)
        rvTags.addItemDecoration(GridSpacingItemDecoration(4, spacing, false))

        tagAdapter = TagAdapter(popularTags, tagColors) { position ->
            val previousSelected = selectedPosition
            selectedPosition = if (selectedPosition == position) -1 else position
            tagAdapter.notifyItemChanged(previousSelected)
            if (selectedPosition != -1) {
                tagAdapter.notifyItemChanged(selectedPosition)
                Toast.makeText(requireContext(), "正在搜索: ${popularTags[position]}", Toast.LENGTH_SHORT).show()
            }
        }
        rvTags.adapter = tagAdapter

        // Orb click navigation with StarrySkyView
        val starrySkyView = view.findViewById<StarrySkyView>(R.id.starry_sky_view)
        val moodPopup = view.findViewById<LinearLayout>(R.id.mood_popup)
        val moodPopupDim = view.findViewById<View>(R.id.mood_popup_dim)
        val tvMoodContent = view.findViewById<TextView>(R.id.tv_mood_content)
        val scrollContent = view.findViewById<ScrollView>(R.id.scroll_content)

        var pendingChatId: String = ""

        fun showMoodPopup() {
            moodPopupDim.isVisible = true
            moodPopup.isVisible = true
            scrollContent.isEnabled = false
        }

        fun hideMoodPopup() {
            moodPopupDim.isVisible = false
            moodPopup.isVisible = false
            scrollContent.isEnabled = true
        }

        starrySkyView.setOnOrbClickListener { index, mood ->
            pendingChatId = ((index % 4) + 1).toString()
            tvMoodContent.text = mood
            showMoodPopup()
        }

        moodPopupDim.setOnClickListener { hideMoodPopup() }
        view.findViewById<View>(R.id.btn_mood_close).setOnClickListener { hideMoodPopup() }
        view.findViewById<TextView>(R.id.btn_mood_cancel).setOnClickListener { hideMoodPopup() }

        view.findViewById<TextView>(R.id.btn_mood_join).setOnClickListener {
            hideMoodPopup()
            Toast.makeText(requireContext(), "正在连接灵魂伴侣...", Toast.LENGTH_SHORT).show()
            view.postDelayed({
                val args = Bundle().apply {
                    putString("chatId", pendingChatId)
                    putString("soulMood", tvMoodContent.text.toString())
                }
                findNavController().navigate(R.id.whisperChatFragment, args)
            }, 500)
        }

        // Search functionality
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim().orEmpty()
                tagAdapter.filter(q)
            }
        })
        
        // Latest Activities Section
        val rvActivities = view.findViewById<RecyclerView>(R.id.rv_activities)
        if (rvActivities != null) {
            rvActivities.layoutManager = LinearLayoutManager(requireContext())
            // Completely new component
            val trends = listOf(
                com.zjgsu.treehole.adapter.TrendingItem("#失眠夜晚的碎碎念", "近24小时有3,212人参与共鸣", "9.8w"),
                com.zjgsu.treehole.adapter.TrendingItem("#今天遇到的温暖瞬间", "2,154人正在分享他们的故事", "8.5w"),
                com.zjgsu.treehole.adapter.TrendingItem("#毕业后的迷茫挣扎", "引起了1,920位同龄人的共鸣", "7.1w"),
                com.zjgsu.treehole.adapter.TrendingItem("#终于放下那个TA", "985人留下了他们的告别语", "5.4w"),
                com.zjgsu.treehole.adapter.TrendingItem("#一个人去看电影的体验", "543次有趣的灵魂相遇", "2.1w")
            )
            rvActivities.adapter = com.zjgsu.treehole.adapter.TrendingAdapter(trends)
        }
    }
}

// Tag Adapter
class TagAdapter(
    private val tags: List<String>,
    private val colors: List<Int>,
    private val onTagClick: (Int) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    private var filteredTags = tags.toMutableList()
    private var selectedPosition = -1

    fun filter(query: String) {
        filteredTags = if (query.isEmpty()) {
            tags.toMutableList()
        } else {
            tags.filter { it.contains(query, ignoreCase = true) }.toMutableList()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = filteredTags[position]
        val colorIndex = tags.indexOf(tag) % colors.size
        val color = colors[colorIndex]
        holder.bind(tag, color, position == selectedPosition) {
            val actualPosition = tags.indexOf(tag)
            val previousSelected = selectedPosition
            selectedPosition = if (selectedPosition == actualPosition) -1 else actualPosition
            notifyItemChanged(previousSelected)
            if (selectedPosition != -1) {
                notifyItemChanged(selectedPosition)
            }
            onTagClick(actualPosition)
        }
    }

    override fun getItemCount() = filteredTags.size

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTag: android.widget.TextView = itemView.findViewById(R.id.tv_tag)

        fun bind(tag: String, color: Int, isSelected: Boolean, onClick: () -> Unit) {
            tvTag.text = tag
            if (isSelected) {
                tvTag.setTextColor(color)
                tvTag.setBackgroundResource(R.drawable.bg_tag_selected)
            } else {
                tvTag.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
                tvTag.setBackgroundResource(R.drawable.bg_tag_default)
            }

            tvTag.setOnClickListener {
                tvTag.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(80)
                    .withEndAction {
                        tvTag.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .start()
                        onClick()
                    }
                    .start()
            }
        }
    }
}

// Grid spacing item decoration
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}
