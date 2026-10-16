# SafeMesh AI — Native Android Setup Guide

This document details how to set up, configure, build, and run the native Android SafeMesh AI application (`android/`).

---

## 1. Prerequisites

### A. Android Studio
- **Recommended Version:** Android Studio Ladybug (2024.2+) or Meerkat (2025.1+).
- **SDK Platforms:** Android 15 (API 35) or Android 14 (API 34).
- **SDK Build Tools:** 34.0.0 or 35.0.0+.

### B. JDK Requirement
- **Required JDK:** Java 17 or Java 21 (or Android Studio's bundled JetBrains Runtime / JBR).
- Set your `JAVA_HOME` environment variable to your JDK path:
  ```powershell
  # Example using Android Studio's bundled JBR:
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  ```

---

## 2. Project Location

The native Android project is located in:
```
SafeMesh-AI-2/
└── android/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    ├── local.properties
    ├── gradlew.bat / gradlew
    ├── gradle/libs.versions.toml
    └── app/
        ├── build.gradle.kts
        └── src/
            ├── main/
            │   ├── AndroidManifest.xml
            │   ├── java/com/safemesh/ai/
            │   └── res/
            └── test/
```

---

## 3. Configuring the Backend URL

SafeMesh AI Android is designed to communicate with the existing Node.js/Express backend server (`server.js`).

The backend URL is **never hardcoded in production** and can be configured:

### A. Inside the App UI:
1. Open the app -> Navigate to the **Profile** tab.
2. Tap **"Backend Server URL Config"**.
3. Enter your active backend endpoint and tap **"Save Server URL"**.

### B. Suggested URLs:
| Testing Environment | Backend URL |
| :--- | :--- |
| **Android Studio Emulator** | `http://10.0.2.2:3001/` *(Default)* |
| **Physical Android Phone on Wi-Fi** | `http://<YOUR_PC_LAN_IP>:3001/` (e.g. `http://192.168.32.211:3001/`) |
| **Remote / Tunnelling (Cloudflare / ngrok)** | `https://your-tunnel-url.trycloudflare.com/` |

---

## 4. How to Build the Debug APK

Run the following commands from the root or the `android/` directory:

```powershell
cd android

# Ensure JAVA_HOME points to your JDK / JBR:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Build debug APK
.\gradlew.bat assembleDebug
```

The compiled APK will be generated at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. How to Run on an Android Emulator

1. Open Android Studio -> Select **"Open"** -> Navigate to `SafeMesh-AI-2/android`.
2. Allow Gradle to sync.
3. Open **Device Manager** in Android Studio and launch any virtual device running API 26+ (recommended API 34/35).
4. Click **Run 'app'** (`Shift + F10`) or use Gradle command line:
   ```powershell
   .\gradlew.bat installDebug
   ```
5. In the Android app, ensure the backend URL in **Profile -> Backend Server URL Config** is set to:
   ```
   http://10.0.2.2:3001/
   ```

---

## 6. How to Run on a Physical Android Phone

1. On your physical Android phone:
   - Enable **Developer Options** (Settings -> About Phone -> tap *Build Number* 7 times).
   - Enable **USB Debugging**.
2. Connect your phone via USB cable to your computer.
3. Ensure both phone and PC are connected to the same Wi-Fi network.
4. Start the Node.js backend on your PC:
   ```powershell
   npm run start:backend
   ```
5. Install the debug APK via `adb` or Android Studio:
   ```powershell
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```
6. In the app on your phone:
   - Go to **Profile -> Backend Server URL Config**.
   - Set the URL to your PC's LAN IP: `http://192.168.x.x:3001/`.
   - Add your emergency contacts under **SOS Contacts**.
   - Test the SOS trigger!

---

## 7. Required Permissions & Privacy Handling

The application requests only necessary permissions and explains them prior to request via the in-app **Permission Manager** (`Profile -> Permissions & Privacy`):

- **Location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`):** Required to retrieve GPS coordinates when SOS is triggered.
- **Microphone (`RECORD_AUDIO`):** Required for distress acoustic monitoring.
- **Notifications (`POST_NOTIFICATIONS` on Android 13+):** Required for critical alert notifications.
- **Bluetooth (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` on Android 12+):** Pre-configured for upcoming offline BLE mesh relay capabilities.
- **Internet (`INTERNET`, `ACCESS_NETWORK_STATE`):** Required to communicate with the SafeMesh backend.
