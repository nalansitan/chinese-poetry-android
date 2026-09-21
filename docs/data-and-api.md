# 数据与 API

## 数据生命周期

项目的数据源由 `server/data_converter.py` 转换为客户端可读取的 SQLite 数据库。典型流程如下：

```text
chinese-poetry JSON 数据
    ↓ data_converter.py
poetry.db
    ↓ FastAPI 文件接口
客户端首次下载数据库
    ↓
Room 打开数据库
    ↓
版本检查 → 增量写入（当前拒绝运行中全量替换）
```

首次启动时，客户端获取数据库信息并下载完整数据库。客户端会发送 HTTP `Range` 请求头并保留临时文件，但当前服务端依赖版本不会返回分段内容，因此实际仍会从头下载。完整断点续传需要服务端正确处理 Range 请求并返回 `206 Partial Content` 后才能启用。

后续更新先查询服务端数据版本。增量响应可包含新增或修改的诗词、作者以及待删除 ID；增量写入使用数据库事务并保留本地收藏字段。内容与用户收藏、阅读历史目前仍在同一个 Room 数据库中，因此客户端会明确拒绝 `full` 响应，避免丢失用户数据或让运行中的 DAO 失效。后续应先拆分用户数据库，再启用全量替换。

## API 一览

所有 URL 都相对于配置的 `BASE_URL`，且基础 URL 必须以 `/` 结尾。

| 方法 | 路径 | 用途 |
|---|---|---|
| `GET` | `/health` | 服务健康检查 |
| `GET` | `/api/v1/poetry/version` | 查询当前数据版本 |
| `GET` | `/api/v1/poetry/update?version=N` | 获取指定版本之后的更新 |
| `GET` | `/api/v1/poetry/extension?poem_id=ID` | 按需获取注释、赏析、翻译等扩展内容 |
| `GET` | `/api/v1/poetry/database/info` | 查询完整数据库文件信息 |
| `GET` | `/api/v1/poetry/database` | 下载完整数据库 |
| `GET` | `/public/apk/version` | 查询最新应用版本 |
| `GET` | `/public/apk/download?version=N` | 下载指定或最新 APK |

FastAPI 启动后可通过 `/docs` 查看由当前服务端模型生成的 OpenAPI 文档。字段定义的权威来源是客户端 `PoetryApiService.kt` 和服务端 `poetry_data_server.py`。

## 版本响应

数据版本响应包括版本号、版本名称、更新时间、诗词总量、更新说明和是否强制更新。客户端比较服务端 `version` 与本地数据版本后决定是否请求更新。

更新响应通过 `updateType` 区分 `incremental` 和 `full`：

- `incremental`：处理 `poems`、`authors` 和 `deleteIds`；
- `full`：当前客户端返回不支持错误，不修改数据库和本地版本；
- 无更新时 `hasUpdate` 为 `false`。

## 数据维护约束

- 转换脚本输出必须与 Room 实体和兼容逻辑保持一致；
- 数据版本只递增，不复用已发布版本号；
- 数据来源、转换规则和许可证需要单独记录并持续更新；
- APK Release 索引使用 `server/config/apk_releases.json`，仓库只提交示例文件；
- 数据库、生成的 API 文件、APK、日志和真实发布索引均不提交 Git。

## 已知数据可靠性问题

- 服务端尚未实现与客户端配套的 HTTP Range 分段响应；
- 当前校验可以发现截断文件和非 SQLite 文件，但发布数据库暂未提供独立 SHA-256 清单或数字签名。
- 运行中全量更新尚未启用；需要先将收藏与历史拆分到独立数据库并设计 Room 重载流程。

## 当前安全边界

客户端支持为数据 API 添加 Basic Auth，但不再从构建配置内置共享凭据。用户自行配置的用户名和密码保存在 Android Keystore 支持的加密偏好中，网络调试日志会屏蔽 `Authorization`。公开 APK 接口不发送认证信息。
