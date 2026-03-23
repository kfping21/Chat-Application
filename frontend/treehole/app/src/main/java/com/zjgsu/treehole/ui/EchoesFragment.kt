package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.ChatPreviewAdapter
import com.zjgsu.treehole.adapter.NotificationAdapter
import com.zjgsu.treehole.model.MockData

class EchoesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_echoes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnComments = view.findViewById<Button>(R.id.btn_tab_comments)
        val btnMessages = view.findViewById<Button>(R.id.btn_tab_messages)
        val vp = view.findViewById<ViewPager2>(R.id.vp_echoes)

        vp.adapter = EchoesPagerAdapter(this)

        btnComments.setOnClickListener {
            vp.currentItem = 0
            btnComments.setBackgroundResource(R.drawable.bg_chip_selected)
            btnComments.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
            btnMessages.setBackgroundResource(R.drawable.bg_chip_default)
            btnMessages.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
        }

        btnMessages.setOnClickListener {
            vp.currentItem = 1
            btnMessages.setBackgroundResource(R.drawable.bg_chip_selected)
            btnMessages.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
            btnComments.setBackgroundResource(R.drawable.bg_chip_default)
            btnComments.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
        }

        vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0) btnComments.performClick()
                else btnMessages.performClick()
            }
        })
    }
}
