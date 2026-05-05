const test = require("node:test");
const assert = require("node:assert/strict");

const { server, realtime } = require("../app/index");

let baseUrl = "";
let ownerToken = "";
let otherToken = "";
let ownerPostId = 0;

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
});

async function request(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, options);
  const body = await response.json();
  return { response, body };
}

test("register two users for permission tests", async () => {
  const u1 = await request("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: `owner_${Date.now()}`, password: "pass123" })
  });
  const u2 = await request("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: `other_${Date.now()}`, password: "pass123" })
  });

  assert.equal(u1.response.status, 201);
  assert.equal(u2.response.status, 201);
  ownerToken = u1.body.data.token;
  otherToken = u2.body.data.token;
});

test("should return 401 when creating post without token", async () => {
  const { response, body } = await request("/api/posts", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ content: "x", emotionCode: "happy" })
  });

  assert.equal(response.status, 401);
  assert.equal(body.code, 401);
});

test("should return 400 for invalid emotionCode", async () => {
  const { response, body } = await request("/api/posts", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${ownerToken}`
    },
    body: JSON.stringify({ content: "bad emotion", emotionCode: "not_exists" })
  });

  assert.equal(response.status, 400);
  assert.equal(body.code, 400);
});

test("owner creates a post for ownership tests", async () => {
  const { response, body } = await request("/api/posts", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${ownerToken}`
    },
    body: JSON.stringify({ content: "owner post", emotionCode: "happy" })
  });
  assert.equal(response.status, 201);
  ownerPostId = body.data.id;
});

test("non-owner update/delete should return 404", async () => {
  const updateResp = await request(`/api/posts/${ownerPostId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${otherToken}`
    },
    body: JSON.stringify({ content: "hijack" })
  });
  const deleteResp = await request(`/api/posts/${ownerPostId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${otherToken}` }
  });

  assert.equal(updateResp.response.status, 404);
  assert.equal(updateResp.body.code, 404);
  assert.equal(deleteResp.response.status, 404);
  assert.equal(deleteResp.body.code, 404);
});

test("invalid token should return 401 on protected endpoint", async () => {
  const { response, body } = await request("/api/me/summary", {
    method: "GET",
    headers: { Authorization: "Bearer invalid_token" }
  });

  assert.equal(response.status, 401);
  assert.equal(body.code, 401);
});

test("owner cleanup", async () => {
  const del = await request(`/api/posts/${ownerPostId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${ownerToken}` }
  });
  const logout1 = await request("/api/auth/logout", {
    method: "POST",
    headers: { Authorization: `Bearer ${ownerToken}` }
  });
  const logout2 = await request("/api/auth/logout", {
    method: "POST",
    headers: { Authorization: `Bearer ${otherToken}` }
  });

  assert.equal(del.response.status, 200);
  assert.equal(logout1.response.status, 200);
  assert.equal(logout2.response.status, 200);
});
