package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.MessageAdapter
import com.zjgsu.treehole.model.ChatMessage
import com.zjgsu.treehole.network.DeepSeekApi
import java.text.SimpleDateFormat
import java.util.*

class WhisperChatFragment : Fragment() {

    private lateinit var adapter: MessageAdapter
    private lateinit var chatMessages: MutableList<ChatMessage>
    private lateinit var rv: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var aiAssistantLayout: LinearLayout
    private lateinit var aiPrimarySuggestion: TextView
    private lateinit var aiSecondarySuggestion: TextView
    private lateinit var aiLoading: ProgressBar
    private lateinit var aiHint: TextView
    private var aiRefreshSeed = 0
    private var currentIncomingMessage = ""
    private var soulMood: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_whisper_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId = arguments?.getString("chatId") ?: "1"
        soulMood = arguments?.getString("soulMood") ?: ""
        val pseudonyms = listOf("落叶的小号", "深海鱼", "夜行者", "云朵", "星辰", "微光")
        val name = pseudonyms[(chatId.toIntOrNull() ?: 1) % pseudonyms.size]

        // Set chat name & avatar
        view.findViewById<TextView>(R.id.tv_chat_name)?.text = name

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

        // Messages RecyclerView - start with empty chat
        rv = view.findViewById(R.id.rv_messages)
        chatMessages = mutableListOf()
        adapter = MessageAdapter(chatMessages)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Send
        messageInput = view.findViewById(R.id.et_message)
        view.findViewById<ImageButton>(R.id.btn_send_message).setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                messageInput.text.clear()
            }
        }

        setupAiAssistant(view)
    }

    private fun setupAiAssistant(root: View) {
        aiAssistantLayout = root.findViewById(R.id.layout_ai_assistant)
        aiPrimarySuggestion = root.findViewById(R.id.tv_ai_suggestion_primary)
        aiSecondarySuggestion = root.findViewById(R.id.tv_ai_suggestion_secondary)
        aiLoading = root.findViewById(R.id.progress_ai_loading)
        aiHint = root.findViewById(R.id.tv_ai_hint)

        // 初始状态：AI 面板和提示都隐藏
        aiAssistantLayout.visibility = View.GONE
        aiHint.visibility = View.GONE

        // 点击 sparkle 图标，生成推荐回复
        root.findViewById<ImageButton>(R.id.btn_ai_toggle).setOnClickListener {
            generateAndShowSuggestions()
        }

        root.findViewById<TextView>(R.id.btn_ai_refresh).setOnClickListener {
            aiRefreshSeed += 1
            generateAndShowSuggestions()
        }

        root.findViewById<ImageButton>(R.id.btn_ai_close).setOnClickListener {
            aiAssistantLayout.visibility = View.GONE
            aiHint.visibility = View.GONE
        }

        aiPrimarySuggestion.setOnClickListener {
            fillInput(aiPrimarySuggestion.text.toString())
        }
        aiSecondarySuggestion.setOnClickListener {
            fillInput(aiSecondarySuggestion.text.toString())
        }
    }

    private fun generateAndShowSuggestions() {
        val latestIncoming = findLatestIncomingMessage()

        // 优先使用灵魂心情作为上下文，其次使用对方最新消息
        val contextMessage = if (latestIncoming.isEmpty() && soulMood.isNotBlank()) {
            soulMood
        } else if (latestIncoming.isEmpty()) {
            Toast.makeText(requireContext(), "请先让对方发送一条消息", Toast.LENGTH_SHORT).show()
            return
        } else {
            latestIncoming
        }

        if (aiRefreshSeed == 0 || contextMessage != currentIncomingMessage) {
            aiRefreshSeed = 0
            currentIncomingMessage = contextMessage
        }

        // 显示提示语
        aiAssistantLayout.visibility = View.VISIBLE
        aiLoading.isVisible = true
        aiPrimarySuggestion.isVisible = false
        aiSecondarySuggestion.isVisible = false
        aiHint.isVisible = true
        aiHint.text = "我来想想帮你怎么开口..."

        // 构建对话历史用于发送给 DeepSeek API
        val chatHistory = chatMessages.map { it.sender to it.text }

        // 如果是灵魂心情上下文，传空字符串避免重复
        val messageForApi = if (contextMessage == soulMood) "" else contextMessage

        // 调用 DeepSeek API 生成推荐回复
        DeepSeekApi.generateReplySuggestions(chatHistory, messageForApi, soulMood) { primary, secondary ->
            activity?.runOnUiThread {
                // 检查是否是错误信息
                val isError = primary.startsWith("__ERROR__")
                if (isError) {
                    // 隐藏面板，显示 Toast
                    aiAssistantLayout.visibility = View.GONE
                    aiHint.visibility = View.GONE
                    val errorMsg = primary.removePrefix("__ERROR__")
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                } else {
                    // 显示推荐回复
                    aiHint.isVisible = false
                    aiLoading.isVisible = false
                    aiPrimarySuggestion.text = primary
                    aiSecondarySuggestion.text = secondary
                    aiPrimarySuggestion.isVisible = true
                    aiSecondarySuggestion.isVisible = true
                }
            }
        }
    }

    private fun findLatestIncomingMessage(): String {
        return chatMessages.asReversed()
            .firstOrNull { it.sender != "me" && it.text.isNotBlank() }
            ?.text
            .orEmpty()
    }

    private fun fillInput(text: String) {
        messageInput.setText(text)
        messageInput.setSelection(text.length)
        messageInput.requestFocus()
    }

    private fun sendMessage(text: String) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val msg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = text,
            sender = "me",
            timestamp = sdf.format(Date())
        )
        adapter.addMessage(msg)
        rv.scrollToPosition(adapter.itemCount - 1)
    }
}
