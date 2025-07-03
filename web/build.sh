#!/bin/bash
source ~/.nvm/nvm.sh
nvm use 20

pnpm -g add pnpm@10

# pnpm install
pnpm install --verbose

# 接收执行build.sh带的参数如 build.sh --mode staging
pnpm build "$@"