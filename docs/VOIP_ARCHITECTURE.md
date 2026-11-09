# DuckCall internet-calling architecture

## Direction

DuckCall will own its calling application and signaling service. The first milestone is app-to-app audio calling between two DuckCall accounts. The existing cellular button remains a separate handoff to Android's system dialer.

## Components

1. **Android client:** Kotlin/Compose UI, account session, call controls, Android audio routing, microphone permission, and WebRTC peer connection.
2. **Signaling service:** authenticated WebSocket service that exchanges call setup messages (offer, answer, ICE candidates). It does not carry voice audio.
3. **WebRTC media:** encrypted real-time audio between devices where network conditions permit.
4. **STUN/TURN:** helps peers discover network routes and relays encrypted media when direct connectivity is blocked. DuckCall should operate its own TURN service and issue short-lived credentials.
5. **Account/API service:** account registration, login, device registration, contact discovery (with consent), and call authorization.
6. **Push notifications:** required for reliable incoming-call alerts when Android has suspended the app.

## Milestones

- [x] Define the architecture and signaling message contract.
- [x] Add a local-development signaling relay prototype.
- [ ] Add authenticated accounts and short-lived call-room authorization.
- [ ] Add Android WebRTC dependency and peer-connection lifecycle.
- [ ] Add microphone permission and call audio controls.
- [ ] Add TURN deployment and temporary credentials.
- [ ] Add Firebase Cloud Messaging for incoming-call wake-up/notification flow.
- [ ] Add persistent call history and automated tests.
- [ ] Add SIP/PSTN gateway integration if DuckCall users should call ordinary phone numbers over the internet.

## Signaling message contract

Client to server:

- `join`: `{ "type": "join", "roomId": "..." }`
- `offer`, `answer`, `ice-candidate`: `{ "type": "...", "payload": { ... } }`
- `leave`: `{ "type": "leave" }`

Server to client:

- `connected`: server-issued connection identifier
- `joined`: room joined and current peer count
- `peer-joined` / `peer-left`: peer presence events
- `offer`, `answer`, `ice-candidate`: relayed signaling payload
- `left` / `error`: leave confirmation or structured error

## Deployment and privacy principles

The prototype server is not production safe. Do not deploy it publicly until authentication, room authorization, rate limits, schema validation, TLS, logging controls, and operational monitoring are implemented. Never store or log call audio. Keep signaling metadata to the minimum needed to establish and troubleshoot calls.

## Telephone network distinction

App-to-app internet calling can be built on DuckCall's own infrastructure. Calling ordinary mobile or landline numbers over the internet requires PSTN connectivity, normally through a SIP trunk or carrier gateway. That integration can be selected after the first app-to-app call milestone.
