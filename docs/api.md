# API 设计文档

## 1. 基础信息

- Base URL：`http://localhost:3000`
- 新增作业接口前缀：`/api`
- 兼容保留历史接口前缀：`/api/v1`
- 数据格式：`application/json; charset=utf-8`
- OpenAPI 文档：`docs/api.yaml`

---

## 2. 统一响应结构（作业接口）

所有 `/api/*` 新增接口统一返回：

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

典型状态码：

- `200` 请求成功
- `201` 创建成功
- `400` 参数错误
- `401` 未认证或认证失败
- `404` 资源不存在
- `500` 服务内部错误

---

## 3. 认证 API

### POST `/api/auth/register`

注册用户并返回 Token。

请求：

```json
{
  "username": "alice",
  "password": "alice123",
  "displayName": "Alice"
}
```

### POST `/api/auth/login`

用户登录并返回 Token。

### POST `/api/auth/logout`

用户登出（需 `Authorization: Bearer <token>`）。

---

## 4. Todo 资源 API（CRUD + 分页筛选）

### GET `/api/todos?page=1&size=10&completed=false`

获取当前登录用户 Todo 列表，支持分页和按完成状态筛选。

### POST `/api/todos`

创建 Todo：

```json
{
  "title": "完成 API 作业",
  "completed": false
}
```

### GET `/api/todos/:id`

获取单个 Todo。

### PUT `/api/todos/:id`

更新 Todo（可更新 `title` 与 `completed`）：

```json
{
  "title": "已完成 API 作业",
  "completed": true
}
```

### DELETE `/api/todos/:id`

删除 Todo。

---

## 5. 鉴权方式

作业接口使用 Bearer Token 鉴权：

```http
Authorization: Bearer <token>
```

---

## 6. 历史业务接口说明

当前项目原有接口（`/api/v1/*`）继续可用，主要包含：

- 帖子流、发帖、评论、点赞
- 通知与已读
- 私信收件箱与消息已读
- 个人摘要与相遇用户

这些接口沿用原有响应结构，不影响已有客户端调用。
