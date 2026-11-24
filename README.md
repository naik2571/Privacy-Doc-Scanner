# Privacy Doc Scanner

> **100% Offline, air-gapped document scanner with on-device ML Kit OCR and PDF export.**

An enterprise-grade, privacy-first Android application developed in modern Kotlin and Jetpack Compose/Views. Built with strict adherence to modern Android architecture, lifecycle awareness, and hardware sensor efficiency.

---

## 🚀 Key Features

- **100% Air-Gapped**: Manifest strictly overrides and removes all internet permissions.
- **On-device OCR**: ML Kit Text Recognition extracts text entirely within local phone RAM.
- Document edge detection and high-contrast B&W document enhancement shaders.
- Direct local PDF generation with embedded searchable text layer.
- Zero cloud telemetry, zero third-party analytics.

---

## 🛠️ Hardware & Sensor Architecture

* **Sensors & APIs:** `CameraX, ML Kit On-Device Text Recognition, PDFDocument Engine`
* **Declared Permissions:** `CAMERA (ZERO INTERNET PERMISSIONS)`
* **Target SDK:** Android 14 (API 34)
* **Minimum SDK:** Android 8.0 (API 26+)
* **Architecture:** MVVM / Unidirectional Data Flow (UDF) with Coroutines & StateFlow

---

## 🔒 Privacy & Permission Transparency

This application is engineered under the **Zero Unnecessary Permissions** doctrine:
* **No Hidden Trackers:** Contains zero advertising SDKs or third-party behavioral analytics.
* **No Accessibility Privilege Misuse:** All background operations utilize sanctioned Android APIs (`WorkManager`, `ForegroundService`, `JobScheduler`).
* **Air-Gapped Operation:** Runs locally on your device hardware without requiring external cloud servers.

---

## 📦 Building and Running

### Prerequisites
* Android Studio Iguana / Jellyfish / Ladybug or newer
* Android SDK 34
* JDK 17+

### Build APK via CLI
```bash
# Clone the repository
git clone https://github.com/naik2571/Privacy-Doc-Scanner.git
cd Privacy-Doc-Scanner

# Build Debug APK
./gradlew assembleDebug

# Install on connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
Licensed under the [Apache License 2.0](LICENSE).
