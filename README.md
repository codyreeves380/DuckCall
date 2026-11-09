# DuckCall

DuckCall is a native Android calling app built with Kotlin and Jetpack Compose.

## Cellular calling foundation

DuckCall is being developed as a replacement default phone app using Android Telecom.

Implemented in the current repository:
- Dial pad registered for Android `ACTION_DIAL` intents, including `tel:` links
- User-initiated request to become Android's default dialer through `RoleManager.ROLE_DIALER` on Android 10+ and the legacy default-dialer intent on older supported versions
- `InCallService` registration and a Telecom-backed call state controller
- Basic incoming-call and active-call UI
- Basic answer, decline, hang-up, mute, speaker, hold, and resume controls
- Incoming/ongoing call notification with a full-screen activity intent
- Outgoing cellular calls submitted through `TelecomManager.placeCall` after DuckCall is selected as the default dialer

The user must approve the Android system prompt to make DuckCall the default phone app. Emergency calls continue to use the preinstalled system dialer according to Android's platform behavior.

**Important:** This is an initial integration, not a production-ready phone app yet. It has not been compiled or tested on a physical device. Before relying on it for daily calls, the app still needs device testing and hardening for multiple simultaneous calls, Bluetooth and audio-route changes, DTMF, call waiting/conferencing, notification and full-screen-intent permission behavior, robust call history, accessibility, and OEM-specific behavior. Keep the preinstalled Phone app available as a fallback during testing.

## Internet / VoIP

VoIP development is paused by request. The existing signaling prototype and architecture notes remain in the repository but are not part of the current cellular implementation. They are not a working internet-calling service.

## Build

Open this repository in the latest stable Android Studio with Android SDK 35 installed. Let Gradle sync, then run the `app` configuration on an Android device or emulator.

- Minimum Android: API 26
- Compile / target SDK: 35
- Language: Kotlin
- UI: Jetpack Compose

## Next steps

1. Compile and fix any Android/Gradle issues.
2. Test the default-dialer role flow on a real Android phone.
3. Exercise incoming, outgoing, declined, missed, held, and ended cellular calls.
4. Add complete multi-call, audio routing, DTMF, and persistent call-history behavior.
5. Add automated tests and accessibility/device compatibility checks.
