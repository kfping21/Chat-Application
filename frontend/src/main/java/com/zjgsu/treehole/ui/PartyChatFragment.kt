package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.zjgsu.treehole.network.PartyMessageDto
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
    private val messages = mutableListOf<PartyMessageDto>()
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
            findNavController().navigateUp()
        }

        rv = view.findViewById(R.id.rv_party_messages)
        etMessage = view.findViewById(R.id.et_party_message)
        btnSend = view.findViewById(R.id.btn_party_send)
        adapter = PartyMessageAdapter(TokenManager.getUserId().orEmpty(), messages)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        btnSend.setOnClickListener {
            sendMessage()
        }

        loadMessages(scrollToBottom = true)
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

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() || roomId.isBlank()) return
        etMessage.text.clear()

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
}
