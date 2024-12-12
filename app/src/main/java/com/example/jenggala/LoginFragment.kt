package com.example.jenggala

import android.content.Context.MODE_PRIVATE
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.jenggala.API.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.jenggala.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        initViews()

        return binding.root
    }

    private fun initViews() {
        val prefs = activity?.getSharedPreferences("myNewPrefs", MODE_PRIVATE)
        val editor = prefs?.edit()

        // Jika token sudah ada, arahkan langsung ke HomeFragment
        if (!prefs?.getString("auth_token", "").isNullOrEmpty()) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // Setup click listener untuk tombol login
        binding.btnLogin.setOnClickListener {
            val username = binding.inputUsername.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            var isValid = true
            binding.usernameInputLayout.error = null
            binding.passwordInputLayout.error = null

            // Validasi Username
            if (username.isEmpty()) {
                binding.usernameInputLayout.error = "Username wajib diisi"
                isValid = false
            }

            // Validasi Password
            if (password.isEmpty()) {
                binding.passwordInputLayout.error = "Password wajib diisi"
                isValid = false
            } else if (password.length < 6) {
                binding.passwordInputLayout.error = "Password minimal 6 karakter"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // Eksekusi proses login dengan coroutine jika valid
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = RetrofitInstance.api.loginUser(username, password)
                    val responseBody = result.body()

                    if (result.isSuccessful && responseBody != null) {
                        withContext(Dispatchers.Main) {
                            if (!responseBody.result.isError && responseBody.user != null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(activity, responseBody.result.message, Toast.LENGTH_SHORT).show()
                                }
                                editor?.putString("username", username)
                                editor?.putString("auth_token", responseBody.user?.auth_token)
                                editor?.apply()
                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                            } else {
                                binding.usernameInputLayout.error = "Username / Password Salah"
                                binding.passwordInputLayout.error = "Username / Password Salah"
                                Log.d("checkApiError", responseBody.result.message)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("LoginError", "Exception during login", e)
                        Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}