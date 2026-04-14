package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.model.MockData

class MyHollowFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_hollow, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rv_my_secrets)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SecretAdapter(MockData.mySecrets)

        // Load personal avatar
        val ivAvatar = view.findViewById<android.widget.ImageView>(R.id.iv_my_avatar)
        com.bumptech.glide.Glide.with(this)
            .load("https://picsum.photos/seed/my_avatar_1/200")
            .circleCrop()
            .into(ivAvatar)

        // Add animations to settings buttons
        val buttons = listOf(
            view.findViewById<LinearLayout>(R.id.btn_privacy),
            view.findViewById<LinearLayout>(R.id.btn_theme),
            view.findViewById<LinearLayout>(R.id.btn_settings),
            view.findViewById<LinearLayout>(R.id.btn_logout)
        )

        buttons.forEach { btn ->
            ClickAnimations.addButtonPressAnimation(btn)
        }

        // Button click handlers
        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            Toast.makeText(requireContext(), "退出登录成功", Toast.LENGTH_SHORT).show()
            // Navigate to login
            findNavController().navigate(R.id.action_my_to_login)
        }

        view.findViewById<View>(R.id.btn_privacy).setOnClickListener {
            Toast.makeText(requireContext(), "隐私保护设置", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_theme).setOnClickListener {
            Toast.makeText(requireContext(), "主题选择", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            Toast.makeText(requireContext(), "设置页面", Toast.LENGTH_SHORT).show()
        }
    }
}
