import { createServer } from "node:http";
import { WebSocketServer, WebSocket } from "ws";
import { randomUUID } from "node:crypto";

const PORT = Number(process.env.PORT || 8080);
const MAX_ROOM_SIZE = 2;
const MAX_MESSAGE_BYTES = 16 * 1024;
const rooms = new Map();

const httpServer = createServer((request, response) => {
  if (request.url === "/health") {
    response.writeHead(200, { "content-type": "application/json" });
    response.end(JSON.stringify({ service: "duckcall-signaling", status: "ok" }));
    return;
  }

  response.writeHead(404, { "content-type": "application/json" });
  response.end(JSON.stringify({ error: "not_found" }));
});

const wss = new WebSocketServer({
  noServer: true,
  maxPayload: MAX_MESSAGE_BYTES
});

httpServer.on("upgrade", (request, socket, head) => {
  if (request.url !== "/signal") {
    socket.write("HTTP/1.1 404 Not Found\r\n\r\n");
    socket.destroy();
    return;
  }

  wss.handleUpgrade(request, socket, head, (webSocket) => {
    wss.emit("connection", webSocket);
  });
});

function send(socket, message) {
  if (socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(message));
  }
}

function removeFromRoom(socket) {
  if (!socket.roomId) return;

  const members = rooms.get(socket.roomId);
  if (members) {
    members.delete(socket);
    for (const peer of members) {
      send(peer, { type: "peer-left" });
    }
    if (members.size === 0) rooms.delete(socket.roomId);
  }

  socket.roomId = undefined;
}

wss.on("connection", (socket) => {
  socket.clientId = randomUUID();

  send(socket, { type: "connected", clientId: socket.clientId });

  socket.on("message", (raw) => {
    let message;
    try {
      message = JSON.parse(raw.toString());
    } catch {
      send(socket, { type: "error", code: "invalid_json" });
      return;
    }

    if (!message || typeof message.type !== "string") {
      send(socket, { type: "error", code: "invalid_message" });
      return;
    }

    if (message.type === "join") {
      const roomId = typeof message.roomId === "string" ? message.roomId.trim() : "";
      if (!/^[a-zA-Z0-9_-]{6,64}$/.test(roomId)) {
        send(socket, { type: "error", code: "invalid_room_id" });
        return;
      }
      if (socket.roomId) {
        send(socket, { type: "error", code: "already_in_room" });
        return;
      }

      const members = rooms.get(roomId) || new Set();
      if (members.size >= MAX_ROOM_SIZE) {
        send(socket, { type: "error", code: "room_full" });
        return;
      }

      members.add(socket);
      rooms.set(roomId, members);
      socket.roomId = roomId;
      send(socket, { type: "joined", roomId, peers: members.size - 1 });

      if (members.size === 2) {
        for (const peer of members) {
          if (peer !== socket) send(peer, { type: "peer-joined" });
        }
      }
      return;
    }

    if (message.type === "leave") {
      removeFromRoom(socket);
      send(socket, { type: "left" });
      return;
    }

    const allowedSignalTypes = new Set(["offer", "answer", "ice-candidate"]);
    if (!allowedSignalTypes.has(message.type)) {
      send(socket, { type: "error", code: "unsupported_message_type" });
      return;
    }

    if (!socket.roomId) {
      send(socket, { type: "error", code: "not_in_room" });
      return;
    }

    const members = rooms.get(socket.roomId) || new Set();
    for (const peer of members) {
      if (peer !== socket) {
        send(peer, {
          type: message.type,
          from: socket.clientId,
          payload: message.payload ?? null
        });
      }
    }
  });

  socket.on("close", () => removeFromRoom(socket));
  socket.on("error", () => removeFromRoom(socket));
});

httpServer.listen(PORT, "0.0.0.0", () => {
  console.log(`DuckCall signaling server listening on port ${PORT}`);
});
