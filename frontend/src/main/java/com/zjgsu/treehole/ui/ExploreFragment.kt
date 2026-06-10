package com.zjgsu.treehole.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import androidx.cardview.widget.CardView
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
import com.zjgsu.treehole.ui.StarrySkyView
import android.widget.ImageView
import android.widget.FrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.google.android.material.appbar.AppBarLayout
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.network.WhisperWebSocket

class ExploreFragment : Fragment() {

    companion object {
        val myJoinedRoomIds = mutableSetOf<String>()
        private val roomAvatarIndices = mutableMapOf<String, List<Int>>()
        private var nextAvatarIndex = 0
    }

    private lateinit var rvActivities: RecyclerView
    private lateinit var starrySkyView: StarrySkyView
    private lateinit var btnMoodClose: ImageView
    private lateinit var btnMoodCancel: TextView
    private lateinit var btnMoodJoin: TextView
    private lateinit var tvMoodContent: TextView
    private lateinit var fabCreateRoom: CardView
    private lateinit var planetContainer: FrameLayout

    private var sphereUsers: List<ExploreUserDto> = emptyList()
    private var partyRoomsRefreshJob: Job? = null
    private var partyRoomsRecyclerView: RecyclerView? = null
    private var partyRoomAdapter: PartyRoomAdapter? = null

    // Mock room data - 与树洞软件相关的聊天室内容
    private val mockRooms = listOf(
        PartyRoomDto(
            id = "mock_1",
            name = "深夜树洞",
            subtitle = "深夜难眠，来这里说说心里话",
            tag = "情感树洞",
            participantCount = 5,
            maxParticipants = 6,
            onlineCount = 141,
            heat = 2341,
            messageCount = 890,
            lastMessage = "讨论中...",
            lastMessageAt = "刚刚",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Mimi", "https://api.dicebear.com/7.x/micah/png?seed=Nala", "https://api.dicebear.com/7.x/micah/png?seed=Oliver")
        ),
        PartyRoomDto(
            id = "mock_2",
            name = "考研压力释放站",
            subtitle = "一起加油，互相鼓励",
            tag = "学业互助",
            participantCount = 4,
            maxParticipants = 6,
            onlineCount = 15,
            heat = 567,
            messageCount = 234,
            lastMessage = "快来聊天",
            lastMessageAt = "5分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Zoe", "https://api.dicebear.com/7.x/micah/png?seed=Aneka")
        ),
        PartyRoomDto(
            id = "mock_3",
            name = "职场吐槽大会",
            subtitle = "工作中的那些烦心事",
            tag = "职场心声",
            participantCount = 3,
            maxParticipants = 6,
            onlineCount = 43,
            heat = 892,
            messageCount = 456,
            lastMessage = "前途在哪",
            lastMessageAt = "10分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Felix")
        ),
        PartyRoomDto(
            id = "mock_4",
            name = "感情困惑求助",
            subtitle = "爱情里，你是谁？",
            tag = "情感树洞",
            participantCount = 6,
            maxParticipants = 6,
            onlineCount = 30,
            heat = 1567,
            messageCount = 789,
            lastMessage = "脱单秘籍分享",
            lastMessageAt = "3分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Leo")
        ),
        PartyRoomDto(
            id = "mock_5",
            name = "匿名秘密分享",
            subtitle = "说出你的小秘密，这里没人认识你",
            tag = "秘密树洞",
            participantCount = 2,
            maxParticipants = 6,
            onlineCount = 62,
            heat = 1234,
            messageCount = 567,
            lastMessage = "太甜了",
            lastMessageAt = "15分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Ruby")
        ),
        PartyRoomDto(
            id = "mock_6",
            name = "毕业焦虑互助组",
            subtitle = "毕业季的迷茫与抉择",
            tag = "学业互助",
            participantCount = 4,
            maxParticipants = 6,
            onlineCount = 22,
            heat = 789,
            messageCount = 345,
            lastMessage = "本地人来",
            lastMessageAt = "20分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Lily", "https://api.dicebear.com/7.x/micah/png?seed=Buster")
        ),
        PartyRoomDto(
            id = "mock_7",
            name = "孤独患者之家",
            subtitle = "在人群中感到孤独的你",
            tag = "情感树洞",
            participantCount = 5,
            maxParticipants = 6,
            onlineCount = 54,
            heat = 2100,
            messageCount = 1234,
            lastMessage = "干杯",
            lastMessageAt = "刚刚",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Max", "https://api.dicebear.com/7.x/micah/png?seed=Chloe")
        ),
        PartyRoomDto(
            id = "mock_8",
            name = "梦想追逐者",
            subtitle = "说说你的梦想，我们一起加油",
            tag = "励志成长",
            participantCount = 3,
            maxParticipants = 6,
            onlineCount = 21,
            heat = 890,
            messageCount = 456,
            lastMessage = "江湖恩怨",
            lastMessageAt = "8分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Aneka", "https://api.dicebear.com/7.x/micah/png?seed=Ruby")
        ),
        PartyRoomDto(
            id = "mock_9",
            name = "音乐分享会",
            subtitle = "你最近单曲循环的歌是什么？",
            tag = "音乐同好",
            participantCount = 6,
            maxParticipants = 6,
            onlineCount = 88,
            heat = 3452,
            messageCount = 1024,
            lastMessage = "这首周杰伦的不错",
            lastMessageAt = "1分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Zoe", "https://api.dicebear.com/7.x/micah/png?seed=Leo")
        ),
        PartyRoomDto(
            id = "mock_10",
            name = "深夜阅读时间",
            subtitle = "分享一本治愈你的书",
            tag = "文学交流",
            participantCount = 3,
            maxParticipants = 6,
            onlineCount = 27,
            heat = 654,
            messageCount = 210,
            lastMessage = "推荐《百年孤独》",
            lastMessageAt = "4分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Oscar", "https://api.dicebear.com/7.x/micah/png?seed=Leo", "https://api.dicebear.com/7.x/micah/png?seed=Penny")
        ),
        PartyRoomDto(
            id = "mock_11",
            name = "减脂打卡监督",
            subtitle = "管住嘴迈开腿，明天你是李宣美",
            tag = "运动打卡",
            participantCount = 5,
            maxParticipants = 6,
            onlineCount = 95,
            heat = 2109,
            messageCount = 876,
            lastMessage = "今天跑了5公里",
            lastMessageAt = "刚刚",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Charlie", "https://api.dicebear.com/7.x/micah/png?seed=Mia", "https://api.dicebear.com/7.x/micah/png?seed=Simba")
        ),
        PartyRoomDto(
            id = "mock_12",
            name = "游戏开黑发车",
            subtitle = "缺个辅助，随便来",
            tag = "游戏组队",
            participantCount = 4,
            maxParticipants = 6,
            onlineCount = 120,
            heat = 4321,
            messageCount = 2234,
            lastMessage = "拉我拉我",
            lastMessageAt = "半分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Charlie", "https://api.dicebear.com/7.x/micah/png?seed=Buster", "https://api.dicebear.com/7.x/micah/png?seed=Mimi")
        ),
        PartyRoomDto(
            id = "mock_13",
            name = "留学生互助圈",
            subtitle = "时差党的抱团取暖",
            tag = "海外生活",
            participantCount = 2,
            maxParticipants = 6,
            onlineCount = 45,
            heat = 987,
            messageCount = 345,
            lastMessage = "这边物价好贵",
            lastMessageAt = "12分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Lily", "https://api.dicebear.com/7.x/micah/png?seed=Ruby")
        ),
        PartyRoomDto(
            id = "mock_14",
            name = "摄影作品交流",
            subtitle = "记录生活中的小确幸",
            tag = "摄影爱好",
            participantCount = 5,
            maxParticipants = 6,
            onlineCount = 76,
            heat = 1543,
            messageCount = 654,
            lastMessage = "这张光影绝了",
            lastMessageAt = "2分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Chloe")
        ),
        PartyRoomDto(
            id = "mock_15",
            name = "如何避免被女性白嫖2.0",
            subtitle = "付出与收获的边界在哪里？",
            tag = "情感探讨",
            participantCount = 6,
            maxParticipants = 6,
            onlineCount = 99,
            heat = 3456,
            messageCount = 1205,
            lastMessage = "别当舔狗就行",
            lastMessageAt = "刚刚",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Milo")
        ),
        PartyRoomDto(
            id = "mock_16",
            name = "夫妻欢乐俱乐部",
            subtitle = "婚后生活吐槽与分享",
            tag = "婚恋生活",
            participantCount = 4,
            maxParticipants = 6,
            onlineCount = 55,
            heat = 1234,
            messageCount = 890,
            lastMessage = "我老公昨天又...",
            lastMessageAt = "5分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Zoe")
        ),
        PartyRoomDto(
            id = "mock_17",
            name = "纯听歌房，经典老歌",
            subtitle = "梦醒时分-半吨",
            tag = "音乐同好",
            participantCount = 6,
            maxParticipants = 6,
            onlineCount = 462,
            heat = 8900,
            messageCount = 5678,
            lastMessage = "好听",
            lastMessageAt = "1分钟前",
            avatars = listOf("https://api.dicebear.com/7.x/micah/png?seed=Toby")
        ),
        PartyRoomDto(
            id = "mock_18",
            name = "改论文",
            subtitle = "回忆青春，怀念过去",
            tag = "学术交流",
            participantCount = 2,
            maxParticipants = 6,
            onlineCount = 28,
            heat = 567,
            messageCount = 120,
            lastMessage = "查重率太高了",
            lastMessageAt = "10分钟前"
        )
    ).mapIndexed { _, room ->
        room.copy(avatars = emptyList())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_explore, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Orb click navigation with StarrySkyView
        val starrySkyView = view.findViewById<StarrySkyView>(R.id.starry_sky_view)
        val moodPopup = view.findViewById<LinearLayout>(R.id.mood_popup)
        planetContainer = view.findViewById(R.id.planet_container)
        
        // Collapse AppBarLayout by default
        val appBar = view.findViewById<AppBarLayout>(R.id.app_bar)
        appBar?.setExpanded(false, false)
        
        val scrollContent = view.findViewById<NestedScrollView>(R.id.scroll_content)
        
        // Initialize class property properly before using it in listener
        fabCreateRoom = view.findViewById(R.id.fab_create_room)

        val restoreAlphaRunnable = Runnable {
            fabCreateRoom.animate().alpha(1f).setDuration(200).start()
        }

        scrollContent.setOnScrollChangeListener { _, _, _, _, _ ->
            if (fabCreateRoom.alpha == 1f) {
                fabCreateRoom.animate().alpha(0.3f).setDuration(200).start()
            }
            fabCreateRoom.removeCallbacks(restoreAlphaRunnable)
            fabCreateRoom.postDelayed(restoreAlphaRunnable, 500)
        }

        val moodPopupDim = view.findViewById<View>(R.id.mood_popup_dim)
        val tvMoodContent = view.findViewById<TextView>(R.id.tv_mood_content)

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
            if (user != null) {
                val args = Bundle().apply {
                    putString("userId", user.id)
                    putString("nickname", user.nickname)
                    putString("avatar", user.avatar)
                }
                findNavController().navigate(R.id.action_global_user_profile, args)
            }
        }

        // The user profile navigation completely replaces the mood popup and join chat button.
        view.findViewById<TextView>(R.id.btn_mood_join).setOnClickListener {
            hideMoodPopup()
        }

        val rvActivities = view.findViewById<RecyclerView>(R.id.rv_activities)
        partyRoomsRecyclerView = rvActivities
        val roomSpacing = (8 * resources.displayMetrics.density).toInt()
        rvActivities?.layoutManager = GridLayoutManager(requireContext(), 2)
        rvActivities?.addItemDecoration(GridSpacingItemDecoration(2, roomSpacing, false))
        partyRoomAdapter = PartyRoomAdapter(mutableListOf()) { room ->
            if (room.id.startsWith("mock_")) {
                return@PartyRoomAdapter
            }
            
            // Record that the user joined this real room
            myJoinedRoomIds.add(room.id)
            
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

        // Floating Action Button click listener
        fabCreateRoom.setOnClickListener {
            showCreateRoomDialog()
        }

        // Soul Match Button click listener
        val btnSoulMatch = view.findViewById<View>(R.id.btn_soul_match)
        btnSoulMatch?.setOnClickListener {
            val dialog = SoulMatchDialogFragment(sphereUsers) { roomId, participantId, nickname, avatar ->
                val args = Bundle().apply {
                    putString("roomId", roomId)
                    putString("participantId", participantId)
                    putString("nickname", nickname)
                    putString("avatar", avatar)
                }
                findNavController().navigate(R.id.action_explore_to_chat, args)
            }
            dialog.show(childFragmentManager, "SoulMatchDialog")
        }

        // Bubble Pond navigation
        val btnBubblePond = view.findViewById<View>(R.id.btn_bubble_pond)
        btnBubblePond?.setOnClickListener {
            findNavController().navigate(R.id.action_explore_to_bubble_pond)
        }
    }

    override fun onResume() {
        super.onResume()
        startPartyRoomsAutoRefresh()
        connectPartyRoomWebSocket()
    }

    override fun onPause() {
        super.onPause()
        partyRoomsRefreshJob?.cancel()
        partyRoomsRefreshJob = null
        WhisperWebSocket.setPartyRoomListener(null)
    }

    private fun connectPartyRoomWebSocket() {
        val myId = TokenManager.getUserId() ?: return
        val nickname = TokenManager.getNickname() ?: ""

        val listener = object : WhisperWebSocket.OnWhisperListener {
            override fun onNewMessage(message: WhisperWebSocket.WhisperMessage) {}
            override fun onMessageSent(message: WhisperWebSocket.WhisperMessage) {}
            override fun onMessageError(message: String) {}
            override fun onMessageLimitReached() {}
            override fun onMessageLimitLifted(roomId: String) {}
            override fun onUnreadUpdate(roomId: String, count: Int) {}
            override fun onFollowUpdated(userId: String, isFollowing: Boolean) {}
            override fun onOnlineUsersUpdated(onlineUserIds: List<String>) {}
            override fun onConnected() {}
            override fun onDisconnected() {}
        }

        val partyRoomListener = object : WhisperWebSocket.OnPartyRoomListener {
            override fun onPartyRoomCreated(room: WhisperWebSocket.PartyRoomDto) {
                activity?.runOnUiThread {
                    if (partyRoomAdapter != null && !room.id.startsWith("mock_")) {
                        partyRoomAdapter?.addToTop(room)
                        partyRoomsRecyclerView?.scrollToPosition(0)
                    }
                }
            }
        }

        WhisperWebSocket.connect(myId, nickname, listener)
        WhisperWebSocket.setPartyRoomListener(partyRoomListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        partyRoomsRefreshJob?.cancel()
        partyRoomsRefreshJob = null
        partyRoomsRecyclerView = null
        partyRoomAdapter = null
    }

    private fun loadSphereUsers(starrySkyView: StarrySkyView) {
        val tvOnlineCount = view?.findViewById<TextView>(R.id.tv_online_count)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.authApi.getExploreUsers(limit = 72)
                }
                if (response.isSuccessful) {
                    val users = response.body()?.users.orEmpty()
                    sphereUsers = users.filter { 
                        it.nickname != "刘苏鸿" && it.nickname != "李泽亿"
                    }
                    val souls = sphereUsers.map { user ->
                            SphereSoul(
                                id = user.id,
                                nickname = user.nickname.ifBlank { "匿名用户" },
                                mood = user.bio.ifBlank { "该用户没有任何介绍" }
                            )
                        }
                    starrySkyView.setSouls(souls)
                    tvOnlineCount?.text = "当前 ${sphereUsers.size} 人在线"
                    // Refresh party rooms to apply new user avatars
                    if (partyRoomAdapter != null) {
                        loadPartyRooms()
                    }
                }
            } catch (_: Exception) {
                starrySkyView.setSouls(emptyList())
                tvOnlineCount?.text = "当前 0 人在线"
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
                    val apiRooms = response.body()?.rooms.orEmpty()
                    // Combine API rooms with mock rooms, real rooms first
                    val allRooms = apiRooms + mockRooms
                    updatePartyRooms(allRooms)
                } else {
                    // If API fails, only show mock rooms if we have nothing else
                    if (partyRoomAdapter?.itemCount == 0 || partyRoomAdapter?.itemCount == mockRooms.size) {
                        updatePartyRooms(mockRooms)
                    }
                }
            } catch (_: Exception) {
                // On error, only show mock rooms if we have nothing else
                if (partyRoomAdapter?.itemCount == 0 || partyRoomAdapter?.itemCount == mockRooms.size) {
                    updatePartyRooms(mockRooms)
                }
            }
        }
    }

    private fun updatePartyRooms(rooms: List<PartyRoomDto>) {
        val rv = partyRoomsRecyclerView ?: return
        val adapter = partyRoomAdapter ?: return
        
        val userAvatars = sphereUsers.map { it.avatar }.filter { it.isNotEmpty() }.distinct()
        
        val finalRooms = rooms.map { room ->
            val isMock = room.id.startsWith("mock_")
            
            if (isMock) {
                if (userAvatars.isNotEmpty()) {
                    val avatarsForRoom = mutableListOf<String>()
                    val count = kotlin.math.min(room.participantCount, 3)
                    val needed = count
                    
                    if (needed > 0) {
                        val indices = roomAvatarIndices.getOrPut(room.id) {
                            val newIndices = mutableListOf<Int>()
                            for (i in 0 until needed) {
                                newIndices.add(nextAvatarIndex % userAvatars.size)
                                nextAvatarIndex++
                            }
                            newIndices
                        }
                        
                        val currentIndices = indices.toMutableList()
                        while (currentIndices.size < needed) {
                            currentIndices.add(nextAvatarIndex % userAvatars.size)
                            nextAvatarIndex++
                            roomAvatarIndices[room.id] = currentIndices
                        }
                        
                        for (i in 0 until needed) {
                            val avatarPath = userAvatars[currentIndices[i] % userAvatars.size]
                            avatarsForRoom.add(if (avatarPath.startsWith("http")) avatarPath else "${RetrofitClient.BASE_URL.trimEnd('/')}$avatarPath")
                        }
                    }
                    room.copy(avatars = avatarsForRoom)
                } else {
                    room
                }
            } else {
                // For REAL rooms, DO NOT mock avatars! 
                // Just correctly format the real participant avatars returned by the backend.
                val realAvatars = room.avatars?.map { avatarPath ->
                    if (avatarPath.startsWith("http")) avatarPath else "${RetrofitClient.BASE_URL.trimEnd('/')}$avatarPath"
                } ?: emptyList()
                
                room.copy(avatars = realAvatars)
            }
        }
        
        adapter.replaceAll(finalRooms)
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

    private var isCreatingRoom = false

    private fun createRoom(name: String, subtitle: String) {
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "聊天室名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (isCreatingRoom) return
        isCreatingRoom = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.partyApi.createRoom(com.zjgsu.treehole.network.CreatePartyRoomRequest(name, subtitle))
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "聊天室已创建", Toast.LENGTH_SHORT).show()
                    // Add new room to top of the list
                    val newRoom = response.body()?.room
                    if (newRoom != null) {
                        myJoinedRoomIds.add(newRoom.id)
                        
                        val args = Bundle().apply {
                            putString("roomId", newRoom.id)
                            putString("roomName", newRoom.name)
                        }
                        findNavController().navigate(R.id.partyChatFragment, args)
                    } else {
                        loadPartyRooms()
                    }
                } else {
                    Toast.makeText(requireContext(), "创建失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "创建失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isCreatingRoom = false
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