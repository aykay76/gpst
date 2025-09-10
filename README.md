# GPS Tracker (GPST)

A simple Android application that tracks your GPS location every 5 seconds and stores it locally on your phone. The app runs in the background and only saves location updates when you've moved more than 5 meters to avoid duplicate entries.

## Features

- **Background GPS Tracking**: Runs as a foreground service to continuously track location
- **Smart Movement Detection**: Only saves locations when you've moved more than 5 meters
- **Daily File Rotation**: Creates a new GPS file each day (format: `gps_YYYY-MM-DD.txt`)
- **Auto-Start on Boot**: Automatically starts tracking when your phone boots up
- **Persistent Notification**: Shows current location in the notification bar
- **Privacy-First**: All data stored locally on your device

## File Format

GPS data is stored in CSV format with the following columns:
```
timestamp,latitude,longitude,altitude,accuracy
2023-12-01 10:30:15,37.7749,-122.4194,45.2,3.0
```

## Installation

1. Open the project in Android Studio
2. Build and install the APK on your Android device
3. Grant the required permissions when prompted:
   - Fine Location Access
   - Coarse Location Access
   - Background Location Access (Android 10+)

## Usage

1. **Start Tracking**: Tap "Start Tracking" to begin GPS logging
2. **Stop Tracking**: Tap "Stop Tracking" to stop GPS logging
3. **View File Info**: The main screen shows information about your GPS files
4. **Background Operation**: The app will continue tracking even when minimized

## File Locations

GPS files are stored in the app's private directory:
- Path: `/data/data/com.example.gpst/files/gps/`
- Format: `gps_YYYY-MM-DD.txt`
- Each line contains: timestamp, latitude, longitude, altitude, accuracy

## Permissions Required

- `ACCESS_FINE_LOCATION`: For precise GPS coordinates
- `ACCESS_COARSE_LOCATION`: For network-based location
- `ACCESS_BACKGROUND_LOCATION`: For tracking when app is not active (Android 10+)
- `FOREGROUND_SERVICE`: To run background service
- `FOREGROUND_SERVICE_LOCATION`: For location-based foreground service
- `WAKE_LOCK`: To keep service running
- `RECEIVE_BOOT_COMPLETED`: To start service on device boot

## Technical Details

- **Update Interval**: 5 seconds
- **Minimum Movement**: 5 meters
- **Service Type**: Foreground service with persistent notification
- **Data Format**: CSV with timestamp, coordinates, altitude, and accuracy
- **File Rotation**: Daily (based on date)

## Privacy & Security

- All GPS data is stored locally on your device
- No data is transmitted to external servers
- GPS files are excluded from cloud backups
- You have full control over your location data

## Building from Source

1. Clone this repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on your Android device

## Requirements

- Android 7.0 (API level 24) or higher
- GPS/Location services enabled
- Sufficient storage space for GPS logs

## Troubleshooting

- **Service not starting**: Check that all location permissions are granted
- **No location updates**: Ensure GPS is enabled and you're not indoors
- **Service stops**: Make sure battery optimization is disabled for the app
- **Permission denied**: Grant all location permissions, including background location

## License

This project is open source. Feel free to modify and distribute as needed.
