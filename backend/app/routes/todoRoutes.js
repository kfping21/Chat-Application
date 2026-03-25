const { requireAuth } = require("./authRoutes");
const {
  listTodos,
  createTodo,
  getTodoById,
  updateTodo,
  deleteTodo
} = require("./store/assignmentStore");
const { sendApiJson, createApiError, parseApiBody } = require("./utils/responseUtils");

function parseTodoId(pathname) {
  const match = pathname.match(/^\/api\/todos\/(\d+)$/);
  return match ? Number(match[1]) : null;
}

async function handleTodoRoutes(req, res, url, deps) {
  const { json, parseJsonBody } = deps;

  if (req.method === "GET" && url.pathname === "/api/todos") {
    const { userId } = requireAuth(req);
    const pageRaw = Number(url.searchParams.get("page"));
    const sizeRaw = Number(url.searchParams.get("size"));
    const completedRaw = url.searchParams.get("completed");

    const page = Number.isFinite(pageRaw) && pageRaw > 0 ? Math.floor(pageRaw) : 1;
    const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.min(100, Math.floor(sizeRaw)) : 10;
    const completed =
      completedRaw === null ? null : completedRaw === "true" ? true : completedRaw === "false" ? false : "invalid";
    if (completed === "invalid") throw createApiError(400, "completed must be true or false");

    const result = listTodos(userId, { page, size, completed });
    sendApiJson(json, res, 200, "ok", {
      page,
      size,
      total: result.total,
      items: result.items
    });
    return true;
  }

  if (req.method === "POST" && url.pathname === "/api/todos") {
    const { userId } = requireAuth(req);
    const body = await parseApiBody(parseJsonBody, req);
    const title = String(body.title || "").trim();
    const completed = body.completed === true;
    if (!title || title.length > 200) throw createApiError(400, "title is required and must be <= 200 chars");

    const created = createTodo(userId, { title, completed });
    sendApiJson(json, res, 201, "created", created);
    return true;
  }

  const todoId = parseTodoId(url.pathname);
  if (todoId && req.method === "GET") {
    const { userId } = requireAuth(req);
    const todo = getTodoById(userId, todoId);
    if (!todo) throw createApiError(404, "todo not found");
    sendApiJson(json, res, 200, "ok", todo);
    return true;
  }

  if (todoId && req.method === "PUT") {
    const { userId } = requireAuth(req);
    const todo = getTodoById(userId, todoId);
    if (!todo) throw createApiError(404, "todo not found");

    const body = await parseApiBody(parseJsonBody, req);
    if (body.title === undefined && body.completed === undefined) {
      throw createApiError(400, "at least one field(title/completed) is required");
    }

    const payload = {};
    if (body.title !== undefined) {
      const title = String(body.title || "").trim();
      if (!title || title.length > 200) throw createApiError(400, "title must be <= 200 chars");
      payload.title = title;
    }
    if (body.completed !== undefined) {
      if (typeof body.completed !== "boolean") throw createApiError(400, "completed must be boolean");
      payload.completed = body.completed;
    }

    const updated = updateTodo(todo, payload);
    sendApiJson(json, res, 200, "updated", updated);
    return true;
  }

  if (todoId && req.method === "DELETE") {
    const { userId } = requireAuth(req);
    const deleted = deleteTodo(userId, todoId);
    if (!deleted) throw createApiError(404, "todo not found");
    sendApiJson(json, res, 200, "deleted", deleted);
    return true;
  }

  return false;
}

module.exports = {
  handleTodoRoutes
};
