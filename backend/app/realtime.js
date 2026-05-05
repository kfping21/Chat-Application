const { WebSocketServer } = require("ws");
const { getSession } = require("./routes/store");

const HEARTBEAT_INTERVAL_MS = 30000;

const userSockets = new Map();
const socketMeta = new WeakMap();

function addUserSocket(userId, ws) {
  if (!userSockets.has(userId)) userSockets.set(userId, new Set());
  userSockets.get(userId).add(ws);
}

function removeUserSocket(userId, ws) {
  const sockets = userSockets.get(userId);
  if (!sockets) return;
  sockets.delete(ws);
  if (sockets.size === 0) userSockets.delete(userId);
}

function safeSend(ws, event, data) {
  if (ws.readyState !== ws.OPEN) return;
  ws.send(JSON.stringify({ event, data, ts: new Date().toISOString() }));
}

function broadcastToUser(userId, event, data) {
  const sockets = userSockets.get(userId);
  if (!sockets) return 0;
  let count = 0;
  for (const ws of sockets) {
    safeSend(ws, event, data);
    count += 1;
  }
  return count;
}

async function parseUserIdFromRequest(req) {
  const url = new URL(req.url, "http://localhost");
  const tokenFromQuery = String(url.searchParams.get("token") || "").trim();
  const auth = req.headers.authorization || "";
  const headerMatch = auth.match(/^Bearer\s+(.+)$/i);
  const token = tokenFromQuery || (headerMatch ? headerMatch[1].trim() : "");
  if (!token) return null;
  const session = await getSession(token);
  return session ? session.userId : null;
}

function createRealtime(server) {
  const wss = new WebSocketServer({ server, path: "/ws" });
  let closed = false;

  wss.on("connection", async (ws, req) => {
    try {
      const userId = await parseUserIdFromRequest(req);
      if (!userId) {
        ws.close(1008, "unauthorized");
        return;
      }

      socketMeta.set(ws, { userId, isAlive: true });
      addUserSocket(userId, ws);
      safeSend(ws, "ws.ready", { userId });

      ws.on("pong", () => {
        const meta = socketMeta.get(ws);
        if (meta) meta.isAlive = true;
      });

      ws.on("message", (raw) => {
        try {
          const message = JSON.parse(raw.toString("utf8"));
          if (message?.type === "ping") {
            safeSend(ws, "ws.pong", { ok: true });
          }
        } catch {
          safeSend(ws, "ws.error", { message: "invalid message format" });
        }
      });

      ws.on("close", () => {
        const meta = socketMeta.get(ws);
        if (!meta) return;
        removeUserSocket(meta.userId, ws);
      });
    } catch {
      ws.close(1011, "internal_error");
    }
  });

  const heartbeat = setInterval(() => {
    for (const client of wss.clients) {
      const meta = socketMeta.get(client);
      if (!meta) continue;
      if (!meta.isAlive) {
        client.terminate();
        continue;
      }
      meta.isAlive = false;
      client.ping();
    }
  }, HEARTBEAT_INTERVAL_MS);

  function close() {
    if (closed) return;
    closed = true;
    clearInterval(heartbeat);
    for (const client of wss.clients) client.close(1001, "server_shutdown");
    wss.close();
  }

  return {
    close,
    emitMessageCreated(message) {
      broadcastToUser(message.receiverUserId, "message.created", message);
      broadcastToUser(message.senderUserId, "message.sent", message);
    },
    emitMessageRead(payload) {
      broadcastToUser(payload.notifyUserId, "message.read", {
        messageId: payload.messageId,
        readerUserId: payload.readerUserId,
        readAt: payload.readAt
      });
    }
  };
}

module.exports = {
  createRealtime
};
