const usersByUsername = new Map();
const sessionsByToken = new Map();
const todos = [];

let nextUserId = 1;
let nextTodoId = 1;

function seed() {
  const now = new Date().toISOString();
  const demoUser = {
    id: nextUserId++,
    username: "demo",
    password: "demo123",
    displayName: "Demo User",
    createdAt: now
  };
  usersByUsername.set(demoUser.username, demoUser);
  todos.push({
    id: nextTodoId++,
    userId: demoUser.id,
    title: "示例任务",
    completed: false,
    createdAt: now,
    updatedAt: now
  });
}

seed();

function createUser({ username, password, displayName }) {
  if (usersByUsername.has(username)) return null;
  const user = {
    id: nextUserId++,
    username,
    password,
    displayName,
    createdAt: new Date().toISOString()
  };
  usersByUsername.set(username, user);
  return user;
}

function getUserByUsername(username) {
  return usersByUsername.get(username) || null;
}

function createSession(userId) {
  const token = `tk_${Math.random().toString(36).slice(2)}${Date.now().toString(36)}`;
  sessionsByToken.set(token, { userId, createdAt: new Date().toISOString() });
  return token;
}

function getSession(token) {
  return sessionsByToken.get(token) || null;
}

function deleteSession(token) {
  sessionsByToken.delete(token);
}

function listTodos(userId, { page, size, completed }) {
  const scoped = todos.filter((item) => item.userId === userId);
  const filtered = completed === null ? scoped : scoped.filter((item) => item.completed === completed);
  const total = filtered.length;
  const offset = (page - 1) * size;
  return {
    total,
    items: filtered.slice(offset, offset + size)
  };
}

function createTodo(userId, { title, completed }) {
  const now = new Date().toISOString();
  const item = {
    id: nextTodoId++,
    userId,
    title,
    completed,
    createdAt: now,
    updatedAt: now
  };
  todos.push(item);
  return item;
}

function getTodoById(userId, todoId) {
  return todos.find((todo) => todo.userId === userId && todo.id === todoId) || null;
}

function updateTodo(todo, payload) {
  if (payload.title !== undefined) todo.title = payload.title;
  if (payload.completed !== undefined) todo.completed = payload.completed;
  todo.updatedAt = new Date().toISOString();
  return todo;
}

function deleteTodo(userId, todoId) {
  const index = todos.findIndex((todo) => todo.userId === userId && todo.id === todoId);
  if (index < 0) return null;
  const [deleted] = todos.splice(index, 1);
  return deleted;
}

module.exports = {
  createUser,
  getUserByUsername,
  createSession,
  getSession,
  deleteSession,
  listTodos,
  createTodo,
  getTodoById,
  updateTodo,
  deleteTodo
};
