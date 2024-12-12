package com.example.jenggala.Profile

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
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
import com.example.jenggala.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.jenggala.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    lateinit var binding : FragmentProfileBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =  DataBindingUtil.inflate(layoutInflater,
            R.layout.fragment_profile, container, false)
        inItView()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun inItView() {
        val prefs = activity?.getSharedPreferences("myNewPrefs", MODE_PRIVATE)
        val editor = prefs?.edit()
        val username = prefs?.getString("username", "")
        val auth_token = prefs?.getString("auth_token", "")

        // Ambil data pengguna dari SharedPreferences
        val userNama = prefs?.getString("user_nama", null)
        val userUsername = prefs?.getString("user_username", null)
        val userEmail = prefs?.getString("user_email", null)
        val userNoHp = prefs?.getString("user_noHp", null)
        val userKabkot = prefs?.getString("user_kabkot", null)
        val userAlamat = prefs?.getString("user_alamat", null)
        val userPengawas = prefs?.getString("user_pengawas", null)

        // Jika data sudah ada, tampilkan langsung
        if (!userNama.isNullOrEmpty() && !userUsername.isNullOrEmpty()) {
            binding.profileName.text = userNama
            binding.usernameTxt.text = userUsername
            binding.emailTxt.text = userEmail
            binding.nohpTxt.text = userNoHp
            binding.kabkotTxt.text = userKabkot
            binding.alamatTxt.text = userAlamat
            binding.pengawasTxt.text = userPengawas
        } else {
            // Jika data belum ada, panggil API untuk mendapatkan data
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = RetrofitInstance.api.getUserDetails(username!!, auth_token!!)
                    val responseBody = result.body()
                    withContext(Dispatchers.Main) {
                        if (responseBody != null && !responseBody.result.isError) {
                            binding.profileName.text = responseBody.user?.nama
                            binding.usernameTxt.text = responseBody.user?.username
                            binding.emailTxt.text = responseBody.user?.email
                            binding.nohpTxt.text = responseBody.user?.noHp
                            binding.kabkotTxt.text = responseBody.user?.kabkot
                            binding.alamatTxt.text = responseBody.user?.alamat
                            binding.pengawasTxt.text = responseBody.user?.pengawas

                            // Simpan data ke SharedPreferences
                            editor?.putString("user_nama", responseBody.user?.nama)
                            editor?.putString("user_username", responseBody.user?.username)
                            editor?.putString("user_email", responseBody.user?.email)
                            editor?.putString("user_noHp", responseBody.user?.noHp)
                            editor?.putString("user_kabkot", responseBody.user?.kabkot)
                            editor?.putString("user_alamat", responseBody.user?.alamat)
                            editor?.putString("user_pengawas", responseBody.user?.pengawas)
                            editor?.apply()
                        }
                    }
                    Log.d("checkApiError", responseBody?.result?.message.toString())
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT)
                            .show()
                    }
                    Log.d("checkApiError", e.message.toString())
                }
            }
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            editor?.clear()
            editor?.apply()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            Toast.makeText(activity, "Logout Berhasil", Toast.LENGTH_SHORT).show()
        }
    }
}