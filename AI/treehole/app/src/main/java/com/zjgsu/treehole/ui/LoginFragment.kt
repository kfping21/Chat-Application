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
import androidx.navigation.fragment.findNavController
import com.zjgsu.treehole.R
import com.zjgsu.treehole.ui.ClickAnimations

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

        // Add button animations
        ClickAnimations.addButtonPressAnimation(btnSubmit)
        ClickAnimations.addButtonPressAnimation(tvToggleAction)

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

            // Simulate login/register
            btnSubmit.isEnabled = false
            btnSubmit.text = if (isLoginMode) "登录中..." else "注册中..."

            btnSubmit.postDelayed({
                if (isLoginMode) {
                    Toast.makeText(requireContext(), "登录成功 ✨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "注册成功 ✨", Toast.LENGTH_SHORT).show()
                }

                // Navigate to main app (feed)
                findNavController().navigate(R.id.nav_feed)
            }, 1000)
        }

        // Forgot password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "请联系客服找回密码", Toast.LENGTH_SHORT).show()
        }
    }
}
