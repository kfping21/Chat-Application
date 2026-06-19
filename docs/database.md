# 数据库设计文档（MongoDB模型设计）

## 1. 设计目标

本数据库用于"匿名树洞聊天系统"后端，覆盖以下核心业务：

- 用户注册、登录与个人资料管理
- 帖子发布与互动（点赞、评论）
- 通知中心与实时推送
- 私信聊天（Whisper）与群聊派对（Party）
- 用户关注与社交关系

数据库类型：MongoDB（文档数据库），使用Mongoose ODM进行模型定义。

---

## 2. 数据模型概览

### 核心模型：
- `User` - 用户模型
- `Post` - 帖子模型
- `Comment` - 评论模型
- `Notification` - 通知模型
- `ChatMessage` - 私信消息模型
- `ChatRoom` - 私信房间模型
- `PartyRoom` - 群聊房间模型
- `PartyMessage` - 群聊消息模型

---

## 3. 模型详细设计

### 3.1 User模型（用户表）

**文件位置**: `backend/src/models/User.js`

**字段设计**:
```javascript
{
  username: String,           // 用户名（唯一，3-20字符）
  password: String,           // 密码（bcrypt加密，最少6位）
  nickname: String,           // 昵称（默认为用户名）
  avatar: String,             // 头像URL
  bio: String,                // 个人简介（最多200字）
  isOnline: Boolean,          // 在线状态
  lastOnlineAt: Date,         // 最后在线时间
  following: [ObjectId],      // 关注列表（引用User）
  followers: [ObjectId],      // 粉丝列表（引用User）
  createdAt: Date             // 注册时间
}
```

**索引设计**:
- `username`: 唯一索引（快速查找用户）
- `following/followers`: 数组索引（社交关系查询）

**特性**:
- 密码自动加密（pre-save hook）
- 密码验证方法（comparePassword）
- 支持关注/粉丝关系

---

### 3.2 Post模型（帖子表）

**文件位置**: `backend/src/models/Post.js`

**字段设计**:
```javascript
{
  userId: ObjectId,           // 发布者ID（引用User）
  content: String,            // 帖子内容（最多2000字）
  mood: String,               // 心情标签（默认"平静"）
  imageUrls: [String],         // 图片URL数组（最多2张）
  likes: Number,              // 点赞数（冗余计数）
  commentCount: Number,       // 评论数（冗余计数）
  likedBy: [ObjectId],        // 点赞用户列表（引用User）
  createdAt: Date             // 发布时间
}
```

**索引设计**:
- `userId`: 单字段索引（用户帖子查询）
- `createdAt`: 单字段索引（时间线排序）
- `userId + createdAt`: 复合索引（用户帖子时间排序）

**特性**:
- 支持图片上传（最多2张）
- 冗余计数优化查询性能
- 点赞用户列表防止重复点赞

---

### 3.3 Comment模型（评论表）

**文件位置**: `backend/src/models/Comment.js`

**字段设计**:
```javascript
{
  postId: ObjectId,           // 帖子ID（引用Post）
  userId: ObjectId,           // 评论者ID（引用User）
  content: String,            // 评论内容（最多500字）
  createdAt: Date             // 评论时间
}
```

**索引设计**:
- `postId`: 单字段索引（帖子评论查询）
- `userId`: 单字段索引（用户评论查询）
- `postId + createdAt`: 复合索引（帖子评论时间排序）

---

### 3.4 Notification模型（通知表）

**文件位置**: `backend/src/models/Notification.js`

**字段设计**:
```javascript
{
  recipientId: ObjectId,      // 接收者ID（引用User）
  senderId: ObjectId,         // 发送者ID（引用User）
  type: String,               // 通知类型（like/comment/follow）
  postId: ObjectId,           // 相关帖子ID（可选）
  commentId: ObjectId,        // 相关评论ID（可选）
  message: String,            // 通知消息内容
  read: Boolean,              // 已读状态
  createdAt: Date             // 创建时间
}
```

**索引设计**:
- `recipientId + createdAt`: 复合索引（用户通知列表）
- `recipientId + read`: 复合索引（未读通知查询）

**特性**:
- 支持三种通知类型：点赞、评论、关注
- 关联业务对象ID便于跳转
- 已读状态管理

---

### 3.5 ChatMessage模型（私信消息表）

**文件位置**: `backend/src/models/ChatMessage.js`

**字段设计**:
```javascript
{
  chatId: String,             // 聊天房间ID（组合ID）
  senderId: String,           // 发送者ID
  senderNickname: String,     // 发送者昵称
  content: String,            // 消息内容
  timestamp: Date             // 消息时间
}
```

**索引设计**:
- `chatId`: 单字段索引（房间消息查询）
- `chatId + timestamp`: 复合索引（聊天历史查询）

---

### 3.6 ChatRoom模型（私信房间表）

**文件位置**: `backend/src/models/ChatRoom.js`

**字段设计**:
```javascript
{
  participants: [ObjectId],   // 参与者列表（引用User）
  lastMessage: String,       // 最后一条消息
  lastActivity: Date          // 最后活动时间
}
```

**索引设计**:
- `participants`: 数组索引（查找房间）
- `lastActivity`: 单字段索引（房间列表排序）

---

### 3.7 Party模型（群聊派对）

**文件位置**: `backend/src/models/Party.js`

包含两个子模型：

#### PartyRoom模型
```javascript
{
  creator: ObjectId,          // 创建者ID（引用User）
  participants: [ObjectId],   // 参与者列表（引用User）
  name: String,               // 房间名称
  description: String,        // 房间描述
  createdAt: Date             // 创建时间
}
```

#### PartyMessage模型
```javascript
{
  roomId: ObjectId,           // 房间ID（引用PartyRoom）
  senderId: ObjectId,         // 发送者ID（引用User）
  senderNickname: String,    // 发送者昵称
  content: String,            // 消息内容
  createdAt: Date             // 消息时间
}
```

---

## 4. 索引策略

### 4.1 单字段索引
- User.username（唯一索引）
- Post.userId, Post.createdAt
- Comment.postId, Comment.userId
- Notification.recipientId

### 4.2 复合索引
- Post(userId + createdAt) - 用户帖子时间排序
- Comment(postId + createdAt) - 帖子评论时间排序
- Notification(recipientId + createdAt) - 用户通知列表
- ChatMessage(chatId + timestamp) - 聊天历史查询

### 4.3 数组索引
- User.following/followers - 社交关系查询
- Post.likedBy - 点赞用户检查
- ChatRoom.participants - 房间参与者查询

---

## 5. 数据一致性设计

### 5.1 点赞一致性
- 点赞时：同时更新Post.likes计数和likedBy数组
- 取消点赞时：同步减少计数和移除用户ID

### 5.2 评论一致性
- 评论时：同时创建Comment并增加Post.commentCount
- 删除评论时：同步减少计数

### 5.3 关注一致性
- 关注时：在双方User文档中更新following和followers数组
- 取消关注时：同步移除双方关系

### 5.4 通知一致性
- 点赞/评论/关注时：创建对应类型的通知
- 取消点赞时：删除对应通知（防止重复）

---

## 6. 技术选型理由

### MongoDB优势：
- 文档模型更适合社交应用
- 无需预定义表结构，灵活扩展
- 内置支持数组、嵌套文档
- 天然支持JSON格式

### 选择MongoDB的原因：
- 社交应用数据结构灵活
- 需要频繁存储数组数据（关注列表、点赞列表）
- 与前端JSON数据格式天然匹配
- 开发效率更高

---

## 7. 后续优化方向

- 引入Redis缓存热门帖子
- 实现消息队列异步处理通知
- 增加数据分片支持大规模用户
- 完善数据统计与报表功能
- 实现数据生命周期管理（自动清理过期数据）