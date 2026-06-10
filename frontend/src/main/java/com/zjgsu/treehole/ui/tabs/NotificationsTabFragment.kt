package com.zjgsu.treehole.ui.tabs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.NotificationAdapter
import com.zjgsu.treehole.model.Notification
import com.zjgsu.treehole.model.NotificationSender
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.util.TimeUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class NotificationsTabFragment : Fragment() {
    companion object {
        private const val TAG = "NotificationsTab"
    }

    private var notificationAdapter: NotificationAdapter? = null
    private var tvEmpty: View? = null
    private lateinit var rvNotifications: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_notifications_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvNotifications = view.findViewById(R.id.rv_notifications)
        tvEmpty = view.findViewById(R.id.tv_empty_notifications)
        tvEmpty?.findViewById<TextView>(R.id.tv_empty_text)?.text = "暂无通知"
        tvEmpty?.findViewById<TextView>(R.id.tv_empty_emoji)?.text = "📭"
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())

        loadNotifications(rvNotifications)
    }

    private fun loadNotifications(rv: RecyclerView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationsApi.getNotifications()
                if (response.isSuccessful) {
                    val notifications = response.body()?.notifications?.map { dto ->
                        Notification(
                            id = dto.id,
                            type = dto.type,
                            message = dto.message,
                            read = dto.read,
                            createdAt = TimeUtils.formatTimeAgo(dto.createdAt),
                            sender = dto.sender?.let {
                                NotificationSender(
                                    id = it.id,
                                    nickname = it.nickname,
                                    avatar = it.avatar
                                )
                            },
                            postId = dto.postId,
                            postContent = dto.postContent
                        )
                    } ?: emptyList()
                    notificationAdapter = NotificationAdapter(notifications) { notification ->
                        if (!notification.read) {
                            notificationAdapter?.markAsRead(notification.id)
                            viewLifecycleOwner.lifecycleScope.launch {
                                try {
                                    RetrofitClient.notificationsApi.markAsRead(notification.id)
                                } catch (e: Exception) {
                                    Log.e(TAG, "markAsRead failed: ${e.message}", e)
                                }
                            }
                        }
                        val postId = notification.postId
                        if (postId.isNullOrBlank()) {
                            Toast.makeText(requireContext(), "该通知没有关联帖子", Toast.LENGTH_SHORT).show()
                        } else {
                            findNavController().navigate(
                                R.id.secretDetailFragment,
                                bundleOf("secretId" to postId)
                            )
                        }
                    }
                    rv.adapter = notificationAdapter
                    tvEmpty?.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    rv.adapter = NotificationAdapter(emptyList())
                    tvEmpty?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                rv.adapter = NotificationAdapter(emptyList())
                tvEmpty?.visibility = View.VISIBLE
            }
        }
    }
}
