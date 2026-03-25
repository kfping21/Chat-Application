# API 设计与实现贡献说明

姓名: 潘嘉伟
学号: 2312190633
日期: 2026-03-25

## 我完成的工作

### 1. API 设计
- [x] 用户认证 API
- [x] 业务资源 API
- [x] 查询接口设计

### 2. 文档编写
- [x] OpenAPI 文档
- [ ] API 使用说明

### 3. 前端实现
- [x] HTTP 客户端配置
- [x] API 调用函数封装
- [x] Mock 数据配置

### 4. 后端实现
- [ ] API 路由定义
- [ ] 业务逻辑处理
- [ ] 错误处理

### 5. 测试
- [ ] Postman/Apifox 测试集合
- [ ] 后端单元测试
- [ ] 测试用例数量: 0 个

## PR 链接

- PR #1: [kfping21/Chat-Application](https://github.com/kfping21/Chat-Application)

## 遇到的问题和解决

1. 问题: 在 Android 模拟器中测试网络请求时，直接访问本地电脑的 `http://localhost:3001` 失败，无法获取 JSON Server 的 Mock 数据。
   解决: 查阅资料后发现 Android 模拟器有独立的虚拟网络，将 Retrofit 的基础 URL 修改为安卓专属的 `http://10.0.2.2:3001`，并在 `AndroidManifest.xml` 中配置了 `android:usesCleartextTraffic="true"` 允许 HTTP 明文请求，解决了本地联调的地址问题。

2. 问题: 运行网络请求测试代码时，Logcat 报错 `网络异常: socket failed: EPERM (Operation not permitted)`，导致请求直接被操作系统拦截。
   解决: 这是因为 Android 应用默认处于沙盒环境中，没有访问网络的权限。在 `AndroidManifest.xml` 文件中（`<application>` 标签外）添加了 `<uses-permission android:name="android.permission.INTERNET" />` 权限声明后，成功允许应用建立网络连接。

## 心得体会

​	作为团队的一员，这次统筹完善 OpenAPI 规范并率先在 Android 端实现 API 访问层，让我深刻体会到契约驱动开发的重要性。一份标准、清晰的 API 文档能极大地降低团队内开发的沟通成本。这次通过 JSON Server 与 Kotlin 协程结合 Retrofit 的快速联调，解决了一系列如网络安全策略限制、Socket 权限缺失等实际开发中的“拦路虎”，不仅验证了接口设计的合理性，也为我们树洞应用后续核心交互的开发打下了扎实的基础。