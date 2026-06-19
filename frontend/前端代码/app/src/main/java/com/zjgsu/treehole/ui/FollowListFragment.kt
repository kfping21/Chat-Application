package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.FollowUserAdapter
import com.zjgsu.treehole.network.AuthApi
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FollowListFragment : Fragment() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageButton

    private var adapter: FollowUserAdapter? = null
    private val users = mutableListOf<FollowUser>()
    private var listType = "following" // "following" or "followers"
    private var currentUserId: String? = null

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_USER_ID = "userId"
        private const val ARG_TITLE = "title"

        fun newInstance(userId: String, type: String, title: String): FollowListFragment {
            return FollowListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                    putString(ARG_USER_ID, userId)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    data class FollowUser(
        val id: String,
        val nickname: String,
        val avatar: String,
        val bio: String,
        val isOnline: Boolean,
        val isFollowing: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_follow_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listType = arguments?.getString(ARG_TYPE) ?: "following"
        currentUserId = arguments?.getString(ARG_USER_ID) ?: TokenManager.getUserId()
        val title = arguments?.getString(ARG_TITLE) ?: if (listType == "following") "我的关注" else "我的粉丝"

        rvUsers = view.findViewById(R.id.rv_users)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tvTitle = view.findViewById(R.id.tv_title)
        btnBack = view.findViewById(R.id.btn_back)

        tvTitle.text = title

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        rvUsers.layoutManager = LinearLayoutManager(requireContext())
        adapter = FollowUserAdapter(
            users,
            listType == "following", // Only show unfollow button for "following" list
            { userId -> unfollowUser(userId) },
            { user -> navigateToProfile(user) }
        )
        rvUsers.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    if (listType == "following") {
                        RetrofitClient.authApi.getFollowing(currentUserId ?: "")
                    } else {
                        RetrofitClient.authApi.getFollowers(currentUserId ?: "")
                    }
                }
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        users.clear()
                        users.addAll(body.users.map {
                            FollowUser(
                                id = it.id,
                                nickname = it.nickname,
                                avatar = it.avatar,
                                bio = it.bio,
                                isOnline = it.isOnline,
                                isFollowing = it.isFollowing
                            )
                        })
                        adapter?.notifyDataSetChanged()
                        tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                        tvEmpty.text = if (listType == "following") "还没有关注任何人" else "还没有粉丝"
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unfollowUser(userId: String) {
        AlertDialog.Builder(requireContext())
            .setMessage("确定要取消关注吗？")
            .setPositiveButton("确定") { _, _ ->
                doUnfollow(userId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doUnfollow(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.whisperApi.followUser(userId)
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "已取消关注", Toast.LENGTH_SHORT).show()
                    // Remove from list
                    val index = users.indexOfFirst { it.id == userId }
                    if (index >= 0) {
                        users.removeAt(index)
                        adapter?.notifyItemRemoved(index)
                    }
                    if (users.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "还没有关注任何人"
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToProfile(user: FollowUser) {
        findNavController().navigate(
            R.id.action_global_user_profile,
            Bundle().apply {
                putString("userId", user.id)
                putString("nickname", user.nickname)
                putString("avatar", user.avatar)
            }
        )
    }
}