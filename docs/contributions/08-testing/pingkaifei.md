# 软件测试贡献说明

姓名：平恺飞  学号：2312190616  角色：后端  日期：2026-04-28

## 完成的测试工作

### 测试文件
- `backend/tests/unit.authStore.test.js`
- `backend/tests/unit.responseUtils.test.js`
- `backend/tests/api.test.js`
- `backend/tests/api.extra.test.js`

### 测试清单
- [x] 正常情况测试（若干）
- [x] 边界 / 异常情况测试（若干）
- [x] Mock 使用（数据库 / `commonStore` / API 依赖）

### 覆盖率
- 核心模块覆盖率：请见 `backend/coverage` 报告（authStore / responseUtils），CI 已产出 `cobertura-coverage.xml` 供 Codecov 使用

### AI 辅助（如有）
- 使用工具：GitHub Copilot
- Prompt 示例：
	- “请按 Node.js node:test 风格，为 authStore 写不少于 8 个单元测试，并通过 Mock 隔离 commonStore 的 query/queryOne，覆盖缺表/缺字段兜底逻辑。”
	- “请为 responseUtils 写工具函数单测，覆盖统一响应格式和异常转换逻辑。”
- AI 生成 + 人工修改的测试数量：若干（具体数量请由提交者补充）

## PR 链接
- PR #X: https://github.com/xxx/xxx/pull/X （如有请填写）

## 遇到的问题和解决
1. 问题：Mock 注入与模块缓存冲突导致测试间相互影响 → 解决：采用在测试用例中临时替换 `commonStore` 的导出并在 afterEach 恢复，避免污染全局模块缓存

## 心得体会
- 使用 Mock 能有效分离数据库依赖，加快单元测试速度；AI 工具（Copilot）能生成初版测试用例，但需人工审校断言与异常分支，保障覆盖质量。

---

（注：如需我将 `核心模块覆盖率` 和 `AI 生成的测试数量` 填入具体数值，请告知我可从 CI 报告或测试运行输出中提取并替换占位符。）
