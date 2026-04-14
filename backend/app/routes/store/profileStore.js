const { query, queryOne, getTopicsMap, mapPostRow, relativeTime } = require("./commonStore");

async function listRecentEncounters(userId, limit) {
  const rows = await query(
    `SELECT e.target_user_id, e.met_at, u.anonymous_name, u.avatar_color
     FROM encounters e
     JOIN users u ON u.id = e.target_user_id
     WHERE e.user_id = ?
     ORDER BY e.met_at DESC
     LIMIT ?`,
    [userId, limit]
  );
  return rows.map((row) => ({
    userId: Number(row.target_user_id),
    anonymousName: row.anonymous_name,
    avatarColor: row.avatar_color,
    metAt: row.met_at,
    timeText: relativeTime(row.met_at)
  }));
}

async function getMeSummary(userId) {
  const user = await queryOne("SELECT id, anonymous_name, joined_at FROM users WHERE id = ? LIMIT 1", [userId]);
  if (!user) return null;
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
    `SELECT p.id, p.user_id, u.anonymous_name, p.content, p.allow_comments, p.is_public, p.likes_count, p.comments_count,
            p.created_at, p.updated_at, e.code AS emotion_code, e.display_name AS emotion_name
     FROM posts p
     JOIN users u ON u.id = p.user_id
     JOIN emotions e ON e.id = p.emotion_id
     WHERE p.user_id = ?
     ORDER BY p.created_at DESC
     LIMIT 10`,
    [userId]
  );
  const mappedPosts = recentPosts.map(mapPostRow);
  const topicsMap = await getTopicsMap(mappedPosts.map((p) => p.id));
  return {
    profile: {
      id: Number(user.id),
      anonymousName: user.anonymous_name,
      joinedAt: user.joined_at
    },
    stats: {
      secretsCount: Number(postCount.cnt),
      collectedEchoesCount: Number(likedCount.cnt),
      encountersCount: Number(encounterCount.cnt)
    },
    myPosts: mappedPosts.map((p) => ({ ...p, topics: topicsMap.get(p.id) || [] }))
  };
}

module.exports = {
  listRecentEncounters,
  getMeSummary
};
