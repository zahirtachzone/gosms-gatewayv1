# GoSMS Gateway — Android App

GoSMS Gateway is an Android application that turns an Android phone into an SMS Gateway for GoSMS.

## Features

- **Isolated Client Binding**: 1 phone = 1 client. Secured with device-bound token and UUID verification.
- **Strict SIM Routing**: Direct SIM slot dispatch without silent fallback.
- **Idempotent Queue**: SQLite/Room with unique `message_id` constraints preventing duplicate SMS.
- **Safe Rate Limiting**: Enforces server `max_per_minute` and minimum 2-second spacing between messages to protect SIMs.
- **Persistent Service**: Foreground service with partial wake lock only during transmission, auto-restart on boot, and WorkManager watchdog.
- **Material 3 Design**: Fully custom design system with dark/light theme support.

---

## How to Build the APK via GitHub Actions

1. Create a repository on GitHub (e.g. `gosms-gateway`).
2. Push this project to GitHub using Git or GitHub Desktop:
   ```
   git init
   git branch -M main
   git add .
   git commit -m "Initial release"
   git remote add origin https://github.com/<your-username>/gosms-gateway.git
   git push -u origin main
   ```
3. Once pushed, navigate to the **Actions** tab in your GitHub repository.
4. The **Build APK** workflow will execute automatically using Gradle 8.10.2 and JDK 17.
5. When complete, download the `GoSMS-Gateway-debug` zip file from the **Artifacts** section.
6. Extract the zip to obtain the installable `.apk` file.

---

## Installation & Android Permissions

Because SMS gateway applications require sensitive permissions (`SEND_SMS`, `READ_PHONE_STATE`), modern Android versions require specific user approvals:

### 1. Allow Restricted Settings (Android 13+)
For sideloaded APKs:
1. Open phone **Settings** → **Apps** → **GoSMS Gateway**.
2. Tap the **⋮** (three dots) in the top-right corner.
3. Tap **Allow restricted settings**.
4. Authenticate with your fingerprint or PIN.
5. Return to the app and grant the SMS permission.

### 2. Unrestricted Battery Optimization
To ensure the gateway service stays running continuously:
1. Open phone **Settings** → **Apps** → **GoSMS Gateway** → **Battery**.
2. Select **Unrestricted** (or "Don't optimize").
3. On Xiaomi/Oppo/Vivo/Samsung devices, also enable **Autostart** and lock the app in the Recent Apps tray.

---

## Pairing with GoSMS Server

1. Open GoSMS Gateway on the phone.
2. Tap **Scan QR code** and scan the pairing QR from your GoSMS admin panel (Devices section), or tap **Enter manually** to input the server URL and 6-digit code.
3. Upon registration, the gateway will activate and begin listening for outgoing SMS jobs.
