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
