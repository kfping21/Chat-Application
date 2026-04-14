const { withTransaction } = require("../../../db");
const { query, queryOne } = require("./commonStore");

async function getInboxRows(userId, { limit, offset }) {
  const unreadRow = await queryOne("SELECT COUNT(*) AS cnt FROM private_messages WHERE receiver_user_id = ? AND is_read = 0", [userId]);
  const rows = await query(
    `SELECT pm.id, pm.sender_user_id, pm.receiver_user_id, pm.content, pm.is_read, pm.created_at,
            u.anonymous_name AS sender_name
     FROM private_messages pm
     JOIN users u ON u.id = pm.sender_user_id
     WHERE pm.receiver_user_id = ?
     ORDER BY pm.created_at DESC
     LIMIT ? OFFSET ?`,
    [userId, limit, offset]
  );
  return { unreadRow, rows };
}

async function createMessageAndEncounters(senderUserId, receiverUserId, content) {
  return withTransaction(async (conn) => {
    const [inserted] = await conn.query(
      `INSERT INTO private_messages (sender_user_id, receiver_user_id, content, is_read)
       VALUES (?, ?, ?, 0)`,
      [senderUserId, receiverUserId, content]
    );
    await conn.query(
      `INSERT INTO encounters (user_id, target_user_id, met_at)
       VALUES (?, ?, CURRENT_TIMESTAMP)
       ON DUPLICATE KEY UPDATE met_at = VALUES(met_at)`,
      [senderUserId, receiverUserId]
    );
    await conn.query(
      `INSERT INTO encounters (user_id, target_user_id, met_at)
       VALUES (?, ?, CURRENT_TIMESTAMP)
       ON DUPLICATE KEY UPDATE met_at = VALUES(met_at)`,
      [receiverUserId, senderUserId]
    );
    return inserted.insertId;
  });
}

async function getMessageById(id) {
  return queryOne(
    `SELECT id, sender_user_id, receiver_user_id, content, is_read, created_at
     FROM private_messages
     WHERE id = ?
     LIMIT 1`,
    [id]
  );
}

async function markMessageRead(msgId, userId) {
  return query("UPDATE private_messages SET is_read = 1 WHERE id = ? AND receiver_user_id = ?", [msgId, userId]);
}

module.exports = {
  getInboxRows,
  createMessageAndEncounters,
  getMessageById,
  markMessageRead
};
