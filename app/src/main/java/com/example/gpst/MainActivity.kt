package com.example.gpst

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
    }
    
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusText: TextView
    private lateinit var fileInfoText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        updateUI()
        
        // Check and request permissions
        checkPermissions()
    }
    
    private fun initViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        fileInfoText = findViewById(R.id.fileInfoText)
    }
    
    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (hasAllPermissions()) {
                startLocationService()
            } else {
                requestPermissions()
            }
        }
        
        stopButton.setOnClickListener {
            stopLocationService()
        }
    }
    
    private fun checkPermissions() {
        if (!hasAllPermissions()) {
            requestPermissions()
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        return fineLocation && coarseLocation && backgroundLocation
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Request background location permission separately (required for Android 10+)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Location permissions granted, now request background permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                        )
                    } else {
                        Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Location permissions are required", Toast.LENGTH_LONG).show()
                }
            }
            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Background location permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Background location permission is required for continuous tracking", Toast.LENGTH_LONG).show()
                }
            }
        }
        updateUI()
    }
    
    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        Toast.makeText(this, "GPS tracking started", Toast.LENGTH_SHORT).show()
        updateUI()
    }
    
    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        stopService(serviceIntent)
        
        Toast.makeText(this, "GPS tracking stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }
    
    private fun updateUI() {
        if (hasAllPermissions()) {
            startButton.isEnabled = true
            stopButton.isEnabled = true
            statusText.text = "Ready to track GPS location"
        } else {
            startButton.isEnabled = false
            stopButton.isEnabled = false
            statusText.text = "Location permissions required"
        }
        
        updateFileInfo()
    }
    
    private fun updateFileInfo() {
        try {
            val gpsDir = File(filesDir, "gps")
            if (gpsDir.exists()) {
                val files = gpsDir.listFiles()?.sortedByDescending { it.lastModified() }
                
                if (files?.isNotEmpty() == true) {
                    val latestFile = files.first()
                    val fileSize = latestFile.length()
                    val lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(latestFile.lastModified()))
                    
                    val lineCount = latestFile.readLines().size
                    
                    fileInfoText.text = """
                        GPS Files: ${files.size}
                        Latest: ${latestFile.name}
                        Size: ${fileSize} bytes
                        Locations: $lineCount
                        Updated: $lastModified
                    """.trimIndent()
                } else {
                    fileInfoText.text = "No GPS files found"
                }
            } else {
                fileInfoText.text = "GPS directory not created yet"
            }
        } catch (e: Exception) {
            fileInfoText.text = "Error reading file info: ${e.message}"
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
