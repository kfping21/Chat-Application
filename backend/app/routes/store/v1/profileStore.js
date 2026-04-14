const { query, queryOne } = require("./commonStore");

async function getProfileSummaryRows(userId) {
  const user = await queryOne("SELECT id, anonymous_name, joined_at FROM users WHERE id = ? LIMIT 1", [userId]);
  const postCount = await queryOne("SELECT COUNT(*) AS cnt FROM posts WHERE user_id = ?", [userId]);
  const likedCount = await queryOne(
    `SELECT COUNT(*) AS cnt
     FROM post_likes pl
     JOIN posts p ON p.id = pl.post_id
     WHERE p.user_id = ?`,
    [userId]
  );
  const encounterCount = await queryOne("SELECT COUNT(*) AS cnt FROM encounters WHERE user_id = ?", [userId]);
  const recentPosts = await query(
    `SELECT p.id, p.content, p.likes_count, p.comments_count, p.created_at, e.display_name AS emotion_name
     FROM posts p
     JOIN emotions e ON e.id = p.emotion_id
     WHERE p.user_id = ?
     ORDER BY p.created_at DESC
     LIMIT 10`,
    [userId]
  );
  return { user, postCount, likedCount, encounterCount, recentPosts };
}

async function listRecentEncounters(userId, limit) {
  return query(
    `SELECT e.target_user_id, e.met_at, u.anonymous_name, u.avatar_color
     FROM encounters e
     JOIN users u ON u.id = e.target_user_id
     WHERE e.user_id = ?
     ORDER BY e.met_at DESC
     LIMIT ?`,
    [userId, limit]
  );
}

module.exports = {
  getProfileSummaryRows,
  listRecentEncounters
};
