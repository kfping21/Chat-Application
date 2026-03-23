# API 设计文档

## 1. 基础信息

- Base URL：`http://localhost:3000`
- 接口前缀：`/api/v1`
- 数据格式：`application/json; charset=utf-8`
- 认证方式：通过请求头 `X-User-Id` 传递用户 ID（未传默认 `1`）

---

## 2. 通用约定

- 成功：返回 `2xx` 与 JSON 数据
- 失败：返回 `{ "error": "..." }`
- 分页参数：`page`（默认 1）、`limit`（默认 20，按接口有上限）
- CORS 方法：`GET, POST, DELETE, OPTIONS`

---

## 3. 健康与基础接口

### GET `/`
返回服务说明。

### GET `/health`
检查服务与数据库连通性。

### GET `/api/v1/ping`
连通性测试，返回 `pong`。

---

## 4. 帖子与评论

### GET `/api/v1/home/feed?page=1&limit=20`
获取公开帖子流（包含 `emotion`、`topics`、点赞/评论计数）。

### GET `/api/v1/discover/topics/hot`
获取热门话题列表。

### GET `/api/v1/meta/emotions`
获取情绪字典列表。

### POST `/api/v1/posts`
创建帖子。

请求示例：
```json
{
  "content": "今天有点累，但也有一点点开心。",
  "emotionCode": "happy",
  "allowComments": true,
  "topicIds": [1, 2]
}
```

说明：
- `content` 必填，长度 `<= 500`
- `emotionCode` 必填且必须存在
- `topicIds` 可选，若存在必须是已存在话题 ID

### GET `/api/v1/posts/:id`
获取帖子详情（`post + comments`）。

### POST `/api/v1/posts/:id/comments`
给帖子发表评论。

请求示例：
```json
{
  "content": "抱抱你，你并不孤单。"
}
```

说明：`content` 必填，长度 `<= 300`。

### POST `/api/v1/posts/:id/like`
点赞帖子。

### DELETE `/api/v1/posts/:id/like`
取消点赞帖子。

### POST `/api/v1/comments/:id/like`
点赞评论。

### DELETE `/api/v1/comments/:id/like`
取消点赞评论。

---

## 5. 个人与相遇

### GET `/api/v1/me/summary`
获取我的概览信息：
- `profile`
- `stats`（发帖数、被共鸣数、相遇数）
- `myPosts`（含 `topics`）

### GET `/api/v1/encounters/recent?limit=20`
获取最近相遇用户列表。

---

## 6. 通知

### GET `/api/v1/notifications?page=1&limit=20`
获取通知列表与未读数。

### POST `/api/v1/notifications/:id/read`
将单条通知设为已读。

### POST `/api/v1/notifications/read-all`
将当前用户全部通知设为已读。

---

## 7. 私信

### GET `/api/v1/messages/inbox?page=1&limit=20`
获取收件箱消息列表与未读数。

### POST `/api/v1/messages`
发送私信（发送后会自动更新双方 `encounters` 记录）。

请求示例：
```json
{
  "receiverUserId": 2,
  "content": "晚安，愿你今晚有个好梦。"
}
```

约束：
- `receiverUserId` 必填且不能等于自己
- `content` 必填，长度 `<= 500`

### POST `/api/v1/messages/:id/read`
将单条私信设为已读（仅接收方可操作）。

---

## 8. 典型错误码

- `400`：参数错误（如长度超限、话题不存在、发给自己）
- `404`：资源不存在（用户/帖子/评论/消息/通知）
- `500`：服务内部错误
