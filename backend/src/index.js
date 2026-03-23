require("dotenv").config();

const http = require("http");
const { URL } = require("url");
const { query, queryOne, withTransaction, healthCheck, closePool } = require("./db");

const port = Number(process.env.PORT) || 3000;

function withCorsHeaders(headers = {}) {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,DELETE,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type,Authorization,X-User-Id",
    ...headers
  };
}

function json(res, statusCode, data) {
  res.writeHead(statusCode, withCorsHeaders({ "Content-Type": "application/json; charset=utf-8" }));
  res.end(JSON.stringify(data));
}

function parseJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk.toString("utf8");
      if (raw.length > 1024 * 1024) reject(new Error("Request body too large"));
    });
    req.on("end", () => {
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch {
        reject(new Error("Invalid JSON body"));
      }
    });
    req.on("error", reject);
  });
}

function relativeTime(isoString) {
  const diff = Date.now() - new Date(isoString).getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))}分钟前`;
  if (diff < day) return `${Math.floor(diff / hour)}小时前`;
  return `${Math.floor(diff / day)}天前`;
}

function getUserId(req) {
  const raw = req.headers["x-user-id"];
  if (!raw) return 1;
  const userId = Number(raw);
  if (Number.isNaN(userId) || userId <= 0) return 1;
  return userId;
}

function readPagination(url, defaultLimit = 20, maxLimit = 50) {
  const pageRaw = Number(url.searchParams.get("page"));
  const limitRaw = Number(url.searchParams.get("limit"));
  const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
  const safeLimit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.floor(limitRaw) : defaultLimit;
  const limit = Math.min(maxLimit, safeLimit);
  const offset = (page - 1) * limit;
  return { page, limit, offset };
}

function postIdFromPath(pathname) {
  const match = pathname.match(/^\/api\/v1\/posts\/(\d+)$/);
  return match ? Number(match[1]) : null;
}

function postIdForCommentPath(pathname) {
  const match = pathname.match(/^\/api\/v1\/posts\/(\d+)\/comments$/);
  return match ? Number(match[1]) : null;
}

function postLikePath(pathname) {
  const match = pathname.match(/^\/api\/v1\/posts\/(\d+)\/like$/);
  return match ? Number(match[1]) : null;
}

function commentLikePath(pathname) {
  const match = pathname.match(/^\/api\/v1\/comments\/(\d+)\/like$/);
  return match ? Number(match[1]) : null;
}

function messageReadPath(pathname) {
  const match = pathname.match(/^\/api\/v1\/messages\/(\d+)\/read$/);
  return match ? Number(match[1]) : null;
}

function notificationReadPath(pathname) {
  const match = pathname.match(/^\/api\/v1\/notifications\/(\d+)\/read$/);
  return match ? Number(match[1]) : null;
}

function uniquePositiveIntegers(values) {
  return [...new Set(values.filter((v) => Number.isInteger(v) && v > 0))];
}

async function ensureTopicsExist(topicIds) {
  if (topicIds.length === 0) return;
  const placeholders = topicIds.map(() => "?").join(",");
  const rows = await query(`SELECT id FROM topics WHERE id IN (${placeholders})`, topicIds);
  const found = new Set(rows.map((row) => row.id));
  const missing = topicIds.filter((id) => !found.has(id));
  if (missing.length > 0) {
    const err = new Error(`topicIds contain non-existing ids: ${missing.join(",")}`);
    err.statusCode = 400;
    throw err;
  }
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
    if (!topicsMap.has(row.post_id)) topicsMap.set(row.post_id, []);
    topicsMap.get(row.post_id).push({ id: row.id, name: row.name });
  }
  return topicsMap;
}

async function ensureUserExists(userId) {
  const user = await queryOne("SELECT id FROM users WHERE id = ? LIMIT 1", [userId]);
  if (!user) {
    const err = new Error("user not found");
    err.statusCode = 404;
    throw err;
  }
}

function mapPostRow(row) {
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

async function getPostDetail(postId) {
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
    post: { ...mapPostRow(post), topics: topicsMap.get(postId) || [] },
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

async function route(req, res) {
  if (req.method === "OPTIONS") {
    res.writeHead(204, withCorsHeaders());
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host}`);

  if (req.method === "GET" && url.pathname === "/") {
    return json(res, 200, {
      service: "treehole-backend",
      ok: true,
      message: "Backend is running. Use /health or /api/v1/* endpoints."
    });
  }

  if (req.method === "GET" && url.pathname === "/health") {
    await healthCheck();
    return json(res, 200, {
      ok: true,
      db: "connected",
      service: "treehole-backend",
      timestamp: new Date().toISOString()
    });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/ping") {
    return json(res, 200, { message: "pong" });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/home/feed") {
    const { page, limit, offset } = readPagination(url, 20, 50);
    const rows = await query(
      `SELECT p.id, p.content, p.likes_count, p.comments_count, p.created_at, p.allow_comments,
              e.code AS emotion_code, e.display_name AS emotion_name
       FROM posts p
       JOIN emotions e ON e.id = p.emotion_id
       WHERE p.is_public = 1
       ORDER BY p.created_at DESC
       LIMIT ? OFFSET ?`,
      [limit, offset]
    );
    const topicsMap = await getTopicsMap(rows.map((row) => row.id));
    return json(res, 200, {
      page,
      limit,
      items: rows.map((row) => ({ ...mapPostRow(row), topics: topicsMap.get(row.id) || [] }))
    });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/discover/topics/hot") {
    const rows = await query(
      `SELECT id, name
       FROM topics
       WHERE is_hot = 1
       ORDER BY id ASC
       LIMIT 50`
    );
    return json(res, 200, { items: rows });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/meta/emotions") {
    const rows = await query(
      `SELECT id, code, display_name
       FROM emotions
       ORDER BY id ASC`
    );
    return json(res, 200, {
      items: rows.map((row) => ({ id: row.id, code: row.code, name: row.display_name }))
    });
  }

  if (req.method === "POST" && url.pathname === "/api/v1/posts") {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const body = await parseJsonBody(req);
    const content = String(body.content || "").trim();
    const emotionCode = String(body.emotionCode || "").trim();
    const allowComments = body.allowComments === false ? 0 : 1;
    const topicIds = Array.isArray(body.topicIds) ? uniquePositiveIntegers(body.topicIds) : [];

    if (!content || content.length > 500) {
      const err = new Error("content is required and must be <= 500 chars");
      err.statusCode = 400;
      throw err;
    }
    if (!emotionCode) {
      const err = new Error("emotionCode is required");
      err.statusCode = 400;
      throw err;
    }

    const emotion = await queryOne("SELECT id FROM emotions WHERE code = ? LIMIT 1", [emotionCode]);
    if (!emotion) {
      const err = new Error("emotionCode does not exist");
      err.statusCode = 400;
      throw err;
    }
    await ensureTopicsExist(topicIds);

    const insertedPostId = await withTransaction(async (conn) => {
      const [inserted] = await conn.query(
        `INSERT INTO posts (user_id, content, emotion_id, allow_comments, is_public)
         VALUES (?, ?, ?, ?, 1)`,
        [userId, content, emotion.id, allowComments]
      );

      for (const topicId of topicIds) {
        await conn.query("INSERT IGNORE INTO post_topics (post_id, topic_id) VALUES (?, ?)", [inserted.insertId, topicId]);
      }

      return inserted.insertId;
    });

    const detail = await getPostDetail(insertedPostId);
    return json(res, 201, { item: detail.post });
  }

  const postId = postIdFromPath(url.pathname);
  if (req.method === "GET" && postId) {
    const detail = await getPostDetail(postId);
    if (!detail) return json(res, 404, { error: "post not found" });
    return json(res, 200, detail);
  }

  const postIdForComment = postIdForCommentPath(url.pathname);
  if (req.method === "POST" && postIdForComment) {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const body = await parseJsonBody(req);
    const content = String(body.content || "").trim();
    if (!content || content.length > 300) {
      const err = new Error("content is required and must be <= 300 chars");
      err.statusCode = 400;
      throw err;
    }

    const post = await queryOne("SELECT id, allow_comments FROM posts WHERE id = ? LIMIT 1", [postIdForComment]);
    if (!post) return json(res, 404, { error: "post not found" });
    if (post.allow_comments !== 1) {
      const err = new Error("comments are disabled for this post");
      err.statusCode = 400;
      throw err;
    }

    const createdCommentId = await withTransaction(async (conn) => {
      const [floorRows] = await conn.query(
        "SELECT COALESCE(MAX(floor_no), 0) AS max_floor FROM comments WHERE post_id = ? FOR UPDATE",
        [postIdForComment]
      );
      const floorNo = Number(floorRows[0].max_floor) + 1;

      const [inserted] = await conn.query(
        `INSERT INTO comments (post_id, user_id, parent_comment_id, floor_no, content)
         VALUES (?, ?, NULL, ?, ?)`,
        [postIdForComment, userId, floorNo, content]
      );

      await conn.query("UPDATE posts SET comments_count = comments_count + 1 WHERE id = ?", [postIdForComment]);
      return inserted.insertId;
    });

    const created = await queryOne(
      "SELECT id, floor_no, content, likes_count, created_at FROM comments WHERE id = ? LIMIT 1",
      [createdCommentId]
    );
    return json(res, 201, {
      item: {
        id: created.id,
        floorNo: created.floor_no,
        content: created.content,
        likesCount: created.likes_count,
        createdAt: created.created_at,
        timeText: relativeTime(created.created_at)
      }
    });
  }

  const postIdForLike = postLikePath(url.pathname);
  if (req.method === "POST" && postIdForLike) {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const post = await queryOne("SELECT id FROM posts WHERE id = ? LIMIT 1", [postIdForLike]);
    if (!post) return json(res, 404, { error: "post not found" });

    await withTransaction(async (conn) => {
      const [result] = await conn.query(
        "INSERT IGNORE INTO post_likes (post_id, user_id) VALUES (?, ?)",
        [postIdForLike, userId]
      );
      if (result.affectedRows > 0) {
        await conn.query("UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?", [postIdForLike]);
      }
    });

    const row = await queryOne("SELECT likes_count FROM posts WHERE id = ? LIMIT 1", [postIdForLike]);
    return json(res, 200, { liked: true, likesCount: row.likes_count });
  }

  if (req.method === "DELETE" && postIdForLike) {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const post = await queryOne("SELECT id FROM posts WHERE id = ? LIMIT 1", [postIdForLike]);
    if (!post) return json(res, 404, { error: "post not found" });

    await withTransaction(async (conn) => {
      const [result] = await conn.query(
        "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?",
        [postIdForLike, userId]
      );
      if (result.affectedRows > 0) {
        await conn.query("UPDATE posts SET likes_count = IF(likes_count > 0, likes_count - 1, 0) WHERE id = ?", [postIdForLike]);
      }
    });

    const row = await queryOne("SELECT likes_count FROM posts WHERE id = ? LIMIT 1", [postIdForLike]);
    return json(res, 200, { liked: false, likesCount: row.likes_count });
  }

  const commentIdForLike = commentLikePath(url.pathname);
  if (req.method === "POST" && commentIdForLike) {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const comment = await queryOne("SELECT id FROM comments WHERE id = ? LIMIT 1", [commentIdForLike]);
    if (!comment) return json(res, 404, { error: "comment not found" });

    await withTransaction(async (conn) => {
      const [result] = await conn.query(
        "INSERT IGNORE INTO comment_likes (comment_id, user_id) VALUES (?, ?)",
        [commentIdForLike, userId]
      );
      if (result.affectedRows > 0) {
        await conn.query("UPDATE comments SET likes_count = likes_count + 1 WHERE id = ?", [commentIdForLike]);
      }
    });

    const row = await queryOne("SELECT likes_count FROM comments WHERE id = ? LIMIT 1", [commentIdForLike]);
    return json(res, 200, { liked: true, likesCount: row.likes_count });
  }

  if (req.method === "DELETE" && commentIdForLike) {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const comment = await queryOne("SELECT id FROM comments WHERE id = ? LIMIT 1", [commentIdForLike]);
    if (!comment) return json(res, 404, { error: "comment not found" });

    await withTransaction(async (conn) => {
      const [result] = await conn.query(
        "DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?",
        [commentIdForLike, userId]
      );
      if (result.affectedRows > 0) {
        await conn.query(
          "UPDATE comments SET likes_count = IF(likes_count > 0, likes_count - 1, 0) WHERE id = ?",
          [commentIdForLike]
        );
      }
    });

    const row = await queryOne("SELECT likes_count FROM comments WHERE id = ? LIMIT 1", [commentIdForLike]);
    return json(res, 200, { liked: false, likesCount: row.likes_count });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/me/summary") {
    const userId = getUserId(req);
    await ensureUserExists(userId);

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
    const recentPostTopicsMap = await getTopicsMap(recentPosts.map((row) => row.id));

    return json(res, 200, {
      profile: { id: user.id, anonymousName: user.anonymous_name, joinedAt: user.joined_at },
      stats: {
        secretsCount: Number(postCount.cnt),
        collectedEchoesCount: Number(likedCount.cnt),
        encountersCount: Number(encounterCount.cnt)
      },
      myPosts: recentPosts.map((row) => ({
        id: row.id,
        content: row.content,
        emotionName: row.emotion_name,
        topics: recentPostTopicsMap.get(row.id) || [],
        likesCount: row.likes_count,
        commentsCount: row.comments_count,
        createdAt: row.created_at,
        timeText: relativeTime(row.created_at)
      }))
    });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/encounters/recent") {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const limitRaw = Number(url.searchParams.get("limit"));
    const limit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.min(100, Math.floor(limitRaw)) : 20;
    const rows = await query(
      `SELECT e.target_user_id, e.met_at, u.anonymous_name, u.avatar_color
       FROM encounters e
       JOIN users u ON u.id = e.target_user_id
       WHERE e.user_id = ?
       ORDER BY e.met_at DESC
       LIMIT ?`,
      [userId, limit]
    );
    return json(res, 200, {
      limit,
      items: rows.map((row) => ({
        userId: row.target_user_id,
        anonymousName: row.anonymous_name,
        avatarColor: row.avatar_color,
        metAt: row.met_at,
        timeText: relativeTime(row.met_at)
      }))
    });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/notifications") {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const { page, limit, offset } = readPagination(url, 20, 100);
    const unreadRow = await queryOne(
      "SELECT COUNT(*) AS cnt FROM notifications WHERE user_id = ? AND is_read = 0",
      [userId]
    );
    const rows = await query(
      `SELECT id, type, ref_id, payload, is_read, created_at
       FROM notifications
       WHERE user_id = ?
       ORDER BY created_at DESC
       LIMIT ? OFFSET ?`,
      [userId, limit, offset]
    );
    return json(res, 200, {
      page,
      limit,
      unreadCount: Number(unreadRow.cnt),
      items: rows.map((row) => ({
        id: row.id,
        type: row.type,
        refId: row.ref_id,
        payload: row.payload,
        isRead: row.is_read === 1,
        createdAt: row.created_at,
        timeText: relativeTime(row.created_at)
      }))
    });
  }

  if (req.method === "POST" && url.pathname === "/api/v1/notifications/read-all") {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const result = await query("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", [userId]);
    return json(res, 200, { updatedCount: result.affectedRows || 0 });
  }

  const notificationId = notificationReadPath(url.pathname);
  if (req.method === "POST" && notificationId) {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const result = await query(
      "UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?",
      [notificationId, userId]
    );
    if (!result.affectedRows) return json(res, 404, { error: "notification not found" });
    return json(res, 200, { ok: true });
  }

  if (req.method === "GET" && url.pathname === "/api/v1/messages/inbox") {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const { page, limit, offset } = readPagination(url, 20, 100);
    const unreadRow = await queryOne(
      "SELECT COUNT(*) AS cnt FROM private_messages WHERE receiver_user_id = ? AND is_read = 0",
      [userId]
    );
    const rows = await query(
      `SELECT pm.id, pm.sender_user_id, pm.receiver_user_id, pm.content, pm.is_read, pm.created_at,
              u.anonymous_name AS sender_name
       FROM private_messages pm
       JOIN users u ON u.id = pm.sender_user_id
       WHERE pm.receiver_user_id = ?
       ORDER BY pm.created_at DESC
       LIMIT ? OFFSET ?`,
      [userId, limit, offset]
    );
    return json(res, 200, {
      page,
      limit,
      unreadCount: Number(unreadRow.cnt),
      items: rows.map((row) => ({
        id: row.id,
        senderUserId: row.sender_user_id,
        senderName: row.sender_name,
        receiverUserId: row.receiver_user_id,
        content: row.content,
        isRead: row.is_read === 1,
        createdAt: row.created_at,
        timeText: relativeTime(row.created_at)
      }))
    });
  }

  if (req.method === "POST" && url.pathname === "/api/v1/messages") {
    const senderUserId = getUserId(req);
    await ensureUserExists(senderUserId);

    const body = await parseJsonBody(req);
    const receiverUserId = Number(body.receiverUserId);
    const content = String(body.content || "").trim();

    if (!receiverUserId || receiverUserId <= 0) {
      const err = new Error("receiverUserId is required");
      err.statusCode = 400;
      throw err;
    }
    if (!content || content.length > 500) {
      const err = new Error("content is required and must be <= 500 chars");
      err.statusCode = 400;
      throw err;
    }
    if (receiverUserId === senderUserId) {
      const err = new Error("cannot send message to self");
      err.statusCode = 400;
      throw err;
    }

    await ensureUserExists(receiverUserId);
    const result = await withTransaction(async (conn) => {
      const [inserted] = await conn.query(
        `INSERT INTO private_messages (sender_user_id, receiver_user_id, content, is_read)
         VALUES (?, ?, ?, 0)`,
        [senderUserId, receiverUserId, content]
      );
      await conn.query(
        `INSERT INTO encounters (user_id, target_user_id, met_at)
         VALUES (?, ?, CURRENT_TIMESTAMP)
         ON DUPLICATE KEY UPDATE met_at = VALUES(met_at)`,
        [senderUserId, receiverUserId]
      );
      await conn.query(
        `INSERT INTO encounters (user_id, target_user_id, met_at)
         VALUES (?, ?, CURRENT_TIMESTAMP)
         ON DUPLICATE KEY UPDATE met_at = VALUES(met_at)`,
        [receiverUserId, senderUserId]
      );
      return inserted;
    });
    const row = await queryOne(
      `SELECT id, sender_user_id, receiver_user_id, content, is_read, created_at
       FROM private_messages
       WHERE id = ?
       LIMIT 1`,
      [result.insertId]
    );
    return json(res, 201, {
      item: {
        id: row.id,
        senderUserId: row.sender_user_id,
        receiverUserId: row.receiver_user_id,
        content: row.content,
        isRead: row.is_read === 1,
        createdAt: row.created_at,
        timeText: relativeTime(row.created_at)
      }
    });
  }

  const msgId = messageReadPath(url.pathname);
  if (req.method === "POST" && msgId) {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const result = await query(
      "UPDATE private_messages SET is_read = 1 WHERE id = ? AND receiver_user_id = ?",
      [msgId, userId]
    );
    if (!result.affectedRows) return json(res, 404, { error: "message not found" });
    return json(res, 200, { ok: true });
  }

  return json(res, 404, { error: "Not Found" });
}

const server = http.createServer(async (req, res) => {
  try {
    await route(req, res);
  } catch (err) {
    const statusCode = err.statusCode || 500;
    return json(res, statusCode, {
      error: statusCode === 500 ? "internal server error" : err.message,
      detail: statusCode === 500 ? err.message : undefined
    });
  }
});

process.on("SIGINT", async () => {
  await closePool();
  process.exit(0);
});

process.on("SIGTERM", async () => {
  await closePool();
  process.exit(0);
});

server.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});
