const { query, queryOne } = require("../../../db");

async function ensureTopicsExist(topicIds) {
  if (topicIds.length === 0) return;
  const placeholders = topicIds.map(() => "?").join(",");
  const rows = await query(`SELECT id FROM topics WHERE id IN (${placeholders})`, topicIds);
  const found = new Set(rows.map((row) => row.id));
  const missing = topicIds.filter((id) => !found.has(id));
  if (missing.length > 0) {
    const err = new Error(`topicIds contain non-existing ids: ${missing.join(",")}`);
    err.statusCode = 400;
    throw err;
  }
}

async function getTopicsMap(postIds) {
  if (postIds.length === 0) return new Map();
  const placeholders = postIds.map(() => "?").join(",");
  const rows = await query(
    `SELECT pt.post_id, t.id, t.name
     FROM post_topics pt
     JOIN topics t ON t.id = pt.topic_id
     WHERE pt.post_id IN (${placeholders})
     ORDER BY t.id ASC`,
    postIds
  );
  const topicsMap = new Map();
  for (const row of rows) {
    if (!topicsMap.has(row.post_id)) topicsMap.set(row.post_id, []);
    topicsMap.get(row.post_id).push({ id: row.id, name: row.name });
  }
  return topicsMap;
}

async function ensureUserExists(userId) {
  const user = await queryOne("SELECT id FROM users WHERE id = ? LIMIT 1", [userId]);
  if (!user) {
    const err = new Error("user not found");
    err.statusCode = 404;
    throw err;
  }
}

module.exports = {
  query,
  queryOne,
  ensureTopicsExist,
  getTopicsMap,
  ensureUserExists
};
