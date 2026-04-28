const test = require("node:test");
const assert = require("node:assert/strict");

const authStorePath = require.resolve("../app/routes/store/authStore");
const commonStorePath = require.resolve("../app/routes/store/commonStore");

function loadAuthStoreWithMock({ query, queryOne }) {
  const cachedCommonStore = require.cache[commonStorePath];
  const cachedAuthStore = require.cache[authStorePath];

  require.cache[commonStorePath] = {
    id: commonStorePath,
    filename: commonStorePath,
    loaded: true,
    exports: { query, queryOne }
  };

  delete require.cache[authStorePath];
  const authStore = require("../app/routes/store/authStore");

  function restore() {
    delete require.cache[authStorePath];
    if (cachedCommonStore) {
      require.cache[commonStorePath] = cachedCommonStore;
    } else {
      delete require.cache[commonStorePath];
    }
    if (cachedAuthStore) {
      require.cache[authStorePath] = cachedAuthStore;
    }
  }

  return { authStore, restore };
}

test("createUser returns null when username already exists", async () => {
  const queryCalls = [];
  const queryOneCalls = [];

  const { authStore, restore } = loadAuthStoreWithMock({
    query: async (sql, params) => {
      queryCalls.push({ sql, params });
      return [];
    },
    queryOne: async (sql, params) => {
      queryOneCalls.push({ sql, params });
      return {
        id: 9,
        anonymous_name: "alice",
        auth_password: "secret",
        joined_at: "2026-01-01T00:00:00.000Z"
      };
    }
  });

  try {
    const result = await authStore.createUser({ username: "alice", password: "secret", displayName: "Alice" });
    assert.equal(result, null);
    assert.equal(queryCalls.length, 0);
    assert.equal(queryOneCalls.length, 1);
  } finally {
    restore();
  }
});

test("createUser inserts user with primary SQL path", async () => {
  const queryCalls = [];

  const { authStore, restore } = loadAuthStoreWithMock({
    query: async (sql, params) => {
      queryCalls.push({ sql, params });
      return { insertId: 123 };
    },
    queryOne: async () => null
  });

  try {
    const created = await authStore.createUser({ username: "new_user", password: "pass123", displayName: "New User" });
    assert.equal(created.id, 123);
    assert.equal(created.username, "new_user");
    assert.equal(created.password, "pass123");
    assert.equal(queryCalls.length, 1);
    assert.match(queryCalls[0].sql, /INSERT INTO users/);
    assert.deepEqual(queryCalls[0].params, ["new_user", "pass123"]);
  } finally {
    restore();
  }
});

test("createUser falls back when auth_password column is missing", async () => {
  let attempt = 0;
  const queryCalls = [];

  const { authStore, restore } = loadAuthStoreWithMock({
    query: async (sql, params) => {
      queryCalls.push({ sql, params });
      attempt += 1;
      if (attempt === 1) {
        const err = new Error("missing column");
        err.code = "ER_BAD_FIELD_ERROR";
        throw err;
      }
      return { insertId: 5 };
    },
    queryOne: async () => null
  });

  try {
    const created = await authStore.createUser({ username: "legacy", password: "blue", displayName: "legacy" });
    assert.equal(created.id, 5);
    assert.equal(queryCalls.length, 2);
    assert.match(queryCalls[1].sql, /avatar_color/);
    assert.deepEqual(queryCalls[1].params, ["legacy", "blue"]);
  } finally {
    restore();
  }
});

test("getUserByUsername maps auth_password from DB row", async () => {
  const { authStore, restore } = loadAuthStoreWithMock({
    query: async () => {
      throw new Error("query should not be called");
    },
    queryOne: async () => ({
      id: 42,
      anonymous_name: "demo",
      auth_password: "pwd",
      joined_at: "2026-02-02T02:02:02.000Z"
    })
  });

  try {
    const user = await authStore.getUserByUsername("demo");
    assert.deepEqual(user, {
      id: 42,
      username: "demo",
      password: "pwd",
      displayName: "demo",
      createdAt: "2026-02-02T02:02:02.000Z"
    });
  } finally {
    restore();
  }
});

test("createSession falls back to notifications when session table is missing", async () => {
  let call = 0;
  const queryCalls = [];

  const { authStore, restore } = loadAuthStoreWithMock({
    query: async (sql, params) => {
      queryCalls.push({ sql, params });
      call += 1;
      if (call === 1) {
        const err = new Error("missing table");
        err.code = "ER_NO_SUCH_TABLE";
        throw err;
      }
      return { affectedRows: 1 };
    },
    queryOne: async () => null
  });

  try {
    const token = await authStore.createSession(7);
    assert.match(token, /^tk_/);
    assert.equal(queryCalls.length, 2);
    assert.match(queryCalls[1].sql, /INSERT INTO notifications/);
    assert.equal(queryCalls[1].params[0], 7);
    assert.equal(queryCalls[1].params[1], 7);
    assert.equal(queryCalls[1].params[2], token);
  } finally {
    restore();
  }
});

test("getSession returns mapped session from user_sessions", async () => {
  const { authStore, restore } = loadAuthStoreWithMock({
    query: async () => {
      throw new Error("query should not be called");
    },
    queryOne: async () => ({ user_id: 11, created_at: "2026-03-03T03:03:03.000Z" })
  });

  try {
    const result = await authStore.getSession("tk_ok");
    assert.deepEqual(result, { userId: 11, createdAt: "2026-03-03T03:03:03.000Z" });
  } finally {
    restore();
  }
});

test("getSession falls back to notifications when session table is missing", async () => {
  let call = 0;
  const queryOneCalls = [];

  const { authStore, restore } = loadAuthStoreWithMock({
    query: async () => [],
    queryOne: async (sql, params) => {
      queryOneCalls.push({ sql, params });
      call += 1;
      if (call === 1) {
        const err = new Error("missing table");
        err.code = "ER_NO_SUCH_TABLE";
        throw err;
      }
      return { user_id: 33, created_at: "2026-04-04T04:04:04.000Z" };
    }
  });

  try {
    const result = await authStore.getSession("tk_fallback");
    assert.deepEqual(result, { userId: 33, createdAt: "2026-04-04T04:04:04.000Z" });
    assert.equal(queryOneCalls.length, 2);
    assert.match(queryOneCalls[1].sql, /FROM notifications/);
  } finally {
    restore();
  }
});

test("deleteSession falls back to notifications when session table is missing", async () => {
  let call = 0;
  const queryCalls = [];

  const { authStore, restore } = loadAuthStoreWithMock({
    query: async (sql, params) => {
      queryCalls.push({ sql, params });
      call += 1;
      if (call === 1) {
        const err = new Error("missing table");
        err.code = "ER_NO_SUCH_TABLE";
        throw err;
      }
      return { affectedRows: 1 };
    },
    queryOne: async () => null
  });

  try {
    await authStore.deleteSession("tk_del");
    assert.equal(queryCalls.length, 2);
    assert.match(queryCalls[0].sql, /UPDATE user_sessions/);
    assert.match(queryCalls[1].sql, /UPDATE notifications/);
    assert.deepEqual(queryCalls[1].params, ["tk_del"]);
  } finally {
    restore();
  }
});
