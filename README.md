# 古诗词

一款开源的 Android 离线古诗词阅读应用。项目基于
[chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)
开源古诗词数据，将原始数据格式化并转换为本地 SQLite 数据库。应用在首次启动时下载或导入诗词数据库，
之后即可长期离线阅读、搜索和收藏古诗词。

An open-source offline Chinese poetry reader for Android. It formats data from
[chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)
and stores it in a local SQLite database. The database can be downloaded or imported from a local file during
initial setup; afterward, poems can be read, searched, and saved offline.

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

### 1. 准备数据（推荐 GitHub Release 数据包）

无需自建服务器的压缩包下载、本地导入和完整发布步骤见 [GitHub Release 指南](docs/github-release.md)。

`server/` 保留数据转换工具和历史服务代码；当前应用以 GitHub Release 为分发渠道，不提供自建服务配置入口。

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
- GitHub Release 工作流支持 `POETRY_DATABASE_URL` + `POETRY_DATABASE_SHA256` 直接分发数据库。签名文件和密码配置在 Actions Secrets 中，详见发布指南。
- 发布版本的 `versionCode` 使用 `major * 10000 + minor * 100 + patch`（缺少 patch 时按 0）；工作流会同时校验 tag、`versionName` 和 `versionCode`。

## 数据更新

在设置中点击“检查数据更新”，检查 GitHub 中有签名清单的 `data-*` Release。
发现新版后下载并校验数据包，在事务中按 ID 合并诗词与作者，保留收藏、浏览历史及新包中缺失的旧记录。
网络或校验失败时保留本地数据。数据发布流程见 [GitHub Release 指南](docs/github-release.md)。

## 功能特性

- [x] 离线阅读（30万+首诗词）
- [x] 首次启动检查签名数据清单，优先下载兼容新版，失败回退内置版本
- [x] GitHub 数据版本检查与保留用户记录的内容更新
- [x] 公开 HTTPS 数据包、gzip 解压与本地文件导入
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
