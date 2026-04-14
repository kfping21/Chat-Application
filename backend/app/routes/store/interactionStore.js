const { withTransaction } = require("../../db");
const { query, queryOne, ensureUserExists, mapCommentRow } = require("./commonStore");

async function createComment(userId, postId, content) {
  return createCommentReply(userId, postId, { content, parentCommentId: null });
}

async function createCommentReply(userId, postId, { content, parentCommentId }) {
  if (!(await ensureUserExists(userId))) return null;
  const post = await queryOne("SELECT id, allow_comments FROM posts WHERE id = ? LIMIT 1", [postId]);
  if (!post) return null;
  if (Number(post.allow_comments) !== 1) return false;
  if (parentCommentId !== null) {
    const parent = await queryOne(
      "SELECT id FROM comments WHERE id = ? AND post_id = ? LIMIT 1",
      [parentCommentId, postId]
    );
    if (!parent) return "parent_not_found";
  }

  const commentId = await withTransaction(async (conn) => {
    const [floorRows] = await conn.query(
      "SELECT COALESCE(MAX(floor_no), 0) AS max_floor FROM comments WHERE post_id = ? FOR UPDATE",
      [postId]
    );
    const floorNo = Number(floorRows[0].max_floor) + 1;
    const [inserted] = await conn.query(
      "INSERT INTO comments (post_id, user_id, parent_comment_id, floor_no, content) VALUES (?, ?, ?, ?, ?)",
      [postId, userId, parentCommentId, floorNo, content]
    );
    await conn.query("UPDATE posts SET comments_count = comments_count + 1 WHERE id = ?", [postId]);
    return Number(inserted.insertId);
  });
  const row = await queryOne(
    "SELECT id, post_id, user_id, parent_comment_id, floor_no, content, likes_count, created_at FROM comments WHERE id = ? LIMIT 1",
    [commentId]
  );
  return mapCommentRow(row);
}

async function listCommentsByPost(postId, { page, size }) {
  const post = await queryOne("SELECT id FROM posts WHERE id = ? LIMIT 1", [postId]);
  if (!post) return null;
  const offset = (page - 1) * size;
  const totalRow = await queryOne("SELECT COUNT(*) AS cnt FROM comments WHERE post_id = ?", [postId]);
  const rows = await query(
    `SELECT id, post_id, user_id, parent_comment_id, floor_no, content, likes_count, created_at
     FROM comments
     WHERE post_id = ?
     ORDER BY floor_no ASC
     LIMIT ? OFFSET ?`,
    [postId, size, offset]
  );
  return {
    page,
    size,
    total: Number(totalRow.cnt),
    items: rows.map(mapCommentRow)
  };
}

async function updateComment(userId, commentId, content) {
  const existing = await queryOne("SELECT id FROM comments WHERE id = ? AND user_id = ? LIMIT 1", [commentId, userId]);
  if (!existing) return null;
  await query("UPDATE comments SET content = ? WHERE id = ?", [content, commentId]);
  const row = await queryOne(
    "SELECT id, post_id, user_id, parent_comment_id, floor_no, content, likes_count, created_at FROM comments WHERE id = ? LIMIT 1",
    [commentId]
  );
  return mapCommentRow(row);
}

async function listCommentReplies(commentId, { page, size }) {
  const parent = await queryOne("SELECT id, post_id FROM comments WHERE id = ? LIMIT 1", [commentId]);
  if (!parent) return null;
  const offset = (page - 1) * size;
  const totalRow = await queryOne("SELECT COUNT(*) AS cnt FROM comments WHERE parent_comment_id = ?", [commentId]);
  const rows = await query(
    `SELECT id, post_id, user_id, parent_comment_id, floor_no, content, likes_count, created_at
     FROM comments
     WHERE parent_comment_id = ?
     ORDER BY created_at ASC
     LIMIT ? OFFSET ?`,
    [commentId, size, offset]
  );
  return {
    page,
    size,
    total: Number(totalRow.cnt),
    items: rows.map(mapCommentRow)
  };
}

async function deleteComment(userId, commentId) {
  const existing = await queryOne("SELECT id, post_id FROM comments WHERE id = ? AND user_id = ? LIMIT 1", [commentId, userId]);
  if (!existing) return null;
  await withTransaction(async (conn) => {
    await conn.query("DELETE FROM comments WHERE id = ?", [commentId]);
    await conn.query(
      "UPDATE posts SET comments_count = IF(comments_count > 0, comments_count - 1, 0) WHERE id = ?",
      [existing.post_id]
    );
  });
  return { id: Number(commentId) };
}

async function setPostLike(userId, postId, liked) {
  if (!(await ensureUserExists(userId))) return null;
  const post = await queryOne("SELECT id FROM posts WHERE id = ? LIMIT 1", [postId]);
  if (!post) return null;

  await withTransaction(async (conn) => {
    if (liked) {
      const [result] = await conn.query("INSERT IGNORE INTO post_likes (post_id, user_id) VALUES (?, ?)", [postId, userId]);
      if (result.affectedRows > 0) await conn.query("UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?", [postId]);
      return;
    }
    const [result] = await conn.query("DELETE FROM post_likes WHERE post_id = ? AND user_id = ?", [postId, userId]);
    if (result.affectedRows > 0) {
      await conn.query("UPDATE posts SET likes_count = IF(likes_count > 0, likes_count - 1, 0) WHERE id = ?", [postId]);
    }
  });

  const row = await queryOne("SELECT likes_count FROM posts WHERE id = ? LIMIT 1", [postId]);
  return { liked, likesCount: Number(row.likes_count) };
}

async function setCommentLike(userId, commentId, liked) {
  if (!(await ensureUserExists(userId))) return null;
  const comment = await queryOne("SELECT id FROM comments WHERE id = ? LIMIT 1", [commentId]);
  if (!comment) return null;

  await withTransaction(async (conn) => {
    if (liked) {
      const [result] = await conn.query("INSERT IGNORE INTO comment_likes (comment_id, user_id) VALUES (?, ?)", [commentId, userId]);
      if (result.affectedRows > 0) await conn.query("UPDATE comments SET likes_count = likes_count + 1 WHERE id = ?", [commentId]);
      return;
    }
    const [result] = await conn.query("DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?", [commentId, userId]);
    if (result.affectedRows > 0) {
      await conn.query("UPDATE comments SET likes_count = IF(likes_count > 0, likes_count - 1, 0) WHERE id = ?", [commentId]);
    }
  });

  const row = await queryOne("SELECT likes_count FROM comments WHERE id = ? LIMIT 1", [commentId]);
  return { liked, likesCount: Number(row.likes_count) };
}

module.exports = {
  createComment,
  createCommentReply,
  listCommentsByPost,
  listCommentReplies,
  updateComment,
  deleteComment,
  setPostLike,
  setCommentLike
};
