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

## Call recording investigation

The owner reports a Samsung Galaxy A16 running Android 17. The project now targets Android API 37. A capability-gated investigation plan for incoming and outgoing cellular call recording is in [docs/CELLULAR_CALL_RECORDING.md](docs/CELLULAR_CALL_RECORDING.md).

**Important:** Being the default dialer and holding `RECORD_AUDIO` permission do not, by themselves, grant access to both sides of cellular call audio. DuckCall will not claim to record a call unless a supported backend has been verified on the actual Samsung firmware. Microphone-only capture must not be presented as two-way call recording.

## Internet / VoIP

VoIP development is paused by request. The existing signaling prototype and architecture notes remain in the repository but are not part of the current cellular implementation. They are not a working internet-calling service.

## Build

Open this repository in a current Android Studio version with Android SDK Platform 37 installed. Android 17 requires `compileSdk = 37`; `targetSdk = 37` opts into Android 17 target behavior. The Android Gradle Plugin is set to 8.9.0.

- Minimum Android: API 26
- Compile / target SDK: 37
- Language: Kotlin
- UI: Jetpack Compose

## Next steps

1. Compile and fix any Android/Gradle issues.
2. Test the default-dialer role flow on a real Android phone.
3. Verify Samsung firmware call-recording capability before implementing an audio backend.
4. Exercise incoming, outgoing, declined, missed, held, and ended cellular calls.
5. Add complete multi-call, audio routing, DTMF, and persistent call-history behavior.
6. Add automated tests and accessibility/device compatibility checks.
