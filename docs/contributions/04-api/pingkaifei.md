# 04-API 个人贡献说明（pingkaifei）

## 本次提交内容

- 设计并编写 OpenAPI 规范文档：`docs/api.yaml`
- 编写 API 使用说明：`docs/api.md`
- 按规范路径实现后端 API 路由：`backend/app/routes/`
  - `assignmentRoutes.js`
  - `authRoutes.js`
  - `todoRoutes.js`
  - `utils/responseUtils.js`
  - `store/assignmentStore.js`
- 后端应用入口与数据库层迁移到 `backend/app/`
  - `backend/app/index.js`
  - `backend/app/db.js`

## 说明

- 作业新增接口统一采用响应结构：`{ code, message, data }`
- 覆盖认证接口（注册、登录、登出）与 Todo 资源 CRUD（含分页与筛选）
- 保留项目历史 `/api/v1/*` 接口兼容，不影响原有功能调用
