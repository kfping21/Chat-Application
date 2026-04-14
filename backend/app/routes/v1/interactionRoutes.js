const {
  getUserId,
  postIdForCommentPath,
  postLikePath,
  commentLikePath,
  relativeTime
} = require("./helpers");
const {
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
} = require("../store/v1");

async function handleV1InteractionRoutes(req, res, url, deps) {
  const { json, parseJsonBody } = deps;

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

    const post = await findPostForComment(postIdForComment);
    if (!post) return json(res, 404, { error: "post not found" });
    if (post.allow_comments !== 1) {
      const err = new Error("comments are disabled for this post");
      err.statusCode = 400;
      throw err;
    }

    const createdCommentId = await createCommentWithCounter(postIdForComment, userId, content);

    const created = await findCommentById(createdCommentId);
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
    const post = await findPostById(postIdForLike);
    if (!post) return json(res, 404, { error: "post not found" });
    await likePost(postIdForLike, userId);
    const likesCount = await getPostLikesCount(postIdForLike);
    return json(res, 200, { liked: true, likesCount });
  }

  if (req.method === "DELETE" && postIdForLike) {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const post = await findPostById(postIdForLike);
    if (!post) return json(res, 404, { error: "post not found" });
    await unlikePost(postIdForLike, userId);
    const likesCount = await getPostLikesCount(postIdForLike);
    return json(res, 200, { liked: false, likesCount });
  }

  const commentIdForLike = commentLikePath(url.pathname);
  if (req.method === "POST" && commentIdForLike) {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const comment = await findCommentEntityById(commentIdForLike);
    if (!comment) return json(res, 404, { error: "comment not found" });
    await likeComment(commentIdForLike, userId);
    const likesCount = await getCommentLikesCount(commentIdForLike);
    return json(res, 200, { liked: true, likesCount });
  }

  if (req.method === "DELETE" && commentIdForLike) {
    const userId = getUserId(req);
    await ensureUserExists(userId);
    const comment = await findCommentEntityById(commentIdForLike);
    if (!comment) return json(res, 404, { error: "comment not found" });
    await unlikeComment(commentIdForLike, userId);
    const likesCount = await getCommentLikesCount(commentIdForLike);
    return json(res, 200, { liked: false, likesCount });
  }

  return false;
}

module.exports = { handleV1InteractionRoutes };
