const { withTransaction } = require("../../db");
const {
  query,
  queryOne,
  uniquePositiveIntegers,
  ensureUserExists,
  ensureTopicsExist,
  getTopicsMap,
  mapPostRow,
  mapCommentRow
} = require("./commonStore");

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
  const items = rows.map(mapPostRow);
  const topicsMap = await getTopicsMap(items.map((item) => item.id));
  return {
    total: Number(countRow.total),
    items: items.map((item) => ({ ...item, topics: topicsMap.get(item.id) || [] }))
  };
}

async function listPostsByTopic({ topicId, page, size }) {
  const offset = (page - 1) * size;
  const countRow = await queryOne(
    `SELECT COUNT(*) AS total
     FROM posts p
     JOIN post_topics pt ON pt.post_id = p.id
     WHERE p.is_public = 1 AND pt.topic_id = ?`,
    [topicId]
  );
  const rows = await query(
    `SELECT p.id, p.user_id, u.anonymous_name, p.content, p.allow_comments, p.is_public, p.likes_count, p.comments_count,
            p.created_at, p.updated_at, e.code AS emotion_code, e.display_name AS emotion_name
     FROM posts p
     JOIN users u ON u.id = p.user_id
     JOIN emotions e ON e.id = p.emotion_id
     JOIN post_topics pt ON pt.post_id = p.id
     WHERE p.is_public = 1 AND pt.topic_id = ?
     ORDER BY p.created_at DESC
     LIMIT ? OFFSET ?`,
    [topicId, size, offset]
  );
  const items = rows.map(mapPostRow);
  const topicsMap = await getTopicsMap(items.map((item) => item.id));
  return {
    page,
    size,
    total: Number(countRow.total),
    items: items.map((item) => ({ ...item, topics: topicsMap.get(item.id) || [] }))
  };
}

async function createPost(userId, payload) {
  if (!(await ensureUserExists(userId))) return null;
  const emotion = await queryOne("SELECT id FROM emotions WHERE code = ? LIMIT 1", [payload.emotionCode]);
  if (!emotion) return null;
  const topicIds = uniquePositiveIntegers(Array.isArray(payload.topicIds) ? payload.topicIds : []);
  if (!(await ensureTopicsExist(topicIds))) return false;
  const insertedId = await withTransaction(async (conn) => {
    const [insertResult] = await conn.query(
      `INSERT INTO posts (user_id, content, emotion_id, allow_comments, is_public)
       VALUES (?, ?, ?, ?, ?)`,
      [userId, payload.content, emotion.id, payload.allowComments ? 1 : 0, payload.isPublic ? 1 : 0]
    );
    for (const topicId of topicIds) {
      await conn.query("INSERT IGNORE INTO post_topics (post_id, topic_id) VALUES (?, ?)", [insertResult.insertId, topicId]);
    }
    return Number(insertResult.insertId);
  });
  return getPostById(insertedId);
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
  if (!row) return null;
  const post = mapPostRow(row);
  const topicsMap = await getTopicsMap([post.id]);
  return { ...post, topics: topicsMap.get(post.id) || [] };
}

async function getPostDetail(postId) {
  const post = await getPostById(postId);
  if (!post) return null;
  const comments = await query(
    `SELECT id, floor_no, content, likes_count, created_at
     FROM comments
     WHERE post_id = ?
     ORDER BY floor_no ASC`,
    [postId]
  );
  return { post, comments: comments.map(mapCommentRow) };
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

  const topicIds = payload.topicIds === undefined
    ? null
    : uniquePositiveIntegers(Array.isArray(payload.topicIds) ? payload.topicIds : []);
  if (topicIds && !(await ensureTopicsExist(topicIds))) return false;

  await withTransaction(async (conn) => {
    await conn.query(
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
    if (topicIds !== null) {
      await conn.query("DELETE FROM post_topics WHERE post_id = ?", [postId]);
      for (const topicId of topicIds) {
        await conn.query("INSERT IGNORE INTO post_topics (post_id, topic_id) VALUES (?, ?)", [postId, topicId]);
      }
    }
  });

  return getPostById(postId);
}

async function deletePost(userId, postId) {
  const existing = await queryOne("SELECT id FROM posts WHERE id = ? AND user_id = ? LIMIT 1", [postId, userId]);
  if (!existing) return null;
  await query("DELETE FROM posts WHERE id = ? AND user_id = ?", [postId, userId]);
  return { id: postId };
}

module.exports = {
  listPosts,
  listPostsByTopic,
  createPost,
  getPostById,
  getPostDetail,
  updatePost,
  deletePost
};
