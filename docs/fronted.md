# 树洞交友软件前端开发说明文档 (Android)

## 1. 模块概述

本项目的前端部分是一个基于 **Android Studio** 开发的“树洞”社交聊天应用。主要目标是为用户提供一个安全、匿名（或半匿名）的倾诉空间，支持发布树洞动态、即时聊天及社交互动。

## 2. 技术选型

为了保证应用的流畅性与开发效率，我们采用以下技术栈：

- **开发环境**：Android Studio Jellyfish / Koala (最新稳定版)
- **编程语言**：Kotlin (推荐) / Java
- **UI 框架**：Material Design 3 (MD3)
- **网络请求**：Retrofit2 + OkHttp3 (处理 API 通信)
- **异步处理**：Kotlin Coroutines (协程)
- **图片加载**：Glide 或 Coil
- **本地存储**：Room Database (用于缓存消息和用户信息)
- **即时通讯**：WebSocket (或集成的第三方 IM SDK，如环信/腾讯云 IM)

## 3. 目录结构

前端代码位于 `/frontend` 目录下，采用典型的 **MVVM** 架构：

Plaintext

```
frontend/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/treehole/app/
│           │   ├── ui/             # 界面层 (Activities, Fragments)
│           │   │   ├── auth/       # 登录、注册
│           │   │   ├── hole/       # 树洞动态流、发布页
│           │   │   └── chat/       # 聊天会话、消息列表
│           │   ├── viewmodel/      # 业务逻辑与数据流转
│           │   ├── model/          # 数据实体类 (POJOs)
│           │   ├── repository/     # 数据仓库 (统一处理本地/网络数据)
│           │   └── network/        # API 接口定义与 Retrofit 配置
│           └── res/                # 资源文件 (Layout, Drawable, Values)
└── build.gradle                    # 项目构建配置
```

## 4. 核心功能实现

| **功能模块**   | **说明**                                                     |
| -------------- | ------------------------------------------------------------ |
| **匿名树洞流** | 展示用户发布的匿名动态，支持瀑布流布局，具备点赞、评论功能。 |
| **即时通讯**   | 实现 1 对 1 私聊。通过 WebSocket 保持长连接，支持文本、表情发送。 |
| **“撕掉”逻辑** | 模拟树洞特性，用户可以设置动态的“有效期”，过期后自动从流中消失。 |
| **个人中心**   | 管理“我的发布”、“我的收藏”以及应用设置。                     |