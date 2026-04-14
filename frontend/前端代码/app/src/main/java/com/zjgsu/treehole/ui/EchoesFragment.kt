package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.zjgsu.treehole.R

class EchoesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_echoes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnComments = view.findViewById<TextView>(R.id.btn_tab_comments)
        val btnMessages = view.findViewById<TextView>(R.id.btn_tab_messages)
        val vp = view.findViewById<ViewPager2>(R.id.vp_echoes)

        vp.adapter = EchoesPagerAdapter(this)

        // Add touch animations to tabs
        ClickAnimations.addButtonPressAnimation(btnComments)
        ClickAnimations.addButtonPressAnimation(btnMessages)

        fun updateTabs(selected: TextView, unselected: TextView) {
            selected.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction {
                    selected.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }

        btnComments.setOnClickListener {
            vp.currentItem = 0
            btnComments.setBackgroundResource(R.drawable.bg_chip_selected)
            btnComments.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
            btnMessages.setBackgroundResource(0)
            btnMessages.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            updateTabs(btnComments, btnMessages)
        }

        btnMessages.setOnClickListener {
            vp.currentItem = 1
            btnMessages.setBackgroundResource(R.drawable.bg_chip_selected)
            btnMessages.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
            btnComments.setBackgroundResource(0)
            btnComments.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            updateTabs(btnMessages, btnComments)
        }

        vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0) btnComments.performClick()
                else btnMessages.performClick()
            }
        })
    }
}
