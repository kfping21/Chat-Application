const test = require("node:test");
const assert = require("node:assert/strict");

const { server } = require("../app/index");

let baseUrl = "";
let token = "";
let createdTodoId = 0;

test.before(async () => {
  await new Promise((resolve) => server.listen(0, resolve));
  const address = server.address();
  baseUrl = `http://127.0.0.1:${address.port}`;
});

test.after(async () => {
  await new Promise((resolve, reject) => {
    server.close((err) => (err ? reject(err) : resolve()));
  });
});

async function request(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, options);
  const body = await response.json();
  return { response, body };
}

test("register should return 201 and token", async () => {
  const username = `user_${Date.now()}`;
  const { response, body } = await request("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password: "pass123", displayName: "Tester" })
  });

  assert.equal(response.status, 201);
  assert.equal(body.code, 201);
  assert.equal(body.data.user.username, username);
  assert.ok(body.data.token);
  token = body.data.token;
});

test("login with invalid password should return 401", async () => {
  const { response, body } = await request("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: "demo", password: "wrong" })
  });

  assert.equal(response.status, 401);
  assert.equal(body.code, 401);
});

test("create todo should return 201", async () => {
  const { response, body } = await request("/api/todos", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ title: "Test Todo", completed: false })
  });

  assert.equal(response.status, 201);
  assert.equal(body.code, 201);
  assert.equal(body.data.title, "Test Todo");
  createdTodoId = body.data.id;
});

test("list todos should support pagination and filter", async () => {
  const { response, body } = await request("/api/todos?page=1&size=5&completed=false", {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` }
  });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
  assert.equal(body.data.page, 1);
  assert.equal(body.data.size, 5);
  assert.ok(Array.isArray(body.data.items));
});

test("update todo should return 200", async () => {
  const { response, body } = await request(`/api/todos/${createdTodoId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ completed: true })
  });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
  assert.equal(body.data.completed, true);
});

test("delete todo should return 200", async () => {
  const { response, body } = await request(`/api/todos/${createdTodoId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` }
  });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
});

test("logout should return 200", async () => {
  const { response, body } = await request("/api/auth/logout", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` }
  });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
});
