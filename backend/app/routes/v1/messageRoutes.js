const { getUserId, readPagination, messageReadPath, relativeTime } = require("./helpers");
const { ensureUserExists, getInboxRows, createMessageAndEncounters, getMessageById, markMessageRead } = require("../store/v1");

async function handleV1MessageRoutes(req, res, url, deps) {
  const { json, parseJsonBody } = deps;

  if (req.method === "GET" && url.pathname === "/api/v1/messages/inbox") {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const { page, limit, offset } = readPagination(url, 20, 100);
    const { unreadRow, rows } = await getInboxRows(userId, { limit, offset });
    return json(res, 200, {
      page,
      limit,
      unreadCount: Number(unreadRow.cnt),
      items: rows.map((row) => ({
        id: row.id,
        senderUserId: row.sender_user_id,
        senderName: row.sender_name,
        receiverUserId: row.receiver_user_id,
        content: row.content,
        isRead: row.is_read === 1,
        createdAt: row.created_at,
        timeText: relativeTime(row.created_at)
      }))
    });
  }

  if (req.method === "POST" && url.pathname === "/api/v1/messages") {
    const senderUserId = getUserId(req);
    await ensureUserExists(senderUserId);
    const body = await parseJsonBody(req);
    const receiverUserId = Number(body.receiverUserId);
    const content = String(body.content || "").trim();

    if (!receiverUserId || receiverUserId <= 0) {
      const err = new Error("receiverUserId is required");
      err.statusCode = 400;
      throw err;
    }
    if (!content || content.length > 500) {
      const err = new Error("content is required and must be <= 500 chars");
      err.statusCode = 400;
      throw err;
    }
    if (receiverUserId === senderUserId) {
      const err = new Error("cannot send message to self");
      err.statusCode = 400;
      throw err;
    }

    await ensureUserExists(receiverUserId);
    const insertId = await createMessageAndEncounters(senderUserId, receiverUserId, content);
    const row = await getMessageById(insertId);
    return json(res, 201, {
      item: {
        id: row.id,
        senderUserId: row.sender_user_id,
        receiverUserId: row.receiver_user_id,
        content: row.content,
        isRead: row.is_read === 1,
        createdAt: row.created_at,
        timeText: relativeTime(row.created_at)
      }
    });
  }

  const msgId = messageReadPath(url.pathname);
  if (req.method === "POST" && msgId) {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const result = await markMessageRead(msgId, userId);
    if (!result.affectedRows) return json(res, 404, { error: "message not found" });
    return json(res, 200, { ok: true });
  }

  return false;
}

module.exports = { handleV1MessageRoutes };
