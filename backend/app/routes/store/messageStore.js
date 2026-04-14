const { withTransaction } = require("../../db");
const { query, queryOne, ensureUserExists, relativeTime } = require("./commonStore");

async function listInboxMessages(userId, { page, size }) {
  const offset = (page - 1) * size;
  const unreadRow = await queryOne(
    "SELECT COUNT(*) AS cnt FROM private_messages WHERE receiver_user_id = ? AND is_read = 0",
    [userId]
  );
  const rows = await query(
    `SELECT pm.id, pm.sender_user_id, pm.receiver_user_id, pm.content, pm.is_read, pm.created_at, u.anonymous_name AS sender_name
     FROM private_messages pm
     JOIN users u ON u.id = pm.sender_user_id
     WHERE pm.receiver_user_id = ?
     ORDER BY pm.created_at DESC
     LIMIT ? OFFSET ?`,
    [userId, size, offset]
  );
  return {
    page,
    size,
    unreadCount: Number(unreadRow.cnt),
    items: rows.map((row) => ({
      id: Number(row.id),
      senderUserId: Number(row.sender_user_id),
      senderName: row.sender_name,
      receiverUserId: Number(row.receiver_user_id),
      content: row.content,
      isRead: row.is_read === 1,
      createdAt: row.created_at,
      timeText: relativeTime(row.created_at)
    }))
  };
}

async function listSentMessages(userId, { page, size }) {
  const offset = (page - 1) * size;
  const rows = await query(
    `SELECT pm.id, pm.sender_user_id, pm.receiver_user_id, pm.content, pm.is_read, pm.created_at, u.anonymous_name AS receiver_name
     FROM private_messages pm
     JOIN users u ON u.id = pm.receiver_user_id
     WHERE pm.sender_user_id = ?
     ORDER BY pm.created_at DESC
     LIMIT ? OFFSET ?`,
    [userId, size, offset]
  );
  return {
    page,
    size,
    items: rows.map((row) => ({
      id: Number(row.id),
      senderUserId: Number(row.sender_user_id),
      receiverUserId: Number(row.receiver_user_id),
      receiverName: row.receiver_name,
      content: row.content,
      isRead: row.is_read === 1,
      createdAt: row.created_at,
      timeText: relativeTime(row.created_at)
    }))
  };
}

async function listConversationMessages(userId, peerUserId, { page, size }) {
  if (!(await ensureUserExists(userId))) return null;
  if (!(await ensureUserExists(peerUserId))) return false;
  const offset = (page - 1) * size;
  const rows = await query(
    `SELECT id, sender_user_id, receiver_user_id, content, is_read, created_at
     FROM private_messages
     WHERE (sender_user_id = ? AND receiver_user_id = ?)
        OR (sender_user_id = ? AND receiver_user_id = ?)
     ORDER BY created_at DESC
     LIMIT ? OFFSET ?`,
    [userId, peerUserId, peerUserId, userId, size, offset]
  );
  return {
    page,
    size,
    peerUserId: Number(peerUserId),
    items: rows.map((row) => ({
      id: Number(row.id),
      senderUserId: Number(row.sender_user_id),
      receiverUserId: Number(row.receiver_user_id),
      content: row.content,
      isRead: row.is_read === 1,
      createdAt: row.created_at,
      timeText: relativeTime(row.created_at)
    }))
  };
}

async function listMessageConversations(userId, { page, size }) {
  if (!(await ensureUserExists(userId))) return null;
  const offset = (page - 1) * size;
  const rows = await query(
    `SELECT c.peer_user_id,
            c.last_message_at,
            c.last_content,
            c.last_sender_user_id,
            c.last_receiver_user_id,
            c.last_is_read,
            c.unread_count,
            u.anonymous_name AS peer_name
     FROM (
       SELECT peer_user_id,
              MAX(created_at) AS last_message_at,
              SUBSTRING_INDEX(
                GROUP_CONCAT(content ORDER BY created_at DESC SEPARATOR '\u001F'),
                '\u001F',
                1
              ) AS last_content,
              SUBSTRING_INDEX(
                GROUP_CONCAT(sender_user_id ORDER BY created_at DESC SEPARATOR ','),
                ',',
                1
              ) AS last_sender_user_id,
              SUBSTRING_INDEX(
                GROUP_CONCAT(receiver_user_id ORDER BY created_at DESC SEPARATOR ','),
                ',',
                1
              ) AS last_receiver_user_id,
              SUBSTRING_INDEX(
                GROUP_CONCAT(is_read ORDER BY created_at DESC SEPARATOR ','),
                ',',
                1
              ) AS last_is_read,
              SUM(CASE WHEN receiver_user_id = ? AND is_read = 0 THEN 1 ELSE 0 END) AS unread_count
       FROM (
         SELECT CASE
                  WHEN sender_user_id = ? THEN receiver_user_id
                  ELSE sender_user_id
                END AS peer_user_id,
                sender_user_id,
                receiver_user_id,
                content,
                is_read,
                created_at
         FROM private_messages
         WHERE sender_user_id = ? OR receiver_user_id = ?
       ) x
       GROUP BY peer_user_id
     ) c
     JOIN users u ON u.id = c.peer_user_id
     ORDER BY c.last_message_at DESC
     LIMIT ? OFFSET ?`,
    [userId, userId, userId, userId, size, offset]
  );

  return {
    page,
    size,
    items: rows.map((row) => ({
      peerUserId: Number(row.peer_user_id),
      peerName: row.peer_name,
      unreadCount: Number(row.unread_count || 0),
      lastMessage: {
        senderUserId: Number(row.last_sender_user_id),
        receiverUserId: Number(row.last_receiver_user_id),
        content: row.last_content,
        isRead: Number(row.last_is_read) === 1,
        createdAt: row.last_message_at,
        timeText: relativeTime(row.last_message_at)
      }
    }))
  };
}

async function sendMessage(senderUserId, receiverUserId, content) {
  if (!(await ensureUserExists(senderUserId))) return null;
  if (!(await ensureUserExists(receiverUserId))) return false;
  const insertedId = await withTransaction(async (conn) => {
    const [inserted] = await conn.query(
      "INSERT INTO private_messages (sender_user_id, receiver_user_id, content, is_read) VALUES (?, ?, ?, 0)",
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
    return Number(inserted.insertId);
  });
  const row = await queryOne(
    "SELECT id, sender_user_id, receiver_user_id, content, is_read, created_at FROM private_messages WHERE id = ? LIMIT 1",
    [insertedId]
  );
  return {
    id: Number(row.id),
    senderUserId: Number(row.sender_user_id),
    receiverUserId: Number(row.receiver_user_id),
    content: row.content,
    isRead: row.is_read === 1,
    createdAt: row.created_at,
    timeText: relativeTime(row.created_at)
  };
}

async function markMessageRead(userId, messageId) {
  const message = await queryOne(
    "SELECT id, sender_user_id, receiver_user_id, is_read FROM private_messages WHERE id = ? AND receiver_user_id = ? LIMIT 1",
    [messageId, userId]
  );
  if (!message) return null;
  await query("UPDATE private_messages SET is_read = 1 WHERE id = ? AND receiver_user_id = ?", [messageId, userId]);
  return {
    messageId: Number(message.id),
    senderUserId: Number(message.sender_user_id),
    receiverUserId: Number(message.receiver_user_id),
    alreadyRead: Number(message.is_read) === 1,
    readAt: new Date().toISOString()
  };
}

module.exports = {
  listInboxMessages,
  listSentMessages,
  listMessageConversations,
  listConversationMessages,
  sendMessage,
  markMessageRead
};
