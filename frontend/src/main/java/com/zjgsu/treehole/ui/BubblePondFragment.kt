package com.zjgsu.treehole.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.zjgsu.treehole.R
import kotlin.random.Random

class BubblePondFragment : Fragment() {

    private lateinit var bubblesContainer: FrameLayout
    private lateinit var btnPostBubble: View
    private lateinit var btnBack: ImageView

    private var globalScrollAnimator: ValueAnimator? = null
    private var isScrollingPaused = false

    // State machine for bubbles
    enum class BubbleState {
        SCROLLING,
        PAUSED,
        SELECTED
    }

    private class BubbleData(
        val id: String = java.util.UUID.randomUUID().toString(),
        val userId: String,
        val view: View,
        var state: BubbleState = BubbleState.SCROLLING,
        var yPosition: Float = 0f,
        var renderYOffset: Float = 0f,
        val speed: Float,
        val name: String,
        val avatarUrl: String,
        val content: String
    )

    private val activeBubbles = mutableListOf<BubbleData>()

    private val mockTopics = listOf(
        "🎧 找自习搭子\n期末不睡，有一起刷高数的吗？",
        "🎮 找王者搭子\n星耀晋级赛，来个辅神带飞",
        "💬 树洞吐槽\n秋招太难了，感觉自己像个废物...",
        "🍜 干饭贼香\n有没有现在去吃烧烤的，校门口集合！",
        "✈️ 热爱旅行\n周末想去周边城市转转，缺个搭子",
        "👻 情绪安慰DD\n今天心情有点低落，求夸夸~",
        "💡 提个问题\n计算机网络实验报告第三题怎么做？",
        "💼 搬砖打工DD\n早八打工魂，一天又结束了",
        "📚 考研党DD\n二战人，每天都在崩溃边缘徘徊",
        "🐟 快乐摸鱼\n刚被老板骂完，摸鱼回血中",
        "💬 闲聊\n今天食堂的糖醋排骨绝了，家人们快去冲！",
        "🐾 找宠物搭子\n周末带我家修狗去草坪，有一起的吗？",
        "🎶 音乐安利\n分享一首超好听的宝藏歌曲！",
        "🎬 电影推荐\n刚看完一部悬疑片，结局直接封神，有人讨论吗？",
        "🏋️ 运动健身\n晚上操场夜跑，5km走起！",
        "📸 摄影日常\n今天晚霞绝了，给大家分享一张原相机直出。",
        "☕ 咖啡续命\n瑞幸又出新品了，有人喝过吗？好不好喝？",
        "💤 熬夜冠军\n这个点还没睡的，都在干嘛呢？"
    )
    
    private val mockTimes = listOf("1分钟前", "3分钟前", "5分钟前", "10分钟前", "半小时前", "1小时前", "2小时前", "5小时前", "昨天")

    private val networkUsers = mutableListOf<com.zjgsu.treehole.network.ExploreUserDto>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bubble_pond, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bubblesContainer = view.findViewById(R.id.bubbles_container)
        btnPostBubble = view.findViewById(R.id.btn_post_bubble)
        btnBack = view.findViewById(R.id.btn_back)

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnPostBubble.setOnClickListener {
            // Navigate to PostBubbleFragment
            findNavController().navigate(R.id.action_bubble_pond_to_post_bubble)
        }
        
        view.setOnClickListener {
            if (isScrollingPaused) {
                isScrollingPaused = false
                activeBubbles.forEach {
                    it.state = BubbleState.SCROLLING
                    it.view.findViewById<View>(R.id.view_cyan_ring).visibility = View.GONE
                    val flAvatarContainer = it.view.findViewById<View>(R.id.fl_avatar_container)
                    flAvatarContainer.scaleX = 1f
                    flAvatarContainer.scaleY = 1f
                    flAvatarContainer.alpha = 1f
                }
            }
        }

        // Add pulse animation to FAB
        val viewFabPulse = view.findViewById<View>(R.id.view_fab_pulse)
        val pulseX = android.animation.ObjectAnimator.ofFloat(viewFabPulse, "scaleX", 1f, 1.4f)
        val pulseY = android.animation.ObjectAnimator.ofFloat(viewFabPulse, "scaleY", 1f, 1.4f)
        val pulseAlpha = android.animation.ObjectAnimator.ofFloat(viewFabPulse, "alpha", 0.6f, 0f)
        val pulseAnim = android.animation.AnimatorSet()
        pulseAnim.playTogether(pulseX, pulseY, pulseAlpha)
        pulseAnim.duration = 1500
        pulseX.repeatCount = android.animation.ObjectAnimator.INFINITE
        pulseY.repeatCount = android.animation.ObjectAnimator.INFINITE
        pulseAlpha.repeatCount = android.animation.ObjectAnimator.INFINITE
        pulseAnim.start()

        // Observe new bubble posted by self via Fragment Result
        requireActivity().supportFragmentManager.setFragmentResultListener("post_bubble", viewLifecycleOwner) { _, bundle ->
            val text = bundle.getString("text")
            if (text != null) {
                pendingSelfBubble = text
            }
        }

        fetchNetworkUsers {
            // Start global scrolling engine
            startGlobalScrollEngine()

            // Generate initial bubbles (fill the screen from top to bottom)
            bubblesContainer.post {
                val height = bubblesContainer.height.toFloat()
                val verticalSpacing = 650f
                var currentY = 0f // Start from the top
                while (currentY < height + verticalSpacing) {
                    val text = pendingSelfBubble
                    if (text != null && currentY >= height - 400f) {
                        spawnBubble(forceY = currentY, selfText = text)
                        pendingSelfBubble = null
                    } else {
                        spawnBubble(forceY = currentY)
                    }
                    currentY += verticalSpacing
                }
                
                val text2 = pendingSelfBubble
                if (text2 != null) {
                    spawnBubble(forceY = currentY, selfText = text2)
                    pendingSelfBubble = null
                }
            }
        }
    }

    private fun fetchNetworkUsers(onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                val response = com.zjgsu.treehole.network.RetrofitClient.authApi.getExploreUsers(60)
                if (response.isSuccessful) {
                    val users = response.body()?.users ?: emptyList()
                    networkUsers.clear()
                    networkUsers.addAll(users.filter { 
                        it.nickname != "刘苏鸿" && it.nickname != "李泽亿"
                    })
                }
            } catch (e: Exception) {
                // Ignore and use empty
            } finally {
                onComplete()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        globalScrollAnimator?.cancel()
        activeBubbles.clear()
    }

    private fun startGlobalScrollEngine() {
        // Runs continuously at 60fps
        globalScrollAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                if (!isScrollingPaused) {
                    updateBubblesPosition()
                }
            }
            start()
        }
    }

    private fun updateBubblesPosition() {
        val height = bubblesContainer.height.toFloat()
        if (height == 0f) return

        var bubblesToRespawn = 0
        val iterator = activeBubbles.iterator()
        while (iterator.hasNext()) {
            val bubbleData = iterator.next()
            bubbleData.yPosition -= bubbleData.speed
            if (bubbleData.renderYOffset != 0f) {
                val step = 15f
                if (bubbleData.renderYOffset > step) {
                    bubbleData.renderYOffset -= step
                } else if (bubbleData.renderYOffset < -step) {
                    bubbleData.renderYOffset += step
                } else {
                    bubbleData.renderYOffset = 0f
                }
            }
            
            // Logically shift self down but render with offset
            val currentRenderY = bubbleData.yPosition + bubbleData.renderYOffset
            bubbleData.view.y = currentRenderY

            // Despawn if out of top bounds visually
            if (currentRenderY < -bubbleData.view.height) {
                bubblesContainer.removeView(bubbleData.view)
                iterator.remove()
                bubblesToRespawn++
            }
        }
        
        for (i in 0 until bubblesToRespawn) {
            spawnBubble()
        }
    }

    private var spawnSideIsLeft = true

    private fun spawnBubble(forceY: Float? = null, selfText: String? = null) {
        val width = bubblesContainer.width
        val height = bubblesContainer.height
        if (width == 0 || height == 0) return

        val bubbleView = layoutInflater.inflate(R.layout.layout_item_bubble, bubblesContainer, false)
        
        val tvUsername = bubbleView.findViewById<TextView>(R.id.tv_username)
        val tvContent = bubbleView.findViewById<TextView>(R.id.tv_content)
        val ivAvatar = bubbleView.findViewById<ImageView>(R.id.iv_avatar)
        val viewCyanRing = bubbleView.findViewById<View>(R.id.view_cyan_ring)
        val flAvatarContainer = bubbleView.findViewById<View>(R.id.fl_avatar_container)
        
        var displayUsername = "神秘人"
        var avatarUrl = ""
        var userId = ""
        var topicText = mockTopics[kotlin.random.Random.nextInt(mockTopics.size)]
        var timeText = mockTimes[kotlin.random.Random.nextInt(mockTimes.size)]
        
        val lowestBubble = activeBubbles.maxByOrNull { it.yPosition }
        val inheritedOffset = lowestBubble?.renderYOffset ?: 0f

        if (selfText != null) {
            userId = com.zjgsu.treehole.network.TokenManager.getUserId() ?: "mock_user_id"
            displayUsername = "我"
            avatarUrl = com.zjgsu.treehole.network.TokenManager.getAvatar() ?: ""
            if (!avatarUrl.startsWith("http") && avatarUrl.isNotEmpty()) {
                avatarUrl = "${com.zjgsu.treehole.network.RetrofitClient.BASE_URL.trimEnd('/')}${avatarUrl}"
            }
            topicText = selfText
            timeText = "刚刚"
            
            // Self bubble specific styling
            tvUsername.setTextColor(android.graphics.Color.parseColor("#FFD700")) // Yellow
            val drawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_avatar_cyan_ring)?.mutate()
            drawable?.setTint(android.graphics.Color.parseColor("#00FF00")) // Green
            viewCyanRing.background = drawable
            viewCyanRing.visibility = View.VISIBLE
        } else {
            if (networkUsers.isNotEmpty()) {
                val user = networkUsers[kotlin.random.Random.nextInt(networkUsers.size)]
                userId = user.id
                displayUsername = user.nickname.ifEmpty { "神秘人" }
                if (!user.avatar.isNullOrEmpty()) {
                    avatarUrl = if (user.avatar.startsWith("http")) user.avatar else "${com.zjgsu.treehole.network.RetrofitClient.BASE_URL.trimEnd('/')}${user.avatar}"
                }
            } else {
                // fallback if network fails
                userId = "mock_user_id"
                displayUsername = "神秘人"
                avatarUrl = "https://api.dicebear.com/7.x/micah/png?seed=${kotlin.random.Random.nextInt()}"
            }
        }

        tvUsername.text = displayUsername
        tvContent.text = topicText
        bubbleView.findViewById<TextView>(R.id.tv_time)?.text = timeText

        if (avatarUrl.isNotEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .into(ivAvatar)
        }

        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bubbleView.layoutParams = params
        
        bubbleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val viewWidth = bubbleView.measuredWidth
        
        val llBubbleRoot = bubbleView.findViewById<android.widget.LinearLayout>(R.id.ll_bubble_root)
        
        // Staggered left/right zigzag layout (fixed margin + small random variation)
        val margin = 32f
        val randomOffset = Random.nextFloat() * 60f
        if (spawnSideIsLeft) {
            bubbleView.x = (margin + randomOffset).coerceAtMost(width / 2f - 40f)
            llBubbleRoot.setGravity(android.view.Gravity.START)
        } else {
            val maxRight = width - viewWidth - margin
            val minRight = (width / 2f).coerceAtMost(maxRight)
            bubbleView.x = (maxRight - randomOffset).coerceIn(minRight, maxRight)
            llBubbleRoot.setGravity(android.view.Gravity.END)
        }
        spawnSideIsLeft = !spawnSideIsLeft
        
        // Vertical Spacing (measured from bottom of previous bubble)
        val verticalSpacing = 650f
        val yPos = forceY ?: run {
            val lowestBubble = activeBubbles.maxByOrNull { it.yPosition }
            val lowestY = lowestBubble?.yPosition ?: height.toFloat()
            lowestY + verticalSpacing
        }
        
        bubbleView.y = yPos
        bubblesContainer.addView(bubbleView)

        val speed = 1.5f // Consistent scroll speed
        val bubbleData = BubbleData(
            userId = userId,
            view = bubbleView,
            state = BubbleState.SCROLLING,
            yPosition = yPos,
            renderYOffset = inheritedOffset, // Inherit visual offset from the lowest bubble to prevent overlap
            speed = 1.5f,
            name = displayUsername,
            avatarUrl = avatarUrl,
            content = topicText
        )
        activeBubbles.add(bubbleData)

        bubbleView.setOnClickListener {
            handleBubbleClick(bubbleData, flAvatarContainer, viewCyanRing)
        }
    }

    private fun handleBubbleClick(bubbleData: BubbleData, avatarContainer: View, cyanRing: View) {
        when (bubbleData.state) {
            BubbleState.SCROLLING -> {
                // First click: Pause global scrolling, show ring, change state to PAUSED
                isScrollingPaused = true
                cyanRing.visibility = View.VISIBLE
                bubbleData.state = BubbleState.PAUSED
            }
            BubbleState.PAUSED -> {
                // Second click: Select this bubble (enlarge avatar slightly)
                bubbleData.state = BubbleState.SELECTED
                
                val scaleX = android.animation.ObjectAnimator.ofFloat(avatarContainer, "scaleX", 1f, 1.1f)
                val scaleY = android.animation.ObjectAnimator.ofFloat(avatarContainer, "scaleY", 1f, 1.1f)
                val set = android.animation.AnimatorSet()
                set.playTogether(scaleX, scaleY)
                set.duration = 200
                set.interpolator = android.view.animation.OvershootInterpolator()
                set.start()
            }
            BubbleState.SELECTED -> {
                // Third click: Pop bubble (ripple burst effect on avatar)
                popBubble(bubbleData, avatarContainer, cyanRing)
            }
        }
    }

    private fun popBubble(bubbleData: BubbleData, avatarContainer: View, cyanRing: View) {
        // Create the shatter view
        val shatterView = com.zjgsu.treehole.ui.widget.ParticleShatterView(requireContext())
        shatterView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        bubblesContainer.addView(shatterView)
        
        // Hide the entire bubble view immediately since it's "shattered"
        bubbleData.view.visibility = View.INVISIBLE
        
        // Explode
        shatterView.explode(avatarContainer) {
            if (isAdded) {
                // Animation finished callback
                bubblesContainer.removeView(bubbleData.view)
                activeBubbles.remove(bubbleData)
                
                // Spawn a new bubble to replace the popped one so we don't run out of bubbles
                spawnBubble()
                
                showPoppedBottomSheet(bubbleData)
            }
        }
        
        // Resume global scrolling immediately for other bubbles
        isScrollingPaused = false
        activeBubbles.forEach { 
            if (it.state != BubbleState.SCROLLING && it != bubbleData) {
                it.state = BubbleState.SCROLLING
                it.view.scaleX = 1f
                it.view.scaleY = 1f
                // Don't hide ring if it's a self bubble
                if (it.name != "我") {
                    it.view.findViewById<View>(R.id.view_cyan_ring).visibility = View.GONE
                }
            }
        }
    }

    private fun showPoppedBottomSheet(bubbleData: BubbleData) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_popped, null)
        
        val tvMessage = view.findViewById<TextView>(R.id.tv_popped_message)
        val ivAvatar = view.findViewById<ImageView>(R.id.iv_popped_avatar)
        val btnPrivateChat = view.findViewById<Button>(R.id.btn_private_chat)
        
        tvMessage.text = "你戳破了${bubbleData.name}的泡泡"
        
        val actualAvatar = if (bubbleData.avatarUrl.isNotEmpty()) bubbleData.avatarUrl else "https://api.dicebear.com/7.x/micah/png?seed=${bubbleData.name}"
        com.bumptech.glide.Glide.with(this)
            .load(actualAvatar)
            .circleCrop()
            .into(ivAvatar)
            
        btnPrivateChat.setOnClickListener {
            bottomSheet.dismiss()
            
            if (bubbleData.userId == "mock_user_id" || bubbleData.userId.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "该用户无法私聊", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = com.zjgsu.treehole.network.RetrofitClient.whisperApi.startChat(bubbleData.userId)
                    if (response.isSuccessful) {
                        val data = response.body()
                        if (data != null) {
                            val bundle = Bundle().apply {
                                putString("roomId", data.roomId)
                                putString("participantId", data.participantId)
                                putString("nickname", data.nickname)
                                putString("avatar", data.avatar)
                            }
                            findNavController().navigate(R.id.whisperChatFragment, bundle)
                        }
                    } else {
                        android.widget.Toast.makeText(requireContext(), "发起聊天失败", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(requireContext(), "网络错误", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    override fun onResume() {
        super.onResume()
        isScrollingPaused = false
        // Check for pending self bubble only if activeBubbles is not empty
        // If empty, onViewCreated's initial loop will handle it cleanly
        val text = pendingSelfBubble
        if (text != null && activeBubbles.isNotEmpty()) {
            bubblesContainer.post {
                spawnBubble(selfText = text)
            }
            pendingSelfBubble = null
        }
    }
    
    override fun onPause() {
        super.onPause()
        isScrollingPaused = true
    }
    
    companion object {
        var pendingSelfBubble: String? = null
    }
}
