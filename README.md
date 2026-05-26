# NoWaste

Android-first Kotlin + Jetpack Compose app for local food expiry tracking.

## Features

- Add, edit, delete food items with expiry dates, category tags, notes, and barcode values.
- Capture a packaging photo from the form and store it locally with the food record.
- Sort the list by expiry date and color-code safe, near-expiry, and expired items.
- Show remaining shelf-life with relative dates and a status-colored progress bar.
- Schedule local daily expiry reminders with WorkManager.
- Scan barcodes with CameraX + ML Kit.
- Lookup barcode product names and categories with Open Food Facts.
- Add note text with Android system voice recognition.
- Search, category-filter, and swipe food items for quick actions.

## Build

This project requires JDK 17 and an Android SDK with API 36.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Run instrumented Room tests on a connected device or emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```
