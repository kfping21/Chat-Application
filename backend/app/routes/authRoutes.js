const {
  createUser,
  getUserByUsername,
  createSession,
  getSession,
  deleteSession
} = require("./store/assignmentStore");
const { sendApiJson, createApiError, parseApiBody } = require("./utils/responseUtils");

function getBearerToken(req) {
  const auth = req.headers.authorization || "";
  const match = auth.match(/^Bearer\s+(.+)$/i);
  return match ? match[1].trim() : "";
}

function requireAuth(req) {
  const token = getBearerToken(req);
  if (!token) throw createApiError(401, "unauthorized");
  const session = getSession(token);
  if (!session) throw createApiError(401, "unauthorized");
  return { token, userId: session.userId };
}

function toPublicUser(user) {
  return {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    createdAt: user.createdAt
  };
}

async function handleAuthRoutes(req, res, url, deps) {
  const { json, parseJsonBody } = deps;

  if (req.method === "POST" && url.pathname === "/api/auth/register") {
    const body = await parseApiBody(parseJsonBody, req);
    const username = String(body.username || "").trim();
    const password = String(body.password || "").trim();
    const displayName = String(body.displayName || username).trim();

    if (!username || !password) throw createApiError(400, "username and password are required");
    if (username.length > 30 || password.length > 64 || displayName.length > 50) {
      throw createApiError(400, "username/password/displayName length is invalid");
    }

    const user = createUser({ username, password, displayName });
    if (!user) throw createApiError(400, "username already exists");
    const token = createSession(user.id);
    sendApiJson(json, res, 201, "registered", { token, user: toPublicUser(user) });
    return true;
  }

  if (req.method === "POST" && url.pathname === "/api/auth/login") {
    const body = await parseApiBody(parseJsonBody, req);
    const username = String(body.username || "").trim();
    const password = String(body.password || "").trim();
    const user = getUserByUsername(username);
    if (!user || user.password !== password) throw createApiError(401, "invalid credentials");
    const token = createSession(user.id);
    sendApiJson(json, res, 200, "login success", { token, user: toPublicUser(user) });
    return true;
  }

  if (req.method === "POST" && url.pathname === "/api/auth/logout") {
    const { token } = requireAuth(req);
    deleteSession(token);
    sendApiJson(json, res, 200, "logout success", null);
    return true;
  }

  return false;
}

module.exports = {
  handleAuthRoutes,
  requireAuth
};
