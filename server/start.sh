#!/bin/sh
# 古诗词数据服务启动脚本

echo "Starting Poetry Data Server..."

# 启动 FastAPI 服务
echo "Starting FastAPI server on port 8000..."
poetry run uvicorn poetry_data_server:app --host 0.0.0.0 --port 8000
