const { handleAuthRoutes } = require("./authRoutes");
const { handlePostRoutes } = require("./postRoutes");
const { handleMetaRoutes } = require("./metaRoutes");
const { handleInteractionRoutes } = require("./interactionRoutes");
const { handleProfileRoutes } = require("./profileRoutes");
const { handleNotificationRoutes } = require("./notificationRoutes");
const { handleMessageRoutes } = require("./messageRoutes");
const { sendApiJson } = require("./utils/responseUtils");

const routeHandlers = [
  handleAuthRoutes,
  handlePostRoutes,
  handleMetaRoutes,
  handleInteractionRoutes,
  handleProfileRoutes,
  handleNotificationRoutes,
  handleMessageRoutes
];

async function handleApiRoutes(req, res, url, deps) {
  if (!url.pathname.startsWith("/api/")) return false;
  if (url.pathname.startsWith("/api/v1/")) return false;

  for (const handleRoute of routeHandlers) {
    if (await handleRoute(req, res, url, deps)) {
      return true;
    }
  }

  if (url.pathname === "/api" || url.pathname === "/api/") {
    sendApiJson(deps.json, res, 200, "ok", { service: "treehole-api" });
    return true;
  }

  return false;
}

module.exports = {
  handleApiRoutes
};
