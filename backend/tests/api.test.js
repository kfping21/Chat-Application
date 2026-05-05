const test = require("node:test");
const assert = require("node:assert/strict");

const { server, realtime } = require("../app/index");
const { closePool } = require("../app/db");

let baseUrl = "";
let token = "";
let createdPostId = 0;
let createdCommentId = 0;

test.before(async () => {
  await new Promise((resolve) => server.listen(0, resolve));
  const address = server.address();
  baseUrl = `http://127.0.0.1:${address.port}`;
});

test.after(async () => {
  realtime.close();
  await new Promise((resolve, reject) => {
    server.close((err) => (err ? reject(err) : resolve()));
  });
  await closePool();
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

test("create post should return 201", async () => {
  const { response, body } = await request("/api/posts", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ content: "Test post", emotionCode: "happy", allowComments: true, isPublic: true })
  });

  assert.equal(response.status, 201);
  assert.equal(body.code, 201);
  assert.equal(body.data.content, "Test post");
  createdPostId = body.data.id;
});

test("list posts should support pagination and filter", async () => {
  const { response, body } = await request("/api/posts?page=1&size=5&emotionCode=happy", { method: "GET" });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
  assert.equal(body.data.page, 1);
  assert.equal(body.data.size, 5);
  assert.ok(Array.isArray(body.data.items));
});

test("create comment should return 201", async () => {
  const { response, body } = await request(`/api/posts/${createdPostId}/comments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ content: "Test comment" })
  });

  assert.equal(response.status, 201);
  assert.equal(body.code, 201);
  createdCommentId = body.data.id;
});

test("get post detail should return comments", async () => {
  const { response, body } = await request(`/api/posts/${createdPostId}`, { method: "GET" });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
  assert.equal(body.data.post.id, createdPostId);
  assert.ok(Array.isArray(body.data.comments));
});

test("update post should return 200", async () => {
  const { response, body } = await request(`/api/posts/${createdPostId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ content: "Updated content", isPublic: true })
  });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
  assert.equal(body.data.content, "Updated content");
});

test("post like should return 200", async () => {
  const { response, body } = await request(`/api/posts/${createdPostId}/like`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` }
  });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
  assert.equal(body.data.liked, true);
});

test("comment like should return 200", async () => {
  const { response, body } = await request(`/api/comments/${createdCommentId}/like`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` }
  });

  assert.equal(response.status, 200);
  assert.equal(body.code, 200);
  assert.equal(body.data.liked, true);
});

test("meta endpoints should return 200", async () => {
  const emotions = await request("/api/meta/emotions", { method: "GET" });
  const topics = await request("/api/discover/topics/hot", { method: "GET" });

  assert.equal(emotions.response.status, 200);
  assert.equal(topics.response.status, 200);
  assert.ok(Array.isArray(emotions.body.data.items));
  assert.ok(Array.isArray(topics.body.data.items));
});

test("messages and notifications endpoints should return 200/201", async () => {
  const summary = await request("/api/me/summary", {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(summary.response.status, 200);

  const encounters = await request("/api/encounters/recent?limit=10", {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(encounters.response.status, 200);

  const notifications = await request("/api/notifications?page=1&size=10", {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(notifications.response.status, 200);

  const readAll = await request("/api/notifications/read-all", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(readAll.response.status, 200);

  const inbox = await request("/api/messages/inbox?page=1&size=10", {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(inbox.response.status, 200);

  const send = await request("/api/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ receiverUserId: 2, content: "hello from test" })
  });
  assert.equal(send.response.status, 201);
});

test("delete post should return 200", async () => {
  const { response, body } = await request(`/api/posts/${createdPostId}`, {
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
