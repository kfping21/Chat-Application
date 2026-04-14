const { query, queryOne } = require("./commonStore");

async function getNotificationList(userId, { limit, offset }) {
  const unreadRow = await queryOne("SELECT COUNT(*) AS cnt FROM notifications WHERE user_id = ? AND is_read = 0", [userId]);
  const rows = await query(
    `SELECT id, type, ref_id, payload, is_read, created_at
     FROM notifications
     WHERE user_id = ?
     ORDER BY created_at DESC
     LIMIT ? OFFSET ?`,
    [userId, limit, offset]
  );
  return { unreadRow, rows };
}

async function markAllNotificationsRead(userId) {
  return query("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", [userId]);
}

async function markNotificationRead(notificationId, userId) {
  return query("UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?", [notificationId, userId]);
}

module.exports = {
  getNotificationList,
  markAllNotificationsRead,
  markNotificationRead
};
