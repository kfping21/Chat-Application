function sendApiJson(json, res, code, message, data = null) {
  return json(res, code, { code, message, data });
}

function createApiError(code, message) {
  const err = new Error(message);
  err.statusCode = code;
  err.useUnifiedResponse = true;
  return err;
}

async function parseApiBody(parseJsonBody, req) {
  try {
    return await parseJsonBody(req);
  } catch (err) {
    throw createApiError(400, err.message || "invalid request body");
  }
}

module.exports = {
  sendApiJson,
  createApiError,
  parseApiBody
};
