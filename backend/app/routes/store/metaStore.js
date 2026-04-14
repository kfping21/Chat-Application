const { query } = require("./commonStore");

async function listEmotions() {
  const rows = await query("SELECT id, code, display_name FROM emotions ORDER BY id ASC");
  return rows.map((row) => ({ id: Number(row.id), code: row.code, name: row.display_name }));
}

async function listHotTopics() {
  const rows = await query("SELECT id, name FROM topics WHERE is_hot = 1 ORDER BY id ASC LIMIT 50");
  return rows.map((row) => ({ id: Number(row.id), name: row.name }));
}

async function listTopics({ page, size, hotOnly }) {
  const whereSql = hotOnly ? "WHERE is_hot = 1" : "";
  const offset = (page - 1) * size;
  const totalRow = await query(`SELECT COUNT(*) AS cnt FROM topics ${whereSql}`);
  const rows = await query(
    `SELECT id, name, is_hot
     FROM topics
     ${whereSql}
     ORDER BY id ASC
     LIMIT ? OFFSET ?`,
    [size, offset]
  );
  return {
    page,
    size,
    total: Number(totalRow[0].cnt),
    items: rows.map((row) => ({ id: Number(row.id), name: row.name, isHot: Number(row.is_hot) === 1 }))
  };
}

module.exports = {
  listEmotions,
  listHotTopics,
  listTopics
};
