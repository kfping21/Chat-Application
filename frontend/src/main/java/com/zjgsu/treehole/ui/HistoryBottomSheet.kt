package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.cache.HistoryManager
import com.zjgsu.treehole.model.Secret

class HistoryBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClear: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_history_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHistory = view.findViewById(R.id.rv_history)
        tvEmpty = view.findViewById(R.id.tv_empty_history)
        btnClear = view.findViewById(R.id.btn_clear_history)

        loadHistory()

        btnClear.setOnClickListener {
            HistoryManager.clearHistory(requireContext())
            Toast.makeText(requireContext(), "足迹已清空", Toast.LENGTH_SHORT).show()
            loadHistory()
        }
    }

    private fun loadHistory() {
        val historyItems = HistoryManager.getHistory(requireContext())
        if (historyItems.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
            btnClear.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            btnClear.visibility = View.VISIBLE

            val secrets = historyItems.map {
                Secret(
                    id = it.id,
                    content = it.content,
                    mood = it.mood,
                    timeAgo = it.timeAgo,
                    likes = 0,
                    comments = 0,
                    avatar = it.avatar,
                    nickname = it.nickname,
                    isLiked = false
                )
            }

            val adapter = SecretAdapter(secrets, onLikeClick = { _, _ -> }, onDeleteClick = null, canDelete = false)
            adapter.setOnItemClickListener { secret ->
                val bundle = bundleOf(
                    "secretId" to secret.id,
                    "userId" to secret.userId,
                    "nickname" to secret.nickname,
                    "avatar" to secret.avatar,
                    "content" to secret.content,
                    "mood" to secret.mood,
                    "timeAgo" to secret.timeAgo,
                    "imageUrls" to secret.imageUrls.toTypedArray(),
                    "likes" to secret.likes,
                    "comments" to secret.comments,
                    "isLiked" to secret.isLiked
                )
                val navController = requireActivity().supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)?.findNavController()
                navController?.navigate(R.id.secretDetailFragment, bundle)
                dismiss()
            }
            rvHistory.layoutManager = LinearLayoutManager(requireContext())
            rvHistory.adapter = adapter
        }
    }
}
