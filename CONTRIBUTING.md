# 贡献指南

感谢参与维护。提交变更前请：

1. 从 `main` 创建主题分支；
2. 不提交数据库、APK、签名文件、本地配置或真实服务凭据；
3. 为行为变更补充测试，并运行 `./gradlew test lint`；
4. 服务端变更运行 `cd server && poetry install && poetry run pytest`；
5. API、数据格式或架构变化同步更新 `docs/`；
6. Pull Request 说明动机、验证方式以及兼容性影响。

数据纠错请注明可靠出处。新增第三方数据必须说明来源和许可证。
