# 监控配置说明

## 概述

本文档说明 TreeHole 后端的监控配置，包括日志管理、健康检查、指标收集等功能。

## 日志管理

### 配置

使用 Winston 进行结构化日志管理：

- **日志文件**: `logs/error.log` (错误日志), `logs/combined.log` (所有日志)
- **日志级别**: `info` (默认), 可通过 `LOG_LEVEL` 环境变量配置
- **日志格式**: JSON 格式，包含时间戳、级别、消息、模块等信息

### 使用方式

```javascript
const logger = require('./utils/logger');

logger.info('请求收到', { method: 'GET', url: '/api/posts' });
logger.error('数据库错误', { error: err.message });
```

## 健康检查端点

### GET /health

返回服务健康状态：

```json
{
  "status": "healthy",
  "timestamp": "2026-06-01T00:00:00.000Z",
  "version": "1.0.0",
  "uptime": "3600s",
  "memory": {
    "rss": "120MB",
    "heapUsed": "80MB"
  }
}
```

## 指标收集

### GET /metrics

返回应用指标：

```json
{
  "requestCount": 1000,
  "errorCount": 5,
  "errorRate": "0.50%",
  "avgResponseTime": "45.32ms",
  "activeUsers": 25,
  "endpoints": {
    "GET:/api/posts": {
      "count": 200,
      "avgTime": "32.15ms",
      "errorRate": "0.00%"
    }
  }
}
```

### 收集的指标

- **请求计数**: 记录每个请求
- **响应时间**: 记录每个请求的响应时间
- **错误率**: 基于 4xx/5xx 状态码统计
- **活跃用户数**: 基于 WebSocket 连接统计

## 告警配置 (可选)

### 基础告警规则

1. **服务不可用**: `/health` 端点返回非 200 状态
2. **错误率超阈值**: 错误率超过 5% 时触发
3. **响应时间过长**: 平均响应时间超过 1 秒

### Sentry 集成 (可选)

```javascript
const sentrySdk = require('@sentry/node');
sentrySdk.init({ dsn: 'your-dsn' });
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| LOG_LEVEL | 日志级别 | info |
| PORT | 服务端口 | 3001 |