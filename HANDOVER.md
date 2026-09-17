# 过期管家 (ExpiryGuard) 项目交接文档

> 本文档面向接手者，目标：30 分钟内从零构建并运行首个 debug APK。

## 1. 项目概述

- **定位**：一款基于 Android Jetpack Compose 的本地过期提醒应用，通过拍照/手动输入记录商品保质期，到期前自动提醒并区分「可退货 / 可下架 / 已过期」状态。
- **技术栈**：Kotlin 2.0.21 + Jetpack Compose (Material3) + Hilt (DI) + Room (本地数据库) + DataStore + WorkManager + CameraX + Coil + Vico(图表)
- **minSdk**: 26 | **targetSdk**: 35 | **compileSdk**: 35
- **包名**：`com.expiryguard.app`
- **应用名**：过期管家
- **当前版本**：`versionCode=3` / `versionName=1.0.2`
- **最新 commit**：`d029bea`（feat: 落地四项用户反馈需求：名称非必填 / 日期取月日默认今年 / 可下架状态 / 两阶段处理）
- **远程仓库**：`https://github.com/Forinxy/get-end-data`

## 2. 开发环境

| 项目 | 要求 |
|------|------|
| OS | Linux / macOS / Windows (WSL2) |
| JDK | OpenJDK 17（推荐 Eclipse Temurin 或 OpenJDK 17.0.x） |
| Android SDK | compileSdk 35，build-tools 35.0.0，platform-tools |
| Gradle | 8.9（由 wrapper 自动下载） |
| 第三方 Key | 本项目离线运行，无需后端 / API Key；若后续接云同步可在此处填写 `<YOUR_API_KEY>` |

> 本仓库的 `.github/workflows/build-apk.yml` 使用 Temurin 17 + ubuntu-latest，可一键 CI 出包。

## 3. 构建运行

```bash
# 克隆
git clone https://github.com/Forinxy/get-end-data.git ExpiryGuard
cd ExpiryGuard

# 配置 Android SDK 路径（可选，CI 或内网可省略）
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 赋予 gradlew 执行权限（Windows 可跳过）
chmod +x gradlew

# 构建 Debug APK
./gradlew :app:assembleDebug --no-daemon

# 产出路径
# app/build/outputs/apk/debug/app-debug.apk
```

- **已知坑**
  - 国内网络访问 Google Maven 偶发超时：`settings.gradle.kts` 已内置阿里云镜像作为优先源，默认已可用。
  - GitHub Actions 中若依赖下载失败，workflow 已配置 temurin 17 并自动从镜像拉取 Gradle（见下节）。
  - 首次构建耗时约 5~10 分钟（全量下载依赖），后续增量构建 30s 内。
- **GitHub Actions 构建**：`.github/workflows/build-apk.yml`，触发条件为 push 到 `main`/`master` 或手动 workflow_dispatch。构建完自动创建 Release 并上传 APK + 源码包。

## 4. 依赖与镜像

| 官方源 | 镜像地址 |
|--------|----------|
| Google Maven | `https://maven.aliyun.com/repository/google` |
| Maven Central | `https://maven.aliyun.com/repository/central` |
| Gradle 发行版 | `https://mirrors.cloud.tencent.com/gradle/` |

- 构建脚本默认启用 mirror（`settings.gradle.kts` 已配置 `maven.aliyun.com/repository/google`、`maven.aliyun.com/repository/public`、`maven.aliyun.com/repository/gradle-plugin`）。
- 如遇 GitHub 依赖（如 AGP、KSP、Hilt 插件）下载失败，可按顺序尝试：
  1. 重试（网络抖动）
  2. 检查代理：`http://proxy.example.com:7890`（若企业内部有代理）
  3. 使用腾讯云镜像替换 Gradle wrapper：在 `gradle/wrapper/gradle-wrapper.properties` 把 `distributionUrl` 改为 `https://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip`
  4. 手动下载 jar 放至 `~/.gradle/wrapper/dists/gradle-8.9-bin/<hash>/gradle-8.9/lib/`

## 5. 本地依赖服务

- **无后端服务**：数据完全本地存储在 Room 数据库（SQLite）。
- 通知提醒由 WorkManager + `NotificationHelper` 驱动，需要设备允许应用发送通知和调度精确闹钟。
- 权限需求：相机（拍照识商品）、读取媒体（选相册图）、通知、精确闹钟、振动、开机自启。

## 6. 开发进度

| 状态 | 内容 |
|------|------|
| ✅ 已完成 | 四模块（首页待办、商品列表、统计、回收站）+ 设置（深色模式、备份恢复、分类管理、保质期组、关于、通知设置）+ 定时 Worker + 通知 + 镜像扫描入口 |
| ✅ 进行中 | 无 |
| 🚫 已搁置 | 无 |
| 最近可运行 commit | `d029bea` |

## 7. 待开发内容

| 功能 | 优先级 | 预估工时 | 前置依赖 |
|------|--------|----------|----------|
| 云同步 / 多端数据打通 | P1 | 3~5 天 | 后端接口 |
| 商品条形码 OCR 识别（本地 / 云端） | P2 | 2~3 天 | ML Kit 接入或后端服务 |
| 订阅 / 自动续订商品功能 | P2 | 1~2 天 | 商品模型扩展 |
| 国际化（i18n） | P3 | 1 天 | 字符串资源抽离 |
| 单元测试 / UI 测试 | P2 | 2~3 天 | 稳定核心逻辑 |

## 8. 架构与关键模块

- **架构模式**：MVVM + Hilt DI + Compose Navigation
- **核心入口**：
  - `ExpiryGuardApp.kt`：Application，初始化 NotificationChannel 与 WorkManager WorkerFactory
  - `MainActivity.kt`：Activity 启动点，Compose 根节点，负责初始化 DataStore、触发 ReminderScheduler、注册 WorkManager worker
- **关键模块**
  - `data/db/`：Room DAO + Entity，`ProductRepository` 封装 CRUD
  - `domain/engine/ExpiryRuleEngine.kt`：状态机核心，输入 `shelfLifeDays` + `expiryDate` 输出 `ProductStatus`（Safe / ExpiringSoon / Returnable / TakeDown / Expired）
  - `domain/model/ProductStatus.kt`：状态枚举，`TakeDown(remainingDays)` 为新加状态
  - `notification/`：`NotificationHelper`（通知渠道/发送）、`ReminderScheduler`（调度）、`ExpiryReminderWorker`（WorkManager 定时任务）
  - `ui/home/`：今日待办列表（滑动标记下架/退货/完成）
  - `ui/product/`：商品增删改查、列表筛选（按状态：全部 / 可下架 / 即将到期 / 可退货 / 已过期）
  - `ui/settings/`：6 个设置子页面
  - `ui/stats/`：饼图 + 条形图统计
  - `ui/trash/`：回收站（按颜色区分已下架/已退货/其他）
- **已知技术债**
  - 部分 Compose `Icons.Filled.ArrowBack` 已弃用，应改用 `Icons.AutoMirrored.Filled.ArrowBack`（警告不影响运行）
  - `statusBarColor` 已弃用，建议迁移至 `WindowInsetsControllerCompat`

## 9. 代码概览与已知问题

- 结构：`app/src/main/java/com/expiryguard/app/` 下按 data/domain/ui 三层；资源在 `res/`。
- 致命/严重问题：**无**。本次交付已通过 `./gradlew assembleDebug` 编译验证（耗时 ~5m24s，仅 warnings 无 errors）。

## 10. 架构简评

- **评价**：良好。分层清晰（data/domain/ui），DI（Hilt）与状态（ViewModel + StateFlow）规整，规则引擎独立便于测试。
- **改进方向**
  1. 状态计算与 UI 耦合较少，但部分 Screen 内重复调用 `ExpiryRuleEngine.calculateStatus()`，可封装为扩展或 ViewModel 派生属性。
  2. `CategoryManagementScreen` / `ShelfLifeGroupManagementScreen` 逻辑偏重，可拆分为独立 ViewModel。
  3. 通知提醒时机可细化（到期当天 vs 提前 2 天等差异化策略）。
  4. 缺少单测覆盖，建议为 `ExpiryRuleEngine` 补充 JUnit 用例。
  5. 备份恢复涉及 FileProvider + 外部存储，可补充边界 case 测试（无 SD 卡、权限拒绝）。

## 11. 测试建议

- **核心流程**
  - 添加商品 → 首页展示 → 滑动标记完成 / 下架 / 退货处理
  - 修改日期为「明天」→ 推送「可下架」通知 → 滑动下架后从今日待办消失
  - 修改日期为「20 天前」→ 进入「可退货」→ 滑动退货处理后消失
  - 已过期商品出现在列表并标红
- **重点模块**：`ExpiryRuleEngine` 边界（零天/负数/闰年 2/29/月份仅输入）；WorkManager 触发时机（系统时钟变更、设备重启）；备份恢复（跨版本兼容）。
- **边界问题**：`DatePickerDialog` 在闰年 2/29 年份非闰年时回退到 2/28；`parseDate("3")` 月份-only 当前按当月 1 号处理，如需提示月份范围需补充。

## 12. 账号与密钥

- 本项目离线运行，无需后端 / 第三方 Key。
- 若后续接入：
  - 云同步后端：`<YOUR_BACKEND_URL>`
  - ML Kit 条形码识别：`<YOUR_GMS_API_KEY>`
  - 统计图表服务（如接）：`<YOUR_API_KEY>`

## 13. 常见问题

| 现象 | 解法 |
|------|------|
| `java: command not found` | 安装 JDK 17：`apt install openjdk-17-jdk-headless` 或 `brew install openjdk@17` |
| `sdkmanager not found` / `ANDROID_HOME empty` | 安装 Android SDK Command Line Tools，设置 `export ANDROID_HOME=$HOME/Android/Sdk` 并在 `local.properties` 写入 `sdk.dir=<路径>` |
| Gradle 下载超时 | 改 `distributionUrl` 到腾讯云镜像（见第 4 节） |
| KSP/Hilt 注解处理器失败 | 确认 `ksp` 插件版本与 Kotlin 2.0.21 对齐；清理 `build/` 后重试 |
| `Unable to strip .so` 警告 | 正常，不影响运行；release 包需签名才能安装 |
| 真机调试「INSTALL_FAILED_UPDATE_INCOMPATIBLE」 | 卸载旧包或换设备；debug keystore 不同会导致安装冲突 |
| GitHub Actions 构建超时 | 默认 timeout 6 分钟，可增大或在 `workflow_dispatch` 时传入 `--max-workers=2` |

## 14. 验收标准

- [ ] clone 仓库后执行 `./gradlew :app:assembleDebug --no-daemon` 成功产出 `app/build/outputs/apk/debug/app-debug.apk`
- [ ] 安装 APK 到真机/模拟器，能打开首页看到今日待办列表
- [ ] 添加一个临期商品，能触发「可退货」「可下架」徽标与通知
- [ ] 滑动「下架」后该商品从今日待办消失；再次添加并滑动「退货处理」同样消失
- [ ] push 到 `main` 后 GitHub Actions 自动构建，Release 页出现 APK + 源码 zip

---
*最后更新：2026-08-21*
*Commit: d029bea*
*仓库：https://github.com/Forinxy/get-end-data*