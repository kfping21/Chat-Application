# Treehole Backend (Node.js + MySQL)

## 1) 环境变量

复制 `.env.example` 为 `.env` 并填写数据库连接：

```env
PORT=3000
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=123456
DB_NAME=treehole
```

## 2) 初始化数据库

```sql
source D:/Chat-Application/backend/sql/init.sql;
```

## 3) 启动

```bash
cd D:\Chat-Application\backend
npm install
npm run start
```

访问 `http://localhost:3000/` 可看到服务说明。

## 4) 作业新增 API（统一响应结构）

新增接口统一返回：

```json
{ "code": 200, "message": "ok", "data": {} }
```

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/meta/emotions`
- `GET /api/discover/topics/hot`
- `GET /api/posts?page=1&size=10&emotionCode=happy`
- `POST /api/posts`
- `GET /api/posts/:id`
- `PUT /api/posts/:id`
- `DELETE /api/posts/:id`
- `POST /api/posts/:id/comments`
- `POST /api/posts/:id/like`
- `DELETE /api/posts/:id/like`
- `POST /api/comments/:id/like`
- `DELETE /api/comments/:id/like`
- `GET /api/me/summary`
- `GET /api/encounters/recent?limit=20`
- `GET /api/notifications?page=1&size=20`
- `POST /api/notifications/:id/read`
- `POST /api/notifications/read-all`
- `GET /api/messages/inbox?page=1&size=20`
- `POST /api/messages`
- `POST /api/messages/:id/read`

实时私信（WebSocket）：

- `ws://localhost:3000/ws?token=<token>`
- 事件：`ws.ready`、`message.created`、`message.sent`、`message.read`

认证方式：

- 通过 `Authorization: Bearer <token>` 访问 `/api/*` 作业接口

## 5) 历史接口（兼容保留）

以下 `/api/v1/*` 接口继续可用，供原项目前后端使用：

- `GET /health`
- `GET /api/v1/ping`
- `GET /api/v1/home/feed?page=1&limit=20`
- `GET /api/v1/discover/topics/hot`
- `GET /api/v1/meta/emotions`
- `POST /api/v1/posts`
- `GET /api/v1/posts/:id`
- `POST /api/v1/posts/:id/comments`
- `POST /api/v1/posts/:id/like`
- `DELETE /api/v1/posts/:id/like`
- `POST /api/v1/comments/:id/like`
- `DELETE /api/v1/comments/:id/like`
- `GET /api/v1/me/summary`
- `GET /api/v1/encounters/recent?limit=20`
- `GET /api/v1/notifications?page=1&limit=20`
- `POST /api/v1/notifications/:id/read`
- `POST /api/v1/notifications/read-all`
- `GET /api/v1/messages/inbox?page=1&limit=20`
- `POST /api/v1/messages`
- `POST /api/v1/messages/:id/read`

## 6) 测试

```bash
npm test
```
