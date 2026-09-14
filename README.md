# Galaxy A12 Cleaner

A lightweight, offline storage & junk cleaner engineered specifically for the Samsung Galaxy A12 and modern Android devices (Android 11+).

[![Build APK](https://github.com/jessicaalbert1234567-netizen/Storage-wipe-out/actions/workflows/build-apk.yml/badge.svg)](https://github.com/jessicaalbert1234567-netizen/Storage-wipe-out/actions/workflows/build-apk.yml)
[![Latest Release](https://img.shields.io/github/v/release/jessicaalbert1234567-netizen/Storage-wipe-out?color=blue&label=Release)](https://github.com/jessicaalbert1234567-netizen/Storage-wipe-out/releases/latest)

---

### 📥 Download Links

- 📲 **[Download Latest APK from Releases](https://github.com/jessicaalbert1234567-netizen/Storage-wipe-out/releases/latest)**
- ⚡ **[Direct APK File Download](https://github.com/jessicaalbert1234567-netizen/Storage-wipe-out/releases/latest/download/GalaxyCleaner-v1.0.0.apk)**
- 📦 **[Download from Latest GitHub Actions Artifacts (GalaxyCleaner-debug)](https://github.com/jessicaalbert1234567-netizen/Storage-wipe-out/actions/workflows/build-apk.yml)**

---

## 🌟 Overview

The **Galaxy A12 Cleaner** is designed to provide clean, transparent, and respectful storage management for devices with modest hardware configurations (such as the MediaTek Helio P35 / Exynos 850 with 3GB–4GB RAM and eMMC 5.1 storage).

Unlike predatory or deceptive "cleaner" apps, Galaxy A12 Cleaner:
- **Operates 100% locally and offline** (zero internet permissions, zero telemetry, zero analytics).
- **Fully respects Android's security model** (no fake promises of clearing third-party app caches without user permission).
- **Employs conservative, safe heuristics** to identify disposable junk while fiercely protecting personal documents, photos, and media.
- **Includes a Samsung One UI-inspired interface** featuring rounded cards, a circular Device Care storage gauge, and high-contrast accessibility.

---

## 📱 Target Device & Compatibility

- **Primary Target:** Samsung Galaxy A12 (SM-A125F, SM-A127F)
- **Android Versions:** Android 11 (API 30), Android 12/12L (API 31/32), Android 13 (API 33), Android 14 (API 34), and Android 15 (API 35+)
- **Minimum SDK:** Android 8.0 Oreo (API 26)
- **Compile SDK:** API 36

---

## 🔒 Android Security & Scoped Storage Compliance

### Why Silent Third-Party App Cache Clearing is Prohibited
Starting with Android 8.0 and strictly enforced in Android 11+ with Scoped Storage:
1. **Sandboxed Data:** Android isolates every application within its own Linux UID. No non-system, non-root app can inspect or delete files from `/data/data/<package>/cache` or `/Android/data/<package>/cache`.
2. **Security Integrity:** A third-party cleaner attempting to invoke `pm clear`, `su`, or `rm -rf` against protected locations will fail and violate Google Play Developer Program policies.
3. **Transparent Approach:**
   - **Direct Cleaning:** Cleans files the application legitimately owns or has permission to delete (internal cache, external cache, temporary logs, downloaded APKs with explicit confirmation, duplicate files explicitly selected).
   - **Manage App Cache:** Provides direct, official Android Intent shortcuts (`Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS` and `Settings.ACTION_INTERNAL_STORAGE_SETTINGS`) so users can view and clear app caches directly in system settings without third-party interference.

---

## 🛡️ File Safety Policy

The built-in `FileSafetyPolicy` enforces strict constraints:
- **Never Auto-Deletes User Media:** Folders such as `DCIM/`, `Pictures/`, `Movies/`, `Music/`, `Documents/`, `WhatsApp/`, and `Telegram/` are strictly excluded from automated cleanup.
- **Protected User Extensions:** Files ending with `.jpg`, `.png`, `.mp4`, `.mp3`, `.pdf`, `.docx`, `.xlsx`, `.zip`, etc. are NEVER treated as junk.
- **Safe Candidates:** Scans for `.tmp`, `.temp`, `.log`, `.cache`, `.bak`, `.dmp`, `.old`, `.chk`, empty temporary files, thumbnail caches, and leftover `.apk` installers in accessible download locations.

---

## ♿ Accessibility Service Policy

The optional `CleanerAccessibilityService` is an idle, companion service declared with minimum permissions:
- **No Hidden UI Automation:** It does **NOT** click "Clear Cache", "Force Stop", or bypass any confirmation screens.
- **Completely Optional:** Normal cleaning workflows do **NOT** require accessibility access.
- **Transparent:** The in-app Settings screen clearly displays service status and offers an official launch button to system accessibility settings.

---

## 📦 Features

1. **Dashboard & Circular Storage Gauge:**
   - Real-time internal storage calculation using standard `StatFs` APIs.
   - Clean color transitions: green (<70%), amber (70–85%), red (>85%).
2. **Category Breakdown:**
   - Junk Files
   - Temporary Files
   - Old APK Files
   - Thumbnails
   - Empty Files
   - Large Files
   - Duplicate Files
3. **3-Stage Duplicate Scanner:**
   - Stage 1: Size grouping (eliminates unique sizes instantly).
   - Stage 2: 4KB partial sample MD5 hash.
   - Stage 3: Full SHA-256 calculation for candidate matches only.
   - Original file is retained and protected by default.
4. **Large File Filter:**
   - Easily isolate space hogs with 100 MB+, 500 MB+, and 1 GB+ threshold filters.
5. **Home Screen Widgets & Shortcuts:**
   - Compact 1x1 / 2x1 widget showing junk volume and quick action.
   - Expandable 2x2 / 4x1 widget with storage breakdown and scan/clean controls.
   - Launcher app shortcuts for "Clean Junk" and "Scan Storage".

---

## 🛠️ Build Instructions

### Prerequisites
- JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK Platform 36 & Build Tools

### Building via Command Line
```bash
# Clone the repository
git clone https://github.com/your-username/galaxy-a12-cleaner.git
cd galaxy-a12-cleaner

# Run Unit & Robolectric Tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 GitHub Actions CI/CD

This repository includes two GitHub Actions workflows:
1. **`.github/workflows/build-apk.yml`:** Automatically executes unit tests and compiles the debug APK upon any push or pull request to `main` or `master`.
2. **`.github/workflows/release.yml`:** Automatically runs tests, packages the release APK, and creates a tagged GitHub Release when a `v*` tag is pushed.

---

## 📄 Permissions Used

| Permission | Purpose |
| :--- | :--- |
| `READ_EXTERNAL_STORAGE` (maxSdkVersion=32) | Scans accessible temporary files and downloads on Android 11 & 12 |
| `READ_MEDIA_IMAGES` | Reads accessible images on Android 13+ |
| `READ_MEDIA_VIDEO` | Reads accessible video files on Android 13+ |
| `READ_MEDIA_AUDIO` | Reads accessible audio files on Android 13+ |

*Note: No `INTERNET`, `MANAGE_EXTERNAL_STORAGE`, or `QUERY_ALL_PACKAGES` permissions are declared.*

---

## ⚖️ License
Licensed under the Apache License, Version 2.0.
