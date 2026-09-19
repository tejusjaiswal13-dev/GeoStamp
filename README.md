# GeoStamp 📍📸

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Kotlin-2.3.20-blue.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-orange.svg)](https://developer.android.com/topic/architecture)

**GeoStamp** is a real-time GPS camera and geotagging Android application that captures photos and permanently burns high-precision geographic telemetry, address information, altitude, speed, heading, and timestamps directly onto the image.

---

## ✨ Features

- 📸 **CameraX Integration**: High-speed camera capture with flash modes (On, Off, Auto) and front/back camera toggles.
- 📍 **Real-time GPS Telemetry**: Accurate latitude, longitude, altitude, accuracy, speed, and heading via Google Play Services `FusedLocationProviderClient`.
- 🏷️ **Watermark & GeoStamp Engine**: Custom canvas-based photo watermarking that burns responsive, high-definition location metadata badges onto captured photos.
- 🗺️ **Interactive OpenStreetMap**: Built-in map view with live location markers and accuracy circles powered by `osmdroid` (no API key required).
- 💾 **Media Storage & Sharing**: Instant save to `Pictures/GeoStamp` via Android MediaStore API with direct options to share or open in gallery.
- 🎨 **Material 3 Design**: Clean, modern dark-themed interface built natively with Jetpack Compose.
- ⚡ **Developer Credit**: Opening splash screen credit and dedicated About section.

---

## 📱 Screenshots & Modes

- **Camera Mode**: Full-screen camera viewfinder with real-time GPS overlay card, shutter control, flash toggle, and photo review dialog.
- **Map Mode**: Full-screen map centered on device location with expandable telemetry details and action buttons (Refresh, Copy Coordinates, Share, Open in Maps).

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Camera**: AndroidX CameraX (`core`, `camera2`, `lifecycle`, `view`)
- **Location**: Google Play Services Location (`FusedLocationProviderClient`)
- **Mapping**: `osmdroid-android`
- **Image Loading**: Coil Compose
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 / 16 (API 36)

---

## 🚀 Building & Running

### Prerequisites
- JDK 17+
- Android SDK (API 24 - 36)

### Build Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 👨‍💻 Author

**Tejus Jaiswal**
- GitHub: [@tejusjaiswal13-dev](https://github.com/tejusjaiswal13-dev)
