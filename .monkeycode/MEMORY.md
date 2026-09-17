# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

### User Instruction Entry
User instruction entries should follow this format:

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

### Project Knowledge Entry
Entries discovered by the Agent during task execution should follow this format:

[Project Knowledge Summary]
- Date: [YYYY-MM-DD]
- Context: Discovered by Agent while performing [specific task description]
- Category: [Operations & Deployment|Build Methods|Testing Methods|Troubleshooting & Debugging|Workflow & Collaboration|Environment Configuration]
- Instructions:
  - [Specific knowledge points, described line by line]

## Deduplication Strategy
- Before adding a new entry, check for similar or identical instructions.
- If a duplicate is found, skip the new entry or merge it with the existing one.
- When merging, update the context or date information.
- This helps avoid redundant entries and keeps the memory file tidy.

## Entries

[Project Knowledge Summary]
- Date: 2026-09-17
- Context: Discovered by Agent while porting ExpiryGuard 到 Liquid Motion UI 玻璃设计系统
- Category: Build Methods
- Instructions:
  - 构建栈必须完全镜像 LiquidUI 模板：Kotlin 2.3.10 / AGP 8.7.3 / Gradle 8.11.1 / Compose BOM 2024.10.01 / compileSdk 36；backdrop 库是 Kotlin 2.3 元数据，升 Kotlin 是硬前提
  - 玻璃层依赖 io.github.kyant0:backdrop-android:2.0.0-alpha03 + io.github.kyant0:shapes-android:1.2.0；com.kyant.shapes.Capsule 来自 shapes-android
  - KSP 新版本号直接对齐 Kotlin（2.3.10）：`ksp("com.google.devtools.ksp:ksp-gradle-plugin:2.3.10")`，勿用旧 `<kotlin>-<ksp>` 形式
  - 设计系统以 com.expiryguard.app.ui.liquid.* 内嵌到业务包，sed 重命名包名 com.monkeycode.liquidui.ui. → com.expiryguard.app.ui.ui.liquid 可能出错，需检查实际替换结果（本任务用 ui.liquid.）
  - 根节点 aurora 作为可见背景+被采样 backdrop（layerBackdrop）；页内玻璃面板采样 LocalGlassBackdrop；悬浮底栏采样 rememberCombinedBackdrop(aurora, content) 且在内容层之外绘制避免采样反馈
  - AppSettingsState（ui/liquid/screens/）以 SharedPreferences 文件 "liquidui.settings" 持久化外观旋钮（glassOpacity/glassRefraction/motionDamping/palette/visualMode 等），可用 rememberAppSettingsState() 直接读写

[User Instruction Summary]
- Date: 2026-08-14
- Context: 用户反馈重新安装后记录全部丢失，并约定发布命名规范
- Instructions:
  - GitHub Release 发布的 APK 命名规范为「应用名+版本号.apk」，例如 ExpiryGuard_1.0.1.apk，不再使用含 debug/unsigned 后缀的命名
  - 每次发布 GitHub Release 时，必须同时附带源码压缩包（应用名+版本号+源码.zip 或类似命名）
  - 用户重视数据不丢失：应用需提供可靠的备份/恢复能力，数据库升级必须使用显式 Migration 而非 destructive 兜底

[Project Knowledge Summary]
- Date: 2026-08-14
- Context: Discovered by Agent while diagnosing 用户重新安装后数据丢失问题
- Category: Troubleshooting & Debugging
- Instructions:
  - 卸载应用会删除本地 Room 数据库（expiry_guard.db），重新安装后数据必然丢失，需依赖应用内「备份与恢复」功能导出/导入数据
  - 该应用旧版本数据库版本为 v1/v2/v3，若从 v2/v1 直接升级到 v4 且无完整迁移链，会触发 fallbackToDestructiveMigration 清空数据
  - 排查数据丢失时先对比新旧 APK 签名（apksigner verify --print-certs）排除签名变更导致的卸载重装

[Project Knowledge Summary]
- Date: 2026-08-16
- Context: Discovered by Agent while implementing v1.0.2 数据安全与状态引擎改进
- Category: Troubleshooting & Debugging
- Instructions:
  - 数据库无迁移链的极老版本（v1/v2）无法补 Migration（Room schema 校验失败会崩溃而非兜底），改用「破坏前自动备份」：buildDatabase 构建前检测版本<3 旧库，复制到应用目录并导出 Download/ExpiryGuard/，destructive 兜底触发前数据可找回
  - ExpiringSoon 状态已启用：退货阈值=0 的短保质期产品（保质期<90天）在剩余 4~30 天返回该状态，填补安全→紧急预警断层；列表「即将到期」筛选按状态匹配而非 days in 1..3
  - 首页待办与启动通知共用 ExpiryRuleEngine.computePendingGroups() 统一口径（今日到期+可退货+已过期+预警，已去重）

[Project Knowledge Summary]
- Date: 2026-08-23
- Context: Discovered by Agent while implementing 长按下架↔退货切换 + 日期年份可选
- Category: Build Methods
- Instructions:
  - 数据库 v5 迁移：products 表新增 completedType 字段（TEXT），取值 TAKE_DOWN=已下架 / RETURN=已退货处理 / null 未完成；旧数据 completedType 为 null 时 completedLabelFor 回退到按状态推断
  - 长按切换逻辑：已完成商品在「已下架」↔「已退货处理」之间切换 completedType；未完成商品按当前状态写入对应 completedType
  - 滑动标记完成时也应写入 completedType（按状态），保证列表/首页标签一致
  - 日期选择器 yearRange 放开至 now.year±100，初始日期保留原年份（不再强制今年）

[Project Knowledge Summary]
- Date: 2026-08-23
- Context: Discovered by Agent while fixing 用户数据丢失 + 备份恢复失败
- Category: Troubleshooting & Debugging
- Instructions:
  - 严重教训：fallbackToDestructiveMigration() 在迁移链断裂时会清空全部数据，绝不能用于生产环境
  - 旧代码仅在 version < 3 时备份，v4→v5 升级未受保护；改为 backupDatabaseOnUpgrade：任何版本变化都先备份旧库到 filesDir/legacy_db_backup 和 Download/ExpiryGuard
  - 数据库升级安全三原则：① 永不使用 destructive 兜底 ② 升级前自动备份旧库文件 ③ 备份文件导出到公共目录供用户手动找回
  - 恢复失败可能原因：备份 JSON 字段与当前实体不兼容（Gson JsonSyntaxException）；已增加详细错误提示

[Project Knowledge Summary]
- Date: 2026-08-26
- Context: Discovered by Agent while implementing 滑动=下架、长按=已处理 语义
- Category: Workflow & Collaboration
- Instructions:
  - 标记手势语义：滑动一律标记「已下架」(TAKE_DOWN)；长按标记「已处理」(null)，已标记时长按在「下架↔已处理」间切换
  - 首页 TaskCard 滑动提示统一为「滑动标记下架」；详情弹层按钮统一为「标记已处理」；不再按商品状态推断 completedType（已删除 markActionLabel/completionTypeFor 辅助函数）
  - ProductListViewModel.swipeComplete 统一传 null（已处理），不再按状态推断

[Project Knowledge Summary]
- Date: 2026-09-17
- Context: Discovered by Agent while fixing 「设置/关于界面操作无效」bug
- Category: Troubleshooting & Debugging
- Instructions:
  - LiquidUI 外观旋钮必须通过唯一 AppSettingsState 实例读写：参考版在 MainActivity 创建一次向下传递；本工程用 CompositionLocal（LocalAppSettings，ExpiryGuardTheme 根节点 provide）共享同一实例，切勿在设置界面另建 rememberAppSettingsState()，否则改动只写 SharedPreferences、主题桥接层不重组、界面操作看似无效
  - 版本号勿硬编码：AboutScreen 与 SettingsViewModel.appVersion 应从 PackageManager 读 versionName（getPackageInfo(...).versionName ?: "1.0.0"），与 build.gradle.kts versionName 保持一致

[Project Knowledge Summary]
- Date: 2026-09-17
- Context: Discovered by Agent while fixing v1.1.1 发布后反馈：恢复失败 ClassCastException + 底栏下方白色底层
- Category: Troubleshooting & Debugging
- Instructions:
  - 恢复失败根因：Kotlin 对 `Gson.fromJson(data, BackupData::class.java) ?: throw` 的泛型 T 推断成 java.lang.Void，字节码 fromJson 后紧跟 `checkcast java/lang/Void`，任何非 null 返回都抛 ClassCastException，且该异常是 RuntimeException 不被 try 的 JsonSyntaxException catch 捕获；修复：先 `val parsed: BackupData = gson.fromJson(...)` 显式声明局部变量类型，再判 null
  - LiquidUI 玻璃底栏正确接线范式（对齐参考版 AppRoot）：Scaffold 的 bottomBar 在 glassEnabled 时仅渲染胶囊（不放高度占位）；NavHost bottom padding 设为 0，内容全屏延伸到底；悬浮胶囊 align(BottomCenter).padding(bottom=14.dp) 且不用 navigationBarsPadding；各 Tab 页滚动容器自留 contentPadding bottom=108.dp 防胶囊遮挡
  - 若保留 Scaffold bottomBar 高度占位，玻璃胶囊只采样到空白背景（浅色 AuroraLight 含纯白 0xFFFFFFFF），底栏下方呈现白色底层

[Project Knowledge Summary]
- Date: 2026-09-17
- Context: Discovered by Agent while implementing 需求3（莫奈取色不应改变整站视觉风格）
- Category: Troubleshooting & Debugging
- Instructions:
  - 莫奈取色与视觉风格必须解耦：ExpiryGuardTheme 桥接层勿把 monetEnabled 硬映射成 VisualMode.Md3（会把整站玻璃换成纯 Material）；正确做法是 LiquidUITheme 新增 useDynamicColors 参数，colorScheme 判定 `(visualMode == VisualMode.Md3 || useDynamicColors) && SDK >= S` 时用动态色，玻璃渲染仍由 chrome.visualMode 决定；bridge 层传 useDynamicColors = monetEnabled 且 visualMode 恒为 settings.visualMode
  - 统计页「清单详情」列表（ProductDetailRow + 它独占的 getStatusColor/getStatusLabel 辅助函数）删除后需同步清理未用 import（如 TextOverflow），避免 dead code
  - 首页「已处理」过滤必须按时间当天过滤：processedCompleted 加 `DateUtils.isToday(it.completedAt ?: 0L)` 条件，仅下架（takeDownCompleted）不受限
  - SettingsScreen 无极调色用到 `HSVToColor`/`colorToHSV`，需补 `import androidx.compose.ui.graphics.toArgb`；BackupRestoreScreen 新增模式选择器需补 `androidx.compose.foundation.border` 与 `BorderStroke` import

[Project Knowledge Summary]
- Date: 2026-09-17
- Context: Discovered by Agent while fixing 用户反馈「主色调不可滑动」「莫奈取色做错」
- Category: Troubleshooting & Debugging
- Instructions:
  - HueSaturationPicker 色相滑杆的 value 是 HSV 色相角（0~360°），必须显式 `valueRange = 0f..360f`；漏设则落到 Slider 默认 0f..1f 范围，色相滑杆无法正常工作
  - 「莫奈配色」=莫奈系列预设调色板（印象派睡莲柔和色调），不是系统动态取色/Material You；实现为 PaletteId.Monet + PaletteSeeds 条目（MonetLight/DarkPrimary 等常量在 ui/liquid/theme/Color.kt）即可，勿接入 dynamicColorScheme
  - 移除动态取色链路要同步清理四处：LiquidUITheme 的 useDynamicColors 参数与 colorScheme 判定、ExpiryGuardTheme 的 monetEnabled 参数、MainActivity 的 KEY_MONET/monetFlow、SettingsViewModel 的 KEY_MONET_ENABLED/isMonetEnabled/toggleMonet 与 SettingsScreen 的 switch_monet 卡片
  - GitHub Release 源码包文件名必须用 ASCII（`_source.zip`），softprops/action-gh-release@v2 上传中文名（`_源码.zip`）会报 `Error creating asset temp dir` 导致整个 CI 步骤失败；workflow 里 Package source 步骤的 zip 名与 Create GitHub Release 的 files/body 引用都要同步改
