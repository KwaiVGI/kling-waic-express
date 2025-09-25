#!/bin/bash

APP_NAME="KlingExpressPrinter"
APP_DIR="${APP_NAME}.app"
CONTENTS_DIR="${APP_DIR}/Contents"
MACOS_DIR="${CONTENTS_DIR}/MacOS"
RESOURCES_DIR="${CONTENTS_DIR}/Resources"
JAVA_DIR="${RESOURCES_DIR}/java"

echo "🔨 开始创建内置 Java 的 macOS 应用程序..."

# 清理旧的应用
rm -rf "${APP_DIR}"

# 创建应用目录结构
mkdir -p "${MACOS_DIR}"
mkdir -p "${RESOURCES_DIR}"
mkdir -p "${JAVA_DIR}"

echo "📦 构建 JAR 文件..."
export JAVA_HOME="$JAVA_17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw clean package -DskipTests

# 复制 JAR 文件
cp target/printer-0.0.1-SNAPSHOT.jar "${RESOURCES_DIR}/app.jar"

# 复制应用图标
if [ -f "klingai-printer-logo.icns" ]; then
    cp klingai-printer-logo.icns "${RESOURCES_DIR}/"
    echo "📎 复制应用图标..."
fi

echo "☕ 复制 Java 17 运行时..."
cp -R "$JAVA_17_HOME"/* "${JAVA_DIR}/"

# 创建启动脚本
cat > "${MACOS_DIR}/${APP_NAME}" << 'EOF'
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RESOURCES_DIR="${DIR}/../Resources"
JAVA_HOME="${RESOURCES_DIR}/java"
JAVA_BIN="${JAVA_HOME}/bin/java"

# 检查内置 Java 是否存在
if [ ! -f "$JAVA_BIN" ]; then
    osascript -e 'display dialog "应用程序包损坏，请重新下载。" buttons {"确定"} default button "确定" with icon caution'
    exit 1
fi

# 显示配置弹窗
CONFIG=$(osascript << 'APPLESCRIPT'
set defaultConfig to "API_SERVER_BASE_URL=https://waic-api.klingai.com
WAIC_MANAGEMENT_ACTIVITY=
WAIC_MANAGEMENT_TOKEN=
PRINTER_PRINTING_MODE=PDF_BATCH
PRINTER_EXTRA_SCALE_FACTOR=1.00
DRAW_IMAGE_X=5.0
DRAW_IMAGE_Y=5.0"

set userConfig to text returned of (display dialog "请配置应用参数:" default answer defaultConfig with title "应用配置" buttons {"取消", "启动"} default button "启动")

return userConfig
APPLESCRIPT
)

# 检查用户是否取消
if [ $? -ne 0 ]; then
    exit 0
fi

# 解析配置并构建 JVM 参数
JVM_ARGS=""
while IFS='=' read -r key value; do
    if [ -n "$key" ] && [ -n "$value" ]; then
        JVM_ARGS="$JVM_ARGS -D${key}=${value}"
    fi
done <<< "$CONFIG"

# 去掉前后多余空格
JVM_ARGS=$(echo "$JVM_ARGS" | xargs)

# 拼接完整命令
RUN_CMD="cd '$RESOURCES_DIR' && '$JAVA_BIN' $JVM_ARGS -jar app.jar"

# 转义双引号，确保 AppleScript 不换行
RUN_CMD_ESCAPED=$(printf '%s' "$RUN_CMD" | sed 's/"/\\"/g')

# 调试输出（可选）
echo "👉 最终命令: $RUN_CMD_ESCAPED"

# 在新终端窗口中启动应用
osascript <<APPLESCRIPT
tell application "Terminal"
    activate
    do script "$RUN_CMD_ESCAPED"
end tell
APPLESCRIPT
EOF

# 使启动脚本可执行
chmod +x "${MACOS_DIR}/${APP_NAME}"

# 创建 Info.plist
cat > "${CONTENTS_DIR}/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIdentifier</key>
    <string>com.kling.printer</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundleVersion</key>
    <string>1.0</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleIconFile</key>
    <string>klingai-printer-logo</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.9</string>
</dict>
</plist>
EOF

echo "✅ 内置 Java 的应用程序创建完成: ${APP_DIR}"
echo "📦 应用大小: $(du -sh ${APP_DIR} | cut -f1)"
echo "🚀 现在可以在任何 macOS 机器上直接运行！"
