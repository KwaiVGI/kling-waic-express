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

echo "[3/5] Building component module..."
cd ..
mvn clean install -pl component -am
echo "Component module built successfully!"

echo "[4/5] Starting Spring Boot app using Maven..."
cd api
# Create logs directory if it doesn't exist
mkdir -p logs

# Start the application using Maven
nohup mvn spring-boot:run > $LOG_FILE 2>&1 &

sleep 3
echo "[5/5] Tailing application log..."
if [ -f "$APP_LOG" ]; then
    tail -f $APP_LOG
else
    echo "Application log not found at $APP_LOG, showing output log instead:"
    tail -f $LOG_FILE
fi
