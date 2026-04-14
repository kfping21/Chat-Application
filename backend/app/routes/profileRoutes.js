const { requireAuth } = require("./authRoutes");
const { listRecentEncounters, getMeSummary } = require("./store");
const { sendApiJson, createApiError } = require("./utils/responseUtils");

async function handleProfileRoutes(req, res, url, deps) {
  const { json } = deps;

  if (req.method === "GET" && url.pathname === "/api/me/summary") {
    const { userId } = await requireAuth(req);
    const summary = await getMeSummary(userId);
    if (!summary) throw createApiError(404, "user not found");
    sendApiJson(json, res, 200, "ok", summary);
    return true;
  }

  if (req.method === "GET" && url.pathname === "/api/encounters/recent") {
    const { userId } = await requireAuth(req);
    const limitRaw = Number(url.searchParams.get("limit"));
    const limit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.min(100, Math.floor(limitRaw)) : 20;
    const items = await listRecentEncounters(userId, limit);
    sendApiJson(json, res, 200, "ok", { limit, items });
    return true;
  }

  return false;
}

module.exports = {
  handleProfileRoutes
};
