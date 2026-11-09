# Cellular call recording investigation

## Device and platform target

- Device: Samsung Galaxy A16 5G, model SM-A166U (owner-confirmed)
- Carrier context: AT&T (previously provided by owner; verify active SIM/CSC on handset)
- OS reported by owner: Android 17
- Android 17 API level: 37
- Samsung firmware listing for this model: A166USQUAEZJ5, Android 17 / One UI 9, released 2026-11-03 (confirm installed build on device)
- App role: user-selected default dialer using Android Telecom
- Scope: incoming and outgoing cellular calls
- VoIP: paused
- Hold music: out of scope

## Device-specific Samsung recording lead

Samsung's A16 user documentation describes a Record control on the in-call screen, with recordings saved in Voice Recorder and an audible notice played to the other participant. Availability can depend on software version, region, and carrier. This is evidence of a Samsung Phone feature, not proof that third-party dialers can invoke the OEM recording service or access its audio output.

First verify whether the stock Samsung Phone app on this exact SM-A166U / AT&T software build shows Record during both incoming and outgoing cellular calls. Record the installed software build and CSC before drawing conclusions.

## Current implementation findings

DuckCall already registers an `InCallService` and receives Telecom call lifecycle callbacks. The service can manage call controls, but Android's public dialer APIs do not provide a general-purpose API for capturing both sides of a cellular call.

The manifest currently requests `RECORD_AUDIO`. That permission alone does not grant access to the cellular downlink/uplink mix. Android's call-audio capture sources and output capture capabilities are restricted to privileged/system components or specially authorized platform implementations. Do not treat microphone capture as two-way call recording.

Samsung's preinstalled Phone app may expose an OEM-supported recording feature on eligible firmware, region, and carrier configurations. The presence of that feature in Samsung Phone does not imply that third-party dialers can invoke the same private service or receive its audio stream.

## Engineering decision

Build the recording feature as a capability-gated subsystem. Never show a successful recording state unless a supported capture backend has actually started and produced a valid audio file.

Backend priority:

1. Public, documented Android API available to ordinary third-party default dialers, if Samsung/Android exposes one on this exact firmware.
2. A documented Samsung partner/OEM API, only if access is legitimately available to this app.
3. Otherwise report cellular call recording as unsupported on this device build. Do not silently fall back to microphone-only capture and label it as a full call recording.

Do not use hidden APIs, reflection into privileged telephony services, root-only workarounds, or accessibility abuse.

## First-device verification checklist

- Android release and API level from Settings > About phone > Software information.
- One UI version and full software build number.
- Model code: SM-A166U.
- Active SIM/carrier and CSC (AT&T expected; verify on device).
- Whether calls use VoLTE/VoNR or Wi-Fi Calling.
- Whether Samsung Phone itself displays a Record call control during an active cellular call.
- Whether the stock Phone app announces recording to participants and where it stores the file.
- Whether the feature is available for both incoming and outgoing calls.

Perform controlled tests with the owner's consent and the other participant's consent. Follow applicable recording-consent laws.

## Recording subsystem design once a supported backend is confirmed

- `CallRecordingController`: start/stop state machine tied to Telecom call identity.
- `CallRecordingBackend`: narrow interface for capability detection, start, stop, and backend errors.
- `RecordingRepository`: persist metadata (call ID, direction, timestamps, duration, file URI, backend, consent/notice state).
- Private app storage by default; explicit export/share action.
- Recording indicator in the in-call UI and call history.
- Recovery cleanup for interrupted recordings and orphaned files.
- Never record emergency calls; stop immediately when the call disconnects.
- Clear user-visible disclosure and consent workflow appropriate to applicable law and carrier/OEM requirements.

## Acceptance criteria

- Capability detection returns a truthful supported/unsupported result on the actual Galaxy A16 firmware.
- Incoming and outgoing calls are independently tested.
- A successful recording contains both caller and remote-party audio, not just microphone audio.
- Recording starts/stops with the correct call and survives ordinary UI recreation.
- File playback, deletion, and export work.
- No recording is claimed when backend start fails or output is empty.
- Default-dialer call handling continues to work when recording is unavailable.

## Current status

Investigation only. No cellular audio capture implementation is claimed yet. The app has not been built or tested on the owner's physical handset in this workflow.
