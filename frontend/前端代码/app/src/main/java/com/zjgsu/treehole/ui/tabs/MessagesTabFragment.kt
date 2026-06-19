package com.zjgsu.treehole.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.ChatPreviewAdapter
import com.zjgsu.treehole.model.ChatPreview
import com.zjgsu.treehole.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MessagesTabFragment : Fragment() {
    private lateinit var rv: RecyclerView
    private var tvEmpty: View? = null
    private var adapter: ChatPreviewAdapter? = null
    private var refreshJob: kotlinx.coroutines.Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_messages_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rv = view.findViewById(R.id.rv_chats)
        tvEmpty = view.findViewById(R.id.tv_empty_chats)
        tvEmpty?.findViewById<TextView>(R.id.tv_empty_text)?.text = "暂无私聊"
        tvEmpty?.findViewById<TextView>(R.id.tv_empty_emoji)?.text = "💬"
        rv.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun startAutoRefresh() {
        loadChatRooms()
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(3000)
                loadChatRooms()
            }
        }
    }

    private fun loadChatRooms() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.whisperApi.getChatRooms()
                if (response.isSuccessful) {
                    val rooms = response.body()?.rooms ?: emptyList()
                    // Ensure newly created chat rooms (with no messages) appear at the very top, 
                    // and others are sorted by latest message time.
                    val sortedRooms = rooms.sortedByDescending { 
                        it.lastMessageAt ?: "9999-12-31T23:59:59.999Z" 
                    }
                    val chats = sortedRooms.map { room ->
                        ChatPreview(
                            id = room.id,
                            participantId = room.participantId,
                            nickname = room.nickname.ifEmpty { "匿名用户" },
                            avatar = room.avatar,
                            lastMessage = room.lastMessage.ifEmpty { "暂无消息" },
                            timeAgo = formatTimeAgo(room.lastMessageAt),
                            unread = room.unreadCount
                        )
                    }
                    adapter = ChatPreviewAdapter(chats) { chat ->
                        // Navigate to chat room
                        val bundle = Bundle().apply {
                            putString("roomId", chat.id)
                            putString("participantId", chat.participantId)
                            putString("nickname", chat.nickname)
                            putString("avatar", chat.avatar)
                        }
                        findNavController().navigate(R.id.whisperChatFragment, bundle)
                    }
                    rv.adapter = adapter
                    tvEmpty?.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    tvEmpty?.visibility = View.VISIBLE
                    tvEmpty?.findViewById<TextView>(R.id.tv_empty_text)?.text = "暂无私聊"
                }
            } catch (e: Exception) {
                tvEmpty?.visibility = View.VISIBLE
                tvEmpty?.findViewById<TextView>(R.id.tv_empty_text)?.text = "加载失败"
            }
        }
    }

    private fun formatTimeAgo(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(dateString) ?: return dateString
            val now = Date()
            val diff = now.time - date.time
            val minutes = diff / 60000
            val hours = minutes / 60
            val days = hours / 24
            when {
                days > 0 -> "${days}天前"
                hours > 0 -> "${hours}小时前"
                minutes > 0 -> "${minutes}分钟前"
                else -> "刚刚"
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
