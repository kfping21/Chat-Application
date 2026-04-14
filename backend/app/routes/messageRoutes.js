const { requireAuth } = require("./authRoutes");
const {
  listInboxMessages,
  listSentMessages,
  listMessageConversations,
  listConversationMessages,
  sendMessage,
  markMessageRead
} = require("./store");
const { sendApiJson, createApiError, parseApiBody } = require("./utils/responseUtils");

async function handleMessageRoutes(req, res, url, deps) {
  const { json, parseJsonBody, realtime } = deps;

  if (req.method === "GET" && url.pathname === "/api/messages/inbox") {
    const { userId } = await requireAuth(req);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listInboxMessages(userId, { page, size });
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  if (req.method === "GET" && url.pathname === "/api/messages/sent") {
    const { userId } = await requireAuth(req);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listSentMessages(userId, { page, size });
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  if (req.method === "GET" && url.pathname === "/api/messages/conversation") {
    const { userId } = await requireAuth(req);
    const peerUserId = Number(url.searchParams.get("peerUserId"));
    if (!Number.isInteger(peerUserId) || peerUserId <= 0) throw createApiError(400, "peerUserId is required");
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listConversationMessages(userId, peerUserId, { page, size });
    if (result === null || result === false) throw createApiError(404, "user not found");
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  if (req.method === "GET" && url.pathname === "/api/messages/conversations") {
    const { userId } = await requireAuth(req);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listMessageConversations(userId, { page, size });
    if (!result) throw createApiError(404, "user not found");
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  if (req.method === "POST" && url.pathname === "/api/messages") {
    const { userId } = await requireAuth(req);
    const body = await parseApiBody(parseJsonBody, req);
    const receiverUserId = Number(body.receiverUserId);
    const content = String(body.content || "").trim();
    if (!receiverUserId || receiverUserId <= 0) throw createApiError(400, "receiverUserId is required");
    if (receiverUserId === userId) throw createApiError(400, "cannot send message to self");
    if (!content || content.length > 500) throw createApiError(400, "content is required and must be <= 500 chars");
    const created = await sendMessage(userId, receiverUserId, content);
    if (created === null || created === false) throw createApiError(404, "user not found");
    if (realtime) realtime.emitMessageCreated(created);
    sendApiJson(json, res, 201, "created", created);
    return true;
  }

  const messageReadMatch = url.pathname.match(/^\/api\/messages\/(\d+)\/read$/);
  if (messageReadMatch && req.method === "POST") {
    const { userId } = await requireAuth(req);
    const readResult = await markMessageRead(userId, Number(messageReadMatch[1]));
    if (!readResult) throw createApiError(404, "message not found");
    if (!readResult.alreadyRead && realtime) {
      realtime.emitMessageRead({
        messageId: readResult.messageId,
        readerUserId: readResult.receiverUserId,
        notifyUserId: readResult.senderUserId,
        readAt: readResult.readAt
      });
    }
    sendApiJson(json, res, 200, "ok", { ok: true, alreadyRead: readResult.alreadyRead });
    return true;
  }

  return false;
}

module.exports = {
  handleMessageRoutes
};
