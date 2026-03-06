# 📺 FreeTV Player — Complete Android IPTV App

A fully-featured Android IPTV Player built with Kotlin, ExoPlayer (Media3), Material Design 3, and MVVM architecture. Streams live TV channels from the open `iptv-org` M3U playlist.

---

## 🏗️ Project Structure

```
FreeTV_Player/
├── build.gradle                         ← Root Gradle config
├── settings.gradle                      ← Module registration
├── gradle.properties                    ← Build properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties        ← Gradle version (8.1.1)
│
└── app/
    ├── build.gradle                     ← App-level deps & config
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/freetv/player/
        │   ├── MainActivity.kt                      ← Entry point, nav, search
        │   ├── activities/
        │   │   └── PlayerActivity.kt                ← ExoPlayer full-screen
        │   ├── adapters/
        │   │   └── ChannelAdapter.kt                ← RecyclerView adapter
        │   ├── fragments/
        │   │   └── ChannelListFragment.kt           ← Channel list, filters
        │   ├── models/
        │   │   └── Channel.kt                       ← Data model + FilterState
        │   ├── parsers/
        │   │   └── M3UParser.kt                     ← M3U/EXTINF parser
        │   ├── repository/
        │   │   └── ChannelRepository.kt             ← Network + cache layer
        │   └── viewmodels/
        │       └── ChannelViewModel.kt              ← LiveData + business logic
        └── res/
            ├── layout/
            │   ├── activity_main.xml
            │   ├── activity_player.xml
            │   ├── custom_player_controls.xml
            │   ├── fragment_channel_list.xml
            │   └── item_channel.xml
            ├── drawable/                ← 20 vector icons + backgrounds
            ├── menu/                    ← bottom_nav_menu, main_menu
            ├── values/                  ← strings, colors, themes
            ├── values-night/            ← Dark mode theme override
            └── xml/                    ← backup_rules, data_extraction_rules
```

---

## 🔧 Architecture

```
┌─────────────────────────────────────────────┐
│                   UI Layer                  │
│  MainActivity ← ChannelListFragment         │
│  PlayerActivity                             │
└──────────────────┬──────────────────────────┘
                   │ observes LiveData
┌──────────────────▼──────────────────────────┐
│              ViewModel Layer                │
│  ChannelViewModel (AndroidViewModel)        │
│  • channels, isLoading, error LiveData      │
│  • search() with 300ms debounce             │
│  • filterByCategory(), filterByCountry()    │
└──────────────────┬──────────────────────────┘
                   │ coroutines (IO dispatcher)
┌──────────────────▼──────────────────────────┐
│             Repository Layer                │
│  ChannelRepository                          │
│  • Memory cache → Disk cache → Network      │
│  • Favorites (JSON file)                    │
│  • Recently Watched (JSON file)             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│              Data Sources                   │
│  M3UParser (parses EXTINF lines)            │
│  OkHttpClient (fetches playlist)            │
│  Gson (serializes/deserializes cache)       │
└─────────────────────────────────────────────┘
```

---

## 📦 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Media3 ExoPlayer | 1.2.0 | HLS/MPEG-TS video playback |
| Media3 HLS | 1.2.0 | HLS (.m3u8) stream support |
| Media3 UI | 1.2.0 | PlayerView widget |
| Glide | 4.16.0 | Channel logo image loading |
| OkHttp | 4.12.0 | HTTP client for M3U + streams |
| Kotlin Coroutines | 1.7.3 | Background threading |
| Material Components | 1.11.0 | Chips, Cards, BottomNav |
| RecyclerView | 1.3.2 | Efficient channel list |
| ViewModel + LiveData | 2.6.2 | MVVM lifecycle handling |
| Gson | 2.10.1 | JSON cache serialization |
| SwipeRefreshLayout | 1.1.0 | Pull-to-refresh |

---

## ✨ Features

| Feature | Details |
|---------|---------|
| 📡 Live IPTV | Loads 10,000+ channels from iptv-org/iptv |
| 🎬 HLS Playback | ExoPlayer Media3 with adaptive streaming |
| 🔍 Search | Real-time search with 300ms debounce |
| 🏷️ Categories | Chip filter: News, Sports, Movies, Kids, Music… |
| 🌍 Country Filter | Dropdown from parsed country codes |
| ❤️ Favorites | Persisted to device storage (JSON) |
| 🕐 Recently Watched | Last 20 channels tracked automatically |
| 🌙 Dark Mode | Material3 DayNight with one-tap toggle |
| ♻️ Pull to Refresh | SwipeRefreshLayout with force-refresh |
| 💾 Caching | 1-hour disk cache; stale cache on error |
| 📶 Error Handling | Retry on network error, error Snackbar |
| 📺 Full Screen | Immersive mode with system bars hidden |
| ⏸️ Buffering UI | Progress indicator + status message |

---

## 🚀 How to Build the APK

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK** API 34 (install via SDK Manager)
- **Internet connection** (for Gradle dependencies)

---

### Step 1 — Open the Project

1. Launch Android Studio
2. Click **File → Open**
3. Navigate to the `FreeTV_Player` folder and click **OK**
4. Wait for Gradle sync to complete (first sync downloads ~200 MB)

---

### Step 2 — Fix Package Name (if needed)

Ensure the package `com.freetv.player` matches across:
- `app/build.gradle` → `applicationId`
- `AndroidManifest.xml` → `package` attribute
- All Kotlin files (`package com.freetv.player`)

---

### Step 3 — Build Debug APK (Quickest)

#### Option A — Android Studio GUI
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Option B — Terminal / Command Line
```bash
# Mac / Linux
cd FreeTV_Player
chmod +x gradlew
./gradlew assembleDebug

# Windows
cd FreeTV_Player
gradlew.bat assembleDebug
```

---

### Step 4 — Build Release APK (For Distribution)

#### 4a. Generate a keystore (one-time)
```bash
keytool -genkey -v \
  -keystore freetv.keystore \
  -alias freetv_key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

#### 4b. Configure signing in `app/build.gradle`
```groovy
android {
    signingConfigs {
        release {
            storeFile     file("../freetv.keystore")
            storePassword "YOUR_STORE_PASSWORD"
            keyAlias      "freetv_key"
            keyPassword   "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
        }
    }
}
```

#### 4c. Build signed release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

---

### Step 5 — Install on Device

```bash
# Enable USB Debugging on phone: Settings → Developer Options → USB Debugging

# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or install release APK
adb install app/build/outputs/apk/release/app-release.apk
```

---

### Step 6 — Run on Emulator

1. **Tools → Device Manager** → Create Virtual Device
2. Choose **Pixel 6** (API 33+)
3. Click ▶ Run button or press `Shift+F10`

---

## 🔄 Common Gradle Issues

| Issue | Solution |
|-------|----------|
| `SDK location not found` | Set `ANDROID_HOME` env var or create `local.properties` with `sdk.dir=/path/to/sdk` |
| `Minimum supported Gradle version` | Run `./gradlew wrapper --gradle-version=8.1.1` |
| `Duplicate class kotlin.*` | Add `implementation(kotlin("stdlib"))` exclusion |
| Build fails on first run | Try **File → Invalidate Caches → Restart** |
| Network error fetching channels | Ensure `usesCleartextTraffic="true"` in Manifest |

---

## 📱 Minimum Requirements

- Android **6.0 (API 23)** or higher
- Internet connection for channel list and streaming
- ~50 MB free storage for app + cache

---

## 🎨 UI Screenshots (Description)

**Main Screen**
- Blue Material3 toolbar with app title
- SearchView with magnifier icon
- Horizontal scrollable category chips (All / News / Sports / Movies / Kids…)
- Country spinner
- Card-based channel list: logo circle + name + category badge + country + ❤️ button
- Bottom navigation: Channels | Favorites | Recent

**Player Screen**
- Full-black background
- ExoPlayer fills entire screen
- Top overlay: ← Back | Channel Name | ⛶ Fullscreen
- Bottom overlay: ⏮ 10s | ▶/⏸ | ⏭ 10s + seek bar
- Center spinner while buffering

---

## 📋 Extending the App

### Add a custom M3U URL
In `ChannelRepository.kt`, change:
```kotlin
private const val M3U_URL = "https://your-custom-playlist.m3u"
```

### Add EPG (Electronic Programme Guide)
Fetch XMLTV data from `https://iptv-org.github.io/epg/` and match by `tvg-id`.

### Add PiP (Picture-in-Picture)
Add `android:supportsPictureInPicture="true"` to `PlayerActivity` in the Manifest,
then call `enterPictureInPictureMode()` in `onUserLeaveHint()`.

---

## 📄 License

This project is open source. The M3U playlist is sourced from
[iptv-org/iptv](https://github.com/iptv-org/iptv) under the Unlicense.
Channel availability depends on stream providers.
name: Build Android APK

on:
  push:
    branches:
      - main

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v3

      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Build APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
