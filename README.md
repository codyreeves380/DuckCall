# DuckCall

DuckCall is a native Android calling app built with Kotlin and Jetpack Compose.

## Current foundation

- Dial pad with editable phone number
- Cellular calling handoff through Android's system dialer (the user confirms the call there)
- App navigation for Dialer, Recents, Contacts, and Settings
- Initial VoIP entry point and a clean seam for adding a real calling provider

## Calling modes

### Cellular
DuckCall opens the Android system dialer with the entered number. This avoids requesting the sensitive `CALL_PHONE` permission and lets the user review the number and place the call using their SIM.

### Internet / VoIP
A production VoIP implementation needs a provider or signaling backend, authentication, push notifications for incoming calls, and a media stack such as WebRTC. No provider credentials or backend have been supplied yet, so the current VoIP control is an honest setup placeholder rather than a simulated call.

## Build

Open this repository in the latest stable Android Studio with Android SDK 35 installed. Let Gradle sync, then run the `app` configuration on an Android device or emulator.

- Minimum Android: API 26
- Compile / target SDK: 35
- Language: Kotlin
- UI: Jetpack Compose

## Next steps

1. Choose the VoIP provider / backend.
2. Add account sign-in and contact integration.
3. Add real outgoing and incoming VoIP calls, audio routing, and call notifications.
4. Add persistent call history and automated tests.
