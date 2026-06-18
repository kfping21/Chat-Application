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
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.Button
import com.zjgsu.treehole.adapter.PartyMemberAdapter
import com.zjgsu.treehole.network.ExploreUserDto
import com.zjgsu.treehole.network.WhisperWebSocket

class PartyChatFragment : Fragment() {
    private var roomId: String = ""
    private var roomName: String = "聊天室"
    private lateinit var rv: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: PartyMessageAdapter
    private val messages = mutableListOf<com.zjgsu.treehole.network.PartyMessageDto>()
    private var pollJob: Job? = null
    
    private var isCreator = false
    private var currentCreatorId = ""
    private var participantsList = emptyList<ExploreUserDto>()
    
    private val partyRoomListener = object : WhisperWebSocket.OnPartyRoomListener {
        override fun onPartyRoomCreated(room: WhisperWebSocket.PartyRoomDto) {}
        override fun onPartyRoomDismissed(dismissedRoomId: String) {
            if (dismissedRoomId == roomId) {
                activity?.runOnUiThread {
                    if (findNavController().currentDestination?.id == R.id.partyChatFragment) {
                        Toast.makeText(context, "房主已解散该派对", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
        override fun onNewPartyMessage(message: com.zjgsu.treehole.network.PartyMessageDto) {
            if (message.roomId == roomId) {
                activity?.runOnUiThread {
                    val exists = messages.any { it.id == message.id }
                    if (!exists) {
                        messages.add(message)
                        adapter.notifyItemInserted(messages.size - 1)
                        rv.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }
    }

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
            if (!isCreator && roomId.isNotBlank()) {
                leaveRoom()
            }
            findNavController().navigateUp()
        }
        
        view.findViewById<ImageButton>(R.id.btn_party_members).setOnClickListener {
            showMembersDialog()
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

        WhisperWebSocket.setPartyRoomListener(partyRoomListener)
        loadMessages(scrollToBottom = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        WhisperWebSocket.setPartyRoomListener(null)
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
                delay(15000)
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
                        isCreator = body.room.isCreator
                        currentCreatorId = body.room.creatorId
                        participantsList = body.room.participants ?: emptyList()
                        
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

    private fun dismissRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.partyApi.dismissRoom(roomId)
                }
                if (response.isSuccessful) {
                    activity?.runOnUiThread {
                        if (findNavController().currentDestination?.id == R.id.partyChatFragment) {
                            Toast.makeText(requireContext(), "派对已解散", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "解散失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "解散出错", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showMembersDialog() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_party_members, null)
        bottomSheet.setContentView(view)

        val rvMembers = view.findViewById<RecyclerView>(R.id.rv_members)
        val btnDismiss = view.findViewById<Button>(R.id.btn_dismiss_party)

        rvMembers.layoutManager = LinearLayoutManager(requireContext())
        val memberAdapter = PartyMemberAdapter(currentCreatorId)
        rvMembers.adapter = memberAdapter
        memberAdapter.replaceAll(participantsList)

        if (isCreator) {
            btnDismiss.visibility = View.VISIBLE
            btnDismiss.setOnClickListener {
                bottomSheet.dismiss()
                dismissRoom()
            }
        } else {
            btnDismiss.visibility = View.GONE
        }

        bottomSheet.show()
    }
}
