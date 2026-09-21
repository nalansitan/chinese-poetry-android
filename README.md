# 古诗词

一款开源的 Android 离线古诗词阅读应用。项目基于
[chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)
开源古诗词数据，将原始数据格式化并转换为本地 SQLite 数据库。应用只需在首次启动时
联网完成诗词数据库初始化，之后即可长期离线阅读、搜索和收藏古诗词。

An open-source offline Chinese poetry reader for Android. It formats data from
[chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)
and stores it in a local SQLite database. An internet connection is required only for the
initial database setup; afterward, poems can be read, searched, and saved offline.

## 项目结构

```
.
├── app/                    # Android 应用（Kotlin + Jetpack Compose）
├── server/                 # 数据服务端（Python + FastAPI）
│   ├── poetry_data_server.py   # API 服务
│   ├── data_converter.py       # 数据转换工具
│   └── pyproject.toml
└── docs/                   # 架构、数据与开发文档
```

详细开发文档见 [docs/README.md](docs/README.md)，后续维护计划见 [TODO.md](TODO.md)。

## 技术栈

### Android 端
- **语言**: Kotlin
- **UI**: Jetpack Compose
- **架构**: MVVM + Clean Architecture
- **数据库**: Room
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp

### 服务端
- **语言**: Python 3.9+
- **框架**: FastAPI
- **依赖管理**: Poetry
- **部署**: Nginx + Uvicorn

## 快速开始

### 1. 准备数据 & 启动服务端

详见 [server/README.md](server/README.md)

```bash
cd server

# 安装依赖
poetry install

# 生成数据库（需要提前克隆 chinese-poetry）
poetry run convert ../chinese-poetry ./poetry.db

# 启动服务
poetry run serve
```

### 2. 运行 Android 应用

使用 Android Studio 打开项目，同步 Gradle 后运行。命令行构建可复制 `local.properties.example`，或设置标准的 `ANDROID_HOME` 环境变量；项目不要求提交 `local.properties`。

### 3. Android 正式包签名与打包

项目已支持通过根目录的 `keystore.properties` 自动读取签名配置。

1. 生成签名证书：

```bash
keytool -genkeypair \
  -v \
  -keystore chinese-poetry-android-release-keystore.jks \
  -alias chinese_poetry_release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

2. 在项目根目录创建 `keystore.properties`：

```properties
STORE_FILE=chinese-poetry-android-release-keystore.jks
STORE_PASSWORD=你的密码
KEY_ALIAS=chinese_poetry_release
KEY_PASSWORD=你的密码
```

3. 构建正式包：

```bash
./gradlew assembleRelease
```

APK 输出目录：
`app/build/outputs/apk/release/chinese-poetry-android-v<version>-release.apk`

同步到服务端：
```bash
rsync -av app/build/outputs/apk/release/chinese-poetry-android-v*-release.apk \
  your-server:/opt/chinese-poetry-android/server/public/apk/
```

4. 如需应用商店分发，构建 AAB：

```bash
./gradlew bundleRelease
```

AAB 输出目录：
`app/build/outputs/bundle/release/app-release.aab`

说明：
- 未提供 `keystore.properties` 时，项目仍可正常进行日常开发编译。
- `chinese-poetry-android-release-keystore.jks` 和 `keystore.properties` 请务必妥善备份，不要提交到版本库。
- 默认服务地址可通过 Gradle 属性 `POETRY_BASE_URL` 或 `local.properties` 中的 `BASE_URL` 覆盖；不要把永久服务端凭据编入 APK。
- GitHub Release 工作流要求在 Actions Variables 中配置 `POETRY_BASE_URL`（必须是以 `/` 结尾的 HTTPS 地址）。域名通常不需要放入 Secret；签名文件和密码应配置在 Actions Secrets 中。
- 发布版本的 `versionCode` 使用 `major * 10000 + minor * 100 + patch`（缺少 patch 时按 0）；工作流会同时校验 tag、`versionName` 和 `versionCode`。

## 部署指南

详见 [server/README.md](server/README.md)

服务端使用 Nginx + FastAPI 部署。Android 客户端支持发送 HTTP Basic Auth，并可在设置中动态修改服务器配置；如需鉴权，应在反向代理或服务端显式配置。

## 数据更新

### 增量更新

修改 `server/poetry_data_server.py` 中的 `get_update_data` 函数：

```python
def get_update_data(client_version: int):
    if client_version >= CURRENT_VERSION:
        return {"hasUpdate": False, "newVersion": CURRENT_VERSION}
    
    return {
        "hasUpdate": True,
        "newVersion": 2,
        "updateType": "incremental",
        "poems": [
            # 新增/修改的诗词（含评论、赏析）
        ],
        "changelog": "新增诗词点评功能"
    }
```

### 全量更新

可以生成新的 `poetry.db` 供首次安装下载。当前客户端不会在运行中接受 `full`
更新，因为内容库仍与收藏、阅读历史共用一个 Room 数据库；直接替换会丢失用户数据。
日常内容更新请先使用增量响应，待用户数据拆库后再启用安全的全量升级。

## 功能特性

- [x] 离线阅读（30万+首诗词）
- [x] 首次启动下载数据
- [x] 增量数据更新
- [x] 动态服务器配置
- [x] HTTP Basic Auth 认证
- [x] 收藏、搜索、分类浏览
- [x] 竖排/横排切换
- [x] 古风文艺界面

## 数据来源

[chinese-poetry](https://github.com/chinese-poetry/chinese-poetry) - 中华古诗词数据库

包含：
- 唐诗（5.5万首）
- 宋诗（26万首）
- 宋词（2.1万首）
- 诗经
- 论语

## License

项目代码采用 [MIT License](LICENSE)。数据来源、版权与再分发声明见 [DATA_SOURCES.md](DATA_SOURCES.md) 和 [NOTICE](NOTICE)。隐私相关说明见 [PRIVACY.md](PRIVACY.md)。
