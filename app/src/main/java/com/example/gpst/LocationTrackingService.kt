package com.example.gpst

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service(), LocationListener {
    
    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val MIN_DISTANCE_CHANGE = 5.0f // 5 meters minimum movement
    }
    
    private lateinit var locationManager: LocationManager
    private lateinit var notificationManager: NotificationManager
    private var lastLocation: Location? = null
    private var handler = Handler(Looper.getMainLooper())
    private var locationUpdateRunnable: Runnable? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        startLocationUpdates()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY // Restart service if killed by system
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopLocationUpdates()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GPS location tracking service"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Tracker Running")
            .setContentText("Tracking your location every 5 seconds")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            return
        }
        
        // Try to use GPS first, fallback to network
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )
        
        for (provider in providers) {
            if (locationManager.isProviderEnabled(provider)) {
                Log.d(TAG, "Starting location updates with provider: $provider")
                locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_UPDATE_INTERVAL,
                    MIN_DISTANCE_CHANGE,
                    this
                )
                break
            }
        }
        
        // Also set up periodic location requests
        startPeriodicLocationUpdates()
    }
    
    private fun startPeriodicLocationUpdates() {
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                getCurrentLocation()
                handler.postDelayed(this, LOCATION_UPDATE_INTERVAL)
            }
        }
        handler.post(locationUpdateRunnable!!)
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )
        
        for (provider in providers) {
            if (locationManager.isProviderEnabled(provider)) {
                val location = locationManager.getLastKnownLocation(provider)
                location?.let { onLocationChanged(it) }
                break
            }
        }
    }
    
    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
        locationUpdateRunnable?.let {
            handler.removeCallbacks(it)
        }
    }
    
    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
        
        // Check if we've moved enough to warrant saving
        if (shouldSaveLocation(location)) {
            saveLocationToFile(location)
            lastLocation = location
            
            // Update notification with current location
            updateNotification(location)
        }
    }
    
    private fun shouldSaveLocation(newLocation: Location): Boolean {
        lastLocation?.let { lastLoc ->
            val distance = lastLoc.distanceTo(newLocation)
            Log.d(TAG, "Distance from last location: ${distance}m")
            return distance >= MIN_DISTANCE_CHANGE
        }
        return true // First location, always save
    }
    
    private fun saveLocationToFile(location: Location) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val today = dateFormat.format(Date())
            
            // Create GPS directory in app's private storage
            val gpsDir = File(filesDir, "gps")
            if (!gpsDir.exists()) {
                gpsDir.mkdirs()
            }
            
            val fileName = "gps_$today.txt"
            val file = File(gpsDir, fileName)
            
            val timestamp = timeFormat.format(Date(location.time))
            val locationData = "$timestamp,${location.latitude},${location.longitude},${location.altitude},${location.accuracy}\n"
            
            FileWriter(file, true).use { writer ->
                writer.append(locationData)
            }
            
            Log.d(TAG, "Location saved to file: $fileName")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving location to file", e)
        }
    }
    
    private fun updateNotification(location: Location) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Tracker Running")
            .setContentText("Last: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }
    
    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Provider disabled: $provider")
    }
    
    @Deprecated("Deprecated in API level 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
        Log.d(TAG, "Provider status changed: $provider, status: $status")
    }
}
