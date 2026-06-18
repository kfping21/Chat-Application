# Docker 部署贡献说明
姓名：panjiawei
学号：2312190633
日期：2026-05-13

## 我完成的工作
### 1. Dockerfile 编写
- [x] 后端 Dockerfile（多阶段构建）
- [x] .dockerignore 文件

### 2. Compose 配置
- [x] 开发环境 compose.yaml
- [x] 生产环境 compose.prod.yaml
- [x] 健康检查配置

### 3. 自动化部署
- 选择了选项 B：本地部署脚本
- 完成了 deploy.sh 部署脚本

## 遇到的问题和解决
1. 问题：MongoDB 连接使用远程地址，在 Docker 环境中需要改用容器服务名
   解决：使用 `db` 作为 MongoDB 容器的主机名，与 docker-compose 中的服务名一致

2. 问题：后端配置使用环境变量 PORT，但 Dockerfile 中需要指定默认端口
   解决：在 Dockerfile 中设置 ENV PORT=3001，并在 docker-compose 中使用 - PORT=3001 映射

3. 问题：非 root 用户运行容器
   解决：使用 adduser 创建 appuser 用户，并在 Dockerfile 末尾使用 USER appuser

4. 问题：健康检查无法正常执行
   解决：后端 /health 端点返回 JSON，mongosh 健康检查需要使用正确的测试命令

## AI 使用情况
- 使用了哪些 Prompt：
  1. "我有一个 Node.js Express 后端项目，使用 MongoDB 数据库，请帮我编写多阶段构建的 Dockerfile"
  2. "如何在 Docker Compose 中配置 MongoDB 的健康检查"
  3. "如何让 Docker 容器以非 root 用户运行"
  4. "如何在 Docker Compose 中配置开发环境热重载"

- AI 帮助解决了哪些问题：
  1. 多阶段 Dockerfile 的结构设计和依赖复制策略
  2. MongoDB 容器健康检查的正确命令
  3. Alpine 镜像创建用户的方法
  4. Docker Compose 服务依赖和健康检查配置

## 心得体会
通过本次 Docker 部署任务，我学习了：
1. 多阶段构建可以有效减小镜像体积
2. 使用非 root 用户运行容器是重要的安全最佳实践
3. Docker Compose 的健康检查机制确保服务依赖正确启动
4. 环境变量分离开发/生产配置，便于管理
5. .dockerignore 文件可以排除不必要的文件，减小构建上下文