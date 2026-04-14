const { query, queryOne } = require("../../db");

function relativeTime(isoString) {
  const diff = Date.now() - new Date(isoString).getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))}分钟前`;
  if (diff < day) return `${Math.floor(diff / hour)}小时前`;
  return `${Math.floor(diff / day)}天前`;
}

function uniquePositiveIntegers(values) {
  return [...new Set(values.filter((v) => Number.isInteger(v) && v > 0))];
}

async function ensureUserExists(userId) {
  const user = await queryOne("SELECT id FROM users WHERE id = ? LIMIT 1", [userId]);
  return Boolean(user);
}

async function ensureTopicsExist(topicIds) {
  if (topicIds.length === 0) return true;
  const placeholders = topicIds.map(() => "?").join(",");
  const rows = await query(`SELECT id FROM topics WHERE id IN (${placeholders})`, topicIds);
  const found = new Set(rows.map((row) => Number(row.id)));
  return topicIds.every((id) => found.has(id));
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
    const postId = Number(row.post_id);
    if (!topicsMap.has(postId)) topicsMap.set(postId, []);
    topicsMap.get(postId).push({ id: Number(row.id), name: row.name });
  }
  return topicsMap;
}

function mapPostRow(row) {
  return {
    id: Number(row.id),
    userId: Number(row.user_id),
    anonymousName: row.anonymous_name,
    content: row.content,
    emotionCode: row.emotion_code,
    emotionName: row.emotion_name,
    allowComments: row.allow_comments === 1,
    isPublic: row.is_public === 1,
    likesCount: Number(row.likes_count),
    commentsCount: Number(row.comments_count),
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    timeText: relativeTime(row.created_at)
  };
}

function mapCommentRow(row) {
  return {
    id: Number(row.id),
    postId: Number(row.post_id),
    userId: Number(row.user_id),
    parentCommentId: row.parent_comment_id ? Number(row.parent_comment_id) : null,
    floorNo: Number(row.floor_no),
    content: row.content,
    likesCount: Number(row.likes_count),
    createdAt: row.created_at,
    timeText: relativeTime(row.created_at)
  };
}

module.exports = {
  query,
  queryOne,
  relativeTime,
  uniquePositiveIntegers,
  ensureUserExists,
  ensureTopicsExist,
  getTopicsMap,
  mapPostRow,
  mapCommentRow
};
