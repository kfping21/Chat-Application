package com.zjgsu.treehole.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.zjgsu.treehole.util.AvatarLoader
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.MessageAdapter
import com.zjgsu.treehole.model.ChatMessage
import com.zjgsu.treehole.network.DeepSeekApi
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.network.WhisperWebSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WhisperChatFragment : Fragment(), WhisperWebSocket.OnWhisperListener {

    private lateinit var adapter: MessageAdapter
    private lateinit var chatMessages: MutableList<ChatMessage>
    private lateinit var rv: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnAiToggle: ImageButton
    private lateinit var btnAiClose: ImageButton
    private lateinit var btnAiRefresh: TextView
    private lateinit var tvLimitHint: TextView
    private lateinit var inputContainer: LinearLayout
    private lateinit var aiHelperContainer: LinearLayout
    private lateinit var progressAiLoading: ProgressBar
    private lateinit var tvAiSuggestionPrimary: TextView
    private lateinit var tvAiSuggestionSecondary: TextView

    private var roomId: String = ""
    private var participantId: String = ""
    private var participantNickname: String = ""
    private var participantAvatar: String = ""
    private var currentUserId: String = ""
    private var currentNickname: String = ""
    private var messageLimitReached = false
    private var isJoined = false
    private var isConnecting = false
    private var isSendingMessage = false  // 防止重复发送
    private var isLoadingHistory = false  // 防止重复加载历史消息
    private val loadedMessageIds = mutableSetOf<String>()  // 已加载的消息ID，用于去重

    companion object {
        private const val TAG = "WhisperChat"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_whisper_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomId = arguments?.getString("roomId") ?: ""
        participantId = arguments?.getString("participantId") ?: ""
        participantNickname = arguments?.getString("nickname") ?: ""
        participantAvatar = arguments?.getString("avatar") ?: ""

        currentUserId = TokenManager.getUserId() ?: ""
        currentNickname = TokenManager.getNickname() ?: "匿名"

        // Set chat name and avatar
        view.findViewById<TextView>(R.id.tv_chat_name)?.text = participantNickname.ifEmpty { "私聊" }
        val chatAvatar = view.findViewById<ImageView>(R.id.iv_chat_avatar)
        if (participantAvatar.isNotEmpty()) {
            AvatarLoader.loadAvatar(requireContext(), participantAvatar, chatAvatar)
        }

        // Setup UI
        rv = view.findViewById(R.id.rv_messages)
        messageInput = view.findViewById(R.id.et_message)
        btnSend = view.findViewById(R.id.btn_send_message)
        tvLimitHint = view.findViewById(R.id.tv_limit_hint)
        inputContainer = view.findViewById(R.id.input_container)
        aiHelperContainer = view.findViewById(R.id.layout_ai_assistant)
        btnAiToggle = view.findViewById(R.id.btn_ai_toggle)
        btnAiClose = view.findViewById(R.id.btn_ai_close)
        btnAiRefresh = view.findViewById(R.id.btn_ai_refresh)
        progressAiLoading = view.findViewById(R.id.progress_ai_loading)
        tvAiSuggestionPrimary = view.findViewById(R.id.tv_ai_suggestion_primary)
        tvAiSuggestionSecondary = view.findViewById(R.id.tv_ai_suggestion_secondary)

        // Hide AI helper in chat by default
        aiHelperContainer.visibility = View.GONE
        progressAiLoading.visibility = View.GONE
        tvAiSuggestionPrimary.visibility = View.GONE
        tvAiSuggestionSecondary.visibility = View.GONE

        chatMessages = mutableListOf()
        adapter = MessageAdapter(chatMessages)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Back button
        view.findViewById<ImageButton>(R.id.btn_back_chat).setOnClickListener {
            findNavController().navigateUp()
        }

        // Send message
        ClickAnimations.addButtonPressAnimation(btnSend)
        btnSend.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty() && !messageLimitReached) {
                sendMessage(text)
                messageInput.text.clear()
            } else if (messageLimitReached) {
                Toast.makeText(requireContext(), "已达消息限制，请等待对方关注", Toast.LENGTH_SHORT).show()
            }
        }

        // AI helper toggle
        ClickAnimations.addButtonPressAnimation(btnAiToggle)
        btnAiToggle.setOnClickListener {
            if (aiHelperContainer.visibility == View.VISIBLE) {
                aiHelperContainer.visibility = View.GONE
            } else {
                aiHelperContainer.visibility = View.VISIBLE
                // Load AI suggestions when opening
                loadAiSuggestions()
            }
        }

        // AI close button
        btnAiClose.setOnClickListener {
            aiHelperContainer.visibility = View.GONE
        }

        // AI refresh button
        btnAiRefresh.setOnClickListener {
            loadAiSuggestions()
        }

        // AI suggestion clicks - insert into message input
        tvAiSuggestionPrimary.setOnClickListener {
            val text = sanitizeAiSuggestion(tvAiSuggestionPrimary.text.toString())
            if (text.isNotEmpty()) {
                messageInput.setText(text)
                messageInput.setSelection(text.length)
            }
        }

        tvAiSuggestionSecondary.setOnClickListener {
            val text = sanitizeAiSuggestion(tvAiSuggestionSecondary.text.toString())
            if (text.isNotEmpty()) {
                messageInput.setText(text)
                messageInput.setSelection(text.length)
            }
        }

        // Connect WebSocket and load history
        if (roomId.isNotEmpty() && !isConnecting) {
            isConnecting = true
            WhisperWebSocket.connect(currentUserId, currentNickname, this)
            // Load history immediately - it will be deduplicated
            loadChatHistory()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload history when resuming to ensure we have latest state
        // This is safe because loadChatHistory clears existing messages first
        if (roomId.isNotEmpty()) {
            loadChatHistory()
        }
    }

    private fun loadChatHistory() {
        if (isLoadingHistory) {
            Log.d(TAG, "History already loading, skipping")
            return
        }
        isLoadingHistory = true
        Log.d(TAG, "loadChatHistory called, roomId=$roomId, token=${TokenManager.getToken()?.take(20)}...")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Making API call to get chat history")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.whisperApi.getChatHistory(roomId)
                }
                Log.d(TAG, "API response code: ${response.code()}")
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "API error body: $errorBody")
                }
                if (response.isSuccessful) {
                    val messages = response.body()?.messages ?: emptyList()
                    Log.d(TAG, "Got ${messages.size} messages from API")
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                    chatMessages.clear()
                    loadedMessageIds.clear()
                    chatMessages.addAll(messages.map { msg ->
                        loadedMessageIds.add(msg.id)  // Track loaded message IDs
                        val date = try {
                            isoFormat.parse(msg.timestamp) ?: Date()
                        } catch (e: Exception) {
                            Date()
                        }
                        ChatMessage(
                            id = msg.id,
                            text = msg.content,
                            sender = if (msg.from == currentUserId) "me" else "them",
                            timestamp = sdf.format(date)
                        )
                    })
                    adapter.notifyDataSetChanged()
                    rv.scrollToPosition(adapter.itemCount - 1)
                    Log.d(TAG, "Loaded ${chatMessages.size} messages into adapter")
                } else {
                    Log.e(TAG, "API error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Load history error: ${e.message}", e)
            } finally {
                isLoadingHistory = false
            }
        }
    }

    private fun loadAiSuggestions() {
        Log.d(TAG, "loadAiSuggestions called")

        // Show loading state
        progressAiLoading.visibility = View.VISIBLE
        tvAiSuggestionPrimary.visibility = View.GONE
        tvAiSuggestionSecondary.visibility = View.GONE

        // Build chat history from chatMessages (only "them" messages - the other person's messages)
        val chatHistory = chatMessages.map { it.sender to it.text }
        val latestMessage = chatMessages.lastOrNull { it.sender == "them" }?.text ?: ""

        if (latestMessage.isEmpty()) {
            progressAiLoading.visibility = View.GONE
            tvAiSuggestionPrimary.text = "等待对方发送消息后使用"
            tvAiSuggestionPrimary.visibility = View.VISIBLE
            tvAiSuggestionSecondary.visibility = View.GONE
            return
        }

        Log.d(TAG, "Calling DeepSeek API with latestMessage: $latestMessage")
        Log.d(TAG, "Chat history: $chatHistory")

        DeepSeekApi.generateReplySuggestions(
            chatHistory = chatHistory,
            latestMessage = latestMessage,
            soulMood = "",
            callback = { primary, secondary ->
                activity?.runOnUiThread {
                    progressAiLoading.visibility = View.GONE

                    if (primary.startsWith("__ERROR__")) {
                        tvAiSuggestionPrimary.text = "AI 生成失败，请稍后重试"
                        tvAiSuggestionPrimary.visibility = View.VISIBLE
                        tvAiSuggestionSecondary.visibility = View.GONE
                    } else {
                        tvAiSuggestionPrimary.text = sanitizeAiSuggestion(primary)
                        tvAiSuggestionSecondary.text = sanitizeAiSuggestion(secondary)
                        tvAiSuggestionPrimary.visibility = View.VISIBLE
                        tvAiSuggestionSecondary.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    private fun sanitizeAiSuggestion(text: String): String {
        return text
            .trim()
            .replace(Regex("^\\s*[（(]?\\d+[)）.、:\\-]\\s*"), "")
            .replace(Regex("^\\s*[①②③④⑤⑥⑦⑧⑨⑩]\\s*"), "")
            .replace(Regex("^\\s*[-*•]\\s*"), "")
            .trim()
    }

    private fun sendMessage(text: String) {
        if (isSendingMessage) {
            Log.d(TAG, "Already sending message, ignoring")
            return
        }
        if (messageLimitReached) {
            Toast.makeText(requireContext(), "已达消息限制，请等待对方关注", Toast.LENGTH_SHORT).show()
            return
        }

        isSendingMessage = true
        Log.d(TAG, "sendMessage called with: $text")

        // Send via WebSocket - message will be added in onNewMessage when server confirms
        WhisperWebSocket.sendMessage(text)

        // Reset flag after a short delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isSendingMessage = false
        }, 1000)
    }

    override fun onConnected() {
        Log.d(TAG, "onConnected called, isConnecting=$isConnecting, roomId=$roomId")
        activity?.runOnUiThread {
            isConnecting = false
            // Join room after connected
            WhisperWebSocket.joinRoom(roomId, currentUserId)
            isJoined = true
            // Do NOT call loadChatHistory here - it will be called via onViewCreated/onResume
            // Mark as read
            WhisperWebSocket.markAsRead(roomId)
        }
    }

    override fun onDisconnected() {
        Log.d(TAG, "WebSocket disconnected")
        activity?.runOnUiThread {
            isConnecting = false
            isJoined = false
        }
    }

    override fun onNewMessage(message: WhisperWebSocket.WhisperMessage) {
        Log.d(TAG, "===== onNewMessage called =====")
        Log.d(TAG, "message.id=${message.id}, chatId=${message.chatId}, roomId=$roomId, content=${message.content}")
        Log.d(TAG, "currentUserId=$currentUserId, isFromMe=${message.from == currentUserId}")
        Log.d(TAG, "loadedMessageIds contains this id? ${loadedMessageIds.contains(message.id)}")
        Log.d(TAG, "chatMessages any match? ${chatMessages.any { it.id == message.id }}")
        Log.d(TAG, "adapter.itemCount before = ${adapter.itemCount}")

        if (message.chatId != roomId) {
            Log.d(TAG, "Message not for this room, ignoring")
            return
        }

        // Deduplication: check if message already loaded
        if (loadedMessageIds.contains(message.id)) {
            Log.d(TAG, "Message ${message.id} already loaded, ignoring duplicate")
            return
        }
        // Also check chatMessages directly as backup
        if (chatMessages.any { it.id == message.id }) {
            Log.d(TAG, "Message ${message.id} already in chatMessages, ignoring")
            loadedMessageIds.add(message.id)
            return
        }
        loadedMessageIds.add(message.id)

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = try {
            isoFormat.parse(message.timestamp) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val chatMsg = ChatMessage(
            id = message.id,
            text = message.content,
            sender = if (message.from == currentUserId) "me" else "them",
            timestamp = sdf.format(date)
        )
        Log.d(TAG, "Adding message to adapter, total messages will be: ${adapter.itemCount + 1}")

        activity?.runOnUiThread {
            adapter.addMessage(chatMsg)
            rv.scrollToPosition(adapter.itemCount - 1)

            // If it's a message from other person, mark as read
            if (message.from != currentUserId) {
                WhisperWebSocket.markAsRead(roomId)
            }

            // Handle message limit from server
            if (message.messageLimitReached) {
                messageLimitReached = true
                inputContainer.alpha = 0.5f
                messageInput.isEnabled = false
                btnSend.isEnabled = false
                tvLimitHint.isVisible = true
                tvLimitHint.text = "已达消息限制，请等待对方关注后继续发送"
            }
        }
    }

    override fun onMessageSent(message: WhisperWebSocket.WhisperMessage) {
        // Message confirmed by server
    }

    override fun onMessageError(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            // If message limit error, disable input
            if (message.contains("消息限制") || message.contains("关注")) {
                messageLimitReached = true
                inputContainer.alpha = 0.5f
                messageInput.isEnabled = false
                btnSend.isEnabled = false
                tvLimitHint.isVisible = true
                tvLimitHint.text = message
            }
        }
    }

    override fun onMessageLimitReached() {
        messageLimitReached = true
        activity?.runOnUiThread {
            inputContainer.alpha = 0.5f
            messageInput.isEnabled = false
            btnSend.isEnabled = false
            tvLimitHint.isVisible = true
            tvLimitHint.text = "已达消息限制，请等待对方关注后继续发送"
        }
    }

    override fun onMessageLimitLifted(roomId: String) {
        if (roomId == this.roomId) {
            messageLimitReached = false
            activity?.runOnUiThread {
                inputContainer.alpha = 1.0f
                messageInput.isEnabled = true
                btnSend.isEnabled = true
                tvLimitHint.isVisible = false
                Toast.makeText(requireContext(), "已解除限制，可以继续发送", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onUnreadUpdate(roomId: String, count: Int) {
        // Update unread count in background - handled by MessagesTabFragment
    }

    override fun onFollowUpdated(userId: String, isFollowing: Boolean) {
        if (userId == participantId) {
            activity?.runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    if (isFollowing) "已关注对方" else "已取消关注",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isJoined) {
            WhisperWebSocket.leaveRoom(roomId)
        }
        WhisperWebSocket.markAsRead(roomId)
        isJoined = false
        isConnecting = false
        isSendingMessage = false
        isLoadingHistory = false
        loadedMessageIds.clear()
        // 重置连接状态，确保下次进入能重新连接
        WhisperWebSocket.resetConnection()
    }
}
