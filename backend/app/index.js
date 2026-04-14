const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "..", ".env") });

const http = require("http");
const { URL } = require("url");
const { query, queryOne, healthCheck, closePool } = require("./db");
const { handleApiRoutes } = require("./routes/apiRoutes");
const { handleV1Routes } = require("./routes/v1");
const { createRealtime } = require("./realtime");

const port = Number(process.env.PORT) || 3000;

function withCorsHeaders(headers = {}) {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,PUT,DELETE,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type,Authorization,X-User-Id",
    ...headers
  };
}

function json(res, statusCode, data) {
  res.writeHead(statusCode, withCorsHeaders({ "Content-Type": "application/json; charset=utf-8" }));
  res.end(JSON.stringify(data));
}

function parseJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk.toString("utf8");
      if (raw.length > 1024 * 1024) reject(new Error("Request body too large"));
    });
    req.on("end", () => {
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch {
        reject(new Error("Invalid JSON body"));
      }
    });
    req.on("error", reject);
  });
}

async function route(req, res) {
  if (req.method === "OPTIONS") {
    res.writeHead(204, withCorsHeaders());
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host}`);

  if (req.method === "GET" && url.pathname === "/") {
    return json(res, 200, {
      service: "treehole-backend",
      ok: true,
      message: "Backend is running. Use /health or /api/v1/* endpoints."
    });
  }

  if (req.method === "GET" && url.pathname === "/health") {
    await healthCheck();
    return json(res, 200, {
      ok: true,
      db: "connected",
      service: "treehole-backend",
      timestamp: new Date().toISOString()
    });
  }

  if (await handleApiRoutes(req, res, url, { parseJsonBody, json, realtime })) {
    return;
  }
  if (await handleV1Routes(req, res, url, { parseJsonBody, json, query, queryOne })) {
    return;
  }

  return json(res, 404, { error: "Not Found" });
}

const server = http.createServer(async (req, res) => {
  try {
    await route(req, res);
  } catch (err) {
    if (res.headersSent) return;
    const statusCode = err.statusCode || 500;
    if (err.useUnifiedResponse) {
      return json(res, statusCode, {
        code: statusCode,
        message: statusCode === 500 ? "internal server error" : err.message,
        data: null
      });
    }
    return json(res, statusCode, {
      error: statusCode === 500 ? "internal server error" : err.message,
      detail: statusCode === 500 ? err.message : undefined
    });
  }
});
const realtime = createRealtime(server);

process.on("SIGINT", async () => {
  realtime.close();
  await closePool();
  process.exit(0);
});

process.on("SIGTERM", async () => {
  realtime.close();
  await closePool();
  process.exit(0);
});

if (require.main === module) {
  server.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
  });
}

module.exports = { server };
