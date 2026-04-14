const { listHotTopics, listEmotions } = require("../store/v1");

async function handleV1MetaRoutes(req, res, url, deps) {
  const { json, query } = deps;

  if (req.method === "GET" && url.pathname === "/api/v1/discover/topics/hot") {
    const rows = await listHotTopics(query);
    return json(res, 200, { items: rows });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/meta/emotions") {
    const rows = await listEmotions(query);
    return json(res, 200, {
      items: rows.map((row) => ({ id: row.id, code: row.code, name: row.display_name }))
    });
  }

  return false;
}

module.exports = { handleV1MetaRoutes };
