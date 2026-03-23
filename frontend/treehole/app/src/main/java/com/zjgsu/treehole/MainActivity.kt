package com.zjgsu.treehole

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zjgsu.treehole.ui.PostSecretFragment

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

        // Wire the custom central Floating Action Button
        val fabPost = findViewById<View>(R.id.fab_post)
        fabPost.setOnClickListener {
            navController.navigate(R.id.nav_post)
        }

        // Hide bottom nav and FAB on detail/chat screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.secretDetailFragment, R.id.whisperChatFragment, R.id.nav_post -> {
                    bottomNav.visibility = View.GONE
                    fabPost.visibility = View.GONE
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                    fabPost.visibility = View.VISIBLE
                }
            }
        }
    }
}