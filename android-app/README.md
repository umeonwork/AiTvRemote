# Android app module

This is a minimal Android app (Kotlin + Jetpack Compose) that connects to the AiTvRemote server REST API.

How it works
- Lists devices from GET /api/devices
- Lets you select a device and send simple commands (POST /api/devices/{id}/command)

Run in emulator
- The default base URL uses 10.0.2.2:3001 which routes to localhost on the host machine when running the Android emulator.
- If you run on a device, set the environment variable AITV_API or change the base URL in MainActivity.createApi().

Build
- Open android-app in Android Studio and build/run.
