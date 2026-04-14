const { handleV1PingRoutes } = require("./pingRoutes");
const { handleV1FeedRoutes } = require("./feedRoutes");
const { handleV1MetaRoutes } = require("./metaRoutes");
const { handleV1PostWriteRoutes } = require("./postWriteRoutes");
const { handleV1InteractionRoutes } = require("./interactionRoutes");
const { handleV1ProfileRoutes } = require("./profileRoutes");
const { handleV1NotificationRoutes } = require("./notificationRoutes");
const { handleV1MessageRoutes } = require("./messageRoutes");

const v1RouteHandlers = [
  handleV1PingRoutes,
  handleV1FeedRoutes,
  handleV1MetaRoutes,
  handleV1PostWriteRoutes,
  handleV1InteractionRoutes,
  handleV1ProfileRoutes,
  handleV1NotificationRoutes,
  handleV1MessageRoutes
];

async function handleV1Routes(req, res, url, deps) {
  if (!url.pathname.startsWith("/api/v1/")) return false;
  for (const handleRoute of v1RouteHandlers) {
    if (await handleRoute(req, res, url, deps)) {
      return true;
    }
  }
  return false;
}

module.exports = { handleV1Routes };
