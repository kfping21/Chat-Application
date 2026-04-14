const { readPagination, postIdFromPath, relativeTime } = require("./helpers");
const { listHomeFeed, getTopicsMap, mapPostRow, getPostDetail } = require("../store/v1");

async function handleV1FeedRoutes(req, res, url, deps) {
  const { json } = deps;

  if (req.method === "GET" && url.pathname === "/api/v1/home/feed") {
    const { page, limit, offset } = readPagination(url, 20, 50);
    const rows = await listHomeFeed({ limit, offset });
    const topicsMap = await getTopicsMap(rows.map((row) => row.id));
    return json(res, 200, {
      page,
      limit,
      items: rows.map((row) => ({ ...mapPostRow(relativeTime, row), topics: topicsMap.get(row.id) || [] }))
    });
  }

  const postId = postIdFromPath(url.pathname);
  if (req.method === "GET" && postId) {
    const detail = await getPostDetail(relativeTime, postId);
    if (!detail) return json(res, 404, { error: "post not found" });
    return json(res, 200, detail);
  }

  return false;
}

module.exports = { handleV1FeedRoutes };
