async function handleV1PingRoutes(req, res, url, deps) {
  const { json } = deps;
  if (req.method === "GET" && url.pathname === "/api/v1/ping") {
    return json(res, 200, { message: "pong" });
  }
  return false;
}

module.exports = { handleV1PingRoutes };
