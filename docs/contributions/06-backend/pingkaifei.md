# 后端开发贡献说明

姓名：平恺飞
学号：2312190616
日期：2026-04-14

## 我完成的工作

### API 实现
- [x] 用户认证 API（注册 / 登录 / 登出）
- [x] 业务资源1 CRUD：帖子（创建 / 查询列表与详情 / 修改 / 删除）
- [x] 业务资源2 接口：互动能力（评论、点赞/取消点赞、通知、私信）
- [x] 统一响应与错误处理（`{ code, message, data }`）

### 数据库
- [x] 数据模型定义（`backend/sql/init.sql`，含 users/posts/comments/likes/messages 等表）
- [ ] ORM 配置（未采用 ORM，当前使用 `mysql2` + SQL）
- [x] 数据库初始化脚本（`backend/sql/init.sql`）

### 部署
- [ ] Dockerfile 编写
- [ ] docker-compose.yml 配置
- [x] 本地联调验证（`.env` + MySQL 初始化后可正常启动并通过测试接口）

## PR 链接
- PR #X: https://github.com/kfping21/Chat-Application/pull/7

## 遇到的问题和解决
1. 问题：数据库连接失败（`Access denied ... using password: NO`）。  
   解决：补充并校验 `backend/.env` 配置，启动入口按约定读取环境变量。

2. 问题：接口处理出现 `ERR_HTTP_HEADERS_SENT`。  
   解决：统一路由响应出口，避免重复写入响应头。

3. 问题：端口被占用导致服务无法启动（`EADDRINUSE`）。  
   解决：释放占用进程或切换端口后重启服务。

## 心得体会
这次后端实现让我更清楚“接口契约一致性”的重要性：路由、响应结构、数据库结构和测试验证必须保持一致，联调效率才会高。后续我会补齐容器化部署（Dockerfile、docker-compose）和更完整的自动化测试，进一步提升可交付性。
