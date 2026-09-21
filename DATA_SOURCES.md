# 数据来源

本项目不在 Git 仓库中直接提交生成的诗词数据库。`server/data_converter.py` 可以将以下来源转换为 SQLite：

- [chinese-poetry/chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)
- 上游许可证：MIT
- 上游版权：Copyright (c) 2016 JackeyGao

完整的上游许可证声明保留在本仓库的 `NOTICE` 中。

本仓库当前不分发生成数据库，因此不声称对应某个固定的数据快照。制作并发布
`poetry.db` 时，发布说明和校验清单必须记录实际使用的上游 commit SHA、获取日期、
本项目 commit SHA 与数据库 SHA-256；不能只记录分支名或 `latest`。

提交新的数据来源时，必须同时记录来源地址、版本或提交哈希、许可证、版权声明和转换规则。古典作品正文可能属于公版，但现代整理、注释、翻译和赏析仍可能具有独立版权；未经明确授权的数据不得加入发布数据库。
