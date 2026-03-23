package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.CommentAdapter
import com.zjgsu.treehole.model.MockData

class SecretDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_secret_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get secretId argument (default "1")
        val secretId = arguments?.getString("secretId") ?: "1"
        val secret = MockData.secrets.firstOrNull { it.id == secretId } ?: MockData.secrets.first()

        // Populate header card
        view.findViewById<TextView>(R.id.tv_detail_mood).text = secret.mood
        view.findViewById<TextView>(R.id.tv_detail_time).text = secret.timeAgo
        view.findViewById<TextView>(R.id.tv_detail_content).text = secret.content
        view.findViewById<TextView>(R.id.tv_like_count).text = secret.likes.toString()
        view.findViewById<TextView>(R.id.tv_echoes_count).text = "回响 ${MockData.comments.size}"

        // Back button
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            findNavController().navigateUp()
        }

        // Comments
        val rv = view.findViewById<RecyclerView>(R.id.rv_comments)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = CommentAdapter(MockData.comments)

        // Send comment
        val etComment = view.findViewById<EditText>(R.id.et_comment)
        view.findViewById<ImageButton>(R.id.btn_send_comment).setOnClickListener {
            etComment.text.clear()
        }
    }
}
