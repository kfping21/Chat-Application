const { getUserId, relativeTime } = require("./helpers");
const { ensureUserExists, getTopicsMap, getProfileSummaryRows, listRecentEncounters } = require("../store/v1");

async function handleV1ProfileRoutes(req, res, url, deps) {
  const { json } = deps;

  if (req.method === "GET" && url.pathname === "/api/v1/me/summary") {
    const userId = getUserId(req);
    await ensureUserExists(userId);

    const { user, postCount, likedCount, encounterCount, recentPosts } = await getProfileSummaryRows(userId);
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
    const rows = await listRecentEncounters(userId, limit);
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

  return false;
}

module.exports = { handleV1ProfileRoutes };
