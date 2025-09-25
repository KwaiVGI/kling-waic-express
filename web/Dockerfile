# 构建阶段
FROM node:20-alpine AS builder
WORKDIR /app

# 设置环境变量避免构建时的问题
ENV NODE_OPTIONS="--max-old-space-size=4096"
ENV VITE_BUILD_TARGET="production"

COPY package.json pnpm-lock.yaml ./
RUN npm install -g pnpm
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

# 生产阶段
FROM nginx:alpine

# 安装 envsubst（在 alpine 里属于 gettext 包）
RUN apk add --no-cache gettext

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 添加 entrypoint 脚本
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/entrypoint.sh"]
