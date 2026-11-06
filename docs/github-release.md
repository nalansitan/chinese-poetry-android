# GitHub Release 与离线数据包

推荐将 APK 和数据库分开发布：数据库使用 `data-2026-11-06` 这样的独立 tag，
应用使用 `v1.2`。仅 `v*` 触发 APK 工作流。不要把数据库提交进 Git 或 Git LFS。

## 1. 生成数据库附件

先按 `server/README.md` 生成数据库，停止写入并完成 WAL checkpoint，再运行：

```bash
python3 scripts/package_database.py server/poetry.db /tmp/poetry-data-release
```

输出 `poetry.db.gz`、`database-manifest.json` 和压缩包的 `SHA256SUMS`。
发布前记录数据来源 commit、获取日期和本项目 commit 到 Release 描述。

```bash
gh release create data-2026-11-06 /tmp/poetry-data-release/* \
  --latest=false --title '诗词数据 2026-11-06' --notes '首次初始化数据包；下载后在应用首页选择本地导入。'
```

以上命令会公开发布附件，请在检查数据来源和文件内容后执行。数据库 Release 必须公开，
地址固定到具体 tag，不要使用 `latest`，也不要覆盖已发布的同名附件。

## 2. 配置 APK 构建

GitHub 仓库 Settings → Environments 创建 `release`，配置以下 Secrets
（也可配置为 repository Secrets）：

- `ANDROID_KEYSTORE_BASE64`：正式签名 JKS 的 Base64 内容。
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

已有分发版本时必须沿用原签名，勿重新生成，否则不能覆盖升级。备份 JKS 和密码。

Actions Variables：

```text
POETRY_DATABASE_URL=https://github.com/nalansitan/chinese-poetry-android/releases/download/data-2026-11-06/poetry.db.gz
POETRY_DATABASE_SHA256=<database-manifest.json 中的 database_sha256>
```

SHA-256 指的是**解压后的数据库**，不是压缩包。应用在线下载和本地导入均校验它。
若仍提供增量诗词更新 API，可另设 `POETRY_BASE_URL`（HTTPS、以 `/` 结尾）。
APK 版本检查已独立使用 GitHub Releases API，不需要自建服务。只有静态附件时，在线增量诗词更新不可用。
首次数据库初始化、搜索、阅读和收藏不依赖自建服务。

本地构建可在 `local.properties` 设置 `DATABASE_URL`、`DATABASE_SHA256`，
或传递同名 `POETRY_*` Gradle 参数。下载 URL 留空时保留旧的公开 API 下载模式。

## 3. 验证并发布 APK

```bash
./gradlew test lint assembleRelease
```

当前 `versionName = "1.2"`、`versionCode = 10200`，对应 tag 为 `v1.2`。
每次升级须增加版本号，并遵守 `major * 10000 + minor * 100 + patch`。
确认变更已提交并推送后，再创建并推送 tag：

```bash
git tag v1.2
git push origin v1.2
```

工作流运行测试、lint、Release 构建和签名验证，再发布 APK 与 `SHA256SUMS`。
若 tag 对应版本已发布，勿覆盖旧资产，应升级版本重新发布。

## 4. 用户下载与导入

- 应用内直接下载 `.db.gz` 或 `.db`。网络失败后点击重试，会携带 Range 续传；
  服务端返回完整文件时自动重下；返回不匹配的范围或 416 时清除无效断点。
- 支持断点保留到应用重启（系统清理缓存后需要重新下载）。不承诺后台持续下载。
- 用户也可在电脑下载相同 Release 的数据库附件，通过 USB、网盘等传到手机，
  首次启动选择“从本地导入数据包”。无需额外的所有文件访问权限。
- 支持 gzip 和原始 SQLite，暂不支持 ZIP。导入期间显示解压校验状态。
- 仅供首次初始化，不覆盖已有收藏和历史。不是用户备份恢复或全量升级入口。
- 下载及解压需要同时存放压缩包和原始库，请预留相应空间。

发布前在真机验证：全新安装、断网续传、进程重启续传、电脑文件导入、损坏包拒绝、
错误版本拒绝、断网阅读，以及使用同一签名覆盖安装后收藏和历史保留。

GitHub 不保证中国大陆所有网络均可访问；断点续传只能减轻传输中断，不能解决完全连不上。
可后续把同一数据包复制到国内存储，修改直链并保留原数据库 SHA-256。

## 版本检查与发布分工

正式 APK 推荐由 GitHub Actions 从版本 tag 构建、测试、验证签名并上传。
本地用于开发、发布前检查与真机测试；数据库包可在本地生成，校验后发布独立的数据 Release。
应急本地发布也必须沿用相同签名，且构建源码必须与 tag 一致。

应用匿名访问 GitHub Releases 列表（最近 100 条），只接受 `v主.次[.补丁]` 的正式版本，
并要求存在 `chinese-poetry-android-v<版本>-release.apk` 附件；忽略草稿、预发布和数据 tag。
它按数值版本选择最高版本，避免新数据 Release 抢占 GitHub `latest` 后误判。
数据库 Release 建议设置 `--latest=false`，保持网页的 Latest 指向应用正式版。

自动检查最多每 24 小时一次（失败也记录尝试时间），手动检查复用一小时成功缓存。
缓存 ETag 并处理 304；403/429 按 `Retry-After` / `X-RateLimit-Reset` 退避。
匿名 API 的主要限额是每公网 IP 每小时 60 次，同一出口共享；不要将私人 Token 编入 APK。
匿名 ETag 请求不能保证免扣请求额度。网络错误保留已发现的更新，APK 从附件的公开下载 URL 获取。
如未来同仓库超过 100 个连续数据 Release，应调整分页策略或拆分数据仓库。

## 发布准备记录（2026-11-06）

- 当前项目目录中没有正式的 `.db` 或 `.db.gz`，数据库文件不纳入 Git。
- 等待项目维护者提供之前清洗好的 SQLite 数据库，优先使用原有成果，不重新清洗。
  可以放到 `server/poetry.db`，也可以提供本机绝对路径；若只有清洗后的 JSON，则先转换。
- 收到数据后，检查 SQLite 完整性、必需表结构与内容，再使用
  `scripts/package_database.py` 生成压缩包、摘要和清单，发布到独立的数据 Release。
- 数据附件发布后，配置 `POETRY_DATABASE_URL` 和 `POETRY_DATABASE_SHA256`。
- 本地 Debug / Release 单元测试各 17 项通过，lint、Debug 和未签名 Release 构建已通过。
  真机首次下载、断点续传、本地导入和覆盖升级仍待验收。
- 正式 APK 由 GitHub Actions 按版本 tag 构建签名并发布；准备签名配置本身不会触发发布。

### 正式签名已配置

- 已生成 RSA-3072 正式签名，有效期 10000 天。
- 密钥文件：项目根目录下 `.signing/release.jks`。
- 口令和别名：项目根目录下 `keystore.properties`，包含 `STORE_PASSWORD`、
  `KEY_PASSWORD`、`KEY_ALIAS` 和 `STORE_FILE`；不要将其内容写进文档或聊天。
- `.signing/` 和 `keystore.properties` 已被 `.gitignore` 忽略；目录权限为 0700，
  密钥及配置文件权限为 0600。需将密钥与配置文件一起保存到安全的离线备份。
- GitHub 仓库 `nalansitan/chinese-poetry-android` 的 `release` Environment 已配置：
  `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、
  `ANDROID_KEY_PASSWORD`。现有发布工作流通过 `environment: release` 使用它们。
- 本地正式签名 APK 构建成功，`apksigner verify` 通过（APK Signature Scheme v2）。
- 公开签名证书 SHA-256：
  `ba732af241ed3ae5ca6b2205d08d42493aaf6e44d379ee6bf0e7183c466b75b4`。
- GitHub 已确认四个 Secret 名称存在；尚未触发云端发布或验证 Actions 签名链路。
  当前生成的 APK 仅供构建/签名验证，正式数据 URL 与摘要仍待数据库到位后配置。
