/Users/vanilla/Library/Android/sdk/platform-tools/adb devices

/Users/vanilla/Library/Android/sdk/platform-tools/adb -s ZY22G35DWX shell "run-as com.example.gpst ls /data/data/com.example.gpst/files/gps"

/Users/vanilla/Library/Android/sdk/platform-tools/adb -s ZY22G35DWX shell "run-as com.example.gpst cat /data/data/com.example.gpst/files/gps/gps_2025-10-04.txt" > gps.txt
