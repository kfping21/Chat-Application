# 后端模块说明

## 1. 功能概述

本后端为匿名树洞聊天应用提供 HTTP API，主要能力包括：

- 帖子流、发帖、帖子详情
- 评论发布与点赞/取消点赞
- 通知列表与已读管理
- 私信收件箱、发私信、私信已读
- 个人主页摘要与最近相遇用户
- 情绪与热门话题元数据查询

---

## 2. 技术选型

- 语言：Node.js（CommonJS）
- Web 层：原生 `http` 模块（无 Express）
- 数据库：MySQL 8.x
- 驱动：`mysql2/promise`
- 配置：`dotenv`

---

## 3. 目录结构（当前实现）

```text
backend/
├── .env.example         # 环境变量示例
├── package.json         # 依赖与脚本
├── README.md            # 后端使用说明
├── sql/
│   └── init.sql         # 建库建表+初始化数据
└── src/
    ├── db.js            # 数据库连接池与通用 DB 方法
    └── index.js         # HTTP 服务与路由处理
```

---

## 4. 核心模块说明

### 4.1 `src/db.js`

职责：
- 创建 MySQL 连接池
- 提供统一查询方法：`query`、`queryOne`
- 提供事务方法：`withTransaction`
- 提供健康检查与连接池关闭：`healthCheck`、`closePool`

说明：
- 连接池参数来自 `.env`（`DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME`）
- 事务封装自动 `beginTransaction/commit/rollback`

### 4.2 `src/index.js`

职责：
- 启动 HTTP 服务
- 统一 CORS 与 JSON 响应
- 请求体解析、分页处理、用户读取
- 路由分发与参数校验
- 错误处理与进程退出时资源释放

关键实现点：
- CORS：支持 `GET, POST, DELETE, OPTIONS`
- 默认用户：`X-User-Id` 未传时使用用户 `1`
- 路由级输入校验（长度、ID 合法性、存在性）
- 使用事务维护计数与关联一致性（点赞/评论/私信相遇）

---

## 5. 数据库交互设计

- 帖子查询返回情绪信息（`emotions`）与话题列表（`topics`）
- 发帖时校验 `emotionCode` 与 `topicIds` 是否有效
- 评论发布采用事务锁定楼层并更新 `posts.comments_count`
- 点赞/取消点赞采用幂等写法（`INSERT IGNORE` + 条件更新）
- 私信发送后同步 upsert 双向 `encounters`

---

## 6. 错误处理策略

- 业务错误：抛出带 `statusCode` 的错误并返回对应 `4xx`
- 未捕获错误：返回 `500` 与 `internal server error`
- 典型错误：
  - 参数不合法：`400`
  - 资源不存在：`404`

---

## 7. 运行方式

1. 进入后端目录并安装依赖：

```bash
cd backend
npm install
```

2. 配置环境变量：

```bash
copy .env.example .env
```

3. 启动服务：

```bash
npm start
```

默认端口：`3000`（可通过 `PORT` 覆盖）。

---

## 8. 后续可扩展方向

- 接入正式认证（JWT/Session）替代 `X-User-Id`
- 抽离路由层与 service 层，提升可维护性
- 引入日志与监控（请求追踪、慢查询）
- 补充自动化测试（接口测试、事务一致性测试）
