package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.zjgsu.treehole.R
import com.zjgsu.treehole.cache.PostCacheManager
import com.zjgsu.treehole.network.*
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var isLoginMode = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Views
        val tvFormTitle = view.findViewById<TextView>(R.id.tv_form_title)
        val tvToggleText = view.findViewById<TextView>(R.id.tv_toggle_text)
        val tvToggleAction = view.findViewById<TextView>(R.id.tv_toggle_action)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tv_forgot_password)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit)
        val llConfirmPassword = view.findViewById<LinearLayout>(R.id.ll_confirm_password_field)

        val etUsername = view.findViewById<EditText>(R.id.et_username)
        val etPassword = view.findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = view.findViewById<EditText>(R.id.et_confirm_password)

        // Button animations removed for stability

        // Toggle between login and register mode
        fun updateUI() {
            if (isLoginMode) {
                tvFormTitle.text = "欢迎回来"
                tvFormTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
                btnSubmit.text = "登录"
                tvToggleText.text = "还没有账号？"
                tvToggleAction.text = "立即注册"
                tvForgotPassword.visibility = View.VISIBLE
                llConfirmPassword.visibility = View.GONE
            } else {
                tvFormTitle.text = "创建账号"
                tvFormTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_primary))
                btnSubmit.text = "注册"
                tvToggleText.text = "已有账号？"
                tvToggleAction.text = "立即登录"
                tvForgotPassword.visibility = View.GONE
                llConfirmPassword.visibility = View.VISIBLE
            }
        }

        tvToggleAction.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }

        // Submit button
        btnSubmit.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(requireContext(), "请输入账号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "请输入密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isLoginMode) {
                val confirmPassword = etConfirmPassword.text.toString()
                if (confirmPassword != password) {
                    Toast.makeText(requireContext(), "两次密码输入不一致", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Disable button and show loading
            btnSubmit.isEnabled = false
            btnSubmit.text = if (isLoginMode) "登录中..." else "注册中..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = if (isLoginMode) {
                        RetrofitClient.authApi.login(LoginRequest(username, password))
                    } else {
                        RetrofitClient.authApi.register(RegisterRequest(username, password))
                    }

                    if (response.isSuccessful) {
                        val body = response.body()!!
                        // Save token and user info
                        val token = body.token ?: return@launch
                        val user = body.user ?: return@launch
                        PostCacheManager.clearCache()
                        TokenManager.saveToken(token)
                        TokenManager.saveUser(
                            user.id,
                            user.username,
                            user.nickname,
                            user.avatar
                        )

                        Toast.makeText(
                            requireContext(),
                            if (isLoginMode) "登录成功 ✨" else "注册成功 ✨",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Check if first login (both nickname and avatar must be set to skip setup)
                        val isFirstLogin = user.nickname.isNullOrEmpty() && user.avatar.isNullOrEmpty()

                        if (isFirstLogin) {
                            // Navigate to setup profile
                            findNavController().navigate(R.id.action_login_to_setup_profile)
                        } else {
                            // Navigate to main app (feed)
                            findNavController().navigate(R.id.nav_feed, null,
                                androidx.navigation.NavOptions.Builder()
                                    .setPopUpTo(R.id.loginFragment, true)
                                    .setLaunchSingleTop(true)
                                    .build()
                            )
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val authResponse = try {
                            com.google.gson.Gson().fromJson(errorBody, AuthResponse::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        val message = authResponse?.message ?: when (response.code()) {
                            409 -> "账号在其他设备登录中，请先退出后重试"
                            else -> "请求失败"
                        }

                        // If force login is available, show dialog to user
                        if (response.code() == 409 && authResponse?.forceLoginAvailable == true) {
                            showForceLoginDialog(username, password, message)
                        } else {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = if (isLoginMode) "登录" else "注册"
                }
            }
        }

        // Forgot password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "请联系客服找回密码", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showForceLoginDialog(username: String, password: String, originalMessage: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("提示")
            .setMessage("$originalMessage\n\n是否强制登录？（将踢掉其他设备的登录）")
            .setPositiveButton("强制登录") { _, _ ->
                doForceLogin(username, password)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doForceLogin(username: String, password: String) {
        val btnSubmit = view?.findViewById<Button>(R.id.btn_submit)
        btnSubmit?.isEnabled = false
        btnSubmit?.text = "登录中..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApi.login(LoginRequest(username, password, forceLogin = true))

                if (response.isSuccessful) {
                    val body = response.body()!!
                    val token = body.token ?: return@launch
                    val user = body.user ?: return@launch
                    PostCacheManager.clearCache()
                    TokenManager.saveToken(token)
                    TokenManager.saveUser(
                        user.id,
                        user.username,
                        user.nickname,
                        user.avatar
                    )

                    Toast.makeText(requireContext(), "强制登录成功", Toast.LENGTH_SHORT).show()

                    findNavController().navigate(R.id.nav_feed, null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                } else {
                    Toast.makeText(requireContext(), "强制登录失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSubmit?.isEnabled = true
                btnSubmit?.text = "登录"
            }
        }
    }
}
