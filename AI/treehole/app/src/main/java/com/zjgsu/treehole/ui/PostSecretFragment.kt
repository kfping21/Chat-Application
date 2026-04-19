package com.zjgsu.treehole.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.MoodAdapter
import com.zjgsu.treehole.model.MoodItem
import com.zjgsu.treehole.model.MockData

class PostSecretFragment : Fragment() {

    private var selectedMood: MoodItem? = null
    private var isSubmitting = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_post_secret, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etContent = view.findViewById<EditText>(R.id.et_content)
        val tvCount = view.findViewById<TextView>(R.id.tv_char_count)
        val rvMoods = view.findViewById<RecyclerView>(R.id.rv_moods)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit)
        val btnClose = view.findViewById<View>(R.id.btn_close)

        // 为按钮添加按压动画
        ClickAnimations.addButtonPressAnimation(btnSubmit)
        ClickAnimations.addButtonPressAnimation(btnClose)

        // Close button: navigate back
        btnClose.setOnClickListener { findNavController().navigateUp() }

        // Character counter
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCount.text = "${s?.length ?: 0}/500"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Moods RecyclerView (horizontal)
        rvMoods.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvMoods.adapter = MoodAdapter(MockData.moods) { mood ->
            selectedMood = mood
        }

        // Submit
        btnSubmit.setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            val content = etContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "请输入你的秘密", Toast.LENGTH_SHORT).show()
                // 输入框抖动提示
                etContent.requestFocus()
                return@setOnClickListener
            }
            if (selectedMood == null) {
                Toast.makeText(requireContext(), "请选择心情", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 开始提交 - 禁用按钮防止重复点击
            isSubmitting = true
            btnSubmit.isEnabled = false
            btnSubmit.text = "埋下秘密中..."

            // 模拟提交延迟
            btnSubmit.postDelayed({
                Toast.makeText(requireContext(), "秘密已埋下 ✨", Toast.LENGTH_SHORT).show()
                isSubmitting = false
                btnSubmit.isEnabled = true
                btnSubmit.text = getString(R.string.btn_post)
                findNavController().navigateUp()
            }, 800)
        }
    }
}
