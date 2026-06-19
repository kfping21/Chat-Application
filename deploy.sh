#!/bin/bash
set -e
echo "开始部署..."
docker compose -f compose.prod.yaml up -d --build
echo "等待服务就绪..."
sleep 15
docker compose -f compose.prod.yaml ps
echo "部署完成"