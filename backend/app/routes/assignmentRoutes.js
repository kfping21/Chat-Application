const { handleAuthRoutes } = require("./authRoutes");
const { handlePostRoutes } = require("./postRoutes");
const { sendApiJson } = require("./utils/responseUtils");

async function handleAssignmentRoutes(req, res, url, deps) {
  if (!url.pathname.startsWith("/api/")) return false;
  if (url.pathname.startsWith("/api/v1/")) return false;

  const handledByAuth = await handleAuthRoutes(req, res, url, deps);
  if (handledByAuth) return true;

  const handledByPosts = await handlePostRoutes(req, res, url, deps);
  if (handledByPosts) return true;

  if (url.pathname === "/api" || url.pathname === "/api/") {
    sendApiJson(deps.json, res, 200, "ok", { service: "assignment-api" });
    return true;
  }

  return false;
}

module.exports = {
  handleAssignmentRoutes
};
