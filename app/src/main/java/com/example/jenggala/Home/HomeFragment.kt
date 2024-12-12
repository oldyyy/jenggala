package com.example.jenggala.Home

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
import com.example.jenggala.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    lateinit var binding : FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =  DataBindingUtil.inflate(layoutInflater, R.layout.fragment_home, container, false)
        inItView()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun inItView() {
        val prefs = activity?.getSharedPreferences("myNewPrefs", MODE_PRIVATE)
        val editor = prefs?.edit()
        val username = prefs?.getString("username", "")
        val auth_token = prefs?.getString("auth_token", "")
        val nama = prefs?.getString("user_nama", null)
        val kabkot = prefs?.getString("user_kabkot", null)
        val kode_petugas = prefs?.getString("kode_petugas", null)

        // Jika data sudah ada di SharedPreferences, gunakan data tersebut
        if (!nama.isNullOrEmpty() && !kabkot.isNullOrEmpty()) {
            binding.nameProfileHome.text = nama
            binding.kabkotProfileHome.text = kabkot
        } else {
            // Jika data tidak ada, panggil Retrofit untuk mengambil data
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = RetrofitInstance.api.getUserDetails(username!!, auth_token!!)
                    val responseBody = result.body()
                    Log.d("checkApiError", responseBody?.result?.message.toString())
                    if (result.isSuccessful && responseBody != null) {
                        withContext(Dispatchers.Main) {
                            if (!responseBody.result.isError) {
                                binding.nameProfileHome.text = responseBody.user?.nama
                                binding.kabkotProfileHome.text = responseBody.user?.kabkot
                                editor?.putString("user_nama", responseBody.user?.nama)
                                editor?.putString("user_kabkot", responseBody.user?.kabkot)
                                editor?.putString("kode_petugas", responseBody.user?.kode_petugas)
                                editor?.apply()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT)
                                .show()
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

        val cardSHP = binding.cardMenu
        cardSHP.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_SHPFragment)
        }

        val cardSHPB = binding.cardMenu2
        cardSHPB.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_SHPBFragment)
        }
    }
}