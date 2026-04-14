const { withTransaction } = require("../../../db");
const { queryOne } = require("./commonStore");

async function findPostForComment(postId) {
  return queryOne("SELECT id, allow_comments FROM posts WHERE id = ? LIMIT 1", [postId]);
}

async function createCommentWithCounter(postId, userId, content) {
  return withTransaction(async (conn) => {
    const [floorRows] = await conn.query(
      "SELECT COALESCE(MAX(floor_no), 0) AS max_floor FROM comments WHERE post_id = ? FOR UPDATE",
      [postId]
    );
    const floorNo = Number(floorRows[0].max_floor) + 1;
    const [inserted] = await conn.query(
      `INSERT INTO comments (post_id, user_id, parent_comment_id, floor_no, content)
       VALUES (?, ?, NULL, ?, ?)`,
      [postId, userId, floorNo, content]
    );
    await conn.query("UPDATE posts SET comments_count = comments_count + 1 WHERE id = ?", [postId]);
    return inserted.insertId;
  });
}

async function findCommentById(commentId) {
  return queryOne("SELECT id, floor_no, content, likes_count, created_at FROM comments WHERE id = ? LIMIT 1", [commentId]);
}

async function findPostById(postId) {
  return queryOne("SELECT id FROM posts WHERE id = ? LIMIT 1", [postId]);
}

async function findCommentEntityById(commentId) {
  return queryOne("SELECT id FROM comments WHERE id = ? LIMIT 1", [commentId]);
}

async function likePost(postId, userId) {
  return withTransaction(async (conn) => {
    const [result] = await conn.query("INSERT IGNORE INTO post_likes (post_id, user_id) VALUES (?, ?)", [postId, userId]);
    if (result.affectedRows > 0) await conn.query("UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?", [postId]);
  });
}

async function unlikePost(postId, userId) {
  return withTransaction(async (conn) => {
    const [result] = await conn.query("DELETE FROM post_likes WHERE post_id = ? AND user_id = ?", [postId, userId]);
    if (result.affectedRows > 0) {
      await conn.query("UPDATE posts SET likes_count = IF(likes_count > 0, likes_count - 1, 0) WHERE id = ?", [postId]);
    }
  });
}

async function getPostLikesCount(postId) {
  const row = await queryOne("SELECT likes_count FROM posts WHERE id = ? LIMIT 1", [postId]);
  return row ? row.likes_count : 0;
}

async function likeComment(commentId, userId) {
  return withTransaction(async (conn) => {
    const [result] = await conn.query("INSERT IGNORE INTO comment_likes (comment_id, user_id) VALUES (?, ?)", [commentId, userId]);
    if (result.affectedRows > 0) await conn.query("UPDATE comments SET likes_count = likes_count + 1 WHERE id = ?", [commentId]);
  });
}

async function unlikeComment(commentId, userId) {
  return withTransaction(async (conn) => {
    const [result] = await conn.query("DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?", [commentId, userId]);
    if (result.affectedRows > 0) {
      await conn.query("UPDATE comments SET likes_count = IF(likes_count > 0, likes_count - 1, 0) WHERE id = ?", [commentId]);
    }
  });
}

async function getCommentLikesCount(commentId) {
  const row = await queryOne("SELECT likes_count FROM comments WHERE id = ? LIMIT 1", [commentId]);
  return row ? row.likes_count : 0;
}

module.exports = {
  findPostForComment,
  createCommentWithCounter,
  findCommentById,
  findPostById,
  findCommentEntityById,
  likePost,
  unlikePost,
  getPostLikesCount,
  likeComment,
  unlikeComment,
  getCommentLikesCount
};
