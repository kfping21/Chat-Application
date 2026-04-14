const { query, queryOne, relativeTime } = require("./commonStore");

async function listNotifications(userId, { page, size }) {
  const offset = (page - 1) * size;
  const unreadRow = await queryOne(
    "SELECT COUNT(*) AS cnt FROM notifications WHERE user_id = ? AND is_read = 0 AND type <> 'auth_session'",
    [userId]
  );
  const rows = await query(
    `SELECT id, type, ref_id, payload, is_read, created_at
     FROM notifications
     WHERE user_id = ?
       AND type <> 'auth_session'
     ORDER BY created_at DESC
     LIMIT ? OFFSET ?`,
    [userId, size, offset]
  );
  return {
    page,
    size,
    unreadCount: Number(unreadRow.cnt),
    items: rows.map((row) => ({
      id: Number(row.id),
      type: row.type,
      refId: row.ref_id ? Number(row.ref_id) : null,
      payload: row.payload,
      isRead: row.is_read === 1,
      createdAt: row.created_at,
      timeText: relativeTime(row.created_at)
    }))
  };
}

async function markNotificationRead(userId, notificationId) {
  const result = await query(
    "UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ? AND type <> 'auth_session'",
    [notificationId, userId]
  );
  return (result.affectedRows || 0) > 0;
}

async function markAllNotificationsRead(userId) {
  const result = await query(
    "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0 AND type <> 'auth_session'",
    [userId]
  );
  return Number(result.affectedRows || 0);
}

async function deleteNotification(userId, notificationId) {
  const result = await query(
    "DELETE FROM notifications WHERE id = ? AND user_id = ? AND type <> 'auth_session'",
    [notificationId, userId]
  );
  return Number(result.affectedRows || 0) > 0;
}

async function clearNotifications(userId, { readOnly }) {
  const result = readOnly
    ? await query(
      "DELETE FROM notifications WHERE user_id = ? AND type <> 'auth_session' AND is_read = 1",
      [userId]
    )
    : await query(
      "DELETE FROM notifications WHERE user_id = ? AND type <> 'auth_session'",
      [userId]
    );
  return Number(result.affectedRows || 0);
}

module.exports = {
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  deleteNotification,
  clearNotifications
};
