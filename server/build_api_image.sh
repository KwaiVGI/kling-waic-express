#!/bin/sh

# 创建并使用 buildx builder（如果不存在）
docker buildx create --name multiarch --use 2>/dev/null || docker buildx use multiarch

# 构建并推送多架构镜像
docker login
docker buildx build --platform linux/amd64,linux/arm64 -f api/Dockerfile -t akang943578/kling-express-api:latest --push .
