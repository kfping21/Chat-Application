package com.zjgsu.treehole.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.ExploreUserDto
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.ui.widget.RadarView
import kotlin.random.Random

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SoulMatchDialogFragment(
    private val cachedSphereUsers: List<ExploreUserDto>,
    private val onMatchSuccess: (roomId: String, participantId: String, nickname: String, avatar: String) -> Unit
) : DialogFragment() {

    private lateinit var radarView: RadarView
    private lateinit var tvStatus: TextView
    private lateinit var layoutMatchSuccess: LinearLayout
    private lateinit var tvSuccessHint: TextView
    private lateinit var ivMyAvatar: ImageView
    private lateinit var ivMatchedAvatar: ImageView
    private lateinit var tvMatchedName: TextView
    
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_soul_match, container, false)
    }

    private var isMatching = true
    private var fetchJob: kotlinx.coroutines.Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        radarView = view.findViewById(R.id.radar_view)
        tvStatus = view.findViewById(R.id.tv_matching_status)
        layoutMatchSuccess = view.findViewById(R.id.layout_match_success)
        tvSuccessHint = view.findViewById(R.id.tv_success_hint)
        ivMyAvatar = view.findViewById(R.id.iv_my_avatar)
        ivMatchedAvatar = view.findViewById(R.id.iv_matched_avatar)
        tvMatchedName = view.findViewById(R.id.tv_matched_name)
        
        val btnCancelMatch = view.findViewById<View>(R.id.btn_cancel_match)
        btnCancelMatch.setOnClickListener {
            isMatching = false
            fetchJob?.cancel()
            com.zjgsu.treehole.network.WhisperWebSocket.cancelSoulMatch()
            radarView.stop()
            handler.removeCallbacksAndMessages(null)
            dismiss()
        }
        
        // Start animation
        radarView.start()
        
        if (isAdded) {
            startMatchingProcess()
        }
    }
    
    private var matchStartTime: Long = 0

    private fun startMatchingProcess() {
        matchStartTime = System.currentTimeMillis()
        val myId = TokenManager.getUserId() ?: ""
        
        val listener = object : com.zjgsu.treehole.network.WhisperWebSocket.OnWhisperListener {
            override fun onNewMessage(message: com.zjgsu.treehole.network.WhisperWebSocket.WhisperMessage) {}
            override fun onMessageSent(message: com.zjgsu.treehole.network.WhisperWebSocket.WhisperMessage) {}
            override fun onMessageError(message: String) {}
            override fun onMessageLimitReached() {}
            override fun onMessageLimitLifted(roomId: String) {}
            override fun onUnreadUpdate(roomId: String, count: Int) {}
            override fun onFollowUpdated(userId: String, isFollowing: Boolean) {}
            
            override fun onOnlineUsersUpdated(onlineUserIds: List<String>) {}
            
            override fun onSoulMatchFound(roomId: String, participantId: String, nickname: String, avatar: String) {
                if (!isMatching) return
                
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (isMatching) {
                        isMatching = false
                        val elapsed = System.currentTimeMillis() - matchStartTime
                        if (elapsed < 1200) {
                            kotlinx.coroutines.delay(1200 - elapsed)
                        }
                        showSuccessAndTransition(roomId, participantId, nickname, avatar)
                    }
                }
            }

            override fun onConnected() {
                com.zjgsu.treehole.network.WhisperWebSocket.requestSoulMatch()
            }
            
            override fun onDisconnected() { }
        }
        
        com.zjgsu.treehole.network.WhisperWebSocket.connect(myId, TokenManager.getNickname() ?: "", listener)
        
        handler.postDelayed({
            if (isMatching) {
                isMatching = false
                fetchJob?.cancel()
                com.zjgsu.treehole.network.WhisperWebSocket.cancelSoulMatch()
                
                showErrorState()
            }
        }, 15000) // 15 seconds timeout
    }
    
    private fun showErrorState() {
        if (isAdded) {
            radarView.stop()
            tvStatus.text = "没能找到合适用户，请稍后重试"
            handler.postDelayed({ if (isAdded) dismiss() }, 1500)
        }
    }
    
    private fun showSuccessAndTransition(roomId: String, participantId: String, nickname: String, avatar: String) {
        if (!isAdded) return
        
        fetchJob?.cancel()
        
        radarView.stop()
        radarView.visibility = View.GONE
        tvStatus.visibility = View.GONE
        
        // Hide cancel button
        view?.findViewById<Button>(R.id.btn_cancel_match)?.visibility = View.GONE
        
        layoutMatchSuccess.visibility = View.VISIBLE
        tvSuccessHint.visibility = View.VISIBLE
        
        val myAvatar = TokenManager.getAvatar() ?: ""
        val myUrl = if (myAvatar.startsWith("http")) myAvatar else "${RetrofitClient.BASE_URL.trimEnd('/')}$myAvatar"
        Glide.with(this).load(myUrl).apply(RequestOptions.circleCropTransform()).into(ivMyAvatar)
        
        tvMatchedName.text = nickname.ifBlank { "匿名用户" }
        val matchedUrl = if (avatar.startsWith("http")) avatar else "${RetrofitClient.BASE_URL.trimEnd('/')}${avatar}"
        Glide.with(this).load(matchedUrl).apply(RequestOptions.circleCropTransform()).into(ivMatchedAvatar)
        
        // Beautiful Animation Setup
        ivMyAvatar.translationX = -300f
        ivMyAvatar.alpha = 0f
        ivMatchedAvatar.translationX = 300f
        ivMatchedAvatar.alpha = 0f
        
        val tvHeart = view?.findViewById<TextView>(R.id.tv_match_heart)
        tvHeart?.scaleX = 0f
        tvHeart?.scaleY = 0f
        
        layoutMatchSuccess.alpha = 1f
        layoutMatchSuccess.scaleX = 1f
        layoutMatchSuccess.scaleY = 1f
        
        // My Avatar slides in
        ivMyAvatar.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(700)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()
            
        // Matched Avatar slides in
        ivMatchedAvatar.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(200)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()
            
        // Heart pops out
        tvHeart?.animate()
            ?.scaleX(1.8f)
            ?.scaleY(1.8f)
            ?.setDuration(600)
            ?.setStartDelay(700)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(2.5f))
            ?.withEndAction {
                // Heart beat continuous pulse
                if (isAdded) {
                    val pulseAnim = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                        tvHeart,
                        android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.4f, 1.8f),
                        android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.4f, 1.8f)
                    )
                    pulseAnim.duration = 400
                    pulseAnim.repeatCount = android.animation.ObjectAnimator.INFINITE
                    pulseAnim.repeatMode = android.animation.ObjectAnimator.REVERSE
                    pulseAnim.start()
                }
            }
            ?.start()
        
        // Show success screen for 4 seconds for better aesthetics
        handler.postDelayed({
            if (isAdded) {
                dismiss()
                onMatchSuccess(roomId, participantId, nickname, avatar)
            }
        }, 4000)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        isMatching = false
        fetchJob?.cancel()
        com.zjgsu.treehole.network.WhisperWebSocket.cancelSoulMatch()
        radarView.stop()
        handler.removeCallbacksAndMessages(null)
    }
}
