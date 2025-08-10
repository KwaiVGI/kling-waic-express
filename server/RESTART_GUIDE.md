# Restart Scripts Guide

## 项目结构说明

经过多模块重构后，项目现在有以下结构：
```
server/
├── pom.xml (父POM，packaging=pom)
├── component/ (组件模块)
├── api/ (API模块，主应用)
├── printer/ (打印模块)
└── restart.sh (主重启脚本)
```

## 重启脚本位置

### 1. 主重启脚本
**位置**: `/server/restart.sh`
**用途**: 从server目录启动整个应用
**使用方法**:
```bash
cd /path/to/server
./restart-api.sh
```

### 2. API模块重启脚本
**位置**: `/server/api/restart.sh`
**用途**: 从api目录直接启动API模块
**使用方法**:
```bash
cd /path/to/server/api
./restart-api.sh
```

## 脚本功能

两个脚本都会执行以下操作：
1. 检查并终止占用8538端口的进程
2. 拉取最新代码 (`git pull`)
3. 使用Maven启动Spring Boot应用
4. 显示应用日志

## 日志文件

- **应用日志**: `server/logs/main.log`
- **启动输出**: `server/output.log` 或 `server/api/output.log`

## 注意事项

### 编译问题
当前项目存在一些编译错误，主要是因为：
1. API模块依赖component模块，但import路径可能需要调整
2. 多模块项目的依赖关系需要正确配置

### 临时解决方案
如果遇到编译错误，可以：
1. 使用 `mvn spring-boot:run` 直接启动（跳过编译检查）
2. 或者先解决依赖问题再使用JAR包启动

### 推荐使用方式
```bash
# 推荐：从server目录使用主脚本
cd /path/to/server
./restart-api.sh

# 或者：直接在api目录使用
cd /path/to/server/api
./restart-api.sh
```

## 测试脚本
运行测试脚本验证环境：
```bash
cd /path/to/server
./test-restart-api.sh
```

## 故障排除

### 端口被占用
如果8538端口被其他进程占用：
```bash
lsof -i:8538
kill -9 <PID>
```

### 权限问题
确保脚本有执行权限：
```bash
chmod +x restart-api.sh
chmod +x api/restart-api.sh
```

### Maven问题
如果Maven命令失败，检查：
1. Maven是否正确安装
2. JAVA_HOME是否设置
3. 项目依赖是否正确
