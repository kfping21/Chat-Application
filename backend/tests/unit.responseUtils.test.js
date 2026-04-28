const test = require("node:test");
const assert = require("node:assert/strict");

const { sendApiJson, createApiError, parseApiBody } = require("../app/routes/utils/responseUtils");

test("sendApiJson wraps unified structure", () => {
  let captured = null;
  const json = (res, code, payload) => {
    captured = { res, code, payload };
    return "ok";
  };

  const res = { id: "fake-res" };
  const result = sendApiJson(json, res, 201, "created", { id: 1 });

  assert.equal(result, "ok");
  assert.equal(captured.code, 201);
  assert.deepEqual(captured.payload, {
    code: 201,
    message: "created",
    data: { id: 1 }
  });
});

test("createApiError sets status and unified-response flag", () => {
  const err = createApiError(401, "unauthorized");
  assert.equal(err.message, "unauthorized");
  assert.equal(err.statusCode, 401);
  assert.equal(err.useUnifiedResponse, true);
});

test("parseApiBody returns parsed object when parser succeeds", async () => {
  const req = { id: 123 };
  const body = await parseApiBody(async () => ({ hello: "world" }), req);
  assert.deepEqual(body, { hello: "world" });
});

test("parseApiBody converts parser error to unified 400 error", async () => {
  await assert.rejects(
    () => parseApiBody(async () => {
      throw new Error("Invalid JSON body");
    }, {}),
    (err) => {
      assert.equal(err.statusCode, 400);
      assert.equal(err.message, "Invalid JSON body");
      assert.equal(err.useUnifiedResponse, true);
      return true;
    }
  );
});
