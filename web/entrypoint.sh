#!/bin/sh

# 用环境变量替换 config.js 里的占位符
envsubst < /usr/share/nginx/html/config.js > /usr/share/nginx/html/config.js.tmp
mv /usr/share/nginx/html/config.js.tmp /usr/share/nginx/html/config.js

# 启动 nginx
exec nginx -g "daemon off;"
