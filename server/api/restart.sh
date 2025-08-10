#!/bin/bash

set -e

PORT=8538
LOG_FILE="output.log"
APP_LOG="logs/main.log"

echo "[1/5] Killing process on port $PORT if exists..."
PID=$(lsof -i:$PORT | awk 'NR==2 {print $2}')
if [ -n "$PID" ]; then
    echo "Killing PID $PID"
    kill -9 $PID
else
    echo "No process found on port $PORT"
fi

echo "[2/5] Pulling latest code..."
git pull

echo "[3/5] Cleaning and compiling the project..."
mvn clean compile

echo "[4/5] Starting Spring Boot app in background..."
nohup mvn spring-boot:run > $LOG_FILE 2>&1 &

sleep 2
echo "[5/5] Tailing application log..."
tail -f $APP_LOG
