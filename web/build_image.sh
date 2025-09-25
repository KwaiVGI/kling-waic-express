#!/bin/sh

docker build -t kling-express-web .

docker login
docker tag kling-express-web:latest akang943578/kling-express-web:latest
docker push akang943578/kling-express-web:latest
