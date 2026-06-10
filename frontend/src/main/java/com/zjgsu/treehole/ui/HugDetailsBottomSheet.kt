package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.TokenManager
import com.zjgsu.treehole.ui.widget.HugAnimationView
import com.zjgsu.treehole.util.AvatarLoader

class HugDetailsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(nickname: String, avatar: String): HugDetailsBottomSheet {
            val args = Bundle().apply {
                putString("nickname", nickname)
                putString("avatar", avatar)
            }
            return HugDetailsBottomSheet().apply { arguments = args }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_hug_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nickname = arguments?.getString("nickname") ?: "匿名用户"
        val avatar = arguments?.getString("avatar")

        val tvUsername: TextView = view.findViewById(R.id.tv_username)
        val ivAvatar: ImageView = view.findViewById(R.id.iv_user_avatar)
        val hugAnimationView: HugAnimationView = view.findViewById(R.id.hug_animation_view)
        val llHugHistory: LinearLayout = view.findViewById(R.id.ll_hug_history)
        val tvHugCount: TextView = view.findViewById(R.id.tv_hug_count)

        tvUsername.text = nickname
        AvatarLoader.loadAvatar(requireContext(), avatar ?: "", ivAvatar)
        
        // Ensure animation stays visible at the end state
        hugAnimationView.keepVisibleAtEnd = true
        hugAnimationView.playAnimation()

        // Generate realistic mock records
        val currentUserNickname = TokenManager.getNickname() ?: "我"
        
        val mockHuggers = listOf(
            currentUserNickname,
            "楠另",
            "skyrise",
            "星空下的猫",
            "风中的蒲公英",
            "助之新原野",
            "君看壹叶舟"
        )
        
        tvHugCount.text = "收到了 ${mockHuggers.size} 个「抱抱」"
        
        llHugHistory.removeAllViews()
        for (hugger in mockHuggers) {
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "$hugger 给了 $nickname 一个抱抱"
                setTextColor(android.graphics.Color.parseColor("#5C98D8"))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 36, 0, 36)
            }
            llHugHistory.addView(tv)
        }
    }
}
