# 架构说明

## 总览

Android 客户端使用 Kotlin 和 Jetpack Compose，整体采用 MVVM，并按表现层、领域层和数据层组织代码。Hilt 负责依赖注入，协程和 Flow 负责异步任务及状态流转。

```text
Compose UI
    ↓ 事件 / ↑ UiState
ViewModel
    ↓
Repository 接口（domain）
    ↓
Repository 实现（data）
    ├── Room / DataStore
    └── Retrofit / OkHttp
```

依赖应从外层指向抽象：表现层调用领域仓库接口，数据层实现接口并处理本地与远程数据。UI 不直接操作 DAO 或 Retrofit。

## 目录职责

```text
app/src/main/java/com/nalansitan/chinesepoetry/
├── data/
│   ├── local/          # Room 实体、DAO、数据库和 DataStore
│   ├── manager/        # 数据库下载、数据更新和应用更新
│   ├── remote/         # Retrofit 服务创建与 API 模型
│   └── repository/     # 领域仓库实现
├── di/                 # Hilt 模块
├── domain/
│   ├── model/          # 领域模型
│   └── repository/     # 仓库接口
└── presentation/
    ├── navigation/     # Compose 路由
    ├── ui/             # 页面、组件和主题
    ├── util/           # 展示层工具
    └── viewmodel/      # 页面状态与业务编排
```

## 本地数据

Room 数据库名为 `poetry.db`，包含：

- `PoemEntity`：诗词正文、分类、扩展内容和收藏状态；
- `AuthorEntity`：作者资料与作品数量；
- `UserActivityEntity`：用户阅读活动。

数据库兼容逻辑集中在 `DatabaseCompatibility` 和 `PoetryDatabase`。修改表结构时，应提供明确迁移并验证服务端生成的数据库能被当前客户端打开，避免继续依赖破坏性迁移。

DataStore 保存显示设置、数据版本、更新状态、搜索历史和服务器 URL。服务器用户名和密码使用 Android Keystore 支持的加密偏好单独保存，应用数据备份默认关闭。

## 网络和依赖注入

`ApiServiceProvider` 根据默认构建配置或用户设置创建 Retrofit 服务。数据 API 和公开 APK API 使用不同的 OkHttp 客户端；前者目前支持 Basic Auth，后者不附带认证头。

Hilt 模块职责：

- `DatabaseModule`：数据库与 DAO；
- `NetworkModule`：API 服务提供者；
- `RepositoryModule`：领域接口和数据层实现的绑定。

## 导航

底部导航包含首页、发现、收藏和“我的”。诗词详情、诗词列表、搜索、设置及历史记录使用独立路由。新增页面时，应在 `Screen` 中定义路由，并由 `MainActivity` 统一组装导航图。
