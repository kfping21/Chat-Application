# 后端模块说明

## 1. 功能概述

本后端为匿名树洞聊天应用提供 HTTP API，主要能力包括：

- 用户注册、登录与JWT认证
- 帖子发布、浏览、点赞、评论
- 通知中心与已读管理
- 私信聊天（Whisper）与实时消息推送
- 群聊派对（Party）功能
- 用户个人资料与关注系统
- 图片上传（本地存储与Cloudinary云存储）
- 实时在线状态与消息推送（Socket.IO）

---

## 2. 技术选型

- **语言**: Node.js (CommonJS)
- **Web框架**: Express.js
- **实时通信**: Socket.IO
- **数据库**: MongoDB (Mongoose ODM)
- **认证**: JWT (jsonwebtoken)
- **密码加密**: bcryptjs
- **文件上传**: multer + multer-storage-cloudinary
- **日志**: Winston
- **性能监控**: response-time + 自定义metrics
- **压缩**: compression (gzip)
- **跨域**: cors
- **环境配置**: dotenv
- **速率限制**: express-rate-limit

---

## 3. 目录结构（当前实现）

```text
backend/
├── .env.example              # 环境变量示例
├── package.json              # 依赖与脚本
├── src/
│   ├── index.js              # Express服务入口 + Socket.IO
│   ├── config/
│   │   ├── db.js             # MongoDB连接配置
│   │   ├── cloudinary.js     # Cloudinary云存储配置
│   │   ├── localStorage.js   # 本地文件存储配置
│   │   └── fixCounts.js      # 数据修复工具
│   ├── middleware/
│   │   └── auth.js           # JWT认证中间件
│   ├── models/
│   │   ├── User.js           # 用户模型
│   │   ├── Post.js           # 帖子模型
│   │   ├── Comment.js        # 评论模型
│   │   ├── Notification.js   # 通知模型
│   │   ├── ChatMessage.js    # 私信消息模型
│   │   ├── ChatRoom.js       # 私信房间模型
│   │   └── Party.js          # 群聊派对模型
│   ├── routes/
│   │   ├── auth.js           # 认证接口 (/api/auth)
│   │   ├── user.js           # 用户接口 (/api/user)
│   │   ├── posts.js          # 帖子接口 (/api/posts)
│   │   ├── notifications.js  # 通知接口 (/api/notifications)
│   │   ├── whisper.js        # 私信接口 (/api/whisper)
│   │   └ party.js            # 群聊接口 (/api/party)
│   ├── scripts/
│   │   ├── seed-users.js     # 测试用户生成脚本
│   │   └ update-avatars.js   # 头像更新脚本
│   ├── utils/
│   │   ├── logger.js         # Winston日志工具
│   │   └ metrics.js          # 性能指标收集
│   └ uploads/                 # 本地上传文件存储
│       ├── avatars/          # 用户头像
│       └── posts/            # 帖子图片
└── backend/
    └── logs/                  # 日志文件目录
```

---

## 4. 核心模块说明

### 4.1 `src/index.js`

职责：
- 创建Express应用与HTTP服务器
- 配置Socket.IO实时通信服务
- 设置中间件（cors、compression、response-time）
- 连接MongoDB数据库
- 挂载所有API路由
- 提供健康检查与性能指标接口
- 管理在线用户状态

关键实现点：
- CORS：支持跨域访问
- Socket.IO：实时消息推送与在线状态管理
- 响应时间记录：每个请求的性能监控
- 静态文件服务：本地上传的图片文件

### 4.2 `src/config/db.js`

职责：
- 连接MongoDB数据库
- 配置Mongoose连接选项
- 处理连接错误与重连

### 4.3 `src/middleware/auth.js`

职责：
- JWT Token验证
- 提取用户ID并注入到请求对象
- 处理Token过期与无效情况

### 4.4 路由模块

#### `routes/auth.js` - 认证接口
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/me` - 获取当前用户信息
- `POST /api/auth/change-password` - 修改密码

#### `routes/posts.js` - 帖子接口
- `POST /api/posts` - 创建帖子（支持图片上传）
- `GET /api/posts/feed` - 获取帖子流
- `GET /api/posts/my` - 获取我的帖子
- `GET /api/posts/search` - 搜索帖子
- `GET /api/posts/user/:userId` - 获取用户帖子
- `GET /api/posts/liked` - 获取我点赞的帖子
- `GET /api/posts/comments/my` - 获取我的评论
- `GET /api/posts/:id` - 获取帖子详情
- `POST /api/posts/:id/like` - 点赞帖子
- `POST /api/posts/:id/comment` - 评论帖子
- `DELETE /api/posts/:id` - 删除帖子
- `PUT /api/posts/:id` - 更新帖子
- `PUT /api/posts/comments/:id` - 更新评论
- `DELETE /api/posts/comments/:id` - 删除评论

#### `routes/user.js` - 用户接口
- `PUT /api/user/profile` - 更新个人资料
- `POST /api/user/avatar` - 上传头像
- `GET /api/user/discover` - 发现用户
- `GET /api/user/:id` - 获取用户信息
- `GET /api/user/:id/following` - 获取关注列表
- `GET /api/user/:id/followers` - 获取粉丝列表

#### `routes/notifications.js` - 通知接口
- `GET /api/notifications` - 获取通知列表
- `PUT /api/notifications/:id/read` - 标记已读
- `PUT /api/notifications/read-all` - 全部标记已读
- `GET /api/notifications/unread-count` - 未读数量
- `POST /api/notifications/cleanup` - 清理重复通知
- `DELETE /api/notifications/clear-all` - 清空通知

#### `routes/whisper.js` - 私信接口
- `GET /api/whisper/rooms` - 获取聊天房间列表
- `GET /api/whisper/history/:roomId` - 获取聊天历史
- `POST /api/whisper/message/:roomId` - 发送消息
- `POST /api/whisper/follow/:userId` - 关注用户
- `GET /api/whisper/user/:userId` - 获取用户信息
- `POST /api/whisper/start/:userId` - 开始聊天
- `POST /api/whisper/read/:roomId` - 标记已读

#### `routes/party.js` - 群聊接口
- `GET /api/party/rooms` - 获取派对房间列表
- `POST /api/party/rooms` - 创建派对房间
- `POST /api/party/rooms/:roomId/join` - 加入房间
- `POST /api/party/rooms/:roomId/dismiss` - 解散房间
- `POST /api/party/rooms/:roomId/leave` - 离开房间
- `GET /api/party/rooms/:roomId` - 获取房间详情
- `GET /api/party/messages/:roomId` - 获取房间消息
- `POST /api/party/messages/:roomId` - 发送房间消息

---

## 5. 数据库模型设计

### 5.1 User模型
- 字段：username, password, nickname, avatar, bio
- 关系：following, followers（引用User）
- 状态：isOnline, lastOnlineAt
- 索引：username（唯一索引）

### 5.2 Post模型
- 字段：userId, content, mood, imageUrls
- 统计：likes, commentCount
- 关系：likedBy（引用User）
- 索引：createdAt, userId

### 5.3 Comment模型
- 字段：postId, userId, content
- 索引：postId + createdAt

### 5.4 Notification模型
- 字段：recipientId, senderId, type, message
- 关系：postId, commentId（可选引用）
- 状态：read（已读标记）
- 索引：recipientId + createdAt

### 5.5 ChatMessage模型
- 字段：chatId, senderId, senderNickname, content
- 索引：chatId + timestamp

### 5.6 ChatRoom模型
- 字段：participants（用户ID数组）
- 状态：lastMessage, lastActivity

### 5.7 Party模型
- PartyRoom：creator, participants, name, description
- PartyMessage：roomId, senderId, content

---

## 6. 实时通信（Socket.IO）

### 6.1 Whisper聊天
- 事件：`join_room`, `send_message`, `receive_message`
- 支持实时消息推送与在线状态更新

### 6.2 Party群聊
- 事件：`join_party`, `party_message`, `leave_party`
- 支持多人实时聊天

---

## 7. 文件上传处理

### 7.1 本地存储
- 头像存储：`uploads/avatars/`
- 帖子图片：`uploads/posts/`
- URL格式：`http://host/uploads/avatars/filename.jpg`

### 7.2 Cloudinary云存储（可选）
- 配置：`config/cloudinary.js`
- 支持图片自动优化与CDN加速

---

## 8. 错误处理策略

- 业务错误：返回对应HTTP状态码（400/401/404/500）
- 统一错误响应格式：`{ message: '错误描述' }`
- JWT认证失败：返回401状态码
- 参数验证失败：返回400状态码
- 资源不存在：返回404状态码

---

## 9. 运行方式

### 9.1 本地开发
```bash
cd backend
npm install
npm run dev  # 使用nodemon自动重启
```

### 9.2 本地运行
```bash
npm start
```

### 9.3 环境变量配置

```env
PORT=3000
MONGODB_URI=mongodb://localhost:27017/treehole
JWT_SECRET=your_jwt_secret_key
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

---

## 10. 性能优化

- Gzip压缩：减少响应体积
- 响应时间监控：记录每个请求耗时
- MongoDB索引：优化查询性能
- 连接池：Mongoose自动管理
- 静态文件缓存：Express静态服务

---

## 11. 安全措施

- JWT认证：保护API接口
- 密码加密：bcryptjs哈希存储
- 速率限制：防止API滥用
- CORS配置：控制跨域访问
- 输入验证：参数长度与格式检查
- 文件类型限制：只允许图片上传

---

## 12. 后续可扩展方向

- 引入Redis缓存热门数据
- 增加消息队列处理异步任务
- 完善单元测试与集成测试
- 添加API文档自动生成（Swagger）
- 实现更细粒度的权限控制
- 支持消息已读回执与撤回功能