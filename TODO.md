# TODO

本清单记录首个公开源码版本之后的维护工作。当前范围是源码发布；应用商店上架、
正式数据分发和面向真实用户的升级，需要先完成对应的发布前项目。

## 当前 Release 准备

- [x] 首次初始化限时检查签名数据清单，支持新版失败回退内置包；已安装数据库不自动覆盖。

- [x] 生成正式签名，配置 GitHub `release` 环境的四个签名 Secrets，并验证本地 APK 签名。

- [x] 已于 2026-11-07 收到 `server/poetry.db`，341,634 首诗词、13,504 位作者。
- [x] 数据库完整性检查、压缩打包及解压摘要核验通过，产物位于 Git 忽略的 `dist/data-2026-11-07/`。
- [x] 核实维护者提供的历史源仓库 commit `909646524737b70544ee7ae17cb76868a0bc8a55`，记录约 2026 年 5 月生成（回忆值、未复现）。
- [x] 已发布独立数据 Release `data-2026-11-07`，附件包含来源核验范围和未知获取日期说明。
- [x] 已配置并读回验证 GitHub `release` 环境的 `POETRY_DATABASE_URL`、`POETRY_DATABASE_SHA256`。
- [ ] 完成真机下载、断点续传、本地导入与覆盖升级验收。
- [ ] 正式签名密钥离线备份（本地 Git 忽略不等于备份）。

详细准备记录见 [发布指南](docs/github-release.md)。

## P0：首次正式分发前

- [ ] 将收藏、阅读历史等用户数据从诗词内容库拆分到独立 Room 数据库。
- [ ] 在用户数据拆库后，实现安全的全量内容库更新及 Room/DAO 重载流程。
- [ ] 为发布数据库提供 SHA-256 清单或数字签名，并在客户端下载后强制验证。（静态数据包路径已实现，旧 API 模式仍待补齐。）
- [ ] 为数据库生成物记录上游数据 commit、获取日期、项目 commit 和文件摘要。
- [ ] 增加真实 SQLite/Room 集成测试，覆盖更新后查询、失败回滚及进程中断恢复。
- [ ] 增加动态切换服务器 URL的端到端测试。
- [ ] 增加增量更新测试，确保收藏、阅读历史、创建时间和数据版本不会回退或丢失。

## P1：发布与运维

- [x] 恢复 GitHub Actions 中真实的 Android `testDebugUnitTest lintDebug assembleDebug` CI（需观察首次运行结果）。
- [ ] 服务端实现 HTTP Range 请求及 `206 Partial Content`，完成真正的断点续传。
- [ ] 为数据库和 APK 发布建立可审计的生成清单与发布流程。
- [ ] 启用 Room schema 导出并纳入版本控制，补充 migration tests。
- [ ] 移除或严格限制 `fallbackToDestructiveMigration()`，为后续 schema 版本提供显式迁移。
- [ ] 为 GitHub Release 工作流增加自动化演练，覆盖签名、校验文件和版本校验。
- [ ] 为服务端镜像固定 Python patch 版本和镜像 digest，并建立定期升级流程。
- [ ] 明确生产环境的日志保留、反向代理可信头、备份和恢复策略。

## P2：维护性与技术债

- [ ] 将 `pyproject.toml` 从旧版 `[tool.poetry]` 元数据迁移到 PEP 621 `[project]`。
- [x] 移除 Basic Auth、账号密码设置和 AndroidX Security Crypto 依赖。
- [ ] 清理 Android lint/编译警告，包括弃用图标、`Divider` 和协程实验 API 标注。
- [ ] 移除未使用依赖与代码，例如确认不再需要后删除 WorkManager 依赖。
- [ ] 避免依赖 Android Gradle Plugin 内部的 `ApkVariantOutputImpl`，迁移到稳定 Variant API。
- [ ] 补充贡献者可复现的数据转换示例、小型测试数据集和故障排查文档。

## 应用商店上架（当前不在范围内）

- [ ] 确认应用名称、图标、截图、隐私政策地址和支持联系方式。
- [ ] 完成数据安全表、内容分级、目标 SDK 和商店政策检查。
- [ ] 建立正式签名密钥的离线备份、轮换及恢复方案。
- [ ] 在真实设备和主流 Android 版本上执行安装、升级、离线与弱网测试。
