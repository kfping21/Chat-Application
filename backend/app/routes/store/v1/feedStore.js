const { query, queryOne, getTopicsMap } = require("./commonStore");

function mapPostRow(relativeTime, row) {
  return {
    id: row.id,
    content: row.content,
    emotion: { code: row.emotion_code, name: row.emotion_name },
    likesCount: row.likes_count,
    commentsCount: row.comments_count,
    allowComments: row.allow_comments === 1,
    createdAt: row.created_at,
    timeText: relativeTime(row.created_at)
  };
}

async function listHomeFeed({ limit, offset }) {
  return query(
    `SELECT p.id, p.content, p.likes_count, p.comments_count, p.created_at, p.allow_comments,
            e.code AS emotion_code, e.display_name AS emotion_name
     FROM posts p
     JOIN emotions e ON e.id = p.emotion_id
     WHERE p.is_public = 1
     ORDER BY p.created_at DESC
     LIMIT ? OFFSET ?`,
    [limit, offset]
  );
}

async function getPostDetail(relativeTime, postId) {
  const post = await queryOne(
    `SELECT p.id, p.content, p.likes_count, p.comments_count, p.created_at, p.allow_comments,
            e.code AS emotion_code, e.display_name AS emotion_name
     FROM posts p
     JOIN emotions e ON e.id = p.emotion_id
     WHERE p.id = ?
     LIMIT 1`,
    [postId]
  );
  if (!post) return null;

  const topicsMap = await getTopicsMap([postId]);
  const comments = await query(
    `SELECT c.id, c.floor_no, c.content, c.likes_count, c.created_at
     FROM comments c
     WHERE c.post_id = ?
     ORDER BY c.floor_no ASC`,
    [postId]
  );

  return {
    post: { ...mapPostRow(relativeTime, post), topics: topicsMap.get(postId) || [] },
    comments: comments.map((row) => ({
      id: row.id,
      floorNo: row.floor_no,
      content: row.content,
      likesCount: row.likes_count,
      createdAt: row.created_at,
      timeText: relativeTime(row.created_at)
    }))
  };
}

module.exports = { listHomeFeed, mapPostRow, getPostDetail };
