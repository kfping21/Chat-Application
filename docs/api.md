# API 使用说明（树洞聊天应用）

## 1. 基础信息

- **Base URL**: `http://<电脑内网IP>:3000`（移动端通过内网访问）
- **API前缀**: `/api`
- **认证方式**: JWT Bearer Token
- **数据格式**: JSON
- **实时通信**: Socket.IO WebSocket

### 1.1 环境配置

复制 `backend/.env.example` 为 `backend/.env`，配置以下环境变量：

```env
PORT=3000
MONGODB_URI=mongodb://localhost:27017/treehole
JWT_SECRET=your_jwt_secret_key
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

---

## 2. 统一响应格式

### 2.1 成功响应

```json
{
  "message": "操作成功",
  "data": {
    // 具体数据
  }
}
```

### 2.2 错误响应

```json
{
  "message": "错误描述"
}
```

---

## 3. 认证接口（/api/auth）

### 3.1 用户注册

**接口**: `POST /api/auth/register`

**请求Body**:
```json
{
  "username": "user001",
  "password": "123456",
  "nickname": "匿名用户001"
}
```

**响应**:
```json
{
  "message": "注册成功",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "username": "user001",
    "nickname": "匿名用户001"
  }
}
```

**状态码**:
- `201`: 注册成功
- `400`: 用户名已存在或参数错误

---

### 3.2 用户登录

**接口**: `POST /api/auth/login`

**请求Body**:
```json
{
  "username": "user001",
  "password": "123456"
}
```

**响应**:
```json
{
  "message": "登录成功",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "username": "user001",
    "nickname": "匿名用户001",
    "avatar": "http://...",
    "bio": "个人简介"
  }
}
```

**状态码**:
- `200`: 登录成功
- `401`: 用户名或密码错误

---

### 3.3 获取当前用户信息

**接口**: `GET /api/auth/me`

**Header**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "username": "user001",
    "nickname": "匿名用户001",
    "avatar": "http://...",
    "bio": "个人简介",
    "following": [],
    "followers": [],
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### 3.4 修改密码

**接口**: `POST /api/auth/change-password`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "currentPassword": "123456",
  "newPassword": "newpassword"
}
```

---

## 4. 帖子接口（/api/posts）

### 4.1 创建帖子

**接口**: `POST /api/posts`

**Header**: `Authorization: Bearer <token>`

**请求Body**（支持图片上传）:
```
Content-Type: multipart/form-data

content: "今天心情不错"
mood: "开心"
images: [File, File]  // 最多2张图片
```

**响应**:
```json
{
  "message": "发布成功",
  "post": {
    "id": "507f1f77bcf86cd799439012",
    "userId": "507f1f77bcf86cd799439011",
    "content": "今天心情不错",
    "mood": "开心",
    "imageUrls": ["http://..."],
    "likes": 0,
    "commentCount": 0,
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### 4.2 获取帖子流

**接口**: `GET /api/posts/feed?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "posts": [
    {
      "id": "...",
      "userId": "...",
      "content": "...",
      "mood": "开心",
      "imageUrls": [],
      "likes": 10,
      "commentCount": 5,
      "likedBy": [],
      "createdAt": "...",
      "user": {
        "id": "...",
        "username": "...",
        "nickname": "...",
        "avatar": "..."
      }
    }
  ],
  "total": 100,
  "page": 1,
  "limit": 20
}
```

---

### 4.3 获取我的帖子

**接口**: `GET /api/posts/my?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

---

### 4.4 搜索帖子

**接口**: `GET /api/posts/search?keyword=心情&page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

---

### 4.5 获取用户帖子

**接口**: `GET /api/posts/user/:userId?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

---

### 4.6 获取我点赞的帖子

**接口**: `GET /api/posts/liked?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

---

### 4.7 获取我的评论

**接口**: `GET /api/posts/comments/my?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

---

### 4.8 获取帖子详情

**接口**: `GET /api/posts/:id`

**Header**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "post": {
    "id": "...",
    "content": "...",
    "mood": "开心",
    "likes": 10,
    "commentCount": 5,
    "user": {
      "id": "...",
      "username": "...",
      "nickname": "...",
      "avatar": "..."
    }
  },
  "comments": [
    {
      "id": "...",
      "userId": "...",
      "content": "...",
      "createdAt": "...",
      "user": {
        "id": "...",
        "username": "...",
        "nickname": "...",
        "avatar": "..."
      }
    }
  ]
}
```

---

### 4.9 点赞帖子

**接口**: `POST /api/posts/:id/like`

**Header**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "message": "点赞成功",
  "likes": 11,
  "liked": true
}
```

---

### 4.10 评论帖子

**接口**: `POST /api/posts/:id/comment`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "content": "很好的帖子！"
}
```

---

### 4.11 删除帖子

**接口**: `DELETE /api/posts/:id`

**Header**: `Authorization: Bearer <token>`

---

### 4.12 更新帖子

**接口**: `PUT /api/posts/:id`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "content": "更新后的内容",
  "mood": "平静"
}
```

---

### 4.13 更新评论

**接口**: `PUT /api/posts/comments/:id`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "content": "更新后的评论"
}
```

---

### 4.14 删除评论

**接口**: `DELETE /api/posts/comments/:id`

**Header**: `Authorization: Bearer <token>`

---

## 5. 用户接口（/api/user）

### 5.1 更新个人资料

**接口**: `PUT /api/user/profile`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "nickname": "新昵称",
  "bio": "新的个人简介"
}
```

---

### 5.2 上传头像

**接口**: `POST /api/user/avatar`

**Header**: `Authorization: Bearer <token>`

**请求Body**（multipart/form-data）:
```
avatar: File
```

---

### 5.3 发现用户

**接口**: `GET /api/user/discover?limit=20`

**Header**: `Authorization: Bearer <token>`

---

### 5.4 获取用户信息

**接口**: `GET /api/user/:id`

**Header**: `Authorization: Bearer <token>`

---

### 5.5 获取关注列表

**接口**: `GET /api/user/:id/following?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

---

### 5.6 获取粉丝列表

**接口**: `GET /api/user/:id/followers?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

---

## 6. 通知接口（/api/notifications）

### 6.1 获取通知列表

**接口**: `GET /api/notifications?page=1&limit=20`

**Header**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "notifications": [
    {
      "id": "...",
      "recipientId": "...",
      "senderId": "...",
      "type": "like",
      "postId": "...",
      "message": "用户A点赞了你的帖子",
      "read": false,
      "createdAt": "...",
      "sender": {
        "id": "...",
        "username": "...",
        "nickname": "...",
        "avatar": "..."
      }
    }
  ],
  "unreadCount": 5
}
```

---

### 6.2 标记已读

**接口**: `PUT /api/notifications/:id/read`

**Header**: `Authorization: Bearer <token>`

---

### 6.3 全部标记已读

**接口**: `PUT /api/notifications/read-all`

**Header**: `Authorization: Bearer <token>`

---

### 6.4 未读数量

**接口**: `GET /api/notifications/unread-count`

**Header**: `Authorization: Bearer <token>`

---

### 6.5 清理重复通知

**接口**: `POST /api/notifications/cleanup`

**Header**: `Authorization: Bearer <token>`

---

### 6.6 清空通知

**接口**: `DELETE /api/notifications/clear-all`

**Header**: `Authorization: Bearer <token>`

---

## 7. 私信接口（/api/whisper）

### 7.1 获取聊天房间列表

**接口**: `GET /api/whisper/rooms`

**Header**: `Authorization: Bearer <token>`

---

### 7.2 获取聊天历史

**接口**: `GET /api/whisper/history/:roomId?page=1&limit=50`

**Header**: `Authorization: Bearer <token>`

---

### 7.3 发送消息

**接口**: `POST /api/whisper/message/:roomId`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "content": "你好！"
}
```

---

### 7.4 关注用户

**接口**: `POST /api/whisper/follow/:userId`

**Header**: `Authorization: Bearer <token>`

---

### 7.5 获取用户信息

**接口**: `GET /api/whisper/user/:userId`

**Header**: `Authorization: Bearer <token>`

---

### 7.6 开始聊天

**接口**: `POST /api/whisper/start/:userId`

**Header**: `Authorization: Bearer <token>`

---

### 7.7 标记已读

**接口**: `POST /api/whisper/read/:roomId`

**Header**: `Authorization: Bearer <token>`

---

## 8. 群聊接口（/api/party）

### 8.1 获取派对房间列表

**接口**: `GET /api/party/rooms`

**Header**: `Authorization: Bearer <token>`

---

### 8.2 创建派对房间

**接口**: `POST /api/party/rooms`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "name": "开心聊天室",
  "description": "分享开心的事情"
}
```

---

### 8.3 加入房间

**接口**: `POST /api/party/rooms/:roomId/join`

**Header**: `Authorization: Bearer <token>`

---

### 8.4 解散房间

**接口**: `POST /api/party/rooms/:roomId/dismiss`

**Header**: `Authorization: Bearer <token>`

---

### 8.5 离开房间

**接口**: `POST /api/party/rooms/:roomId/leave`

**Header**: `Authorization: Bearer <token>`

---

### 8.6 获取房间详情

**接口**: `GET /api/party/rooms/:roomId`

**Header**: `Authorization: Bearer <token>`

---

### 8.7 获取房间消息

**接口**: `GET /api/party/messages/:roomId?page=1&limit=50`

**Header**: `Authorization: Bearer <token>`

---

### 8.8 发送房间消息

**接口**: `POST /api/party/messages/:roomId`

**Header**: `Authorization: Bearer <token>`

**请求Body**:
```json
{
  "content": "大家好！"
}
```

---

## 9. 实时通信（Socket.IO）

### 9.1 连接地址

**WebSocket URL**: `ws://server-url`

### 9.2 认证方式

连接时携带JWT Token：
```javascript
const socket = io('ws://server-url', {
  auth: {
    token: 'Bearer <your_token>'
  }
});
```

### 9.3 Whisper聊天事件

**加入房间**:
```javascript
socket.emit('join_room', { roomId: 'room123' });
```

**发送消息**:
```javascript
socket.emit('send_message', {
  roomId: 'room123',
  content: '你好！'
});
```

**接收消息**:
```javascript
socket.on('receive_message', (data) => {
  console.log('收到消息:', data);
});
```

### 9.4 Party群聊事件

**加入派对**:
```javascript
socket.emit('join_party', { roomId: 'party123' });
```

**发送群聊消息**:
```javascript
socket.emit('party_message', {
  roomId: 'party123',
  content: '大家好！'
});
```

**离开派对**:
```javascript
socket.emit('leave_party', { roomId: 'party123' });
```

---

## 10. 状态码说明

- `200`: 成功
- `201`: 创建成功
- `400`: 参数错误
- `401`: 未授权（Token无效或过期）
- `404`: 资源不存在
- `500`: 服务器内部错误

---

## 11. 测试建议

### 11.1 基础测试流程

1. 注册用户：`POST /api/auth/register`
2. 登录获取Token：`POST /api/auth/login`
3. 创建帖子：`POST /api/posts`（携带Token）
4. 点赞帖子：`POST /api/posts/:id/like`
5. 评论帖子：`POST /api/posts/:id/comment`
6. 获取通知：`GET /api/notifications`
7. 发送私信：`POST /api/whisper/message/:roomId`

### 11.2 错误测试

- 不带Token访问需要认证的接口（期望401）
- 使用过期Token访问接口（期望401）
- 参数缺失或格式错误（期望400）
- 访问不存在的资源（期望404）

---

## 12. 注意事项

### 12.1 Token有效期

- JWT Token有效期为7天
- Token过期后需要重新登录获取新Token
- 建议客户端存储Token时检查有效期

### 12.2 文件上传限制

- 图片上传最多2张
- 文件类型限制为图片格式
- 文件大小建议不超过5MB

### 12.3 实时消息

- Socket.IO连接需要JWT认证
- 断线后会自动重连
- 消息格式为JSON

### 12.4 分页参数

- 默认page=1, limit=20
- 最大limit=100
- 使用skip+limit实现分页