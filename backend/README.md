# Treehole Backend (MySQL)

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

你已建库后，执行一次：

```sql
source d:/树洞聊天软件/sql/init.sql;
```

## 3) 启动

```bash
cd d:\树洞聊天软件
npm install
npm run start
```

访问 `http://localhost:3000/` 可看到服务说明。

## 4) 当前 API

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

补充说明：
- `GET /api/v1/home/feed`、`GET /api/v1/posts/:id`、`GET /api/v1/me/summary` 现已返回帖子 `topics` 字段。
- `POST /api/v1/posts` 的 `topicIds` 会校验是否都存在，不存在会返回 `400`。
- `POST /api/v1/messages` 发送私信时会自动更新双方 `encounters` 记录。

默认通过请求头 `X-User-Id` 指定用户（不传则默认用户 `1`）。
