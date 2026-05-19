package com.zjgsu.treehole

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.zjgsu.treehole.ui.ClickAnimations

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    // Custom tab views
    private lateinit var tabFeed: LinearLayout
    private lateinit var tabExplore: LinearLayout
    private lateinit var tabEchoes: LinearLayout
    private lateinit var tabMy: LinearLayout

    private lateinit var tabFeedIcon: ImageView
    private lateinit var tabExploreIcon: ImageView
    private lateinit var tabEchoesIcon: ImageView
    private lateinit var tabMyIcon: ImageView

    private lateinit var tabFeedLabel: TextView
    private lateinit var tabExploreLabel: TextView
    private lateinit var tabEchoesLabel: TextView
    private lateinit var tabMyLabel: TextView

    private lateinit var bottomNavContainer: View
    private lateinit var fabPostWrapper: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Find custom tab views
        bottomNavContainer = findViewById(R.id.bottom_nav_container)
        fabPostWrapper = findViewById(R.id.fab_post_wrapper)

        tabFeed = findViewById(R.id.tab_feed)
        tabExplore = findViewById(R.id.tab_explore)
        tabEchoes = findViewById(R.id.tab_echoes)
        tabMy = findViewById(R.id.tab_my)

        tabFeedIcon = findViewById(R.id.tab_feed_icon)
        tabExploreIcon = findViewById(R.id.tab_explore_icon)
        tabEchoesIcon = findViewById(R.id.tab_echoes_icon)
        tabMyIcon = findViewById(R.id.tab_my_icon)

        tabFeedLabel = findViewById(R.id.tab_feed_label)
        tabExploreLabel = findViewById(R.id.tab_explore_label)
        tabEchoesLabel = findViewById(R.id.tab_echoes_label)
        tabMyLabel = findViewById(R.id.tab_my_label)

        // Set click listeners for tabs
        tabFeed.setOnClickListener { navigateToTab(R.id.nav_feed) }
        tabExplore.setOnClickListener { navigateToTab(R.id.nav_explore) }
        tabEchoes.setOnClickListener { navigateToTab(R.id.nav_echoes) }
        tabMy.setOnClickListener { navigateToTab(R.id.nav_my_hollow) }

        // Set initial selected state
        updateTabSelection(R.id.nav_feed)

        // Wire the custom central Floating Action Button
        val fabPost = findViewById<View>(R.id.fab_post)

        // Add FAB press animation
        ClickAnimations.addFabPressAnimation(fabPost)

        fabPost.setOnClickListener {
            navController.navigate(R.id.nav_post)
        }

        // Listen for destination changes to update tab highlights & visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.secretDetailFragment, R.id.whisperChatFragment, R.id.nav_post,
                R.id.loginFragment -> {
                    bottomNavContainer.visibility = View.GONE
                }
                else -> {
                    bottomNavContainer.visibility = View.VISIBLE
                    updateTabSelection(destination.id)
                }
            }
        }

        // ── API 联调测试 (暂时禁用，等待后端接入) ──
        // lifecycleScope.launch {
        //     try {
        //         val response = RetrofitClient.postsApi.getFeed()
        //         if (response.isSuccessful) {
        //             val data = response.body().toString()
        //             Log.d("API_TEST", "✅ 请求成功！拿到的树洞数据是: $data")
        //         } else {
        //             Log.e("API_TEST", "❌ 请求失败，状态码: ${response.code()}")
        //         }
        //     } catch (e: Exception) {
        //         Log.e("API_TEST", "❌ 网络异常: ${e.message}")
        //     }
        // }
    }

    private fun navigateToTab(destinationId: Int) {
        val currentDest = navController.currentDestination?.id
        if (currentDest != destinationId) {
            navController.navigate(destinationId, null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_feed, false)
                    .setLaunchSingleTop(true)
                    .build()
            )
        }
    }

    private fun updateTabSelection(selectedId: Int) {
        val goldColor = ContextCompat.getColor(this, R.color.gold_primary)
        val mutedColor = ContextCompat.getColor(this, R.color.text_muted)

        // Reset all tabs to inactive
        tabFeedIcon.setColorFilter(mutedColor)
        tabExploreIcon.setColorFilter(mutedColor)
        tabEchoesIcon.setColorFilter(mutedColor)
        tabMyIcon.setColorFilter(mutedColor)

        tabFeedLabel.setTextColor(mutedColor)
        tabExploreLabel.setTextColor(mutedColor)
        tabEchoesLabel.setTextColor(mutedColor)
        tabMyLabel.setTextColor(mutedColor)

        tabFeedLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
        tabExploreLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
        tabEchoesLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
        tabMyLabel.setTypeface(null, android.graphics.Typeface.NORMAL)

        // Highlight the selected tab
        when (selectedId) {
            R.id.nav_feed -> {
                tabFeedIcon.setColorFilter(goldColor)
                tabFeedLabel.setTextColor(goldColor)
                tabFeedLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            R.id.nav_explore -> {
                tabExploreIcon.setColorFilter(goldColor)
                tabExploreLabel.setTextColor(goldColor)
                tabExploreLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            R.id.nav_echoes -> {
                tabEchoesIcon.setColorFilter(goldColor)
                tabEchoesLabel.setTextColor(goldColor)
                tabEchoesLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            R.id.nav_my_hollow -> {
                tabMyIcon.setColorFilter(goldColor)
                tabMyLabel.setTextColor(goldColor)
                tabMyLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }
}
