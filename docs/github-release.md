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

SHA-256 指的是**解压后的数据库**，不是压缩包。此配置是内置保底包的摘要；新版动态初始化使用通过签名验证的远程清单摘要，详见文末。
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

## 数据库接收与检查（2026-11-07）

此前待提供的数据库已收到：`server/poetry.db`，大小 142,266,368 字节（135.68 MiB）。
SQLite `integrity_check` 返回 `ok`，外键检查无违规，包含 341,634 首诗词、13,504 位作者。
没有空 ID/标题/正文或重复诗词 ID；收藏及用户活动记录均为 0。

文件包含客户端所需的三张表及内容字段；部分主键和作者计数字段的可空性差异
由现有 `DatabaseCompatibility` 重建流程处理。332,546 条正文含换行，客户端原来只按
`|` 分段，现补充兼容换行及 CRLF，原数据库文件保持不变。实际 Android/Room 导入仍待真机验证。

已生成以下本地产物（均在 Git 忽略目录中，尚未上传）：

- `dist/data-2026-11-07/poetry.db.gz`：62,452,272 字节（59.56 MiB）。
- `database-manifest.json`、`SHA256SUMS`：数据库和压缩包的摘要。
- `validation-report.json`：接收日期、计数、完整性结果和来源核验范围。
- `NOTICE`：随数据附件提供的上游版权与 MIT 许可声明。

解压后 SHA-256 与原库一致：
`500dfab1453d1ebfc73c8ec2dfd565560fe6ccf4e810c3dd4db133a491169262`。

数据库内部没有上游 commit 或清洗时间元数据。维护者回忆约在 2026 年 5 月生成，
来源为 `chinese-poetry/chinese-poetry`；月份仅为近似信息，尚未独立验证。
已于 2026-11-07 检查维护者提供的历史源仓库：

- 远端：`https://github.com/chinese-poetry/chinese-poetry.git`。
- commit：`909646524737b70544ee7ae17cb76868a0bc8a55`。
- 上游提交时间：2025-04-02 17:42:19 +08:00。
- 完整 Git 历史（非浅克隆），工作区干净，无未提交修改。
- 已核对上游 MIT 许可及 `Copyright (c) 2016 JackeyGao`，与项目 NOTICE 一致。

来源关联依据是维护者提供的历史 checkout，尚未重新转换并比较数据库内容，不能声称已实现逐字节复现。
上游提交日期不代表数据库生成日期；原始数据获取日期未知，接收日期也不视为获取或清洗日期。
来源信息已补入 `database-manifest.json` 和 `validation-report.json`，原始数据库与 gzip 文件未改动。

换行分段兼容修复已通过 Debug 单元测试、lint 和 APK 构建；正式上传和真机导入待完成。

## 数据 Release 发布（2026-11-07）

维护者更新源仓库后，HEAD 为 `b8594f81a89752241442f2ce267d6f66f96704ee`。
与历史快照 `909646524737b70544ee7ae17cb76868a0bc8a55` 对比，最终差异仅为
`loader/data_loader.py` 的目录排序及缺省 excludes 处理；区间提交只涉及 README 和 loader，
无数据文件变更。因此原始数据库与压缩包保持不变，未重新清洗。

已发布 [data-2026-11-07](https://github.com/nalansitan/chinese-poetry-android/releases/tag/data-2026-11-07)：

- `poetry.db.gz`
- `database-manifest.json`
- `validation-report.json`
- `SHA256SUMS`
- `NOTICE`

发布前逐个核对 GitHub 返回的附件大小与 SHA-256，五项均与本地一致。
标签对应项目提交 `2b1066db6e3b17b5c53b8d4f115c2dafe93c8511`，该数据 Release 不标记为 Latest。
应用 APK 尚未发布；换行分段兼容修复仍在本地工作区，应用发布前应先提交该修复。

`release` Environment 已配置并读回验证 `POETRY_DATABASE_URL` 和 `POETRY_DATABASE_SHA256`。
匿名请求公开附件 `Range: bytes=0-15` 返回 `206 Partial Content`，
`Content-Range: bytes 0-15/62452272`，响应体为正确 gzip 文件头，确认当前链接支持分段下载。
此网络检查不代表中国大陆所有网络的可达性，也不代替真机断点续传测试。

## 动态初始化数据（2026-11-07）

新版客户端在**首次初始化、尚未安装数据库时**检查 GitHub 数据 Release；已经初始化的用户
不会自动替换内容库，收藏和阅读历史迁移仍是单独的后续工作。

- 内置的 URL、数据库 SHA-256 和 `DATABASE_VERSION` 构成保底配置。当前版本号为 `2026110701`。
- 检查整个过程共用 8 秒网络预算；最多查看最近 100 个 Release 中最新的 3 个带签名清单的数据 Release。
- 每次进程启动最多选择一次，一小时内复用已验证清单；缓存使用前重新验签。
- 只识别 `data-*` 正式 Release，排除草稿和 prerelease。应用的 `v*` 与 APK 检查独立。
- 新包必须具有更大的 `data_version`、受支持的 `schema_version = 1`，且清单签名、URL 和附件大小有效。
- 没有可用新版、无法连接、超时或清单无效时使用内置版本；新版下载、摘要校验或安装失败时只回退一次。
- 保底版本本身仍需联网下载。完全离线时可导入内置包；此前已验签并缓存的新包也可以本地导入。
  未获得可信清单的新数据包不能仅凭用户选中的 `.db` 自动取得信任。
- 断点文件按 URL 和数据库摘要隔离；不同版本不会共用临时文件。安装成功后记录实际数据版本和摘要。

### 清单签名

APK 的 `app/src/main/assets/database-signing-cert.der` 是**公开证书**，用于 SHA256withRSA 验签，
可以提交 Git。私钥仍只存在本地忽略的 `.signing/release.jks` 和 GitHub 签名 Secrets 中。
当前使用同一正式签名密钥签署清单；已有 APK 需要持续使用对应密钥签名的新清单。
未经部署新的受信任公钥，不应更换清单签名密钥。

`database-manifest.json` 需要包含：

```json
{
  "data_version": 2026110801,
  "schema_version": 1,
  "download_url": "https://github.com/nalansitan/chinese-poetry-android/releases/download/data-2026-11-08/poetry.db.gz",
  "database_sha256": "<解压后的数据库 SHA-256>",
  "database_size": 142266368,
  "archive_size": 62452272
}
```

以上版本号、路径和大小仅示例。真实值由打包脚本生成；可在签名前补充来源元数据。
签名是对 JSON **原始字节**生成的二进制 `database-manifest.sig`，签名后不要再改 JSON。

后续发布新数据（替换为实际新版本，不覆盖旧 Release 附件）：

```bash
python3 scripts/package_database.py server/poetry.db dist/data-2026-11-08 \
  --data-version 2026110801 \
  --download-url https://github.com/nalansitan/chinese-poetry-android/releases/download/data-2026-11-08/poetry.db.gz
# 补全清单中的来源 commit、获取/生成时间等信息后再签名。
cp NOTICE dist/data-2026-11-08/NOTICE
mise exec -- java scripts/SignDatabaseManifest.java \
  dist/data-2026-11-08/database-manifest.json keystore.properties
# 签名工具同时更新 SHA256SUMS，覆盖数据包、清单、签名和附加说明。
gh release create data-2026-11-08 dist/data-2026-11-08/* \
  --latest=false --title '诗词数据库 2026-11-08' --notes-file /path/to/release-notes.md
```

仅更新初始化数据时不需要构建 APK，也不用改 Actions 中的保底变量。
若后续发新版 APK 并想更换它的保底数据，应同步配置 `POETRY_DATABASE_URL`、
`POETRY_DATABASE_SHA256`、`POETRY_DATABASE_VERSION` 三项变量。

已发布的 `data-2026-11-07` 保持原样，不补签或覆盖历史附件；它由 APK 内置摘要保证完整性。
本地 `dist/data-manifest-verification/` 为签名验证样例，没有上传为新版数据。

### GitHub 模式的设置入口

设置页和初始化页已移除旧的服务器地址配置。“检查数据更新”入口改为手动查询 GitHub 的签名数据 Release；旧自建服务的启动增量检查已移除。初始化数据继续从 GitHub 选择经过签名验证的新数据包，失败时使用内置保底版本，也支持本地导入。已有数据库不会自动覆盖。

已初始化应用可在设置中检查数据更新，按安装版本比较 `data_version`，点击“下载并更新数据”后复用断点下载和签名摘要校验。网络错误明确提示，不视为已是最新；更新失败不回退覆盖旧库。内容以 ID 在 SQLite 事务中合并，保留本地收藏、创建时间和浏览历史，新包缺失的旧诗词也保留；版本号与内容在同一事务内提交。首次从旧版本升级时，尚无安装记录的数据库以 APK 保底版本作为比较基线。

### 正式包构建与真机启动验证

正式交付使用 `./gradlew clean test lintRelease assembleRelease --no-build-cache`，并传入上述数据 URL、摘要和版本参数。2026-11-07 的本地增量包曾在 Hilt 生成组件中出现缺失合成方法的 `NoSuchMethodError`；不能仅凭构建和安装成功判断可运行。覆盖安装后需实际冷启动应用，检查进程仍存活、Activity 处于前台且本次启动没有新增崩溃日志。排查此类构建问题无需卸载或清除用户数据。
