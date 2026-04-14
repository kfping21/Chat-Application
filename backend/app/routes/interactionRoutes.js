const { requireAuth } = require("./authRoutes");
const {
  createComment,
  createCommentReply,
  listCommentsByPost,
  listCommentReplies,
  updateComment,
  deleteComment,
  setPostLike,
  setCommentLike
} = require("./store");
const { sendApiJson, createApiError, parseApiBody } = require("./utils/responseUtils");

async function handleInteractionRoutes(req, res, url, deps) {
  const { json, parseJsonBody } = deps;

  const commentMatch = url.pathname.match(/^\/api\/posts\/(\d+)\/comments$/);
  if (commentMatch && req.method === "POST") {
    const { userId } = await requireAuth(req);
    const postId = Number(commentMatch[1]);
    const body = await parseApiBody(parseJsonBody, req);
    const content = String(body.content || "").trim();
    if (!content || content.length > 300) throw createApiError(400, "content is required and must be <= 300 chars");
    const parentCommentId =
      body.parentCommentId === undefined || body.parentCommentId === null ? null : Number(body.parentCommentId);
    if (parentCommentId !== null && (!Number.isInteger(parentCommentId) || parentCommentId <= 0)) {
      throw createApiError(400, "parentCommentId must be positive integer");
    }
    const created = parentCommentId === null
      ? await createComment(userId, postId, content)
      : await createCommentReply(userId, postId, { content, parentCommentId });
    if (created === null) throw createApiError(404, "post not found");
    if (created === "parent_not_found") throw createApiError(404, "parent comment not found");
    if (created === false) throw createApiError(400, "comments are disabled for this post");
    sendApiJson(json, res, 201, "created", created);
    return true;
  }

  const postLikeMatch = url.pathname.match(/^\/api\/posts\/(\d+)\/like$/);
  if (postLikeMatch && (req.method === "POST" || req.method === "DELETE")) {
    const { userId } = await requireAuth(req);
    const postId = Number(postLikeMatch[1]);
    const result = await setPostLike(userId, postId, req.method === "POST");
    if (!result) throw createApiError(404, "post not found");
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  const commentLikeMatch = url.pathname.match(/^\/api\/comments\/(\d+)\/like$/);
  if (commentLikeMatch && (req.method === "POST" || req.method === "DELETE")) {
    const { userId } = await requireAuth(req);
    const commentId = Number(commentLikeMatch[1]);
    const result = await setCommentLike(userId, commentId, req.method === "POST");
    if (!result) throw createApiError(404, "comment not found");
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  const commentListMatch = url.pathname.match(/^\/api\/posts\/(\d+)\/comments$/);
  if (commentListMatch && req.method === "GET") {
    const postId = Number(commentListMatch[1]);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listCommentsByPost(postId, { page, size });
    if (!result) throw createApiError(404, "post not found");
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }

  const commentIdMatch = url.pathname.match(/^\/api\/comments\/(\d+)$/);
  const commentRepliesMatch = url.pathname.match(/^\/api\/comments\/(\d+)\/replies$/);
  if (commentRepliesMatch && req.method === "GET") {
    const commentId = Number(commentRepliesMatch[1]);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 20;
    const result = await listCommentReplies(commentId, { page, size });
    if (!result) throw createApiError(404, "comment not found");
    sendApiJson(json, res, 200, "ok", result);
    return true;
  }
  if (commentIdMatch && req.method === "PUT") {
    const { userId } = await requireAuth(req);
    const commentId = Number(commentIdMatch[1]);
    const body = await parseApiBody(parseJsonBody, req);
    const content = String(body.content || "").trim();
    if (!content || content.length > 300) throw createApiError(400, "content is required and must be <= 300 chars");
    const updated = await updateComment(userId, commentId, content);
    if (!updated) throw createApiError(404, "comment not found");
    sendApiJson(json, res, 200, "updated", updated);
    return true;
  }

  if (commentIdMatch && req.method === "DELETE") {
    const { userId } = await requireAuth(req);
    const commentId = Number(commentIdMatch[1]);
    const deleted = await deleteComment(userId, commentId);
    if (!deleted) throw createApiError(404, "comment not found");
    sendApiJson(json, res, 200, "deleted", deleted);
    return true;
  }

  return false;
}

module.exports = {
  handleInteractionRoutes
};
