package com.example.jenggala

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.navigation.NavDestination
import com.example.jenggala.databinding.ActivityMainBinding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import androidx.activity.OnBackPressedCallback
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mendapatkan NavController dari NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        // Mengatur visibilitas BottomNavigationView dan menu aktif berdasarkan fragment yang ditampilkan
        navController.addOnDestinationChangedListener { _, destination: NavDestination, _ ->
            when (destination.id) {
                R.id.homeFragment -> binding.bottomNavigationView.visibility = View.VISIBLE
                R.id.profileFragment -> binding.bottomNavigationView.visibility = View.VISIBLE
                else -> binding.bottomNavigationView.visibility = View.GONE
            }

            // Menandai item aktif di BottomNavigationView
            binding.bottomNavigationView.menu.findItem(
                when (destination.id) {
                    R.id.homeFragment -> R.id.item_home
                    R.id.profileFragment -> R.id.item_profile
                    else -> null
                } ?: return@addOnDestinationChangedListener
            ).isChecked = true
        }

        // Listener untuk navigasi di BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.item_profile -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }

        // Menyesuaikan tombol Back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == R.id.homeFragment || navController.currentDestination?.id == R.id.loginFragment) {
                    finish() // Keluar dari aplikasi jika di home
                } else {
                    navController.navigateUp() // Navigasi ke fragment sebelumnya
                }
            }
        })

        // Cek apakah intent berasal dari notifikasi
        if (intent.getBooleanExtra("navigateToTrackingFragment", false)) {
            if (navController.currentDestination?.id != R.id.trackingFragment) {
                navController.navigate(R.id.action_global_to_trackingFragment)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (it.getBooleanExtra("navigateToTrackingFragment", false)) {
                if (navController.currentDestination?.id != R.id.trackingFragment) {
                    navController.navigate(R.id.action_global_to_trackingFragment)
                }
            }
        }
    }
}