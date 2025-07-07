package com.example.ca

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 加载布局
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // 绑定视图
        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)

        // 登录按钮点击事件
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 简单验证
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    try {
                        val result = ApiService.login(username, password)
                        if (result == "Login Success") {
                            val bundle = Bundle().apply {
                                putString("username", username)
                            }
                            findNavController().navigate(R.id.action_login_to_fetch, bundle)
                            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "登录失败：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        return view
    }
}