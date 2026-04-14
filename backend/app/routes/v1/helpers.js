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

module.exports = {
  relativeTime,
  getUserId,
  readPagination,
  postIdFromPath,
  postIdForCommentPath,
  postLikePath,
  commentLikePath,
  messageReadPath,
  notificationReadPath,
  uniquePositiveIntegers
};
