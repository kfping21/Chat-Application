const { requireAuth } = require("./authRoutes");
const { listNotifications, markNotificationRead, markAllNotificationsRead, deleteNotification, clearNotifications } = require("./store");
const { sendApiJson, createApiError } = require("./utils/responseUtils");

async function handleNotificationRoutes(req, res, url, deps) {
  const { json } = deps;

  if (req.method === "GET" && url.pathname === "/api/notifications") {
    const { userId } = await requireAuth(req);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listNotifications(userId, { page, size });
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  if (req.method === "POST" && url.pathname === "/api/notifications/read-all") {
    const { userId } = await requireAuth(req);
    const updatedCount = await markAllNotificationsRead(userId);
    sendApiJson(json, res, 200, "ok", { updatedCount });
    return true;
  }

  if (req.method === "DELETE" && url.pathname === "/api/notifications") {
    const { userId } = await requireAuth(req);
    const readOnly = url.searchParams.get("readOnly") === "true";
    const deletedCount = await clearNotifications(userId, { readOnly });
    sendApiJson(json, res, 200, "ok", { deletedCount, readOnly });
    return true;
  }

  const notificationReadMatch = url.pathname.match(/^\/api\/notifications\/(\d+)\/read$/);
  if (notificationReadMatch && req.method === "POST") {
    const { userId } = await requireAuth(req);
    const ok = await markNotificationRead(userId, Number(notificationReadMatch[1]));
    if (!ok) throw createApiError(404, "notification not found");
    sendApiJson(json, res, 200, "ok", { ok: true });
    return true;
  }

  const notificationIdMatch = url.pathname.match(/^\/api\/notifications\/(\d+)$/);
  if (notificationIdMatch && req.method === "DELETE") {
    const { userId } = await requireAuth(req);
    const ok = await deleteNotification(userId, Number(notificationIdMatch[1]));
    if (!ok) throw createApiError(404, "notification not found");
    sendApiJson(json, res, 200, "ok", { ok: true });
    return true;
  }

  return false;
}

module.exports = {
  handleNotificationRoutes
};
