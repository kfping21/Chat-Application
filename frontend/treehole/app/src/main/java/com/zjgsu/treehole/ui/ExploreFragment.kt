package com.zjgsu.treehole.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.zjgsu.treehole.R

class ExploreFragment : Fragment() {

    private val popularTags = listOf(
        "#深夜话题", "#分享喜悦", "#倾诉烦恼", "#人生困惑",
        "#温暖瞬间", "#失眠夜晚", "#工作压力", "#感情故事"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_explore, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Build popular tag chips
        val llTags = view.findViewById<LinearLayout>(R.id.ll_tags)
        val chips = mutableListOf<TextView>()
        var selectedChip: TextView? = null

        popularTags.forEach { tag ->
            val chip = TextView(requireContext()).apply {
                text = tag
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                setBackgroundResource(R.drawable.bg_tag_default)
                setPadding(32, 16, 32, 16)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = 16
                layoutParams = lp
                setOnClickListener {
                    selectedChip?.let { prev ->
                        prev.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                        prev.setBackgroundResource(R.drawable.bg_tag_default)
                    }
                    selectedChip = this
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
                    setBackgroundResource(R.drawable.bg_tag_selected)
                }
            }
            chips.add(chip)
            llTags.addView(chip)
        }

        // Orb click navigation (遇见 -> 悄悄话)
        val orbViews = listOf(
            view.findViewById<View>(R.id.orb_1),
            view.findViewById<View>(R.id.orb_2),
            view.findViewById<View>(R.id.orb_3),
            view.findViewById<View>(R.id.orb_4),
            view.findViewById<View>(R.id.orb_5),
            view.findViewById<View>(R.id.orb_6),
            view.findViewById<View>(R.id.orb_7),
        )
        orbViews.forEachIndexed { index, orb ->
            orb.setOnClickListener {
                val chatId = ((index % 4) + 1).toString() // Mock: 1..4
                val args = Bundle().apply { putString("chatId", chatId) }
                findNavController().navigate(R.id.whisperChatFragment, args)
            }
        }

        // Search tags: filter chips by input text
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim().orEmpty()
                chips.forEach { chip ->
                    val tagText = chip.text.toString()
                    chip.visibility = if (q.isEmpty() || tagText.contains(q, ignoreCase = true)) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        })
    }
}
