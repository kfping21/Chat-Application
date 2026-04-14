const { getUserId, readPagination, notificationReadPath, relativeTime } = require("./helpers");
const { ensureUserExists, getNotificationList, markAllNotificationsRead, markNotificationRead } = require("../store/v1");

async function handleV1NotificationRoutes(req, res, url, deps) {
  const { json } = deps;

  if (req.method === "GET" && url.pathname === "/api/v1/notifications") {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const { page, limit, offset } = readPagination(url, 20, 100);
    const { unreadRow, rows } = await getNotificationList(userId, { limit, offset });
    return json(res, 200, {
      page,
      limit,
      unreadCount: Number(unreadRow.cnt),
      items: rows.map((row) => ({
        id: row.id,
        type: row.type,
        refId: row.ref_id,
        payload: row.payload,
        isRead: row.is_read === 1,
        createdAt: row.created_at,
        timeText: relativeTime(row.created_at)
      }))
    });
  }

  if (req.method === "POST" && url.pathname === "/api/v1/notifications/read-all") {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const result = await markAllNotificationsRead(userId);
    return json(res, 200, { updatedCount: result.affectedRows || 0 });
  }

  const notificationId = notificationReadPath(url.pathname);
  if (req.method === "POST" && notificationId) {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const result = await markNotificationRead(notificationId, userId);
    if (!result.affectedRows) return json(res, 404, { error: "notification not found" });
    return json(res, 200, { ok: true });
  }

  return false;
}

module.exports = { handleV1NotificationRoutes };
