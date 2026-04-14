# API 使用说明（树洞聊天业务）

## 1. 基础信息

- Base URL：`http://localhost:3000`
- 作业接口前缀：`/api`
- 历史接口前缀：`/api/v1`（保持兼容）
- OpenAPI：`docs/api.yaml`
- 测试前置：配置 `backend/.env` 数据库连接并执行 `backend/sql/init.sql`

### 1.1 数据库连接配置

复制 `backend/.env.example` 为 `backend/.env`，按你的 MySQL 实际账号修改：

```env
PORT=3000
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=你的MySQL密码
DB_NAME=treehole
```

如果报错：

`Access denied for user 'root'@'localhost' (using password: NO)`

说明后端没有拿到正确密码，重点检查 `DB_PASSWORD` 是否为空、`.env` 是否放在 `backend/` 根目录。

## 2. 统一响应格式（作业接口）

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

## 3. 认证接口

### 3.1 注册

- `POST /api/auth/register`
- Body:

```json
{
  "username": "u001",
  "password": "123456",
  "displayName": "匿名用户001"
}
```

### 3.2 登录

- `POST /api/auth/login`
- 使用注册成功后的用户名与密码（用户名映射 `users.anonymous_name`，密码映射 `users.auth_password`）

### 3.3 登出

- `POST /api/auth/logout`
- Header：`Authorization: Bearer <token>`

## 4. 元数据接口

### 4.1 情绪字典

- `GET /api/meta/emotions`

### 4.2 热门话题

- `GET /api/discover/topics/hot`

### 4.3 话题列表（分页）

- `GET /api/topics?page=1&size=20&hotOnly=true`

## 5. 核心业务资源（帖子）与互动

### 5.1 列表（分页+筛选）

- `GET /api/posts?page=1&size=10&emotionCode=happy`
- `emotionCode` 可选，对应 `emotions.code`

### 5.1.1 按话题筛选帖子

- `GET /api/topics/{topicId}/posts?page=1&size=10`

### 5.2 创建帖子

- `POST /api/posts`
- Header：`Authorization: Bearer <token>`
- Body:

```json
{
  "content": "今天有点累，但我在坚持。",
  "emotionCode": "happy",
  "topicIds": [1, 2],
  "allowComments": true,
  "isPublic": true
}
```

### 5.3 获取单个帖子详情

- `GET /api/posts/{id}`
- 返回结构：
  - `data.post`：帖子详情
  - `data.comments`：评论列表

### 5.4 更新帖子（作者本人）

- `PUT /api/posts/{id}`
- Header：`Authorization: Bearer <token>`
- Body（至少一个字段）：

```json
{
  "content": "更新后的内容",
  "emotionCode": "calm",
  "topicIds": [3, 5],
  "allowComments": false,
  "isPublic": true
}
```

### 5.5 删除帖子（作者本人）

- `DELETE /api/posts/{id}`
- Header：`Authorization: Bearer <token>`

### 5.6 发表评论

- `POST /api/posts/{id}/comments`
- Header：`Authorization: Bearer <token>`
- Body:

```json
{
  "content": "抱抱你，你不是一个人。"
}
```

支持回复评论（可选字段）：

```json
{
  "content": "回复上一条评论",
  "parentCommentId": 12
}
```

### 5.7 帖子点赞/取消点赞

- `POST /api/posts/{id}/like`
- `DELETE /api/posts/{id}/like`
- Header：`Authorization: Bearer <token>`

### 5.8 评论点赞/取消点赞

- `POST /api/comments/{id}/like`
- `DELETE /api/comments/{id}/like`
- Header：`Authorization: Bearer <token>`

### 5.9 评论列表（分页）

- `GET /api/posts/{id}/comments?page=1&size=20`

### 5.10 更新评论（作者本人）

- `PUT /api/comments/{id}`
- Header：`Authorization: Bearer <token>`

```json
{
  "content": "更新后的评论内容"
}
```

### 5.11 删除评论（作者本人）

- `DELETE /api/comments/{id}`
- Header：`Authorization: Bearer <token>`

### 5.12 评论回复列表（分页）

- `GET /api/comments/{id}/replies?page=1&size=20`

## 6. 个人中心、通知、私信

### 6.1 我的摘要

- `GET /api/me/summary`
- Header：`Authorization: Bearer <token>`

### 6.2 最近相遇

- `GET /api/encounters/recent?limit=20`
- Header：`Authorization: Bearer <token>`

### 6.3 通知列表与已读

- `GET /api/notifications?page=1&size=20`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`
- `DELETE /api/notifications/{id}`
- `DELETE /api/notifications?readOnly=true`（仅清理已读）
- `DELETE /api/notifications`（清理全部业务通知，不含 auth_session）
- Header：`Authorization: Bearer <token>`

### 6.4 私信收件箱与发送

- `GET /api/messages/inbox?page=1&size=20`
- `GET /api/messages/sent?page=1&size=20`
- `GET /api/messages/conversations?page=1&size=20`（会话列表，按最近消息排序）
- `GET /api/messages/conversation?peerUserId=2&page=1&size=20`
- `POST /api/messages`
- `POST /api/messages/{id}/read`
- Header：`Authorization: Bearer <token>`
- 发送 Body:

```json
{
  "receiverUserId": 2,
  "content": "你好呀，愿你今晚有个好梦。"
}
```

### 6.5 WebSocket 实时私信

- 地址：`ws://localhost:3000/ws`
- 鉴权（二选一）：
  - Query：`ws://localhost:3000/ws?token=<token>`
  - Header：`Authorization: Bearer <token>`
- 服务端事件：
  - `ws.ready`：连接鉴权成功
  - `message.created`：当前用户收到新私信（作为接收方）
  - `message.sent`：当前用户发送私信成功后的回显（作为发送方）
  - `message.read`：对方已读你发送的私信
  - `ws.pong`：客户端发送 `{"type":"ping"}` 后的响应

## 7. 状态码

- `200` 成功
- `201` 创建成功
- `400` 参数错误
- `401` 未授权
- `404` 资源不存在（或无权限）
- `500` 服务内部错误

## 8. 测试建议（至少 5 条，含 auth + post）

1. `POST /api/auth/register`：注册新用户（期望 `201`）。
2. `POST /api/auth/login`：用刚注册账号登录（期望 `200`，返回 token）。
3. `POST /api/posts`：携带 Bearer token 创建帖子（期望 `201`）。
4. `POST /api/posts/{id}/comments`：新增评论（期望 `201`）。
5. `POST /api/posts/{id}/like`：帖子点赞（期望 `200`）。
6. `GET /api/notifications?page=1&size=10`：查询通知（期望 `200`）。
7. `POST /api/messages`：发送私信（期望 `201`）。
8. 负例：不带 token 调 `POST /api/posts`（期望 `401`）。
