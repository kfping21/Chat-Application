# 后端模块说明

## 1. 功能概述

本后端为匿名树洞聊天应用提供 HTTP API，主要能力包括：

- 帖子流、发帖、帖子详情
- 评论发布与点赞/取消点赞
- 通知列表与已读管理
- 私信收件箱、发私信、私信已读
- 私信实时推送与已读回执（WebSocket）
- 个人主页摘要与最近相遇用户
- 情绪与热门话题元数据查询

---

## 2. 技术选型

- 语言：Node.js（CommonJS）
- Web 层：原生 `http` 模块（无 Express）
- 实时层：`ws`（WebSocket）
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
└── app/
    ├── db.js            # 数据库连接池与通用 DB 方法
    ├── index.js         # HTTP 服务与路由处理
    ├── realtime.js      # WebSocket 鉴权、连接管理与事件推送
    └── routes/
        ├── apiRoutes.js           # /api 聚合分发入口
        ├── authRoutes.js          # 认证接口
        ├── postRoutes.js          # 帖子 CRUD
        ├── interactionRoutes.js   # 评论与点赞
        ├── metaRoutes.js          # 情绪与话题元数据
        ├── profileRoutes.js       # 个人摘要与相遇
        ├── notificationRoutes.js  # 通知中心
        ├── messageRoutes.js       # 私信接口
        ├── v1/
        │   ├── index.js           # /api/v1 聚合分发入口
        │   ├── helpers.js         # v1 公共工具
        │   ├── pingRoutes.js
        │   ├── feedRoutes.js
        │   ├── metaRoutes.js
        │   ├── postWriteRoutes.js
        │   ├── interactionRoutes.js
        │   ├── profileRoutes.js
        │   ├── notificationRoutes.js
        │   └── messageRoutes.js
        ├── store/
        │   ├── index.js             # 聚合出口
        │   ├── commonStore.js       # 通用查询/映射/校验工具
        │   ├── authStore.js
        │   ├── postStore.js
        │   ├── interactionStore.js
        │   ├── metaStore.js
        │   ├── profileStore.js
        │   ├── notificationStore.js
        │   └── messageStore.js
        │   └── v1/
        │       ├── index.js
        │       ├── feedStore.js
        │       ├── metaStore.js
        │       ├── postWriteStore.js
        │       ├── interactionStore.js
        │       ├── profileStore.js
        │       ├── notificationStore.js
        │       └── messageStore.js
        └── utils/responseUtils.js
```

---

## 4. 核心模块说明

### 4.1 `app/db.js`

职责：
- 创建 MySQL 连接池
- 提供统一查询方法：`query`、`queryOne`
- 提供事务方法：`withTransaction`
- 提供健康检查与连接池关闭：`healthCheck`、`closePool`

说明：
- 连接池参数来自 `.env`（`DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME`）
- 事务封装自动 `beginTransaction/commit/rollback`

### 4.2 `app/index.js`

职责：
- 启动 HTTP 服务
- 统一 CORS 与 JSON 响应
- 请求体解析、分页处理、用户读取
- 路由分发与参数校验
- 错误处理与进程退出时资源释放
- 挂载 WebSocket 实时服务

关键实现点：
- CORS：支持 `GET, POST, DELETE, OPTIONS`
- 默认用户：`X-User-Id` 未传时使用用户 `1`
- 路由级输入校验（长度、ID 合法性、存在性）
- 使用事务维护计数与关联一致性（点赞/评论/私信相遇）

### 4.3 `app/routes/apiRoutes.js`

职责：
- 作为 `/api` 聚合入口，统一分发到业务路由模块。
- 采用“注册式路由处理器数组”顺序执行，命中即返回。

说明：
- 这种方式避免聚合文件里不断增加 `if` 链。
- 新增业务模块时，只需新增路由文件并注册到处理器数组。

### 4.4 `app/routes/v1/index.js`

职责：
- 作为 `/api/v1` 聚合入口，负责将历史接口按业务域分发到独立模块。
- 保持既有接口行为与响应格式兼容，仅做结构化拆分。

说明：
- `routes/v1/helpers.js` 仅保留纯工具（分页、路径解析、时间文案、用户ID提取）。
- 所有 v1 数据访问统一下沉到 `routes/store/v1/*Store.js` 与 `routes/store/v1/commonStore.js`。

### 4.5 `app/realtime.js`

职责：
- 创建 `/ws` WebSocket 服务并进行 token 鉴权
- 维护用户与多连接映射（同一账号多端在线）
- 对私信发送与已读事件进行定向推送
- 心跳探测与断连清理

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
