# DuckCall signaling server

A small WebSocket signaling relay for the first DuckCall-to-DuckCall WebRTC prototype.

## Run locally

Requirements: Node.js 20 or newer.

```sh
cd server
npm install
npm start
```

- Health check: `GET http://localhost:8080/health`
- WebSocket signaling: `ws://localhost:8080/signal`

A client connects and sends:

```json
{ "type": "join", "roomId": "a-random-room-id" }
```

The first peer waits. When a second peer joins, the first receives `peer-joined`. Peers can relay `offer`, `answer`, and `ice-candidate` messages. A room is limited to two connected peers.

## Important security status

**Prototype only. Do not expose this service to the public internet or use it for real calls yet.**

This first version deliberately demonstrates signaling flow, but it does not yet include user authentication, authorization, abuse prevention, persistent call records, or TURN credential issuance. Room IDs are not authentication. Before production, add authenticated short-lived call-room tokens, strict message schemas, connection/rate limits, TLS (WSS), monitoring, and secure TURN credentials. WebRTC media encryption does not replace server-side access control.

The relay does not carry voice audio. WebRTC peers exchange media directly when possible, or through a TURN relay when required. A production deployment needs a properly configured STUN/TURN service and firewall rules.
