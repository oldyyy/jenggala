package com.example.jenggala.Home.Tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.jenggala.MainActivity
import com.example.jenggala.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng

class TrackingService : Service() {

    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isRunning = false

    private lateinit var handler: Handler
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private val NOTIFICATION_CHANNEL_ID = "tracking_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize handler for timer updates
        handler = Handler(mainLooper)

        // Create Notification Channel
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startForegroundService()
        }
        when (intent?.action) {
            "START_TRACKING" -> startTracking()
            "PAUSE_TRACKING" -> pauseTracking()
            "RESUME_TRACKING" -> resumeTracking()
            "FINISH_TRACKING" -> finishTracking()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking in Progress")
            .setContentText("Tracking your location and activity")
            .setSmallIcon(R.drawable.logo_jenggala)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startTracking() {
        if (!isRunning) {
            startTime = SystemClock.elapsedRealtime()
            isRunning = true
            updateNotification("Tracking started")
            handler.post(timerRunnable)
        }
    }

    private fun pauseTracking() {
        if (isRunning) {
            elapsedTime = SystemClock.elapsedRealtime() - startTime
            isRunning = false
            handler.removeCallbacks(timerRunnable)
            updateNotification("Tracking paused")
        }
    }

    private fun resumeTracking() {
        if (!isRunning) {
            startTime = SystemClock.elapsedRealtime() - elapsedTime
            isRunning = true
            handler.post(timerRunnable)
            updateNotification("Tracking resumed")
        }
    }

    private fun finishTracking() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            elapsedTime = SystemClock.elapsedRealtime() - startTime
            val formattedTime = formatElapsedTime(elapsedTime)
            updateNotification("Elapsed time: $formattedTime")

            // Simpan lokasi saat ini (opsional)
            updateCurrentLocation()

            if (isRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun updateCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    // Anda bisa menyimpan atau mengolah lokasi ini
                }
            }
        } else {
            // Handle the case where the permission is not granted
        }
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        val hours = elapsedTime / 3600000
        val minutes = (elapsedTime / 60000) % 60
        val seconds = (elapsedTime / 1000) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateNotification(contentText: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigateToTrackingFragment", true) // Tambahkan informasi untuk navigasi
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking in Progress")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.logo_jenggala)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Jangan biarkan notifikasi dihapus manual
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Tracking Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        finishTracking() // Hentikan semua proses dan layanan
    }

    override fun onBind(intent: Intent?): IBinder? = null
}