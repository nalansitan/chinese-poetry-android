# TODO

本清单记录首个公开源码版本之后的维护工作。当前范围是源码发布；应用商店上架、
正式数据分发和面向真实用户的升级，需要先完成对应的发布前项目。

## P0：首次正式分发前

- [ ] 将收藏、阅读历史等用户数据从诗词内容库拆分到独立 Room 数据库。
- [ ] 在用户数据拆库后，实现安全的全量内容库更新及 Room/DAO 重载流程。
- [ ] 为发布数据库提供 SHA-256 清单或数字签名，并在客户端下载后强制验证。
- [ ] 为数据库生成物记录上游数据 commit、获取日期、项目 commit 和文件摘要。
- [ ] 增加真实 SQLite/Room 集成测试，覆盖更新后查询、失败回滚及进程中断恢复。
- [ ] 增加动态切换服务器 URL/用户名/密码的端到端测试。
- [ ] 增加增量更新测试，确保收藏、阅读历史、创建时间和数据版本不会回退或丢失。

## P1：发布与运维

- [ ] 恢复 GitHub Actions 中真实的 Android `test lint assembleDebug` CI；当前 Android job 仅为临时占位检查。
- [ ] 服务端实现 HTTP Range 请求及 `206 Partial Content`，完成真正的断点续传。
- [ ] 为数据库和 APK 发布建立可审计的生成清单与发布流程。
- [ ] 启用 Room schema 导出并纳入版本控制，补充 migration tests。
- [ ] 移除或严格限制 `fallbackToDestructiveMigration()`，为后续 schema 版本提供显式迁移。
- [ ] 为 GitHub Release 工作流增加自动化演练，覆盖签名、校验文件和版本校验。
- [ ] 为服务端镜像固定 Python patch 版本和镜像 digest，并建立定期升级流程。
- [ ] 明确生产环境的日志保留、反向代理可信头、备份和恢复策略。

## P2：维护性与技术债

- [ ] 将 `pyproject.toml` 从旧版 `[tool.poetry]` 元数据迁移到 PEP 621 `[project]`。
- [ ] 评估替换已弃用的 AndroidX Security Crypto API，同时保证已有凭据可迁移。
- [ ] 清理 Android lint/编译警告，包括弃用图标、`Divider` 和协程实验 API 标注。
- [ ] 移除未使用依赖与代码，例如确认不再需要后删除 WorkManager 依赖。
- [ ] 避免依赖 Android Gradle Plugin 内部的 `ApkVariantOutputImpl`，迁移到稳定 Variant API。
- [ ] 补充贡献者可复现的数据转换示例、小型测试数据集和故障排查文档。

## 应用商店上架（当前不在范围内）

- [ ] 确认应用名称、图标、截图、隐私政策地址和支持联系方式。
- [ ] 完成数据安全表、内容分级、目标 SDK 和商店政策检查。
- [ ] 建立正式签名密钥的离线备份、轮换及恢复方案。
- [ ] 在真实设备和主流 Android 版本上执行安装、升级、离线与弱网测试。
