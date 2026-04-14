package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.MessageAdapter
import com.zjgsu.treehole.model.ChatMessage
import com.zjgsu.treehole.model.MockData
import java.text.SimpleDateFormat
import java.util.*

class WhisperChatFragment : Fragment() {

    private lateinit var adapter: MessageAdapter
    private lateinit var rv: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_whisper_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId = arguments?.getString("chatId") ?: "1"
        val pseudonyms = listOf("落叶的小号", "深海鱼", "夜行者", "云朵", "星辰", "微光")
        val name = pseudonyms[(chatId.toIntOrNull() ?: 1) % pseudonyms.size]

        // Set chat name & avatar
        view.findViewById<android.widget.TextView>(R.id.tv_chat_name)?.text = name
        
        val ivAvatar = view.findViewById<android.widget.ImageView>(R.id.iv_chat_avatar)
        if (ivAvatar != null) {
            com.bumptech.glide.Glide.with(this)
                .load("https://picsum.photos/seed/chat_${chatId}/200")
                .circleCrop()
                .into(ivAvatar)
        }

        // Back
        view.findViewById<ImageButton>(R.id.btn_back_chat).setOnClickListener {
            findNavController().navigateUp()
        }

        // Messages RecyclerView
        rv = view.findViewById(R.id.rv_messages)
        adapter = MessageAdapter(MockData.mockMessages.toMutableList())
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Send
        val et = view.findViewById<EditText>(R.id.et_message)
        view.findViewById<ImageButton>(R.id.btn_send_message).setOnClickListener {
            val text = et.text.toString().trim()
            if (text.isNotEmpty()) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val msg = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = text,
                    sender = "me",
                    timestamp = sdf.format(Date())
                )
                adapter.addMessage(msg)
                rv.scrollToPosition(adapter.itemCount - 1)
                et.text.clear()
            }
        }
    }
}
