const { requireAuth } = require("./authRoutes");
const {
  listPosts,
  listSimplePosts,
  listPostsByTopic,
  createPost,
  getPostDetail,
  updatePost,
  deletePost
} = require("./store");
const { sendApiJson, createApiError, parseApiBody } = require("./utils/responseUtils");

function parsePostId(pathname) {
  const match = pathname.match(/^\/api\/posts\/(\d+)$/);
  return match ? Number(match[1]) : null;
}

async function handlePostRoutes(req, res, url, deps) {
  const { json, parseJsonBody } = deps;

  if (req.method === "GET" && url.pathname === "/api/posts") {
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const emotionCode = String(url.searchParams.get("emotionCode") || "").trim();
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(50, Math.floor(sizeRaw)) : 10;

    const result = await listPosts({ page, size, emotionCode: emotionCode || null });
    sendApiJson(json, res, 200, "ok", { page, size, total: result.total, items: result.items });
    return true;
  }

  const topicPostsMatch = url.pathname.match(/^\/api\/topics\/(\d+)\/posts$/);
  if (req.method === "GET" && topicPostsMatch) {
    const topicId = Number(topicPostsMatch[1]);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(50, Math.floor(sizeRaw)) : 10;
    const result = await listPostsByTopic({ topicId, page, size });
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  if (req.method === "POST" && url.pathname === "/api/posts") {
    const { userId } = await requireAuth(req);
    const body = await parseApiBody(parseJsonBody, req);
    const content = String(body.content || "").trim();
    const emotionCode = String(body.emotionCode || "").trim();
    const allowComments = body.allowComments === false ? false : true;
    const isPublic = body.isPublic === false ? false : true;
    const topicIds = Array.isArray(body.topicIds) ? body.topicIds : [];
    if (!content || content.length > 500) throw createApiError(400, "content is required and must be <= 500 chars");
    if (!emotionCode) throw createApiError(400, "emotionCode is required");

    const created = await createPost(userId, { content, emotionCode, allowComments, isPublic, topicIds });
    if (created === false) throw createApiError(400, "topicIds contain non-existing ids");
    if (!created) throw createApiError(400, "emotionCode does not exist");
    sendApiJson(json, res, 201, "created", created);
    return true;
  }

  const postId = parsePostId(url.pathname);
  if (postId && req.method === "GET") {
    const detail = await getPostDetail(postId);
    const post = detail ? detail.post : null;
    if (!post || !post.isPublic) throw createApiError(404, "post not found");
    sendApiJson(json, res, 200, "ok", detail);
    return true;
  }

  if (postId && req.method === "PUT") {
    const { userId } = await requireAuth(req);
    const body = await parseApiBody(parseJsonBody, req);
    const payload = {};
    if (body.content !== undefined) {
      const content = String(body.content || "").trim();
      if (!content || content.length > 500) throw createApiError(400, "content must be <= 500 chars");
      payload.content = content;
    }
    if (body.emotionCode !== undefined) {
      const emotionCode = String(body.emotionCode || "").trim();
      if (!emotionCode) throw createApiError(400, "emotionCode must not be empty");
      payload.emotionCode = emotionCode;
    }
    if (body.allowComments !== undefined) {
      if (typeof body.allowComments !== "boolean") throw createApiError(400, "allowComments must be boolean");
      payload.allowComments = body.allowComments;
    }
    if (body.isPublic !== undefined) {
      if (typeof body.isPublic !== "boolean") throw createApiError(400, "isPublic must be boolean");
      payload.isPublic = body.isPublic;
    }
    if (body.topicIds !== undefined) {
      if (!Array.isArray(body.topicIds)) throw createApiError(400, "topicIds must be array");
      payload.topicIds = body.topicIds;
    }
    if (Object.keys(payload).length === 0) throw createApiError(400, "at least one field is required");

    const updated = await updatePost(userId, postId, payload);
    if (updated === null) throw createApiError(404, "post not found");
    if (updated === false) throw createApiError(400, "emotionCode or topicIds is invalid");
    sendApiJson(json, res, 200, "updated", updated);
    return true;
  }

  if (postId && req.method === "DELETE") {
    const { userId } = await requireAuth(req);
    const deleted = await deletePost(userId, postId);
    if (!deleted) throw createApiError(404, "post not found");
    sendApiJson(json, res, 200, "deleted", deleted);
    return true;
  }

  return false;
}

async function handleLegacyPostRoutes(req, res, url, deps) {
  const { json } = deps;
  if (req.method === "GET" && url.pathname === "/posts") {
    const pageRaw = Number(url.searchParams.get("_page") || url.searchParams.get("page"));
    const limitRaw = Number(url.searchParams.get("_limit") || url.searchParams.get("limit"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const limit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.min(100, Math.floor(limitRaw)) : 20;
    const offset = (page - 1) * limit;
    const items = await listSimplePosts({ limit, offset });
    json(res, 200, items);
    return true;
  }
  return false;
}

module.exports = {
  handlePostRoutes,
  handleLegacyPostRoutes
};
