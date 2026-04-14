const {
  getUserId,
  uniquePositiveIntegers
} = require("./helpers");
const { ensureUserExists, ensureTopicsExist, findEmotionByCode, createPostWithTopics, getPostDetail } = require("../store/v1");

async function handleV1PostWriteRoutes(req, res, url, deps) {
  const { json, parseJsonBody } = deps;

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

    const emotion = await findEmotionByCode(emotionCode);
    if (!emotion) {
      const err = new Error("emotionCode does not exist");
      err.statusCode = 400;
      throw err;
    }
    await ensureTopicsExist(topicIds);

    const insertedPostId = await createPostWithTopics(userId, {
      content,
      emotionId: emotion.id,
      allowComments,
      topicIds
    });

    const { relativeTime } = require("./helpers");
    const detail = await getPostDetail(relativeTime, insertedPostId);
    return json(res, 201, { item: detail.post });
  }

  return false;
}

module.exports = { handleV1PostWriteRoutes };
