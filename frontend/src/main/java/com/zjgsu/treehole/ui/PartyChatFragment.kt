package com.zjgsu.treehole.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.PartyMessageAdapter
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.SendPartyMessageRequest
import com.zjgsu.treehole.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PartyChatFragment : Fragment() {
    private var roomId: String = ""
    private var roomName: String = "聊天室"
    private lateinit var rv: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: PartyMessageAdapter
    private val messages = mutableListOf<com.zjgsu.treehole.network.PartyMessageDto>()
    private var pollJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_party_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomId = arguments?.getString("roomId") ?: ""
        roomName = arguments?.getString("roomName") ?: "聊天室"

        view.findViewById<TextView>(R.id.tv_party_room_name).text = roomName
        view.findViewById<ImageButton>(R.id.btn_party_back).setOnClickListener {
            hideKeyboard()
            leaveRoom()
            findNavController().navigateUp()
        }

        rv = view.findViewById(R.id.rv_party_messages)
        etMessage = view.findViewById(R.id.et_party_message)
        btnSend = view.findViewById(R.id.btn_party_send)
        adapter = PartyMessageAdapter(TokenManager.getUserId().orEmpty(), messages)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // 设置发送按钮点击 - 微信风格：发送后隐藏键盘，输入框保持位置
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty() && roomId.isNotBlank()) {
                sendMessage(text)
            }
        }

        // 点击输入框时自动显示键盘（如果用户没有开启）
        etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard(etMessage)
            }
        }

        loadMessages(scrollToBottom = true)
    }

    private fun sendMessage(text: String) {
        etMessage.text.clear()
        hideKeyboard()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.partyApi.sendMessage(roomId, SendPartyMessageRequest(text))
                }
                if (response.isSuccessful) {
                    loadMessages(scrollToBottom = true)
                } else {
                    Toast.makeText(requireContext(), "发送失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        pollJob?.cancel()
        pollJob = null
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                loadMessages(scrollToBottom = false)
                delay(3000)
            }
        }
    }

    private fun loadMessages(scrollToBottom: Boolean) {
        if (roomId.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.partyApi.getMessages(roomId, 120)
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val oldSize = messages.size
                        adapter.replaceAll(body.messages)
                        if (scrollToBottom || body.messages.size > oldSize) {
                            rv.scrollToPosition((body.messages.size - 1).coerceAtLeast(0))
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun hideKeyboard() {
        activity?.currentFocus?.let { view ->
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showKeyboard(editText: EditText) {
        editText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun leaveRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.partyApi.leaveRoom(roomId)
            } catch (e: Exception) {
                // Ignore leave errors
            }
        }
    }
}
