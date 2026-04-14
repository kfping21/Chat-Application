const { listEmotions, listHotTopics, listTopics } = require("./store");
const { sendApiJson } = require("./utils/responseUtils");

async function handleMetaRoutes(req, res, url, deps) {
  const { json } = deps;

  if (req.method === "GET" && url.pathname === "/api/meta/emotions") {
    const items = await listEmotions();
    sendApiJson(json, res, 200, "ok", { items });
    return true;
  }

  if (req.method === "GET" && url.pathname === "/api/discover/topics/hot") {
    const items = await listHotTopics();
    sendApiJson(json, res, 200, "ok", { items });
    return true;
  }

  if (req.method === "GET" && url.pathname === "/api/topics") {
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const hotOnly = url.searchParams.get("hotOnly") === "true";
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listTopics({ page, size, hotOnly });
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  return false;
}

module.exports = {
  handleMetaRoutes
};
