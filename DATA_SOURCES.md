# 数据来源

本项目不在 Git 仓库中直接提交生成的诗词数据库。`server/data_converter.py` 可以将以下来源转换为 SQLite：

- [chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)
- 上游许可证：MIT
- 上游版权：Copyright (c) 2016 JackeyGao

完整的上游许可证声明保留在本仓库的 `NOTICE` 中。

数据库通过独立 GitHub Release 分发，不提交进 Git。发布说明和校验清单记录
上游 commit、本项目打包脚本 commit、数据库 SHA-256 与来源核验范围；
获取日期或生成日期无法精确确认时明确标注，不以接收日期替代。

提交新的数据来源时，必须同时记录来源地址、版本或提交哈希、许可证、版权声明和转换规则。古典作品正文可能属于公版，但现代整理、注释、翻译和赏析仍可能具有独立版权；未经明确授权的数据不得加入发布数据库。

## 已接收的本地数据快照（2026-11-07）

维护者提供了 `server/poetry.db` 及对应的历史源仓库目录。所提供源仓库 HEAD 为
`909646524737b70544ee7ae17cb76868a0bc8a55`，提交时间为 2025-04-02 17:42:19 +08:00，
远端为上述 chinese-poetry 官方仓库，工作区没有未提交修改。

数据库生成时间据维护者回忆约为 2026 年 5 月，原始数据获取日期未知。
数据库 SHA-256 为 `500dfab1453d1ebfc73c8ec2dfd565560fe6ccf4e810c3dd4db133a491169262`。
来源关联依据为维护者提供的历史 checkout，尚未从该快照重新转换验证，不能声称逐字节可复现。
数据包已发布到 [data-2026-11-07](https://github.com/nalansitan/chinese-poetry-android/releases/tag/data-2026-11-07)，
随附 NOTICE、来源清单与校验报告。
2026-11-07 对比更新后上游 commit `b8594f81a89752241442f2ce267d6f66f96704ee`，
仅 `loader/data_loader.py` 存在净变化，没有数据文件变更，因此继续使用原数据库。
详细计数、校验结果及包清单见 [发布记录](docs/github-release.md)。
