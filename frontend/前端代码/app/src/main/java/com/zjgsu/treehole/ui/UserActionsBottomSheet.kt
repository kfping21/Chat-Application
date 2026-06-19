package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.RetrofitClient
import kotlinx.coroutines.launch

class UserActionsBottomSheet : BottomSheetDialogFragment() {

    private var userId: String = ""
    private var userNickname: String = ""
    private var userAvatar: String = ""

    companion object {
        const val TAG = "UserActionsSheet"

        fun newInstance(userId: String, nickname: String, avatar: String): UserActionsBottomSheet {
            return UserActionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                    putString("nickname", nickname)
                    putString("avatar", avatar)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_user_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getString("userId") ?: ""
        userNickname = arguments?.getString("nickname") ?: ""
        userAvatar = arguments?.getString("avatar") ?: ""

        view.findViewById<LinearLayout>(R.id.btn_start_chat).setOnClickListener {
            startChat()
        }

        view.findViewById<LinearLayout>(R.id.btn_view_profile).setOnClickListener {
            // TODO: Navigate to user profile
            Toast.makeText(requireContext(), "查看主页功能开发中", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun startChat() {
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "无法发起私聊", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.whisperApi.startChat(userId)
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        dismiss()
                        // Navigate to chat
                        val bundle = Bundle().apply {
                            putString("roomId", data.roomId)
                            putString("participantId", data.participantId)
                            putString("nickname", data.nickname)
                            putString("avatar", data.avatar)
                        }
                        try {
                            findNavController().navigate(R.id.whisperChatFragment, bundle)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "无法打开聊天", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "无法发起私聊", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
