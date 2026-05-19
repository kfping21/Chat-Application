package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.CommentAdapter
import com.zjgsu.treehole.model.MockData

class SecretDetailFragment : Fragment() {

    private var isLiked = false
    private var likeCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_secret_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get secretId argument (default "1")
        val secretId = arguments?.getString("secretId") ?: "1"
        val secret = MockData.secrets.firstOrNull { it.id == secretId } ?: MockData.secrets.first()

        likeCount = secret.likes

        // Populate header card
        view.findViewById<TextView>(R.id.tv_detail_mood).text = secret.mood
        view.findViewById<TextView>(R.id.tv_detail_time).text = secret.timeAgo
        view.findViewById<TextView>(R.id.tv_detail_content).text = secret.content
        view.findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()
        view.findViewById<TextView>(R.id.tv_echoes_count).text = "回响 ${MockData.comments.size}"

        // 获取点赞按钮和图标
        val btnLike = view.findViewById<LinearLayout>(R.id.btn_like_detail)
        val ivLikeIcon = view.findViewById<ImageView>(R.id.iv_like_icon)

        // 添加按钮按压动画
        ClickAnimations.addButtonPressAnimation(btnLike)

        // 点赞按钮点击
        btnLike.setOnClickListener {
            isLiked = !isLiked
            likeCount = if (isLiked) likeCount + 1 else likeCount - 1
            view.findViewById<TextView>(R.id.tv_like_count).text = likeCount.toString()

            if (isLiked) {
                ivLikeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.unread_badge))
                ClickAnimations.animateLike(ivLikeIcon)
                Toast.makeText(requireContext(), "给予温暖 ✨", Toast.LENGTH_SHORT).show()
            } else {
                ivLikeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_muted))
            }
        }

        // Back button
        val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
        ClickAnimations.addButtonPressAnimation(btnBack)
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Comments
        val rv = view.findViewById<RecyclerView>(R.id.rv_comments)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = CommentAdapter(MockData.comments)

        // Comment input
        val etComment = view.findViewById<EditText>(R.id.et_comment)

        // Send comment button with animation
        val btnSend = view.findViewById<ImageButton>(R.id.btn_send_comment)
        btnSend.setOnClickListener {
            val content = etComment.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "请输入你的回响", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 发送动画
            ClickAnimations.animateLike(btnSend)
            etComment.text.clear()
            Toast.makeText(requireContext(), "回响已发送 ✨", Toast.LENGTH_SHORT).show()
        }
    }
}
