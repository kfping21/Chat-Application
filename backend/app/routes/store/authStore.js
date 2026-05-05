const { query, queryOne } = require("./commonStore");
const crypto = require("crypto");

function isMissingColumnError(err) {
  return err && (err.code === "ER_BAD_FIELD_ERROR" || err.code === "ER_NO_DEFAULT_FOR_FIELD");
}

function isMissingTableError(err) {
  return err && err.code === "ER_NO_SUCH_TABLE";
}

async function createUser({ username, password, displayName }) {
  const existing = await getUserByUsername(username);
  if (existing) return null;
  let result;
  try {
    result = await query(
      "INSERT INTO users (anonymous_name, auth_password, avatar_color) VALUES (?, ?, 'yellow')",
      [username, password]
    );
  } catch (err) {
    if (!isMissingColumnError(err)) throw err;
    result = await query("INSERT INTO users (anonymous_name, avatar_color) VALUES (?, ?)", [username, password]);
  }
  return {
    id: Number(result.insertId),
    username,
    password,
    displayName: displayName || username,
    createdAt: new Date().toISOString()
  };
}

async function getUserByUsername(username) {
  let user;
  try {
    user = await queryOne(
      "SELECT id, anonymous_name, auth_password, joined_at FROM users WHERE anonymous_name = ? LIMIT 1",
      [username]
    );
  } catch (err) {
    if (!isMissingColumnError(err)) throw err;
    user = await queryOne(
      "SELECT id, anonymous_name, avatar_color, joined_at FROM users WHERE anonymous_name = ? LIMIT 1",
      [username]
    );
  }
  if (!user) return null;
  return {
    id: Number(user.id),
    username: user.anonymous_name,
    password: user.auth_password ?? user.avatar_color,
    displayName: user.anonymous_name,
    createdAt: user.joined_at
  };
}

async function createSession(userId) {
  const token = `tk_${crypto.randomBytes(24).toString("base64url")}`;
  try {
    await query("INSERT INTO user_sessions (user_id, token, is_active) VALUES (?, ?, 1)", [userId, token]);
  } catch (err) {
    if (!isMissingTableError(err)) throw err;
    await query(
      `INSERT INTO notifications (user_id, type, ref_id, payload, is_read, created_at)
       VALUES (?, 'auth_session', ?, JSON_OBJECT('token', ?), 0, CURRENT_TIMESTAMP)`,
      [userId, userId, token]
    );
  }
  return token;
}

async function getSession(token) {
  let row;
  try {
    row = await queryOne(
      `SELECT user_id, created_at
       FROM user_sessions
       WHERE token = ?
         AND is_active = 1
       ORDER BY id DESC
       LIMIT 1`,
      [token]
    );
  } catch (err) {
    if (!isMissingTableError(err)) throw err;
    row = await queryOne(
      `SELECT user_id, created_at
       FROM notifications
       WHERE type = 'auth_session'
         AND is_read = 0
         AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.token')) = ?
       ORDER BY id DESC
       LIMIT 1`,
      [token]
    );
  }
  if (!row) return null;
  return { userId: Number(row.user_id), createdAt: row.created_at };
}

async function deleteSession(token) {
  try {
    await query("UPDATE user_sessions SET is_active = 0 WHERE token = ? AND is_active = 1", [token]);
  } catch (err) {
    if (!isMissingTableError(err)) throw err;
    await query(
      `UPDATE notifications
       SET is_read = 1
       WHERE type = 'auth_session'
         AND is_read = 0
         AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.token')) = ?`,
      [token]
    );
  }
}

module.exports = {
  createUser,
  getUserByUsername,
  createSession,
  getSession,
  deleteSession
};
