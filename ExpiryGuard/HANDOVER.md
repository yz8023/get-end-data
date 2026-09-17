# 过期管家 (ExpiryGuard) 项目交接文档

> 最后更新：2026-08-16
> 版本：1.0.2 (versionCode=3)
> 源码仓库：`https://github.com/Forinxy/get-end-data`（私有仓库）
> 交接包产出：HANDOVER.md / ExpiryGuard_1.0.2_source.zip / ExpiryGuard_1.0.2.apk
> 交付状态：v1.0.2 已发布。提交 `468c8e2`（功能）、`d69759d`（口径统一）、`83cfa31`（防丢数据）、`48ee75b`（ExpiringSoon）、`20a8664`（文档）已推送 origin/main，Release v1.0.2 双资产已更新

---

## 1. 项目概述

**过期管家** 是一款完全离线的 Android 原生应用，帮助用户记录商品保质期、到期自动提醒，避免食品和日用品过期浪费。应用数据全部存储在本地 Room 数据库，无需网络与后端。

| 维度 | 详情 |
|------|------|
| 应用名 | 过期管家 (ExpiryGuard) |
| 包名 / applicationId | `com.expiryguard.app` |
| 技术栈 | Kotlin 2.0.21 + Jetpack Compose (Material3) + Hilt + Room + MVVM |
| 架构模式 | MVVM + Hilt 依赖注入 + Room 本地数据库 + StateFlow |
| minSdk / targetSdk / compileSdk | 26 (Android 8.0) / 35 / 35 |
| UI 风格 | 毛玻璃拟态 (Glassmorphism)，支持浅/深色 + Android 12+ Monet 动态取色 |
| 离线能力 | 完全离线，无后端、无第三方账号 |

---

## 2. 开发环境

### 2.1 必备工具

| 工具 | 版本要求 | 验证命令 |
|------|---------|---------|
| 操作系统 | macOS / Windows / Linux | — |
| JDK | **17** | `java -version` |
| Android SDK | Platform 35 + Build Tools 35.0.0 | `sdkmanager --list` |
| Gradle | **8.9**（wrapper 自动下载） | `./gradlew --version` |
| AGP | 8.7.0（`build.gradle.kts` 声明） | — |
| Kotlin | 2.0.21（含 Compose 编译器插件） | — |

### 2.2 环境变量

| 变量 | 说明 |
|------|------|
| `ANDROID_HOME` | Android SDK 路径，如 `~/Android/Sdk` |
| `JAVA_HOME` | JDK 17 安装路径 |
| `sdk.dir` | 可在项目根 `local.properties` 中设置（`sdk.dir=/opt/android-sdk`） |

### 2.3 第三方 Key

**无需任何第三方 API 密钥。** 应用完全离线运行，无后端、无推送服务商、无地图/支付/广告 SDK。

---

## 3. 构建运行

### 3.1 从零构建 Debug APK（复制粘贴即可执行）

```bash
# 1. 克隆仓库（需要 GitHub 账号有该私有仓库权限）
git clone https://github.com/Forinxy/get-end-data.git
cd get-end-data

# 2. 确认 JDK 17
java -version

# 3. 设置 SDK 路径（如未自动检测）
export ANDROID_HOME=~/Android/Sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 4. 构建 Debug APK
./gradlew assembleDebug

# 5. 产出路径
#    app/build/outputs/apk/debug/app-debug.apk

# 6. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3.2 已知坑与解法

1. **首次构建较慢**：需下载 Gradle 8.9 发行版 + 全部依赖（阿里云镜像），约 5-15 分钟，属正常现象。
2. **构建报 SDK Build-Tools 34 缺失**：AGP 会自动通过 sdkmanager 安装，需确认网络可达 `dl.google.com`；或手动执行 `sdkmanager "build-tools;34.0.0"`。
3. **`gradlew: Permission denied`**：`chmod +x gradlew` 后重试。
4. **内存不足**：`gradle.properties` 中 `org.gradle.jvmargs=-Xmx1536m`，低内存机器可进一步调低；CI 中已用 `-Xmx2048m` 覆盖。
5. **镜像失效**：`settings.gradle.kts` 配置了阿里云镜像，若失效可删除镜像配置回退官方 Google/Maven Central。

### 3.3 GitHub Actions 自动构建

- **脚本路径**：`.github/workflows/build-apk.yml`
- **触发方式**：
  - push 到 `main` / `master` 分支
  - 创建 pull request
  - 手动触发：仓库 Actions 页 → Build APK → Run workflow
- **产物**：Debug APK（`app-debug.apk`）与未签名 Release APK（`app-release-unsigned.apk`），作为 workflow artifact 提供下载。
- 本地验证：`chmod +x gradlew && ./gradlew assembleDebug` 已在本机通过（BUILD SUCCESSFUL，约 13 分钟）。

---

## 4. 依赖与镜像

### 4.1 主要依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| androidx.compose:compose-bom | 2024.10.01 | Compose 全家桶 |
| androidx.room | 2.6.1 | 本地数据库 |
| com.google.dagger:hilt-android | 2.51.1 | 依赖注入 |
| androidx.navigation:navigation-compose | 2.8.3 | 导航 |
| io.coil-kt:coil-compose | 2.7.0 | 图片加载 |
| com.patrykandpatrick.vico:compose | 2.0.0-beta.2 | 统计图表 |
| androidx.datastore:datastore-preferences | 1.1.1 | 偏好存储 |
| androidx.work:work-runtime-ktx | 2.9.1 | 后台任务（暂未实现 Worker） |
| com.google.code.gson | 2.11.0 | JSON 序列化 |

### 4.2 镜像配置

`settings.gradle.kts` 已内置阿里云镜像（优先于官方源）：

| 官方源 | 镜像地址 |
|--------|----------|
| Google Maven | `https://maven.aliyun.com/repository/google` |
| Maven Central | `https://maven.aliyun.com/repository/public` |
| Gradle 插件 | `https://maven.aliyun.com/repository/gradle-plugin` |

Gradle 发行版使用腾讯云镜像（`gradle/wrapper/gradle-wrapper.properties`）：
`https://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip`

**获取优先级**：本地缓存 → 国内镜像（已配置）→ 官方源 → 手动下载。
本工程所有依赖均从公开镜像可获取，无需 GitHub 代理或手动下载。

---

## 5. 本地依赖服务

**无需任何本地/远程服务。** 无后端、无数据库服务器、无 Redis、无消息队列。应用数据保存在设备本地 Room 数据库中，直接安装运行即可完整使用。

---

## 6. 开发进度

### 6.1 已完成

| 功能 | 状态 | 说明 |
|------|:---:|------|
| 清单 CRUD | ✅ | 添加/编辑/删除/查看详情，支持拍照、生产日期、保质期预设+自定义天数 |
| 到期状态引擎 | ✅ | 5 种状态：安全/即将到期/可退货/紧急/过期，支持自定义退货阈值 |
| 保质期分组 | ✅ | 5 个默认分组（当天/3月以下/3~6月/6月~1年/1年以上），可自定义管理 |
| 首页今日待办 | ✅ | 今日到期+可退货+已过期+预警（1天），支持左右滑动标记完成，下拉刷新 |
| 清单列表 | ✅ | 列表/网格双视图，筛选（全部/安全/即将到期/可退货/过期），搜索，多选批量删除/备注 |
| 统计页 | ✅ | 状态分布、到期趋势、分类统计图表（Vico 图表库） |
| 分类管理 | ✅ | 自定义分类，支持颜色 |
| 回收站 | ✅ | 软删除→恢复/永久删除 |
| 设置页 | ✅ | 深色模式/Monet 取色/通知开关/提醒天数/自动清理（UI） |
| 数据备份恢复 | ✅ | 导出 CSV/JSON，备份到本地文件，从文件恢复 |
| 通知提醒 | ✅ | 启动时检查待处理清单发送通知，支持测试通知 |
| 毛玻璃 UI | ✅ | 浅色/深色主题，自适应，毛玻璃拟态风格 |

### 6.2 性能与交互优化补丁（2026-08-13 ~ 2026-08-14）

第一轮（2026-08-13，性能修复，已推送 commit `885e97b`）：

| 优化项 | 说明 |
|------|------|
| 首页列表懒加载 | `Column + verticalScroll` → `LazyColumn`，任务卡片改为 `items` 懒加载，消除任务多时一次性渲染卡顿 |
| 图片保存压缩 | `ImageUtils.saveImage` 改为采样压缩（默认最大 1080px）后保存，替代原图直拷贝 |
| Coil 加载限尺寸 | 列表缩略图 `.size(128)`，详情/编辑图 `.size(720/1080)`，降低解码内存与 IO |
| 图片 IO 移线程 | 拍照保存、`pickImage` 改为 `Dispatchers.IO` 执行，避免阻塞主线程 |
| 主线程计算迁移 | `HomeViewModel` / `ProductListViewModel` 过滤计算移至 `Dispatchers.Default` |
| 启动开销优化 | `MainActivity` 通知检查的 DB 查询与过滤移至 IO 线程 |
| DateUtils 优化 | 缓存 `ZoneId`，避免每次调用 `systemDefault()` |
| CI 自动构建 | 新增 `.github/workflows/build-apk.yml`，构建后自动发布 GitHub Release |

第二轮（2026-08-14，本次交接，加载/区分已完成/防误触）：

| 优化项 | 说明 |
|------|------|
| 加载显示优化 | 首页首次加载显示加载指示器（`CircularProgressIndicator`），避免白屏/空状态闪烁 |
| 区分今日/历史已完成 | 数据库新增 `completedAt` 字段（v3→v4 显式 Migration，不清数据），首页「已处理」拆分为「今日已完成」与「之前已完成」两组，历史组最多展示 20 条避免首页过长 |
| 防误触标记 | 滑动触发阈值从默认 50% 提高至卡片宽度 65%，轻滑不再误触发 |
| 滑动操作可撤销 | 标记/取消完成后弹出 Snackbar 提示具体清单名，并带「撤销」按钮，误触可一键恢复 |
| 滑动背景提示 | 未完成时背景显示绿色「滑动标记完成」，已完成时显示灰色「滑动取消完成」，操作意图清晰 |

第三轮（2026-08-14，本次交接，v1.0.2 日期输入/大图/统计优化）：

| 优化项 | 说明 |
|------|------|
| 日期多格式文本输入 | 新增 `DateInputField` 组件（`ui/components/DateInputField.kt`），支持直接输入 `2019.1.1` / `2019年1月1日` / `2019 1 1` / `19.1.1` 等格式即时识别回写，附带日历选择兜底；替代原「点击弹日历」单一交互 |
| 日期解析工具 | `DateUtils.parseDate` / `parseDateToTimestamp` 新增多格式解析，两位年份自动补 `20xx`，仅年月按当月 1 号处理 |
| 生产/到期日期防点错 | 添加与编辑页日期字段改用 `DateInputField`，到期日红色（Red500）、生产日蓝色（Blue500）色点标签区分，输入即校验 |
| 首页详情大图 | 首页底部详情弹层图片点击全屏放大查看（`FullScreenImageViewer`，黑底 + 点任意处/右上角关闭） |
| 统计页计算优化 | `StatsViewModel` 状态统计由每项 4 次 `calculateStatus` 改为单次遍历累加，减少无谓计算 |
| 通知与首页口径统一 | 抽取 `ExpiryRuleEngine.computePendingGroups()` 统一分组计算，`MainActivity` 启动通知与 `HomeViewModel` 首页复用同一口径（含已过期项），数量不再不一致 |
| 旧库破坏前自动备份 | 数据库构建时检测版本 <3 的旧库（无迁移链），自动复制到应用目录并导出 `Download/ExpiryGuard/`，destructive 兜底触发前数据可找回 |
| ExpiringSoon 状态启用 | 短保质期产品（<90 天，退货阈值=0）剩余 4~30 天返回「即将到期」，填补安全→紧急预警断层；列表「即将到期」筛选改为按状态匹配 |
| 产品名称自定义 | 添加页新增第一步「清单名称」输入，保存校验非空，替代固定「清单」名称 |
| 定时后台通知 | `ExpiryReminderWorker` 每日周期检查待处理清单并发送通知，复用统一口径与通知开关，应用启动时注册调度 |
| 自动清理回收站 | 定时 Worker 同时按 `auto_cleanup_days` 设置，永久删除回收站中超过指定天数的清单 |

### 6.3 进行中 / 已搁置

| 功能 | 状态 | 说明 |
|------|------|------|
| 状态标记调优 | 进行中 | 根据反馈调整过期/安全/可退货标记视觉（ExpiryGuard/ 子目录为旧版备份） |
| 条形码扫描 | 已搁置 | 已声明 CAMERA 权限和 CameraX 依赖，但扫描功能未实现 |
| 定时后台通知 | 已搁置 | 已引入 WorkManager，但未实现定时 Worker |
| 自动清理 | 已搁置 | 设置中已有 UI 配置，未实现自动清理逻辑 |
| 云同步 / 小组件 / 多语言 | 已搁置 | 未规划 |

### 6.4 最近可运行 commit

当前推送后的最新 commit（v1.0.2，含日期输入/大图/统计优化）。注意：仓库根目录存在 `ExpiryGuard/` 子目录，为较早版本的整体备份（ProductCard 视觉不同），主工程以根目录为准。

---

## 7. 待开发内容 (Backlog)

| 优先级 | 功能 | 估算工作量 | 前置依赖 |
|:---:|------|:---:|------|
| P1 | 条形码扫描识别（CameraX + ML Kit 或 ZXing） | 5-7 天 | 选择扫码方案 |
| P1 | 自动清理过期清单 | 2-3 天 | 无 |
| P2 | 产品名称自动填充（取代默认"清单"名称） | 1-2 天 | 无 |
| P2 | 批量导入（CSV/JSON 恢复支持合并而非覆盖） | 2-3 天 | 无 |
| P2 | 多语言支持（至少英文） | 3-5 天 | 提取 strings.xml |
| P3 | 桌面小组件 (App Widget) | 5-7 天 | 无 |
| P3 | 分类统计图表交互优化 | 2-3 天 | 无 |
| P3 | 产品照片编辑/裁剪 | 2-3 天 | 无 |

---

## 8. 架构与关键模块

### 8.1 项目结构

```
app/src/main/java/com/expiryguard/app/
├── ExpiryGuardApp.kt          # Application 入口，初始化通知渠道
├── MainActivity.kt            # 唯一 Activity，Hilt 入口，加载主题和导航
├── data/
│   ├── db/
│   │   ├── entity/             # Room 实体：ProductEntity, CategoryEntity, ShelfLifeGroupEntity
│   │   ├── dao/                # DAO 接口：ProductDao, CategoryDao, ShelfLifeGroupDao
│   │   └── AppDatabase.kt     # Room 数据库（v4）+ 默认分组初始化 + Migration
│   └── repository/
│       └── ProductRepository.kt # 数据仓库，封装所有 DAO 操作
├── di/
│   ├── AppModule.kt           # Hilt：提供 DataStore<Preferences>
│   └── DatabaseModule.kt      # Hilt：提供 Room DB、DAO、Repository
├── domain/
│   ├── engine/ExpiryRuleEngine.kt  # 到期规则引擎（单例 object）
│   └── model/ProductStatus.kt      # 密封类：Safe/ExpiringSoon/Returnable/Urgent/Expired
├── navigation/AppNavigation.kt     # 路由 + 底部导航 + 相机集成
├── notification/NotificationHelper.kt # 通知渠道 + 待办提醒
├── ui/
│   ├── components/            # GlassCard/ProductCard/StatusBadge/StatCard/DatePicker/EmptyState
│   ├── home/                  # 首页：HomeScreen + HomeViewModel
│   ├── product/               # 清单：Add/Edit/Detail/List + ViewModel
│   ├── settings/              # 设置相关 6 个页面
│   ├── stats/                 # 统计：StatsScreen + StatsViewModel
│   ├── theme/                 # 主题：Color/Theme/Type
│   └── trash/                 # 回收站：TrashScreen + TrashViewModel
└── util/
    ├── DateUtils.kt           # 日期工具（java.time，缓存 ZoneId）
    └── ImageUtils.kt          # 图片保存/压缩/删除
```

### 8.2 核心入口

| 入口 | 文件 | 职责 |
|------|------|------|
| Application | `ExpiryGuardApp.kt` | Hilt 入口，创建通知渠道 |
| Activity | `MainActivity.kt` | 加载 DataStore 偏好 → 初始化主题 → 渲染 `AppNavigation`；启动通知检查在 IO 线程 |
| 导航 | `AppNavigation.kt` | 12+ 路由，4 Tab 底部导航，相机拍照回调（IO 线程压缩保存） |

### 8.3 数据流

```
UI (Compose Screen)
  ↑ collectAsState()
ViewModel (Hilt @HiltViewModel)
  ↑ Flow（filter/sort 在 Dispatchers.Default）
ProductRepository (单例, @Inject)
  ↑ Flow / suspend fun → Result<T>
Room DAO (ProductDao, CategoryDao, ShelfLifeGroupDao)
  ↑
Room Database (AppDatabase, v4, Hilt 单例)
```

### 8.4 到期规则引擎

`ExpiryRuleEngine.calculateStatus(shelfLifeDays, expiryDate)` 判定优先级：
1. `remainingDays <= 0` → `Expired(daysOverdue)`
2. `remainingDays <= 3` → `Urgent(remainingDays)`
3. `remainingDays <= threshold` → `Returnable(remainingDays, threshold)`
4. 否则 → `Safe(remainingDays)`

退货阈值：保质期 ≥365 天→45 天；>183 天→20 天；≥90 天→15 天；<90 天→0（不可退货）。

### 8.5 首页今日待办筛选

`HomeViewModel.processProducts()` 合并四个来源：今日到期（`isToday` + 未完成）、可退货（`Returnable` 状态）、已过期（`days<0`）、预警（`days==1`），去重后按到期日期排序。已处理区仅显示四类中已完成项。

### 8.6 已知技术债

1. **`.gitignore` 缺失于仓库根**：构建产物 `build/`、`.gradle/`、`local.properties` 可能被误提交（本机已建 `local.properties`，未提交）。
2. **ProGuard 规则不完整**：仅保留 Gson 注解，Release 混淆时 Room/Hilt/Coil 需补充规则（当前 `isMinifyEnabled=false`，无实际影响）。
3. **定时后台通知已实现**：`ExpiryReminderWorker`（@HiltWorker + hilt-work）每日周期检查待处理清单并发送通知，`ReminderScheduler.scheduleDailyReminder` 在应用启动时注册。
4. **产品名称固定"清单"已解决**：`AddProductViewModel` 新增 `name` 状态与 `updateName`，添加页第一步输入清单名称，保存时校验非空。
5. **仓库存在重复工程**：根目录为主工程，`ExpiryGuard/` 为旧版备份目录，建议清理以避免混淆。
6. **数据库迁移策略部分解决**：v3→v4 已提供显式 Migration（新增 `completedAt` 列）；v1/v2 无历史 schema 可补迁移链，但已在 `buildDatabase` 增加旧库自动备份（版本 <3 时复制到应用目录并导出 Download/ExpiryGuard/），destructive 兜底触发前数据可找回。未来每次升级都应补充显式 Migration。

---

## 9. 代码概览与已知问题

### 9.1 代码概览

- 代码总量约 40 个 Kotlin 文件，单一 `app` 模块，无多模块拆分。
- 使用 KSP（非 KAPT）处理 Room/Hilt，编译速度较快。
- UI 全部为 Compose，无 XML 布局；主题为毛玻璃拟态风格。

### 9.2 严重问题（需优先处理）

1. **`fallbackToDestructiveMigration()` 兜底**：数据库升级依赖显式 Migration；若未来未提供 Migration 会清空用户数据。修复方向：每次升级补充 `Migration` 对象，逐步移除兜底。

> 已解决：~~启动通知逻辑与首页待办口径不一致~~——`ExpiryRuleEngine.computePendingGroups()` 已抽取统一口径，`MainActivity` 通知与 `HomeViewModel` 首页复用同一函数（含已过期项）。

> 其余为轻微问题（命名、重复代码、未使用依赖等），不影响稳定运行，见第 8.6 节技术债清单。

---

## 10. 架构简评

- **架构模式**：MVVM + 单向数据流（StateFlow → Compose），分层清晰（data/domain/ui）。
- **整体评价**：**良好**。模块职责划分合理，Hilt + Room 使用规范，数据流明确。
- **突出问题与改进方向**：
  1. 数据处理多处重复（首页/列表/通知各自写过滤逻辑），建议抽取领域层聚合函数统一口径。
  2. 图片处理无统一封装，`ImageUtils` 可扩展生成缩略图 + 原图双存储策略，进一步降低列表内存。
  3. 无 Repository 层缓存，每次状态流变化全量重算；数据量大时可引入分页（Paging 3）。
  4. 数据库无索引、无迁移策略，需要补 Migration 方案与必要索引。
  5. 单 Activity 单模块，功能已较多，未来可按 feature 拆分模块。

---

## 11. 测试建议

### 11.1 核心流程（可手工验证）

1. 首次启动 → 首页空状态 → 添加清单（拍照/相册 + 生产日期 + 保质期）→ 保存成功
2. 首页出现新清单，状态色条与 StatusBadge 正确
3. 添加过期清单 → 首页深红色过期标记 → 左滑标记完成 → 绿色"已处理"
4. 清单列表：筛选/搜索/多选批量删除/列表↔网格切换
5. 统计页图表渲染正常
6. 设置页深色模式/备份恢复正常

### 11.2 重点测试模块

| 模块 | 重点场景 |
|------|------|
| 首页 HomeScreen | 大量待办（50+ 条）时上下滑动流畅度、下拉刷新、滑动标记完成 |
| 图片加载 | 相机拍照后列表滚动时内存占用（可用 Android Studio Profiler 观察） |
| 搜索过滤 | 输入搜索时主线程无 jank（过滤已迁移 Default 线程） |
| 数据备份恢复 | CSV/JSON 导出 → 清数据 → 恢复一致性 |

### 11.3 已知/可能的边界问题

- 图片路径为空/文件被删除时 `AsyncImage` 静默失败（显示占位背景），无崩溃风险。
- 时区切换后 `DateUtils` 缓存 `ZoneId` 为启动时值，跨时区用户重启 App 前展示可能延迟一天更新。
- 深色模式切换时 `GlassCard` 颜色即时更新（`isDarkTheme()` 每帧计算，开销小）。

---

## 12. 账号与密钥

**应用完全离线，当前无需任何账号或密钥。**

未来若接入以下功能需自行申请（均用占位符）：

| 功能 | 提供方 | 申请方式 |
|------|--------|---------|
| 条形码识别 | ML Kit / ZXing | 无需 Key（本地识别），或用 `<YOUR_API_KEY>`（云端方案） |
| 云同步 | Firebase / 自建后端 | Firebase 控制台创建项目，填入 `<YOUR_GOOGLE_SERVICES_JSON>` |
| 崩溃上报 | Firebase Crashlytics / Sentry | 官方控制台申请 `<YOUR_API_KEY>` |

---

## 13. 常见问题

| 问题 | 解法 |
|------|------|
| `Unable to locate package openjdk-17` | `apt-get update` 后重装 |
| `./gradlew: Permission denied` | `chmod +x gradlew` |
| 依赖下载失败（阿里云镜像不可达） | 编辑 `settings.gradle.kts` 删除镜像配置，回退官方源 |
| Gradle 发行版下载失败 | 编辑 `gradle-wrapper.properties` 改回 `https://services.gradle.org/distributions/gradle-8.9-bin.zip` |
| 构建报 Build-Tools 34 缺失 | `sdkmanager "build-tools;34.0.0"` 手动安装 |
| 构建 OOM / 卡死 | 调低 `gradle.properties` 中 `-Xmx`，或关闭其他内存占用进程 |
| 运行闪退 | 查看 `adb logcat`，重点看 Room/Hilt 初始化错误 |

---

## 14. 验收标准

- [ ] 环境：JDK 17 + Android SDK 35 配置完成，`java -version` 通过
- [ ] 构建：`./gradlew assembleDebug` 成功，生成 `app/build/outputs/apk/debug/app-debug.apk`
- [ ] 运行：APK 安装后正常启动到首页，无崩溃
- [ ] 功能：添加/编辑/删除清单、状态标记、首页滑动标记完成、筛选/搜索、统计图表、备份恢复全部正常
- [ ] 性能：首页 50+ 待办时滑动流畅（LazyColumn 生效），拍照保存不卡 UI（IO 线程 + 压缩生效）
- [ ] CI：push 到 main/master 后，GitHub Actions `Build APK` workflow 自动构建并产出 APK artifact
- [ ] 代码：能说出 `ExpiryRuleEngine.calculateStatus` 优先级、首页四来源合并逻辑、性能优化点（LazyColumn/图片压缩/线程迁移）

---

## 附：本次修改文件清单（性能优化 + 加载/区分已完成/防误触补丁）

第一轮（性能修复，commit `885e97b`）：

| 文件 | 修改内容 |
|------|------|
| `ui/home/HomeScreen.kt` | verticalScroll → LazyColumn；todayTasksSection 懒加载；AsyncImage 限尺寸；remember 缓存 |
| `ui/components/ProductCard.kt` | AsyncImage 缩略图 `.size(128)` |
| `util/ImageUtils.kt` | saveImage 采样压缩后保存 |
| `ui/product/AddProductViewModel.kt` / `EditProductViewModel.kt` | pickImage 移 IO 线程 |
| `navigation/AppNavigation.kt` | 拍照保存移 IO 线程 + lifecycleScope |
| `ui/home/HomeViewModel.kt` | 过滤计算 flowOn(Default) |
| `ui/product/ProductListViewModel.kt` | 过滤计算 Default 线程；setSearchQuery 异步 |
| `MainActivity.kt` | 启动通知检查移 IO 线程 |
| `util/DateUtils.kt` | 缓存 ZoneId |
| `ui/product/ProductDetailScreen.kt` / `AddProductScreen.kt` / `EditProductScreen.kt` | AsyncImage 限尺寸 |
| `.github/workflows/build-apk.yml` | 新增 GitHub Actions 自动构建 + Release 发布 |

第二轮（本次交接，commit 待推送）：

| 文件 | 修改内容 |
|------|------|
| `data/db/entity/ProductEntity.kt` | 新增 `completedAt` 字段（完成时间） |
| `data/db/AppDatabase.kt` | 数据库 v3→v4 显式 Migration 新增 completedAt 列 |
| `data/db/dao/ProductDao.kt` | `updateCompletionStatus` 标记完成时记录/清空 completedAt |
| `ui/home/HomeViewModel.kt` | 已完成拆分「今日已完成」与「之前已完成」 |
| `ui/home/HomeScreen.kt` | 加载指示器；已完成分组展示；滑动阈值 65%；Snackbar 撤销；滑动背景提示 |
