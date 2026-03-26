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
- 使用注册成功后的用户名与密码（用户名映射 `users.anonymous_name`，密码映射 `users.avatar_color`）

### 3.3 登出

- `POST /api/auth/logout`
- Header：`Authorization: Bearer <token>`

## 4. 核心业务资源（帖子）CRUD

### 4.1 列表（分页+筛选）

- `GET /api/posts?page=1&size=10&emotionCode=happy`
- `emotionCode` 可选，对应 `emotions.code`

### 4.2 创建帖子

- `POST /api/posts`
- Header：`Authorization: Bearer <token>`
- Body:

```json
{
  "content": "今天有点累，但我在坚持。",
  "emotionCode": "happy",
  "allowComments": true,
  "isPublic": true
}
```

### 4.3 获取单个帖子

- `GET /api/posts/{id}`

### 4.4 更新帖子（作者本人）

- `PUT /api/posts/{id}`
- Header：`Authorization: Bearer <token>`
- Body（至少一个字段）：

```json
{
  "content": "更新后的内容",
  "emotionCode": "calm",
  "allowComments": false,
  "isPublic": true
}
```

### 4.5 删除帖子（作者本人）

- `DELETE /api/posts/{id}`
- Header：`Authorization: Bearer <token>`

## 5. 状态码

- `200` 成功
- `201` 创建成功
- `400` 参数错误
- `401` 未授权
- `404` 资源不存在（或无权限）
- `500` 服务内部错误

## 6. 测试建议（至少 5 条，含 auth + post）

1. `POST /api/auth/register`：注册新用户（期望 `201`）。
2. `POST /api/auth/login`：用刚注册账号登录（期望 `200`，返回 token）。
3. `POST /api/posts`：携带 Bearer token 创建帖子（期望 `201`）。
4. `GET /api/posts?page=1&size=10&emotionCode=happy`：分页+筛选（期望 `200`）。
5. `PUT /api/posts/{id}`：更新自己创建的帖子（期望 `200`）。
6. `DELETE /api/posts/{id}`：删除自己创建的帖子（期望 `200`）。
7. 负例：不带 token 调 `POST /api/posts`（期望 `401`）。
