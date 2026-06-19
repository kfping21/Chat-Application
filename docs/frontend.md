# 树洞交友软件前端开发说明文档 (Android)

## 1. 模块概述

本项目的前端部分是一个基于 **Android Studio** 开发的"树洞"社交聊天应用。主要目标是为用户提供一个安全、匿名（或半匿名）的倾诉空间，支持发布树洞动态、即时聊天及社交互动。

---

## 2. 技术选型

为了保证应用的流畅性与开发效率，我们采用以下技术栈：

- **开发环境**: Android Studio Jellyfish / Koala (最新稳定版)
- **编程语言**: Kotlin (推荐) / Java
- **UI 框架**: Material Design 3 (MD3)
- **网络请求**: Retrofit2 + OkHttp3 (处理 API 通信)
- **异步处理**: Kotlin Coroutines (协程)
- **图片加载**: Glide 或 Coil
- **本地存储**: Room Database (用于缓存消息和用户信息)
- **即时通讯**: Socket.IO (WebSocket客户端)
- **架构模式**: MVVM (Model-View-ViewModel)

---

## 3. 目录结构

前端代码位于 `/frontend` 目录下，采用典型的 **MVVM** 架构：

```text
frontend/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/zjgsu/treehole/
│           │   ├── ui/             # 界面层 (Activities, Fragments)
│           │   │   ├── auth/       # 登录、注册
│           │   │   ├── hole/       # 树洞动态流、发布页
│           │   │   ├── chat/       # 聊天会话、消息列表
│           │   │   ├── profile/    # 个人中心
│           │   │   ├── party/      # 群聊派对
│           │   │   └── widgets/    # 自定义UI组件
│           │   ├── adapter/        # RecyclerView适配器
│           │   ├── model/          # 数据实体类 (POJOs)
│           │   ├── network/        # API 接口定义与 Retrofit 配置
│           │   │   ├── RetrofitClient.kt
│           │   │   ├── AuthApi.kt
│           │   │   ├── PostsApi.kt
│           │   │   ├── WhisperApi.kt
│           │   │   ├── PartyApi.kt
│           │   │   └── NotificationsApi.kt
│           │   ├── cache/          # 本地缓存管理
│           │   │   ├── DraftManager.kt
│           │   │   ├── HistoryManager.kt
│           │   │   ├── PostCacheManager.kt
│           │   │   └── SearchHistoryManager.kt
│           │   ├── util/           # 工具类
│           │   │   ├── AvatarLoader.kt
│           │   │   ├── ImageUtils.kt
│           │   │   ├── NetworkMonitor.kt
│           │   │   ├── NetworkToastManager.kt
│           │   │   ├── TimeUtils.kt
│           │   ├── service/        # 后台服务
│           │   │   └── PostPreloadService.kt
│           │   └── MainActivity.kt # 主Activity
│           └── res/                # 资源文件 (Layout, Drawable, Values)
│               ├── drawable/       # 图标与背景
│               ├── layout/         # 布局文件
│               ├── values/         # 字符串、颜色、样式
│               └── anim/           # 动画资源
└── build.gradle.kts               # 项目构建配置
```

---

## 4. 核心功能实现

### 4.1 匿名树洞流

**功能说明**:
- 展示用户发布的匿名动态
- 支持瀑布流布局或列表布局
- 具备点赞、评论功能
- 支持图片展示（最多2张）
- 心情标签显示

**技术实现**:
- RecyclerView + Adapter
- Glide图片加载
- 分页加载（Retrofit + Coroutines）
- 点赞动画效果

---

### 4.2 即时通讯

**功能说明**:
- 实现 1 对 1 私聊（Whisper）
- 支持多人群聊（Party）
- 通过 Socket.IO 保持长连接
- 支持文本、表情发送
- 实时消息推送与在线状态显示

**技术实现**:
- Socket.IO客户端
- WebSocket连接管理
- 消息缓存（Room Database）
- 实时UI更新（LiveData + ViewModel）

---

### 4.3 "撕掉"逻辑

**功能说明**:
- 模拟树洞特性，用户可以设置动态的"有效期"
- 过期后自动从流中消失
- 支持定时删除功能

**技术实现**:
- 后台服务（Service）
- 定时任务（WorkManager）
- 数据库时间戳管理

---

### 4.4 个人中心

**功能说明**:
- 管理"我的发布"、"我的收藏"
- 个人资料编辑
- 关注/粉丝列表
- 应用设置

**技术实现**:
- Fragment切换
- TabLayout + ViewPager
- 个人资料上传（头像、昵称、简介）

---

### 4.5 通知中心

**功能说明**:
- 点赞、评论、关注通知
- 未读消息提醒
- 通知已读管理

**技术实现**:
- NotificationAdapter
- 未读Badge显示
- 实时通知更新

---

## 5. 网络通信架构

### 5.1 Retrofit配置

**RetrofitClient.kt**:
```kotlin
object RetrofitClient {
    // 使用电脑的内网IP地址，确保手机和电脑在同一WiFi下
    private const val BASE_URL = "http://192.168.x.x:3000/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(TokenInterceptor())
        .addInterceptor(LoggingInterceptor())
        .build()

    val instance: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

### 5.2 Token管理

**TokenManager.kt**:
```kotlin
object TokenManager {
    private const val TOKEN_KEY = "jwt_token"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().remove(TOKEN_KEY).apply()
    }
}
```

### 5.3 API接口定义

**AuthApi.kt**:
```kotlin
interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>
}
```

**PostsApi.kt**:
```kotlin
interface PostsApi {
    @GET("posts/feed")
    suspend fun getFeed(@Query("page") page: Int, @Query("limit") limit: Int): Response<FeedResponse>

    @POST("posts")
    @Multipart
    suspend fun createPost(
        @Part("content") content: RequestBody,
        @Part("mood") mood: RequestBody?,
        @Part images: List<MultipartBody.Part>?
    ): Response<PostResponse>

    @POST("posts/{id}/like")
    suspend fun likePost(@Path("id") postId: String): Response<LikeResponse>

    @POST("posts/{id}/comment")
    suspend fun commentPost(@Path("id") postId: String, @Body request: CommentRequest): Response<CommentResponse>
}
```

---

## 6. Socket.IO实时通信

### 6.1 WebSocket连接

**WhisperWebSocket.kt**:
```kotlin
class WhisperWebSocket private constructor() {
    private var socket: Socket? = null

    fun connect(token: String) {
        val options = IO.Options()
        options.auth = mapOf("token" to "Bearer $token")

        // 使用电脑的内网IP地址，确保手机和电脑在同一WiFi下
        socket = IO.socket("ws://192.168.x.x:3000", options)
        socket?.on(Socket.EVENT_CONNECT, onConnect)
        socket?.on("receive_message", onReceiveMessage)
        socket?.connect()
    }

    fun joinRoom(roomId: String) {
        socket?.emit("join_room", JSONObject().put("roomId", roomId))
    }

    fun sendMessage(roomId: String, content: String) {
        val data = JSONObject()
        data.put("roomId", roomId)
        data.put("content", content)
        socket?.emit("send_message", data)
    }

    private val onReceiveMessage = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        // 处理接收到的消息
    }
}
```

---

## 7. 本地缓存管理

### 7.1 Room Database配置

**Models.kt**:
```kotlin
@Entity(tableName = "cached_posts")
data class CachedPost(
    @PrimaryKey val id: String,
    val content: String,
    val userId: String,
    val createdAt: Long
)

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val mood: String?,
    val createdAt: Long
)

@Dao
interface PostDao {
    @Query("SELECT * FROM cached_posts ORDER BY createdAt DESC")
    suspend fun getAllPosts(): List<CachedPost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CachedPost)

    @Delete
    suspend fun deletePost(post: CachedPost)
}
```

---

## 8. UI组件与动画

### 8.1 自定义组件

**AvatarHaloView.kt**: 头像光环效果
**CyberpunkBackgroundView.kt**: 赛博朋克背景
**HugAnimationView.kt**: 拥抱动画效果
**ParticleShatterView.kt**: 粒子破碎效果
**RadarView.kt**: 雷达扫描效果
**SplashParticleView.kt**: 启动页粒子效果

### 8.2 Material Design 3

- 使用MD3组件（MaterialButton、MaterialCardView）
- 动态颜色主题
- 圆角卡片设计
- 深色模式支持

---

## 9. 性能优化

### 9.1 图片加载优化

- Glide图片缓存
- 图片预加载（PostPreloadService）
- 图片压缩（ImageUtils）

### 9.2 网络优化

- 网络状态监听（NetworkMonitor）
- 网络Toast提示（NetworkToastManager）
- Retrofit缓存策略

### 9.3 内存优化

- RecyclerView优化（ViewHolder复用）
- 图片内存管理
- 协程生命周期管理

---

## 10. 安全与权限

### 10.1 权限管理

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 10.2 数据安全

- Token加密存储（SharedPreferences加密）
- HTTPS通信（Certificate Pinning）
- 日志输出控制（Release版本关闭Debug日志）
- ProGuard/R8混淆（建议）

---

## 11. 测试与调试

### 11.1 单元测试

- ViewModel测试
- Repository测试
- API接口测试

### 11.2 UI测试

- Espresso界面测试
- Fragment导航测试
- RecyclerView滚动测试

---

## 12. 构建与部署

### 12.1 构建配置

**build.gradle.kts**:
```kotlin
android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zjgsu.treehole"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### 12.2 部署流程

1. 编译Release版本APK
2. 签名打包
3. 上传到应用商店或分发平台

---

## 13. 后续优化方向

- 引入Jetpack Compose现代化UI
- 完善离线模式支持
- 增加视频发布功能
- 实现消息加密传输
- 优化启动速度与内存占用
- 增加多语言支持
- 实现深色模式动态切换