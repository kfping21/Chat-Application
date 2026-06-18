package com.zjgsu.treehole

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zjgsu.treehole.network.RetrofitClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Disable the center placeholder item so it can't be selected
        bottomNav.menu.findItem(R.id.nav_post_placeholder)?.isEnabled = false

        // Make the center placeholder item invisible (FAB overlays it)
        bottomNav.post {
            val menuView = bottomNav.getChildAt(0) as? android.view.ViewGroup
            menuView?.let { mv ->
                val centerIndex = 2 // 0-indexed, the 3rd item
                if (centerIndex < mv.childCount) {
                    mv.getChildAt(centerIndex).visibility = View.INVISIBLE
                }
            }
        }

        // Wire the custom central Floating Action Button wrapper
        val fabPostWrapper = findViewById<View>(R.id.fab_post_wrapper)
        val fabPost = findViewById<View>(R.id.fab_post)

        fabPost.setOnClickListener {
            navController.navigate(R.id.nav_post)
        }

        // Hide bottom nav and FAB on detail/chat screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.secretDetailFragment, R.id.whisperChatFragment, R.id.nav_post -> {
                    bottomNav.visibility = View.GONE
                    fabPostWrapper.visibility = View.GONE
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                    fabPostWrapper.visibility = View.VISIBLE
                }
            }
        }

        // ── API 联调测试：启动时拉取树洞广场数据 ──
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.postsApi.getFeed()
                if (response.isSuccessful) {
                    val data = response.body().toString()
                    Log.d("API_TEST", "✅ 请求成功！拿到的树洞数据是: $data")
                } else {
                    Log.e("API_TEST", "❌ 请求失败，状态码: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API_TEST", "❌ 网络异常: ${e.message}")
            }
        }
    }
}
