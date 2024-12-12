package com.example.jenggala.Home.Tracking

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.jenggala.databinding.FragmentTrackingBinding
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.example.jenggala.API.Responden
import com.example.jenggala.API.RetrofitInstance
import com.example.jenggala.Home.RespondenService.RespondenInfoDialogFragment
import com.example.jenggala.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackingFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentTrackingBinding
    private val args: TrackingFragmentArgs by navArgs()
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isRunning = false

    private var startLatLng: LatLng? = null
    private var finishLatLng: LatLng? = null

    private var startTimestamp: Long? = null
    private var finishTimestamp: Long? = null

    private lateinit var progressOverlay: View
    private lateinit var progressBar: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Akses dari notifikasi
//        val fromNotification = arguments?.getBoolean("fromNotification") ?: false

        // Tambahkan overlay progress
        progressOverlay = binding.root.findViewById(R.id.progress_overlay)
        progressBar = binding.root.findViewById(R.id.progressBar)

        // Tambahkan menu info responden
        setHasOptionsMenu(true)

        // Data Responden
        val responden = args.responden
        Log.d("TrackingFragment", "Responden: $responden")

        // Toolbar setup
        (binding.toolbarTracking as androidx.appcompat.widget.Toolbar).apply {
            (requireActivity() as AppCompatActivity).setSupportActionBar(this)
        }
        binding.toolbarTracking.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Initialize fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Initialize map
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        // Check permissions
        requestPermissionsIfNeeded()

        // Setup button listeners
        setupButtonListeners()

        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelTrackingDialog()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.info_responden_tracking, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                val responden = args.responden
                showRespondenInfoDialog(responden)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getKodePetugas(): String {
        val prefs = activity?.getSharedPreferences("myNewPrefs", MODE_PRIVATE)
        return prefs?.getString("kode_petugas", "") ?: ""
    }

    private fun showRespondenInfoDialog(responden: Responden) {
        val dialog = RespondenInfoDialogFragment()

        // Pass data to dialog using arguments
        val args = Bundle().apply {
            putString("nama", responden.nama_perusahaan)
            putString("alamat", responden.alamat_perusahaan)
            putString("noHp", responden.no_telepon)
        }
        dialog.arguments = args

        // Show the dialog
        dialog.show(parentFragmentManager, "RespondenInfoDialog")
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsNeeded = mutableListOf<String>()

        // Cek izin lokasi
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Cek izin notifikasi (hanya untuk API 33 atau lebih baru)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Minta semua izin yang belum diberikan
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsNeeded.toTypedArray(), 100)
        }
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupButtonListeners() {
        binding.btnToggleTrack.setOnClickListener {
            if (!isRunning) {
                if (startLatLng == null) {
                    startTracking()
                    sendCommandToService("START_TRACKING") // Memulai layanan
                } else {
                    resumeTracking()
                    sendCommandToService("RESUME_TRACKING") // Melanjutkan layanan
                }
            } else {
                stopTracking()
                sendCommandToService("PAUSE_TRACKING") // Menghentikan layanan
            }
        }

        binding.btnFinishTrack.setOnClickListener {
            if (isRunning) stopTracking() // Pastikan tracking dihentikan
            showFinishConfirmationDialog() // Tampilkan dialog konfirmasi
        }
    }

    private fun startTracking() {
        binding.btnToggleTrack.text = "Stop"
        binding.btnFinishTrack.visibility = View.GONE

        startTime = SystemClock.elapsedRealtime()
        isRunning = true
        binding.tvTimer.post(timerRunnable)

        startTimestamp = System.currentTimeMillis()
        Log.d("TrackingFragment", "Start Timestamp: $startTimestamp")

        getCurrentLocation { latLng ->
            startLatLng = latLng
            googleMap.addMarker(MarkerOptions().position(latLng).title("Start"))
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            Log.d("TrackingFragment", "Start LatLng: $latLng")
        }
    }

    private fun resumeTracking() {
        startTime = SystemClock.elapsedRealtime()
        isRunning = true
        binding.tvTimer.post(timerRunnable)

        binding.btnToggleTrack.text = "Stop"
        binding.btnFinishTrack.visibility = View.GONE
    }

    private fun stopTracking() {
        elapsedTime += SystemClock.elapsedRealtime() - startTime
        isRunning = false
        binding.tvTimer.removeCallbacks(timerRunnable)

        binding.btnToggleTrack.text = "Resume"
        binding.btnFinishTrack.visibility = View.VISIBLE
    }

    private fun finishTracking() {
        isRunning = false
        binding.tvTimer.removeCallbacks(timerRunnable)

        finishTimestamp = System.currentTimeMillis()

        val startDateTime = startTimestamp?.let { formatTimestamp(it) }
        val finishDateTime = finishTimestamp?.let { formatTimestamp(it) }

        getCurrentLocation { latLng ->
            finishLatLng = latLng
            googleMap.addMarker(MarkerOptions().position(latLng).title("Finish"))
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            val totalTime = formatElapsedTime(elapsedTime)
        }

        binding.btnToggleTrack.text = "Start"
        binding.btnFinishTrack.visibility = View.GONE
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsedTimeNow = elapsedTime + (SystemClock.elapsedRealtime() - startTime)
            binding.tvTimer.text = formatElapsedTime(elapsedTimeNow)
            if (isRunning) binding.tvTimer.postDelayed(this, 1000)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return format.format(date)
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        val hours = elapsedTime / 3600000
        val minutes = (elapsedTime / 60000) % 60
        val seconds = (elapsedTime / 1000) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun getCurrentLocation(callback: (LatLng) -> Unit) {
        try {
            if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        callback(latLng)
                    } else {
                        Toast.makeText(requireContext(), "Failed to get current location", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Error retrieving location: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e("TrackingFragment", "SecurityException: ${e.message}")
            Toast.makeText(requireContext(), "Security error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommandToService(action: String) {
        val intent = Intent(requireContext(), TrackingService::class.java)
        intent.action = action
        requireContext().startService(intent)
    }

    private fun showCancelTrackingDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin membatalkan tracking? Data tracking tidak akan tersimpan.")
            .setPositiveButton("Ya") { _, _ ->
                isRunning = false // Hentikan tracking
                sendCommandToService("FINISH_TRACKING") // Hentikan layanan
                findNavController().navigateUp() // Kembali ke halaman sebelumnya
            }
            .setNegativeButton("Tidak", null) // Tutup dialog jika "Tidak" dipilih
            .setCancelable(false)
            .show()
    }

    private fun showFinishConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin mengakhiri tracking?")
            .setPositiveButton("Ya") { _, _ ->
                finishTracking()
                sendCommandToService("FINISH_TRACKING")
                showStatusDialog() // Tampilkan dialog untuk status pendataan
            }
            .setNegativeButton("Tidak") { _, _ ->
                resumeTracking() // Jika tidak, waktu kembali berjalan
                sendCommandToService("RESUME_TRACKING")
            }
            .setCancelable(false)
            .show()
    }

    private fun showStatusDialog() {
        val statuses = arrayOf("Berhasil", "Menolak", "Reschedule")
        AlertDialog.Builder(requireContext())
            .setTitle("Status Pendataan")
            .setItems(statuses) { _, which ->
                val kodeStatus = when (which) {
                    0 -> 1 // Berhasil
                    1 -> 2 // Menolak
                    2 -> 3 // Reschedule
                    else -> 0
                }
                showConfirmStatusDialog(kodeStatus)
            }
            .setCancelable(false)
            .show()
    }

    private fun showConfirmStatusDialog(kodeStatus: Int) {
        val dialog_confirm = AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Status")
            .setMessage(
                when (kodeStatus) {
                    1 -> "Apakah Anda yakin pendataan BERHASIL?"
                    2 -> "Apakah Anda yakin pendataan DITOLAK?"
                    3 -> "Apakah Anda yakin pendataan DIRESCHEDULE?"
                    else -> "Apakah Anda yakin?"
                }
            )
            .setPositiveButton("Ya") { _, _ ->
                when (kodeStatus) {
                    2 -> showKeteranganDialog(kodeStatus) // Jika kodeStatus = 2, tampilkan dialog keterangan
                    3 -> showKeteranganDialog(kodeStatus) // Jika kodeStatus = 3, tampilkan dialog keterangan
                    else -> saveStatus(kodeStatus, null)
                }
            }
            .setNegativeButton("Tidak", null)
            .setCancelable(false)
            .create()

        dialog_confirm.setOnShowListener {
            val cancelButton = dialog_confirm.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancelButton.setOnClickListener {
                dialog_confirm.dismiss()
                showStatusDialog()
            }
        }

        dialog_confirm.show()
    }

    private fun showKeteranganDialog(kodeStatus: Int) {
        val inputEditText = android.widget.EditText(requireContext()).apply {
            hint = "Masukkan keterangan"
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val dialog_keterangan = AlertDialog.Builder(requireContext())
            .setTitle(
                when (kodeStatus) {
                    2 -> "Keterangan Menolak"
                    3 -> "Keterangan Reschedule"
                    else -> "Keterangan"
                }
            )
            .setView(inputEditText)
            .setPositiveButton("Simpan", null) // Tambahkan listener nanti
            .setNegativeButton("Batal", null) // Tambahkan aksi "Batal" untuk kembali
            .setCancelable(false)
            .create()

        dialog_keterangan.setOnShowListener {
            // Aksi untuk tombol "Simpan"
            val saveButton = dialog_keterangan.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val keterangan = inputEditText.text.toString()
                if (keterangan.isNotBlank()) {
                    saveStatus(kodeStatus, keterangan) // Simpan dengan keterangan
                    dialog_keterangan.dismiss() // Tutup dialog jika sukses
                } else {
                    inputEditText.error = "Keterangan tidak boleh kosong!" // Tampilkan error
                }
            }

            // Aksi untuk tombol "Batal"
            val cancelButton = dialog_keterangan.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancelButton.setOnClickListener {
                dialog_keterangan.dismiss() // Tutup dialog
                showStatusDialog() // Kembali ke status dialog
            }
        }

        dialog_keterangan.show()
    }

    // Fungsi untuk menampilkan ProgressBar
    private fun showProgress() {
        progressOverlay.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        binding.root.findViewById<TextView>(R.id.progressText).visibility = View.VISIBLE
    }

    // Fungsi untuk menyembunyikan ProgressBar
    private fun hideProgress() {
        progressOverlay.visibility = View.GONE
        progressBar.visibility = View.GONE
        binding.root.findViewById<TextView>(R.id.progressText).visibility = View.GONE

    }

    private fun saveStatus(kodeStatus: Int, keterangan: String?) {
        Log.d("TrackingFragment", "Kode Status Pendataan: $kodeStatus, Keterangan: $keterangan")
        saveToDatabase(kodeStatus, keterangan) // Simpan ke database
    }

    private fun saveToDatabase(kodeStatus: Int, keterangan: String?) {
        val kodeResponden = args.responden.kode_responden ?: ""
        val kodePetugas = getKodePetugas()
        val kodeKegiatan = args.responden.kode_kegiatan ?: ""
        val startTimestamp = startTimestamp?.let { formatTimestamp(it) } ?: ""
        val finishTimestamp = finishTimestamp?.let { formatTimestamp(it) } ?: ""
        val totalTime = formatElapsedTime(elapsedTime)
        val startLat = startLatLng?.latitude ?: 0.0
        val startLng = startLatLng?.longitude ?: 0.0
        val finishLat = finishLatLng?.latitude ?: 0.0
        val finishLng = finishLatLng?.longitude ?: 0.0

        Log.d("TrackingFragment", "Keterangan: $keterangan")

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                showProgress() // Tampilkan ProgressBar
            }
            try {
                val response = RetrofitInstance.api.insertTracking(
                    kodeResponden, kodePetugas, kodeKegiatan, startTimestamp, finishTimestamp, totalTime,
                    startLat, startLng, finishLat, finishLng, kodeStatus, keterangan
                )

                Log.d("RawResponse", response.raw().toString())

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val message = body?.result?.message
                        Toast.makeText(requireContext(), message ?: "Berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        Log.d("TrackingFragment", "Status berhasil disimpan: $body")
                        findNavController().navigateUp()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(requireContext(), "Gagal menyimpan status!", Toast.LENGTH_SHORT).show()
                        Log.e("TrackingFragment", "Error: $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal menghubungi server: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("TrackingFragment", "Failure: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    hideProgress() // Sembunyikan ProgressBar
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {
                googleMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.e("TrackingFragment", "SecurityException: ${e.message}")
            }
        } else {
            Toast.makeText(requireContext(), "Location permission required to show your location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Reattach the back press callback when Fragment resumes
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isRunning) {
                    showCancelTrackingDialog()
                } else {
                    findNavController().navigateUp()
                }
            }
        })

        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}