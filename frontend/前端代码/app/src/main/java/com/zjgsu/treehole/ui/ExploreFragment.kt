package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.PartyRoomAdapter
import com.zjgsu.treehole.network.CreatePartyRoomRequest
import com.zjgsu.treehole.network.ExploreUserDto
import com.zjgsu.treehole.network.PartyRoomDto
import com.zjgsu.treehole.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExploreFragment : Fragment() {
    private var sphereUsers: List<ExploreUserDto> = emptyList()
    private var partyRoomsRefreshJob: Job? = null
    private var partyRoomsRecyclerView: RecyclerView? = null
    private var partyRoomAdapter: PartyRoomAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_explore, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Orb click navigation with StarrySkyView
        val starrySkyView = view.findViewById<StarrySkyView>(R.id.starry_sky_view)
        val moodPopup = view.findViewById<LinearLayout>(R.id.mood_popup)
        val moodPopupDim = view.findViewById<View>(R.id.mood_popup_dim)
        val tvMoodContent = view.findViewById<TextView>(R.id.tv_mood_content)
        val scrollContent = view.findViewById<ScrollView>(R.id.scroll_content)

        var pendingUser: ExploreUserDto? = null

        loadSphereUsers(starrySkyView)

        fun showMoodPopup() {
            moodPopupDim.isVisible = true
            moodPopup.isVisible = true
            scrollContent.isEnabled = false
        }

        fun hideMoodPopup() {
            moodPopupDim.isVisible = false
            moodPopup.isVisible = false
            scrollContent.isEnabled = true
        }

        starrySkyView.setOnOrbClickListener { index, mood ->
            val user = sphereUsers.getOrNull(index)
            pendingUser = user
            if (user != null) {
                val intro = user.bio.ifBlank { "这个人有点害羞，暂时还没写个人介绍。" }
                tvMoodContent.text = "${user.nickname.ifBlank { "匿名用户" }}\n\n$intro"
            } else {
                tvMoodContent.text = mood
            }
            showMoodPopup()
        }

        moodPopupDim.setOnClickListener { hideMoodPopup() }
        view.findViewById<View>(R.id.btn_mood_close).setOnClickListener { hideMoodPopup() }
        view.findViewById<TextView>(R.id.btn_mood_cancel).setOnClickListener { hideMoodPopup() }

        view.findViewById<TextView>(R.id.btn_mood_join).setOnClickListener {
            val user = pendingUser
            if (user == null) {
                Toast.makeText(requireContext(), "未找到该用户信息，请重试", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            hideMoodPopup()
            Toast.makeText(requireContext(), "正在连接灵魂伴侣...", Toast.LENGTH_SHORT).show()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.whisperApi.startChat(user.id)
                    }
                    if (response.isSuccessful) {
                        val data = response.body()
                        if (data != null) {
                            val args = Bundle().apply {
                                putString("roomId", data.roomId)
                                putString("participantId", data.participantId)
                                putString("nickname", data.nickname)
                                putString("avatar", data.avatar)
                            }
                            findNavController().navigate(R.id.whisperChatFragment, args)
                        } else {
                            Toast.makeText(requireContext(), "发起私聊失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "发起私聊失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "发起私聊失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val rvActivities = view.findViewById<RecyclerView>(R.id.rv_activities)
        partyRoomsRecyclerView = rvActivities
        val roomSpacing = (8 * resources.displayMetrics.density).toInt()
        rvActivities?.layoutManager = GridLayoutManager(requireContext(), 2)
        rvActivities?.addItemDecoration(GridSpacingItemDecoration(2, roomSpacing, false))
        partyRoomAdapter = PartyRoomAdapter(mutableListOf()) { room ->
            // First try to join the room
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.partyApi.joinRoom(room.id)
                    }
                    if (response.isSuccessful || response.code() == 200) {
                        // Join successful or already in room
                        val args = Bundle().apply {
                            putString("roomId", room.id)
                            putString("roomName", room.name)
                        }
                        findNavController().navigate(R.id.partyChatFragment, args)
                    } else if (response.code() == 400) {
                        Toast.makeText(requireContext(), "聊天室已满（最多6人）", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "加入失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "加入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvActivities?.adapter = partyRoomAdapter
        loadPartyRooms()

        view.findViewById<TextView>(R.id.btn_create_room)?.setOnClickListener {
            showCreateRoomDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        startPartyRoomsAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        partyRoomsRefreshJob?.cancel()
        partyRoomsRefreshJob = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        partyRoomsRefreshJob?.cancel()
        partyRoomsRefreshJob = null
        partyRoomsRecyclerView = null
        partyRoomAdapter = null
    }

    private fun loadSphereUsers(starrySkyView: StarrySkyView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.authApi.getExploreUsers(limit = 72)
                }
                if (response.isSuccessful) {
                    val users = response.body()?.users.orEmpty()
                    // Show all users - if no bio, show "该用户没有任何介绍"
                    sphereUsers = users
                    val souls = users.map { user ->
                            SphereSoul(
                                id = user.id,
                                nickname = user.nickname.ifBlank { "匿名用户" },
                                mood = user.bio.ifBlank { "该用户没有任何介绍" }
                            )
                        }
                    starrySkyView.setSouls(souls)
                }
            } catch (_: Exception) {
                // Show empty when API fails
                starrySkyView.setSouls(emptyList())
            }
        }
    }

    private fun startPartyRoomsAutoRefresh() {
        loadPartyRooms()
        if (partyRoomsRefreshJob?.isActive == true) return
        partyRoomsRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(3000)
                loadPartyRooms()
            }
        }
    }

    private fun loadPartyRooms() {
        partyRoomsRecyclerView ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.partyApi.getRooms()
                }
                if (response.isSuccessful) {
                    val rooms = response.body()?.rooms.orEmpty()
                    updatePartyRooms(rooms)
                }
            } catch (_: Exception) { }
        }
    }

    private fun updatePartyRooms(rooms: List<PartyRoomDto>) {
        val rv = partyRoomsRecyclerView ?: return
        val adapter = partyRoomAdapter ?: return
        adapter.replaceAll(rooms)
        rv.post { rv.requestLayout() }
    }

    private fun showCreateRoomDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 0)
        }
        val etName = EditText(requireContext()).apply {
            hint = "聊天室名称（必填）"
            maxLines = 1
        }
        val etSubtitle = EditText(requireContext()).apply {
            hint = "一句简介（可选）"
            maxLines = 2
        }
        container.addView(etName)
        container.addView(etSubtitle)

        AlertDialog.Builder(requireContext())
            .setTitle("创建聊天室")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建") { _, _ ->
                val name = etName.text.toString().trim()
                val subtitle = etSubtitle.text.toString().trim()
                createRoom(name, subtitle)
            }
            .show()
    }

    private fun createRoom(name: String, subtitle: String) {
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "聊天室名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.partyApi.createRoom(CreatePartyRoomRequest(name, subtitle))
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "聊天室已创建", Toast.LENGTH_SHORT).show()
                    loadPartyRooms()
                } else {
                    Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "创建失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// Grid spacing item decoration
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}
