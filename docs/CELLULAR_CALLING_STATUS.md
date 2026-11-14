# DuckCall cellular calling status

## Implemented in this branch

- Android Telecom is the cellular call owner.
- The app can request the Android default dialer role.
- The dialer hands outgoing numbers to `TelecomManager.placeCall()`.
- `DuckCallInCallService` receives Telecom call objects and exposes answer, decline/end, mute, speaker, hold, and resume controls to the Compose UI.
- The manifest declares the default-dialer entry points and binds the in-call service.
- The app declares `CALL_PHONE` and requests it at runtime before relying on outgoing call placement.
- Notification permission is requested after the call permission flow, avoiding two permission dialogs being launched at once.
- The app's palette has been moved toward DuckCall's black, blue, yellow, and red identity.

## Device acceptance test

Run these tests on the Galaxy A16 (SM-A166U), with an active AT&T SIM and DuckCall selected as the default phone app:

1. Install a debug build and launch DuckCall.
2. Grant the phone permission. Confirm the notification permission prompt follows separately on Android 13+.
3. In Android Settings, verify DuckCall is selected as the default phone app.
4. Place an outgoing call from the dial pad to a known test number.
5. Place an outgoing call from another app using an ACTION_DIAL `tel:` link and confirm DuckCall receives the number.
6. Receive a cellular call while DuckCall is foregrounded, backgrounded, and the screen is locked.
7. Answer and end the incoming call.
8. During a connected call, test mute/unmute, speaker/earpiece, hold/resume, and ending the call.
9. Repeat with Bluetooth audio connected and verify Android's audio route behavior.
10. Confirm the notification opens DuckCall and that call UI recovers after returning from another app.

Record Android version, One UI version, app build, SIM/carrier state, and results for each test.

## Known limitations and honest boundaries

- This is a Telecom integration foundation, not yet a device-verified production dialer. It has not been built or exercised on the Galaxy A16 in this environment.
- The current Recents list is in-memory and only reflects numbers dialed during the current app session. A production dialer still needs a proper call-log implementation and permission/role handling.
- Contacts integration, robust multi-call/swap/conference handling, DTMF keypad, Bluetooth route selection UI, accessibility review, and comprehensive lifecycle testing remain open.
- Android's public `InCallService` API controls cellular call state and audio routing. It does not provide a supported general-purpose API for capturing both sides of carrier call audio. Recording must remain a separately validated, device/OEM/carrier-specific feature. No hidden API or AccessibilityService workaround is used.
- Hold/resume here uses Telecom's call controls. It does not inject network-side hold music.

## Release gate

Do not call cellular calling complete until the debug build compiles and the device acceptance tests above pass on the target Galaxy A16 and AT&T setup.
