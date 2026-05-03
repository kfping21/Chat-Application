# CI/CD 配置贡献说明

姓名：潘嘉伟 学号：2312190633 角色：前端 日期：2026-05-03

## 完成的工作

### 工作流相关

- [x] 创建 `.github/workflows/ci.yml` 工作流文件
- [x] 配置 Android 项目的 CI 测试流程（修复路径问题，指向 `frontend/前端代码/`）
- [x] 配置 Gradle 缓存加快 CI 构建速度
- [x] 添加 APK 构建产物上传
- [x] 添加 README 状态徽章

### 代码适配

- [x] Android 项目结构适配 CI 流程
- [x] 添加 Java 17 和 Gradle 缓存配置
- [x] 调整 frontend job 的工作目录为 `./frontend/前端代码/`

## PR 链接

- PR: https://github.com/kfping21/Chat-Application/pull/X

## CI 运行链接

- https://github.com/kfping21/Chat-Application/actions/runs/XXX

## 遇到的问题和解决

1. **问题**：Android 项目需要 JDK 17 和 Gradle，CI 默认没有
   - **解决**：使用 `actions/setup-java@v4` 配置 temurin JDK 17，并配置 Gradle 缓存

2. **问题**：Android 项目的 lint 和 test 任务需要先完成 assembleDebug
   - **解决**：调整任务顺序，确保依赖任务在前面执行

3. **问题**：CI 配置中的工作目录错误，指向了不存在的 `./app` 目录
   - **解决**：修正工作目录为 `./frontend/前端代码/`，匹配实际项目结构

## 心得体会

通过本次 CI/CD 配置，我学习到了：
- GitHub Actions 的基本配置和使用
- Android 项目的 CI 构建流程
- 如何配置 Gradle 缓存加速 CI
- Codecov 覆盖率报告的集成方法
- README 状态徽章的添加方式
- 如何根据实际项目结构调整 CI 配置路径