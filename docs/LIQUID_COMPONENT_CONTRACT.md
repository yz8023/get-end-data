# LiquidUI 组件契约（屏幕重写专用）

本文档是 ExpiryGuard 13 屏重写为 LiquidUI 液态玻璃设计语言的唯一依据。
重写目标：**功能 1:1 完整保留，视觉统一为 glass 语言**。禁止删减功能、禁止改动 ViewModel 调用。

## 硬性规则

1. 只允许改 `ui/*/` 下的 *Screen.kt 文件（可含文件内私有 Composable），**禁止修改**任何 ViewModel、domain、data、navigation、theme 主体逻辑。
2. 屏幕结构（Scaffold/topBar/bottomBar/snackbar 宿主）保持不动，除非本契约明确要求换壳。
3. 所有功能入口、导航参数、ViewModel 状态字段、ProductStatus 语义、DateUtils 格式化、completedType 常量必须原样保留。
4. 替换后必须保留同等功能语义（如 switch 开关、输入框校验、日期选择、批量操作、弹窗确认）。
5. 若某控件 liquid 无等价物，保留 Material3 原控件，不要硬造。
6. 删除 `@OptIn(ExperimentalMaterial3Api::class)` 仅当该文件内不再使用 Experimental API（如 TopAppBar/DatePicker/Slider/ModalBottomSheet 被移除）。

## 状态色（保持不变）

- Safe = `Green500`，ExpiringSoon = `Yellow500`，Returnable = `Blue500`，TakeDown = `Red500`（可闪烁），Expired = `Red700`，已完成 = `Green500`
- 这些常量来自 `com.expiryguard.app.ui.theme`。

## Material3 → Liquid 映射表

| Material3 原控件 | 替换为 liquid | 说明 |
|---|---|---|
| `Button` | `PrimaryButton` | 内容用 `{ Text("...") }` 包裹 |
| `OutlinedButton` / 次要 `Button` | `SecondaryButton` | 同上 |
| `Switch` | `CompactSwitch` | 拖拽物理，写法同参数 |
| `OutlinedTextField` | `GlassTextField` | `label/placeholder/supportingText` 是 `String?`（非 composable） |
| `FilterChip` / 单选过滤组 | `SegmentedTabs(labels, selectedIndex, onSelected)` | 滑动 thumb |
| `AlertDialog` / `MaterialAlertDialog` | `GlassDialog(onDismissRequest, title, onConfirm, modifier, confirmText, dismissText, confirmEnabled) { ... }` | 宿主已在 AppRoot，直接调用即可 |
| `Card`（列表/设置项外壳） | `PreferenceCard`（设置类）或 `GlassCard`（业务卡片） | 见旧组件层 |
| `IconButton`（顶栏操作） | `GlassIconButton(icon, contentDescription, onClick, tint)` | 44dp 玻璃圆钮 |
| `FloatingActionButton` | `PrimaryButton` 或 `GlassIconButton`（按语义） | 若在主内容区用全宽 PrimaryButton |
| `Checkbox` | 无等价物 → 保留 Material3 Checkbox | 多选场景 |
| `Slider` | 无等价物 → 保留 Material3 Slider | 外观旋钮场景 |
| `DatePicker` / `TimePicker` | 无等价物 → 保留 Material3 | 日期选择场景 |
| `ModalBottomSheet` | 无等价物 → 保留，但 Sheet 内容容器改用 `GlassCard` 语言 | 详情/操作底部弹层 |
| `TopAppBar` | 旧 `GlassTopAppBar`（已委托 liquidGlass） | 见旧组件层 |
| 普通 `Text` + 纯色圆点标签 | `Tag(text, accent)` 或旧 `StatusBadge` | 见旧组件层 |

## liquid 组件精确签名（`com.expiryguard.app.ui.liquid.components`）

### 按钮
```kotlin
fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit)
fun SecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit)
fun GlassIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface)
```

### 开关
```kotlin
fun CompactSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)
```

### 输入框
```kotlin
fun GlassTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String? = null, placeholder: String? = null, supportingText: String? = null,
    enabled: Boolean = true, singleLine: Boolean = true, isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
)
```
注意：`label`/`placeholder`/`supportingText` 全部是 `String?`，传字符串即可；要显示横线视觉变换用 `PasswordVisualTransformation()`。

### 分段选择 / 标签 / 进度
```kotlin
fun SegmentedTabs(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier)
fun Tag(text: String, accent: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier)
fun Meter(progress: Float, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary)
fun GradientBar(colors: List<Color>, fraction: Float, onFractionChange: (Float) -> Unit, modifier: Modifier = Modifier)
```

### 设置行 / 卡片
```kotlin
fun SectionTitle(text: String, modifier: Modifier = Modifier)
fun PreferenceRow(icon: ImageVector, title: String, modifier: Modifier = Modifier, subtitle: String? = null, enabled: Boolean = true, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null)
fun PreferenceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit)
fun DisplayTitle(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.displayLarge)
fun SupportingText(text: String, modifier: Modifier = Modifier)
```

### 反馈
```kotlin
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null)
fun InfoBanner(text: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary)
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary)
```

### 对话框
```kotlin
fun GlassDialog(
    onDismissRequest: () -> Unit, title: String, onConfirm: () -> Unit,
    modifier: Modifier = Modifier, confirmText: String = "确定", dismissText: String? = "取消",
    confirmEnabled: Boolean = true, content: @Composable ColumnScope.() -> Unit
)
```
- `GlassDialog` 同窗口覆盖层，宿主在 AppRoot，屏幕内直接调用即可（会被自动渲染在 tab 栏之上）。
- `dismissText = null` 时隐藏取消按钮。
- 需要从屏幕移除自身时，在 `onConfirm`/`onDismissRequest` 回调里把触发状态置回 false。

### 玻璃修饰与容器
```kotlin
fun Modifier.liquidGlass(shape: Shape, tint: Color? = null, elevation: Dp = 2.dp, borderAlpha: Float = 0.3f)
fun GlassCard(modifier: Modifier = Modifier, shape: Shape, tint: Color? = null, ...)  // liquid 的 GlassCard 与旧组件层不同名包，勿混淆
fun GlassPill(...)
```
- 业务屏幕优先复用旧组件层（见下节），不要直接 import liquid 的 GlassCard。

## 旧组件层（`com.expiryguard.app.ui.components`，内部已 liquid 化，API 不变）

屏幕内已在使用、**无需改动调用**，直接复用：

```kotlin
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit)
fun GlassCard(modifier: Modifier = Modifier, selected: Boolean = false, onClick: (() -> Unit)? = null, shape: RoundedCornerShape = RoundedCornerShape(16.dp), elevation: Dp = 4.dp, statusColor: Color? = null, content: @Composable () -> Unit)
fun GlassCardSmall(modifier: Modifier = Modifier, selected: Boolean = false, onClick: (() -> Unit)? = null, statusColor: Color? = null, content: @Composable () -> Unit)
fun GlassCardGlow(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(16.dp), glowColor: Color = Color.White, content: @Composable () -> Unit)
fun GradientTopBar(modifier: Modifier = Modifier)
fun GlassTopAppBar(title: String, modifier: Modifier = Modifier, navigationIcon: @Composable (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, scrollBehavior: TopAppBarScrollBehavior? = null, colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors())
fun EmptyState(message: String, icon: ImageVector, actionText: String, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier)   // 注意：旧 EmptyState 参数顺序 title/message 不同，勿与 liquid EmptyState 混淆
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier)
fun DateInputField(label: String, hint: String, value: Long?, accentColor: Color, onDateSelected: (Long) -> Unit, onDateCleared: () -> Unit, modifier: Modifier = Modifier)
fun DatePickerDialog(initialDate: LocalDate = LocalDate.now(), onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit)
fun ProductCard(product: ProductEntity, status: ProductStatus, categoryName: String = "", isCompleted: Boolean = false, onClick: () -> Unit)
fun StatusBadge(status: ProductStatus)
fun StatusBadgeCompleted(label: String = "已处理")
```

## 各类屏幕替换指引

### 设置类（Settings/Notification/About/BackupRestore/CategoryMgmt/ShelfLifeGroupMgmt）
- 分组用 `SectionTitle`；每项用 `PreferenceCard { PreferenceRow(...) }`；开关用 `CompactSwitch` 放 trailing；跳转/点击行用 `onClick`；输入用 `GlassTextField`；确认用 `GlassDialog`。
- 列表编辑（分类/保质期组）增删改弹窗：`GlassDialog` + 内部 `GlassTextField`。

### 首页/列表类（Home/ProductList/Stats/Trash）
- 保留现有 Scaffold + GlassTopAppBar + 底部统计等布局结构；卡片统一旧 `GlassCard`/`ProductCard`/`StatCard`/`StatTile`。
- 顶部过滤 Chip 组 → `SegmentedTabs`；切换按钮 → `GlassIconButton`；空态 → 旧 `EmptyState`。
- 批量操作确认 → `GlassDialog`。
- 详情底部弹层 → 保留 ModalBottomSheet，sheet 内容器用 `GlassCard`。

### 表单类（AddProduct/EditProduct）
- `OutlinedTextField` → `GlassTextField`（label 传字符串）；日期选择保持 `DateInputField` + `DatePickerDialog`；保质期预设选择用 `SegmentedTabs` 或保留原 Chip；保存按钮 → `PrimaryButton`（enabled 绑定校验）；图片选择按钮 → `GlassIconButton` 或 `SecondaryButton`。

## 编译验证（改动后必跑）

```bash
cd /workspace && JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew :app:compileDebugKotlin --no-daemon
```
环境 cgroup 内存限制 3.89GiB，务必 `--no-daemon`。失败时读取报错修复到通过为止。
