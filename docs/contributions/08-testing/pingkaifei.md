# 后端测试贡献说明

姓名：平恺飞  
学号：2312190616  
日期：2026-04-28

## 任务范围

- 后端单元测试（业务逻辑 + Mock）
- 后端 API 接口测试（正常路径 + 参数校验 + 异常路径）
- 覆盖率脚本与 Codecov 上传文件准备

## 本次完成内容

### 1) 单元测试（含 Mock）

新增文件：`backend/tests/unit.authStore.test.js`

覆盖核心逻辑：
- createUser：已存在用户分支
- createUser：主插入路径
- createUser：缺字段兜底插入路径
- getUserByUsername：用户映射
- createSession：缺表兜底写 notifications
- getSession：主查询路径
- getSession：缺表兜底查询 notifications
- deleteSession：缺表兜底更新 notifications

Mock 方式：
- 通过替换 `commonStore` 模块导出的 `query/queryOne`，隔离真实数据库连接。

新增文件：`backend/tests/unit.responseUtils.test.js`

覆盖工具函数：
- sendApiJson 统一响应封装
- createApiError 错误对象结构
- parseApiBody 成功与异常分支

### 2) API 接口测试

已存在并复用：
- `backend/tests/api.test.js`
- `backend/tests/api.extra.test.js`

覆盖场景包括：
- 注册/登录/登出
- 帖子创建/列表/详情/更新/删除
- 评论与点赞
- 通知与消息
- 异常路径（无 token、无效 token、参数非法、越权修改/删除）

### 3) 覆盖率体系

更新：`backend/package.json`

- `npm run test:coverage`：生成核心模块（authStore/responseUtils）覆盖率 + lcov + cobertura
- `npm run test:coverage:ci`：在上一步基础上额外复制 `coverage/cobertura-coverage.xml` 到 `backend/coverage.xml`

新增：`.github/workflows/backend-test.yml`

- 后端 CI 自动执行 `npm test` 与 `npm run test:coverage:ci`
- 自动上传 `backend/coverage.xml` 到 Codecov（`flags: backend`）

更新：`README.md`

- 新增后端 Codecov 徽章（flag=backend）

## AI 辅助测试记录（加分项）

使用工具：GitHub Copilot（GPT-5.3-Codex）

使用 Prompt（核心）：

1. “请按 Node.js node:test 风格，为 authStore 写不少于 8 个单元测试，并通过 Mock 隔离 commonStore 的 query/queryOne，覆盖缺表/缺字段兜底逻辑。”
2. “请为 responseUtils 写工具函数单测，覆盖统一响应格式和异常转换逻辑。”
3. “请补充 package.json 覆盖率脚本，产出 cobertura 文件，便于 Codecov 上传。”

人工修改与校验：

- 调整了 Mock 注入方式，避免影响其他测试文件的模块缓存。
- 补充断言：SQL 分支路径命中与参数值校验。
- 增加 `test:coverage:ci` 脚本，确保 CI 可直接读取 `backend/coverage.xml`。

## 运行命令

```bash
cd backend
npm install
npm test
npm run test:coverage
```
