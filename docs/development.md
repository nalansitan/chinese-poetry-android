# 开发指南

## 环境

- JDK 17
- Android SDK 34
- Android Studio 或 Gradle Wrapper
- 服务端可选：Python 3.9+、Poetry

项目当前使用 Kotlin、Jetpack Compose、Room、Hilt、Retrofit、OkHttp、Paging 和 WorkManager。依赖版本以 Gradle 文件为准。

## 本地配置

根目录的 `local.properties` 不提交 Git。本机至少需要 Android SDK 配置；现有构建还从该文件读取：

```properties
sdk.dir=/path/to/Android/sdk
BASE_URL=https://example.com/
```

`BASE_URL` 必须以 `/` 结尾。公开构建不应内置永久 API 凭据，因为 `BuildConfig` 内容可以从 APK 中读取。

Release 签名使用被忽略的 `keystore.properties`：

```properties
STORE_FILE=release.jks
STORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

签名文件和密码不得提交。CI 发布时应通过受保护的 GitHub Environment Secrets 临时还原。

## 常用命令

```bash
./gradlew :app:compileDebugKotlin
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
./gradlew lint
```

服务端开发和数据转换命令见 `server/README.md`。

## 开发约定

- 包名统一使用 `com.nalansitan.chinesepoetry`；
- UI 只渲染状态并上报事件，异步业务逻辑放在 ViewModel 或数据管理类；
- 领域层不依赖 Android UI 或具体网络、数据库实现；
- DAO 查询、远程模型与领域模型之间的转换放在数据层；
- 长耗时任务使用协程的合适调度器，不在主线程执行数据库或网络操作；
- 新增依赖通过 Hilt 提供，避免在页面中手动构造服务；
- 修改 API、数据库结构或更新协议时同步修改 `docs/`。

## 建议的提交检查

提交 Pull Request 前至少完成：

1. Debug 编译；
2. 受影响模块的单元测试；
3. Android lint；
4. 首次下载、搜索、收藏或更新等相关手工验证；
5. 检查没有提交 `local.properties`、签名文件、数据库、APK 和真实服务凭据。

GitHub Actions 会执行 Android 测试、lint、Debug 构建以及服务端测试。新增功能或修复缺陷时，仍应优先为 ViewModel、Repository、数据转换和版本更新逻辑补充测试。
