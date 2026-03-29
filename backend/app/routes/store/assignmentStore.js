const { query, queryOne } = require("../../db");

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
    updatedAt: row.updated_at
  };
}

async function createUser({ username, password, displayName }) {
  const existing = await getUserByUsername(username);
  if (existing) return null;
  const result = await query("INSERT INTO users (anonymous_name, avatar_color) VALUES (?, ?)", [username, password]);
  const created = {
    id: Number(result.insertId),
    username,
    password,
    displayName: username,
    createdAt: new Date().toISOString()
  };
  return created;
}

async function getUserByUsername(username) {
  const user = await queryOne(
    "SELECT id, anonymous_name, avatar_color, joined_at FROM users WHERE anonymous_name = ? LIMIT 1",
    [username]
  );
  if (!user) return null;
  return {
    id: Number(user.id),
    username: user.anonymous_name,
    password: user.avatar_color,
    displayName: user.anonymous_name,
    createdAt: user.joined_at
  };
}

async function createSession(userId) {
  const token = `tk_${Math.random().toString(36).slice(2)}${Date.now().toString(36)}`;
  await query(
    `INSERT INTO notifications (user_id, type, ref_id, payload, is_read, created_at)
     VALUES (?, 'auth_session', ?, JSON_OBJECT('token', ?), 0, CURRENT_TIMESTAMP)`,
    [userId, userId, token]
  );
  return token;
}

async function getSession(token) {
  const row = await queryOne(
    `SELECT user_id, created_at
     FROM notifications
     WHERE type = 'auth_session'
       AND is_read = 0
       AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.token')) = ?
     ORDER BY id DESC
     LIMIT 1`,
    [token]
  );
  if (!row) return null;
  return { userId: Number(row.user_id), createdAt: row.created_at };
}

async function deleteSession(token) {
  await query(
    `UPDATE notifications
     SET is_read = 1
     WHERE type = 'auth_session'
       AND is_read = 0
       AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.token')) = ?`,
    [token]
  );
}

async function listPosts({ page, size, emotionCode }) {
  const params = [];
  let whereSql = "WHERE p.is_public = 1";
  if (emotionCode) {
    whereSql += " AND e.code = ?";
    params.push(emotionCode);
  }
  const countRow = await queryOne(
    `SELECT COUNT(*) AS total
     FROM posts p
     JOIN emotions e ON e.id = p.emotion_id
     ${whereSql}`,
    params
  );
  const offset = (page - 1) * size;
  const rows = await query(
    `SELECT p.id, p.user_id, u.anonymous_name, p.content, p.allow_comments, p.is_public, p.likes_count, p.comments_count,
            p.created_at, p.updated_at, e.code AS emotion_code, e.display_name AS emotion_name
     FROM posts p
     JOIN users u ON u.id = p.user_id
     JOIN emotions e ON e.id = p.emotion_id
     ${whereSql}
     ORDER BY p.created_at DESC
     LIMIT ? OFFSET ?`,
    [...params, size, offset]
  );
  return { total: Number(countRow.total), items: rows.map(mapPostRow) };
}

async function createPost(userId, payload) {
  const emotion = await queryOne("SELECT id FROM emotions WHERE code = ? LIMIT 1", [payload.emotionCode]);
  if (!emotion) return null;
  const insertResult = await query(
    `INSERT INTO posts (user_id, content, emotion_id, allow_comments, is_public)
     VALUES (?, ?, ?, ?, ?)`,
    [userId, payload.content, emotion.id, payload.allowComments ? 1 : 0, payload.isPublic ? 1 : 0]
  );
  return getPostById(insertResult.insertId);
}

async function getPostById(postId) {
  const row = await queryOne(
    `SELECT p.id, p.user_id, u.anonymous_name, p.content, p.allow_comments, p.is_public, p.likes_count, p.comments_count,
            p.created_at, p.updated_at, e.code AS emotion_code, e.display_name AS emotion_name
     FROM posts p
     JOIN users u ON u.id = p.user_id
     JOIN emotions e ON e.id = p.emotion_id
     WHERE p.id = ?
     LIMIT 1`,
    [postId]
  );
  return row ? mapPostRow(row) : null;
}

async function updatePost(userId, postId, payload) {
  const existing = await queryOne("SELECT id FROM posts WHERE id = ? AND user_id = ? LIMIT 1", [postId, userId]);
  if (!existing) return null;

  let emotionId = null;
  if (payload.emotionCode !== undefined) {
    const emotion = await queryOne("SELECT id FROM emotions WHERE code = ? LIMIT 1", [payload.emotionCode]);
    if (!emotion) return false;
    emotionId = emotion.id;
  }

  await query(
    `UPDATE posts
     SET content = COALESCE(?, content),
         emotion_id = COALESCE(?, emotion_id),
         allow_comments = COALESCE(?, allow_comments),
         is_public = COALESCE(?, is_public)
     WHERE id = ?`,
    [
      payload.content ?? null,
      emotionId,
      payload.allowComments === undefined ? null : payload.allowComments ? 1 : 0,
      payload.isPublic === undefined ? null : payload.isPublic ? 1 : 0,
      postId
    ]
  );
  return getPostById(postId);
}

async function deletePost(userId, postId) {
  const existing = await queryOne("SELECT id FROM posts WHERE id = ? AND user_id = ? LIMIT 1", [postId, userId]);
  if (!existing) return null;
  await query("DELETE FROM posts WHERE id = ? AND user_id = ?", [postId, userId]);
  return { id: postId };
}

module.exports = {
  createUser,
  getUserByUsername,
  createSession,
  getSession,
  deleteSession,
  listPosts,
  createPost,
  getPostById,
  updatePost,
  deletePost
};
