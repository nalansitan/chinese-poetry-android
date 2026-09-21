# 古诗词数据服务端

FastAPI 实现的诗词数据 API 服务，支持数据下载和增量更新。

## 安装

```bash
# 安装 Poetry 依赖
poetry install
```

## 数据准备

### 1. 克隆诗词数据源

```bash
git clone https://github.com/chinese-poetry/chinese-poetry.git
```

### 2. 转换数据为 SQLite

```bash
# 方式一：直接运行
poetry run python data_converter.py ../chinese-poetry ./poetry.db

# 方式二：使用 Poetry script
poetry run convert ../chinese-poetry ./poetry.db
```

生成的 `poetry.db` 包含：

| 数据集 | 来源目录 | 朝代 | 类型 | 说明 |
|---|---|---|---|---|
| 全唐诗 | `全唐诗/poet.tang.*.json` | 唐 | tang_shi | 繁体，约 5.5 万首 |
| 全宋诗 | `全唐诗/poet.song.*.json` | 宋 | song_shi | 繁体，约 26 万首 |
| 全宋词 | `宋词/ci.song.*.json` | 宋 | song_ci | 简体，约 2.1 万首 |
| 诗经 | `诗经/shijing.json` | 先秦 | shi_jing | 简体，311 篇 |
| 论语 | `论语/lunyu.json` | 先秦 | lun_yu | 繁体，20 篇 |
| 楚辞 | `楚辞/chuci.json` | 先秦 | chu_ci | 简体 |
| 元曲 | `元曲/yuanqu.json` | 元 | yuan_qu | |
| 纳兰性德 | `纳兰性德/纳兰性德诗集.json` | 清 | nalan_ci | 269 首 |
| 五代·花间集 | `五代诗词/huajianji/*.json` | 五代 | hua_jian_ji | |
| 五代·南唐 | `五代诗词/nantang/poetrys.json` | 五代 | nan_tang | |
| 曹操诗集 | `曹操诗集/caocao.json` | 三国 | cao_cao | 26 首 |
| 幽梦影 | `幽梦影/youmengying.json` | 清 | you_meng_ying | 张潮文集 |
| 四书五经 | `四书五经/*.json` | 先秦 | si_shu | 孟子、大学、中庸 |
| 作者信息 | 各目录 authors JSON | - | - | 唐/宋/五代词人 |

数据源：[chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)

## 文本规范化

转换脚本会在入库前自动将标题、作者、正文、注释、简介等文本统一转换为简体中文。
这样客户端可以只面向简体数据开发，无需再额外引入简繁转换逻辑。

## 启动服务

### 开发模式

```bash
# 方式一
poetry run uvicorn poetry_data_server:app --host 0.0.0.0 --port 8000

# 方式二（热重载）
poetry run uvicorn poetry_data_server:app --reload
```

### Docker 部署

```bash
docker-compose up -d
```

服务启动后访问：
- API 文档：http://localhost:8000/docs
- 健康检查：http://localhost:8000/health
- APK 下载页：http://localhost:8000/public/apk
- APK 版本信息：http://localhost:8000/public/apk/version

### 生产部署

使用 Nginx + Uvicorn：

```nginx
server {
    listen 443 ssl;
    server_name poetry.yourdomain.com;
    
    # SSL 配置...
    
    location /api/ {
        # Basic Auth
        auth_basic "Poetry API";
        auth_basic_user_file /etc/nginx/.htpasswd;
        
        proxy_pass http://localhost:8000/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /public/apk/ {
        auth_basic off;
        proxy_pass http://localhost:8000/public/apk/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

生成密码文件：
```bash
htpasswd -c /etc/nginx/.htpasswd poetry_user
```

## API 接口

### 1. 下载完整数据库

```bash
GET /api/v1/poetry/database
```

用于客户端首次安装时初始化数据。

### 2. 检查版本

```bash
GET /api/v1/poetry/version
```

返回：
```json
{
  "version": 1,
  "versionName": "1.0.0",
  "updateTime": "2024-01-01T00:00:00",
  "totalPoems": 300000,
  "changelog": "初始版本",
  "isForceUpdate": false
}
```

### 3. 获取增量更新

```bash
GET /api/v1/poetry/update?version=1
```

返回：
```json
{
  "hasUpdate": true,
  "newVersion": 2,
  "updateType": "incremental",
  "poems": [...],
  "authors": [...],
  "deleteIds": [],
  "changelog": "新增诗词点评"
}
```

### 4. 获取扩展数据

```bash
GET /api/v1/poetry/extension?poem_id=xxx
```

返回诗词的评论、赏析、注释、译文等扩展信息。

### 5. 获取最新 APK 版本

```bash
GET /public/apk/version
```

返回：
```json
{
  "versionCode": 1,
  "versionName": "1.0.0",
  "downloadUrl": "/public/apk/download?version=1",
  "changelog": "初始版本",
  "forceUpdate": false,
  "fileSize": 12345678,
  "releaseTime": "2026-04-10T00:00:00"
}
```

### 6. APK 下载页

```bash
GET /public/apk
```

返回一个公开的下载介绍页，展示版本信息、更新说明和“下载最新版 APK”按钮。

### 7. 下载 APK

```bash
GET /public/apk/download?version=1
```

不传 `version` 时默认下载当前最新版本。

## 数据更新

修改 `poetry_data_server.py` 中的配置：

```python
CURRENT_VERSION = 2  # 提升版本号

def get_update_data(client_version: int):
    if client_version >= CURRENT_VERSION:
        return {"hasUpdate": False, "newVersion": CURRENT_VERSION}
    
    return {
        "hasUpdate": True,
        "newVersion": CURRENT_VERSION,
        "updateType": "incremental",  # full 为保留值，当前客户端会明确拒绝
        "poems": [
            # 新增/修改的诗词（含评论、赏析等）
        ],
        "changelog": "更新说明"
    }
```

## 生成静态文件（CDN 部署）

```bash
poetry run generate
```

生成 `api/` 和 `extensions/` 目录，可上传到 OSS/COS + CDN。

## 配置说明

### 数据目录

```python
DATA_DIR = "./chinese-poetry"  # 诗词 JSON 数据源
DB_FILE = "./poetry.db"        # SQLite 数据库文件
```

### 版本配置

```python
CURRENT_VERSION = 1            # 当前数据版本
VERSION_NAME = "1.0.0"         # 版本名称
```

### APK 发布配置

真实发布配置从 `server/config/apk_releases.json` 读取，该文件已加入 Git 忽略。
仓库内提供示例文件 `server/config/apk_releases.example.json`，可复制后修改：

```json
{
  "1": {
    "versionName": "1.0.0",
    "filename": "chinese-poetry-android-v1.0.0.apk",
    "changelog": "初始版本",
    "forceUpdate": false,
    "downloadable": true,
    "releaseTime": "2026-04-10T00:00:00"
  }
}
```

使用方式：

1. 复制示例文件：

```bash
cp server/config/apk_releases.example.json server/config/apk_releases.json
```

2. 将已签名 APK 放到 `server/public/apk/` 目录下。

3. 在 `server/config/apk_releases.json` 中登记版本号、文件名和更新说明。
如果希望某个旧版本不再允许下载，但仍保留版本记录，可将 `"downloadable"` 设为 `false`。

服务端会在请求时动态读取该文件，因此后续发版无需再改 Python 代码。

## 访问日志

服务端会将访问日志持久化到：

`server/logs/access.log`

日志会按天轮转，默认保留最近 7 天，不记录查询字符串。可通过 `POETRY_ACCESS_LOG_RETENTION_DAYS` 调整保留时间。

默认只记录直接连接 IP。确认服务仅能通过受信任反向代理访问后，可以设置 `POETRY_TRUST_PROXY_HEADERS=true`，再读取：

- `X-Forwarded-For`
- `X-Real-IP`

反向代理配置示例：

```nginx
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

## 文件说明

| 文件 | 说明 |
|---|---|
| `poetry_data_server.py` | FastAPI 服务主文件 |
| `data_converter.py` | 数据转换工具（JSON → SQLite） |
| `pyproject.toml` | Poetry 依赖配置 |
| `docker-compose.yml` | Docker Compose 部署配置 |
| `start.sh` | 容器启动脚本 |
| `poetry.db` | 生成的 SQLite 数据库（Git 忽略） |
| `public/apk/` | 对外分发的 APK 文件目录（Git 忽略） |
| `config/apk_releases.json` | APK 发布配置（Git 忽略） |
| `logs/access.log` | 服务端访问日志（Git 忽略） |
