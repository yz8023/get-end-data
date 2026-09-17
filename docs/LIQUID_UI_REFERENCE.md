# Liquid Motion UI 参考设计系统 — 复用开发文档

> 供 ExpiryGuard 工程师将已分解出的 13 个业务屏幕正确无误地套用到 LiquidUI 液态玻璃设计系统上。
> 参考源：`/tmp/liquid-ref/android/app/src/main/java/com/monkeycode/liquidui/ui/`（包名 `com.monkeycode.liquidui`）。
> ExpiryGuard 落点：`/workspace/app/src/main/java/com/expiryguard/app/ui/liquid/`（包名 `com.expiryguard.app.ui.liquid`）。
> 两者除包名外，绝大多数声明逐字节一致（已验证 diff）。

目录
1. 顶层组件与导航壳（AppRoot / Screen / 底部栏 / 对话框）
2. `motion/` 公开 API（Motion token、Modifiers、DampedDragAnimation、InteractiveHighlight、inspectDragGestures）
3. `theme/` token（Shape / Type）
4. `components/` 公开 API 清单
5. 组件组合范例（ComponentsScreen / MotionScreen 的用法）
6. AppIcons 图标清单
7. 参考 UI vs 工作区 `ui/liquid` API 差异审计（含被删改项 + 工作区新增项）
8. 13 个业务屏幕接入指引（切换动画、底部栏、玻璃服务、token 使用规范）

---

## 1. 顶层导航壳与全局服务

### 1.1 `Screen.kt` — 四个目的地的枚举

```kotlin
enum class Screen(val title: String, val subtitle: String, val icon: ImageVector) {
    Home("首页", "设计系统概览", AppIcons.BlurOn),
    Components("组件", "控件与状态", AppIcons.Widgets),
    Motion("动效", "交互物理演示", AppIcons.Animation),
    Settings("设置", "主题与偏好", AppIcons.Settings)
}
```

- 用枚举字段控制底部栏 tab 的数量、标题、图标。业务若要加第 5 个 tab，往这里加一行即可。
- 目前有两个顶层服务文件被 ExpiryGuard 移入自己的导航系统（见 §7），屏幕上 `Screen` 仅是模板壳用途。

### 1.2 `AppRoot.kt` — App 外壳（关键公共契约）

```kotlin
@Composable
fun AppRoot(settings: AppSettingsState)
```

内部结构（`AppRoot.kt:73-212`）：

1. **两个 kyant`Backdrop` 层驱动玻璃**：
   - `auroraBackdrop = rememberLayerBackdrop { ... }`：记录装饰层（Aurora 或壁纸）。所有页内玻璃面板采样它。
   - `contentBackdrop = rememberLayerBackdrop { drawContent() }`：记录屏幕内容。液态底栏采样 `rememberCombinedBackdrop(auroraBackdrop, contentBackdrop)`，让滚动内容在底栏下折射。
   - `fullSceneBackdrop = if (glassEnabled) rememberCombinedBackdrop(auroraBackdrop, contentBackdrop) else null` —— 供给对话框遮挡层。

2. **向整棵树提供三个 CompositionLocal**：
   ```kotlin
   LocalGlassBackdrop provides (if (glassEnabled) auroraBackdrop else null)  // Glass.kt:130
   LocalGlassContentBackdrop provides fullSceneBackdrop                     // Glass.kt:140
   LocalGlassDialogHost provides dialogHost                                 // Dialogs.kt:63
   ```
   - 业务屏幕内的 `liquidGlass`（见 §4）会静默读取 `LocalGlassBackdrop`；`Null` 时退化为半透明 tint。
   - 对话框需在同一窗口内采样，因此用 `GlassDialogHost` 把 overlay `push/remove` 到根节点，由 `AppRoot` 在 `.items.forEach { key(it) { it() } }` 渲染。

3. **AnimatedContent 页面切换动画**（业务替换时建议照抄）：
   ```kotlin
   val bounceSpec = if (chrome.reducedMotion) tween<Float>(Motion.Duration.Fast)
                    else Motion.bouncy(0.001f)
   AnimatedContent(
       targetState = selectedIndex,
       transitionSpec = {
           (scaleIn(initialScale = 0.94f, animationSpec = bounceSpec) +
               fadeIn(animationSpec = tween(Motion.Duration.Fast))) togetherWith
               (scaleOut(targetScale = 0.97f, animationSpec = tween(Motion.Duration.Fast)) +
                   fadeOut(animationSpec = tween(Motion.Duration.Fast)))
       },
       label = "screenSwitch"
   ) { index -> /* when (screen) { ... } */ }
   ```

4. **底部栏用法**（玻璃态）：
   ```kotlin
   LiquidBottomTabs(
       selectedTabIndex = { selectedIndex },
       onTabSelected = { selectedIndex = it },
       backdrop = rememberCombinedBackdrop(auroraBackdrop, contentBackdrop),
       tabsCount = screens.size,
       modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
   ) {
       screens.forEachIndexed { index, screen ->
           LiquidBottomTab(onClick = { selectedIndex = index }, index = index) {
               Icon(imageVector = screen.icon, contentDescription = screen.title, modifier = Modifier.size(22.dp))
               Text(text = screen.title, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1,
                    softWrap = false, overflow = TextOverflow.Visible)
           }
       }
   }
   ```
   - 非玻璃态（`Normal`/`Md3`）走私有 `SolidBottomTabs`（`NavigationBar` + `NavigationBarItem`，默认实现）。
   - **关键点**：`LiquidBottomTabs` 的 `backdrop` 参数必须传**组合层**（aurora+content），否则底栏无法折射滚动内容。

5. **壁纸背景**：`WallpaperManager.getInstance(context).drawable` 读取失败（API31+ 需要权限）会回退到 `surfaceColor`/Aurora；启用壁纸时 aurora 仍会作为被采样层。

---

## 2. `motion/` 公开 API

### 2.1 `Motion.kt` — 动效 token 字典（`object Motion`）

**全局阻尼旋钮**（设置页滑块写它，全部 spring 通过它重调）：
- `val motionDamping = mutableFloatStateOf(0.5f)` — 0f..1f，越低越弹。
- `fun DampedRatio(base: Float): Float` — `(base * lerp(0.72f, 1.28f, d)).coerceIn(0.25f, 1.25f)`。**在组合期读取**，改设置即重生效。

**Duration 时间刻度**（ms）— `object Duration`：
| Token | 值 | 用途 |
|---|---|---|
| `Instant` | 90 | 光标/气泡反馈 |
| `Fast` | 180 | 悬停、按钮按下、开关 |
| `Standard` | 320 | 卡片展开、抽屉 |
| `Medium` | 460 | 分区出现、图片打开 |
| `Slow` | 700 | 页面进入、英雄区、路由切换 |
| `Cinematic` | 1200 | 冷开场、幕布扫过 |

**Easing 字典**（CubicBezierEasing）：
- `EaseOutExpo` (0.16,1,0.3,1) — 默认出场
- `EaseInExpo` (0.7,0,0.84,0) — 仅退场
- `EaseInOutQuart` (0.76,0,0.24,1) — 自动播放循环
- `EaseOutBack` (0.34,1.56,0.64,1) — 有冲量着陆，俏皮悬停
- `EaseOutCirc` (0,0.55,0.45,1) — 干脆减速
- `EaseSharp` (0.65,0,0.45,1) — 品牌强硬
- `EaseExpressive` (0.68,-0.6,0.32,1.6) — 两端过冲，慎用

**Spring 预设**（`visibilityThreshold: T? = null`，泛型）— 全签名：

```kotlin
fun <T> gentle(visibilityThreshold: T? = null): FiniteAnimationSpec<T>   // DampedRatio(0.90), stiffness 240
fun <T> standard(visibilityThreshold: T? = null): FiniteAnimationSpec<T> // DampedRatio(0.82), 520
fun <T> snappy(visibilityThreshold: T? = null): FiniteAnimationSpec<T>   // DampedRatio(0.76), 900
fun <T> bouncy(visibilityThreshold: T? = null): FiniteAnimationSpec<T>   // DampedRatio(0.46), 420
fun <T> heavy(visibilityThreshold: T? = null): FiniteAnimationSpec<T>    // DampedRatio(1.0),  180
fun <T> stiff(visibilityThreshold: T? = null): FiniteAnimationSpec<T>    // DampedRatio(0.90), 2200
```

**具体规格别名**（组件直接引用）：
- `Press: FiniteAnimationSpec<Float> = snappy(0.001f)`（按钮按下）
- `TabSlide: FiniteAnimationSpec<Float> = snappy(0.001f)`
- `PanelExpand: FiniteAnimationSpec<Float> = standard(0.01f)`
- `HeroReveal: FiniteAnimationSpec<Float> = gentle(0.01f)`
- `OffsetSnap: FiniteAnimationSpec<IntOffset> = snappy(IntOffset(1, 1))`

**Stagger 错峰刻度**（`object Stagger`，每项延迟 ms）：
- `TightGrid = 40`（密集数据格）
- `StandardList = 70`（卡片列表/功能格/导航链）— **默认**
- `EditorialLine = 90`（文本逐行出现）
- `DramaticCascade = 150`（获奖站英雄区大字）

**Follow 指数跟随**（`object Follow`，帧率无关，`tightness` 单位为 1/s）：
- `CursorTightness = 18f` / `CameraTightness = 4.5f` / `RailTightness = 12f` / `LevelTightness = 2.2f`
- 需要“到达并停住”的场景配 `standard`/`gentle`；要“带重量落地”用 `bouncy`/`snappy`。

**其他**：`const val CriticalDamping = Spring.DampingRatioNoBouncy`。

> 注意：参考库中**没有顶层 `swipeable` modifier** —— 液态底栏/开关的拖拽是由 `DampedDragAnimation` + `inspectDragGestures` 实现的（见 §2.2/2.4）。业务若要“可滑动”，直接复用这两者，不要引入 `Modifier.swipeable`。

### 2.2 `DampedDragAnimation.kt`

```kotlin
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    val valueRange: ClosedRange<Float>,
    visibilityThreshold: Float,
    initialScale: Float,
    val pressedScale: Float,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit
)
```

- 5 条独立弹簧驱动一次交互：`value`（活动胶囊位置，snappy 落位）、`velocity`（指针速度，回注 stretch）、`pressProgress`（0..1 按压 tint）、`scaleX`/`scaleY`（各向异性挤压，体积守恒：scaleX 除以速度项、scaleY 乘以小速度项）。
- 读取属性：`value`、`targetValue`、`pressProgress`、`scaleX`、`scaleY`、`velocity`、`isPressed`（仅 `press()`/`release()` 置位）。
- 已内置：`val modifier = Modifier.pointerInput(Unit) { inspectDragGestures(...) }` — 直接 `.then(dampedDragAnimation.modifier)`。
- 操作：`press()` / `release()` / `updateValue(Float)`（跟手）/ `animateToValue(Float)`（带互斥和到达等待）。
- 参考用法（`LiquidBottomTabs.kt`）：`DampedDragAnimation` 的 `canDrag` 用胶囊当前 `value * tabWidth` 换算全局触碰 X 是否在容器内；`onDragStopped` 里对 `targetValue.roundToInt()` 后 `animateToValue`；`onDrag` 里 `updateValue(targetValue + dragAmount.x / tabWidth)`。

### 2.3 `InteractiveHighlight.kt` — 玻璃面指针跟随高光

```kotlin
class InteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
)
```

- 在按压期间用 `BlendMode.Plus` 画两层：整面 flat wash（白 0.08×progress）+ 指尖径向“透镜”（白 0.16×progress，半径 `size.minDimension*0.72`）。两者随 `pressProgress` 淡入淡出。
- 读取 `pressProgress: Float`；挂 `modifier`（绘图层）与 `gestureModifier`（手势层，用 `inspectDragGestures`），需同时 `.then(highlight.modifier).then(highlight.gestureModifier)`。
- `position` 返回高光中心（参考里把胶囊位置换算成局部坐标；超界自动 `fastCoerceIn`）。

### 2.4 `DragGestureInspector.kt`

```kotlin
suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
)
```
- 按下**首帧即回报**（含一条 `dragAmount = Offset.Zero` 的“零距离拖拽”），绕过平台 touch-slop，让按压反馈始于落指。
- 手写循环支持多指换 pointer、`changedToUpIgnoreConsumed`；`onDragEnd` 返回 up 那一帧的 change。

### 2.5 `Modifiers.kt` — 手感动效原语

```kotlin
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.94f,
    enabled: Boolean = true
): Modifier
```
- 按下用 `Motion.snappy` 弹簧压扁，替代朴素 ripple；与 `clickable(interactionSource=…)` 组合使用（参考交互：传同一个 `MutableInteractionSource`）。

```kotlin
fun Modifier.impactShake(
    trigger: Any?,            // 改变该值即触发；null 永不触发
    amplitude: Float = 10f,
    durationMs: Int = 300
): Modifier
```
- X/Y 用不同频率（47Hz / 31Hz），包络线性衰减到 0 →「有冲击力」而非「抖成糊」；每次冲击只调一次。

```kotlin
@Composable
fun Modifier.idleBreathing(
    periodMs: Int = 5600,
    amplitudeX: Float = 0.012f,
    amplitudeY: Float = 0.020f
): Modifier
```
- 无限循环双频错相（`sin(phase)` / `sin(phase*0.84+0.7)`），画面永不静止。

```kotlin
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    staggerMs: Int = Motion.Stagger.StandardList,
    startDelayMs: Int = 60,
    translateYDp: Int = 18,
    content: @Composable () -> Unit,
)
```
- 内部包一个 `Box`，延迟 `startDelayMs + index*staggerMs` 后用 `Motion.gentle` 淡入上移。`enabled=false` 时终态为默认（“减动效”用户不会卡在不可见）。

---

## 3. `theme/` token

### 3.1 `Shape.kt`

Material 槽位（`val Shapes`）：
| 槽位 | 圆角 |
|---|---|
| `extraSmall` | 10dp |
| `small` | 14dp |
| `medium` | 18dp |
| `large` | 22dp |
| `extraLarge` | 28dp |

`object AppShape`（Material 五槽之外）：
- `card = RoundedCornerShape(22.dp)`
- `cardLarge = RoundedCornerShape(28.dp)`
- `control = RoundedCornerShape(17.dp)` — **输入框/按钮统一**
- `pill = RoundedCornerShape(percent = 50)`
- `sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`

### 3.2 `Type.kt`（`val Typography`，全部 `FontFamily.Default`）

| 槽位 | 字重 | 字号/行高 | 字距 |
|---|---|---|---|
| `displayLarge` | Bold | 52/56sp | -1.6sp |
| `displayMedium` | Bold | 40/44sp | -1.2sp |
| `displaySmall` | SemiBold | 32/38sp | -0.8sp |
| `headlineMedium` | SemiBold | 26/32sp | -0.4sp |
| `headlineSmall` | SemiBold | 21/28sp | -0.2sp |
| `titleLarge` | SemiBold | 18/24sp | 0sp |
| `titleMedium` | Medium | 16/22sp | 0.1sp |
| `bodyLarge` | Normal | 15/23sp | 0.2sp |
| `bodyMedium` | Normal | 13.5/20sp | 0.2sp |
| `bodySmall` | Normal | 12/17sp | 0.2sp |
| `labelLarge` | Medium | 13/18sp | 0.4sp |
| `labelMedium` | Medium | 11/15sp | 0.5sp |
| `labelSmall` | Medium | 10/14sp | 0.6sp |

display 层启用 `LineHeightStyle(Alignment.Center, Trim.None)`（近 1.0 行高、显示层微负字距）。

### 3.3 `Color.kt`
- 中性色（`Ink`/`Paper`/`Night` 系列）、品牌色种子（Mint/Sunset/Mono/Coral/Sakura/Galaxy 各 light/dark 三主色）、Aurora 背景装饰列表，全部 `internal`。
- **唯一 public 的高亮色**：`AuroraMint=0xFF4FD1A5`、`AuroraAmber=0xFFFFB65C`、`AuroraRose=0xFFFF7D93`、`AuroraViolet=0xFF9D8CFF`（组件展示/Tag/装饰用）。

### 3.4 `Theme.kt`
- `enum class PaletteId(label)`：Aurora(冰蓝)/Mint(薄荷)/Sunset(落日)/Mono(素灰)/Coral(珊瑚)/Sakura(樱花)/Galaxy(星夜) — **工作区版本不含 Custom**（审计见 §7）。
- `enum class ThemeMode { System, Light, Dark }`。
- `enum class VisualMode(label) { Glass(玻璃), Normal(普通), Md3(MD3) }` — AppRoot 据此决定是否走玻璃层。
- `@Immutable data class AppChrome(visualMode, glassOpacity=0.55f, glassRefraction=1f, cornerScale=1f, reducedMotion=false, motionDamping=0.5f, wallpaperBackground=false)`。
- `val LocalAppChrome = staticCompositionLocalOf { AppChrome() }`。
- `val supportsBackdropBlur: Boolean`（SDK_INT >= S）。
- `@Composable fun LiquidUITheme(themeMode=System, visualMode=Glass, palette=Aurora, chrome=AppChrome(), content: @Composable ()->Unit)` — 设置状态栏/导航栏、组合 `LocalAppChrome` + `MaterialTheme(typography=T, shapes=Shapes/scaledShapes())`。
- `@Composable fun rememberAppSettingsState(): AppSettingsState`（见 §5，位于 `screens/AppSettingsState.kt`）。

---

## 4. `components/` 公开 API 清单

### 4.1 `Glass.kt`
| 符号 | 签名 | 说明 |
|---|---|---|
| `AuroraBackground` | `@Composable fun (modifier: Modifier=Modifier, content: @Composable BoxScope.()->Unit)` | 装饰层（28s 周期径向 blob），玻璃面板折射它的视觉效果 |
| `LocalGlassBackdrop` | `staticCompositionLocalOf<Backdrop?> { null }` | 玻璃面板采样的背景层；Null → 退化为半透明 tint |
| `LocalGlassContentBackdrop` | `staticCompositionLocalOf<Backdrop?> { null }` | 全场景组合层（aurora+content），对话框采样它 |
| `Modifier.liquidGlass` | `(shape: Shape = shapes.large, tint: Color? = null, elevation: Dp=1.dp, borderWidth: Dp=1.dp, borderAlpha: Float=0.42f)` | ⭐ 全系统核心面板表面：vibrancy+blur+lens away from backdrop，高光边缘、阴影、按 `glassOpacity`/`glassRefraction` 折射；非 Glass 模式 = 纯色 Material；无 backdrop = tint 兜底 |
| `GlassCard` | `@Composable (modifier, shape, tint, contentPadding=PaddingValues(18.dp), content: ColumnScope.()->Unit)` | 玻璃卡片（标准内边距 18dp） |
| `GlassPill` | `@Composable (modifier, tint, content)` | 胶囊小徽章/芯片（pill 形状，borderAlpha 0.30） |

### 4.2 `Controls.kt`
| 符号 | 签名 | 说明 |
|---|---|---|
| `PrimaryButton` | `@Composable (onClick, modifier, enabled=true, content: RowScope.()->Unit)` | 主操作，pressScale(0.96)，control 圆角 17dp，无 ripple |
| `SecondaryButton` | 同 `PrimaryButton` | `OutlinedButton` 变体 |
| `GlassIconButton` | `@Composable (icon: ImageVector, contentDescription: String?, onClick, modifier, tint=onSurface)` | 44dp 圆形玻璃图标钮，pressScale(0.90)，icon 20dp |
| `CompactSwitch` | `@Composable (checked, onCheckedChange, modifier, enabled=true)` | 紧凑开关（44×26dp，thumb 20dp）。**⚠ 工作区版本已删拖拽**（见 §7）：仅点击 + spring 滑轨 |
| `SegmentedTabs` | `@Composable (labels: List<String>, selectedIndex: Int, onSelected: (Int)->Unit, modifier)` | 玻璃分段控件，滑簧 thumb。**⚠ 工作区版本已删横向拖拽** |
| `Tag` | `@Composable (text: String, accent=primary, modifier)` | 带色点的小标签 |
| `Meter` | `@Composable (progress: Float, modifier, accent=primary)` | 6dp 细进度条，`Motion.gentle` 弹簧填充 |
| `volumePreservingScale` | `internal fun (amount: Float): Pair<Float,Float>` | 动效演示辅助 |
| ~~`GradientBar`~~ | ⚠ **已从工作区删除**（参考里有：`(colors: List<Color>, fraction: Float, onFractionChange, modifier)`，渐变滑动条） | |

### 4.3 `Rows.kt`
| 符号 | 签名 | 说明 |
|---|---|---|
| `SectionTitle` | `@Composable (text: String, modifier)` | labelLarge primary SemiBold，`padding(start=20,bottom=8)` |
| `PreferenceRow` | `@Composable (icon: ImageVector, title: String, modifier, subtitle: String?=null, enabled=true, onClick:(()->Unit)?=null, trailing:(()->Unit)?=null)` | 22dp 玻璃设置行：icon(22dp)+标题+可选副标题+可选尾控件 |
| `PreferenceCard` | `@Composable (modifier, content: @Composable ()->Unit)` | 包一层玻璃卡片（AppShape.card，elevation 1，横向 16dp） |
| `DisplayTitle` | `@Composable (text, modifier, style=displayLarge)` | 大标题 |

### 4.4 `TextFields.kt`
| 符号 | 签名 |
|---|---|
| `GlassTextField` | `@Composable (value, onValueChange, modifier, label, placeholder, supportingText, enabled=true, singleLine=true, isError=false, keyboardOptions=Default, visualTransformation=None, trailingIcon: (()->Unit)?=null)` — 17dp 圆角 `OutlinedTextField`，聚焦边框 primary，容器 `surface.copy(alpha=0.55/.40/.25)` |

### 4.5 `Feedback.kt`
| 符号 | 签名 | 说明 |
|---|---|---|
| `EmptyState` | `@Composable (title, message, modifier, icon: ImageVector?=null, action: (@Composable ()->Unit)?=null)` | 空态：Canvas 同心圆+轨道点+刻度线，`idleBreathing`，可选 icon/action |
| `InfoBanner` | `@Composable (text, modifier, accent=primary)` | 玻璃信息条（medium 圆角，色点前缀） |
| `StatTile` | `@Composable (label: String, value: String, modifier, accent=primary)` | 玻璃指标瓦片（headlineSmall Bold value + labelSmall label） |

### 4.6 `Dialogs.kt`
| 符号 | 签名 | 说明 |
|---|---|---|
| `GlassDialogHost` | `class`，`items = mutableStateListOf<@Composable ()->Unit>`，`push(overlay)` / `remove(overlay)` | 同窗口 overlay 栈（对话框无法跨窗口采样玻璃层） |
| `LocalGlassDialogHost` | `staticCompositionLocalOf { GlassDialogHost() }` | 由 AppRoot 提供 |
| `GlassDialog` | `@Composable (onDismissRequest, title: String, onConfirm, modifier, confirmText="确定", dismissText: String?="取消", confirmEnabled=true, content: ColumnScope.()->Unit)` | 玻璃对话框：scrim 用 `rememberLayerBackdrop` 统一变暗，玻璃卡采样 `LocalGlassContentBackdrop`、cardLarge 28dp 圆角、进出场 spring(0.7,320) |

### 4.7 `LiquidBottomTabs.kt`
| 符号 | 签名 |
|---|---|
| `LiquidBottomTabs` | `@Composable (selectedTabIndex: ()->Int, onTabSelected: (Int)->Unit, backdrop: Backdrop, tabsCount: Int, modifier, content: @Composable RowScope.()->Unit)` |
| `LiquidBottomTab` | `@Composable fun RowScope. (onClick, modifier, index: Int=-1, content: @Composable ColumnScope.()->Unit)` |

（实现：胶囊容器 vibrancy+blur(4)+lens(24*refraction) 采样 `backdrop`；隐藏的 accent 副本被活动胶囊按压采样；活动玻璃胶囊 `DampedDragAnimation` + `InteractiveHighlight`，velocity 依赖各向异性拉长，InnerShadow 随 pressProgress。内部 Local：`LocalLiquidBottomTabScale`、`LocalLiquidBottomTabOriginalAlpha`。）

---

## 5. 组件组合范例（参考壳的用法）

### 5.1 `ComponentsScreen.kt`（页面骨架范式）

标准玻璃列表页结构 —— **业务屏幕抄这一段即可**：
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
    contentPadding = PaddingValues(top = 20.dp, bottom = 108.dp),   // bottom 108 给液态底栏留空间
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    item {
        Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.displayMedium, color = onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
        }
    }
    item { SectionTitle("分组标题", Modifier.padding(top = 8.dp)) }
    item {
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(onClick = {}) { Text("主要") }
                SecondaryButton(onClick = {}) { Text("次要") }
            }
            // ...
        }
    }
}
```

组件搭配清单：
- 按钮组：`GlassCard { Row { PrimaryButton / SecondaryButton / GlassIconButton(icon, contentDescription, onClick) } }`；禁用态 `PrimaryButton(onClick, enabled=false)`。
- 输入：`GlassTextField(value, onValueChange, label, placeholder)`；密码：`keyboardOptions=KeyboardOptions(keyboardType=Password)` + `visualTransformation=PasswordVisualTransformation()` + `supportingText`。
- 选择：`SegmentedTabs(labels=listOf(...), selectedIndex, onSelected, modifier.fillMaxWidth())`；`Tag(text, accent=AuroraMint/...)`。
- 偏好开关：`PreferenceCard { PreferenceRow(icon, title, subtitle, trailing = { CompactSwitch(checked, onCheckedChange) }) }`。
- 进度：`Meter(progress)` + `Text("${(p*100).toInt()}%", titleLarge, primary)` + `SecondaryButton` 切换。
- 弹窗：`GlassDialog(onDismissRequest, title, onConfirm, confirmText) { Text(...) }`（showDialog 状态 `remember { mutableStateOf(false) }`）。
- 色板预览：`clip(RoundedCornerShape(14.dp)).background(color)`。

### 5.2 `MotionScreen.kt`（动效组合范例）

| 场景 | 用法 |
|---|---|
| 弹簧回弹 | `Animatable` + `animateTo(target, if (reduced) spring(1f,900f) else springPreset(i))`，`preset = Motion.gentle/standard/snappy/bouncy/heavy(0.001f)`；拖拽把手 `liquidGlass(CircleShape, elevation=3.dp, borderAlpha=0.6f)` |
| 错峰列表 | `delay(index * Motion.Stagger.StandardList)` 后 `progress.animateTo(1f, Motion.standard(0.01f))`，`graphicsLayer { alpha; translationY=(1-progress)*26f }`，条目 `liquidGlass(RoundedCornerShape(14.dp), elevation=0.dp, borderAlpha=0.26f)` |
| 冲击反馈 | `modifier.impactShake(trigger = if (reduced) null else triggerCount, amplitude = ...)` — 触发器增加即摇 |
| 常态呼吸 | `Modifier.idleBreathing(periodMs=5200+index*900, amplitudeX=..., amplitudeY=...)`；`liquidGlass(RoundedCornerShape(18.dp), elevation=1.dp)` |
| 减动效级联 | `reducedMotion = settings.reducedMotion`；每个 demo 把 spring 换成 `spring(1f,900f)` 或把 trigger 置 null |

demo 总纲（`MotionScreen(settings: AppSettingsState)`）：头部 displayMedium + bodyMedium 说明 → `SpringPlayground` → `StaggerDemo` → `ShakeDemo` → `BreathingDemo` → `InfoBanner`（说明减动效降级）。

---

## 6. AppIcons 图标清单（完整）

`object AppIcons` — **共 11 个**（文档注释写“nine”是笔误；`val ... : ImageVector by lazy { ... }` 均为 24×24dp 黑色实心路径导出，品牌用它替代 material-icons-extended 省 APK）：

| # | 图标 | 说明 |
|---|---|---|
| 1 | `AppIcons.Animation` | 动效页 tab |
| 2 | `AppIcons.BlurOn` | 首页 tab |
| 3 | `AppIcons.Notifications` | 通知设置 |
| 4 | `AppIcons.Palette` | 调色板 |
| 5 | `AppIcons.RotateRight` | 旋转/动效 |
| 6 | `AppIcons.Settings` | 设置 tab |
| 7 | `AppIcons.Shield` | 隐私/安全 |
| 8 | `AppIcons.Speed` | 速度/性能 |
| 9 | `AppIcons.WaterDrop` | 水滴/湿度 |
| 10 | `AppIcons.Image` | 图片 |
| 11 | `AppIcons.Widgets` | 组件 tab |

**没有其它图标**。⚠ 业务如需更多图标：参考工程刻意不引入 `material-icons-extended`；但 ExpiryGuard 的 `app/build.gradle.kts` 已依赖 `androidx.compose.material:material-icons-extended`（见 §7），可直接用 `Icons.Default/Outlined/AutoMirrored.*` 扩展，无需改 AppIcons。

---

## 7. 参考 vs 工作区 `ui/liquid` 差异审计

对比基准：参考 `/tmp/liquid-ref/android/app/src/main/java/com/monkeycode/liquidui/ui/`（24 个 kt）与工作区 `/workspace/app/src/main/java/com/expiryguard/app/ui/liquid/`（18 个 kt）。**去除包名/import 路径差异后逐行 diff 的结果**：

### 7.1 逐字节一致（仅包名重命名）— 11 个文件
`theme/AppIcons.kt`、`theme/Color.kt`、`theme/Shape.kt`、`theme/Type.kt`、`motion/Motion.kt`、`motion/Modifiers.kt`、`motion/DampedDragAnimation.kt`、`motion/DragGestureInspector.kt`、`motion/InteractiveHighlight.kt`、`components/Glass.kt`、`components/Rows.kt`、`components/TextFields.kt`、`components/Feedback.kt`、`components/LiquidBottomTabs.kt` — 逐字节一致，可放心依赖。

### 7.2 被删除/改动的 API（工作区相对参考）

**(a) `components/Controls.kt` — 有删改**
- `GradientBar(colors, fraction, onFractionChange, modifier)` — **整个 Composable 已删除**（参考 251-308 行）。若业务要“渐变/色板滑动条”，需从参考拷回。
- `CompactSwitch`：**删除了拇指可拖拽**。参考实现是 `Animatable`+`detectHorizontalDragGestures`，松手吸附近端；工作区改为纯 `animateDpAsState`/`animateFloatAsState` spring 滑轨 + `clickable` 点击翻转。**公共签名未变**：`(checked, onCheckedChange, modifier, enabled)`。
- `SegmentedTabs`：**删除横向拖拽**。参考是 `Animatable`+`onSizeChanged`+`detectHorizontalDragGestures`，拖后 `roundToInt` 吸附并回调 `onSelected`；工作区变成 `animateFloatAsState(selectedIndex)`。**公共签名未变**：`(labels, selectedIndex, onSelected, modifier)`。
- `PrimaryButton/SecondaryButton/GlassIconButton/Tag/Meter/volumePreservingScale`：一致。

**(b) `components/Dialogs.kt` — 仅注释**
- 仅 KDoc 里的包引用字符串不同；行为/API 全一致。

**(c) `theme/Theme.kt` — 删除了 Custom 取色 + 主题平滑过渡**
- `PaletteId`：参考含 `Custom("自定义")`（8 值）；工作区**删掉 Custom**（7 值）。
- `val CustomPalette = PaletteId.Custom`：已删。
- `palettePrimaryLight()`：参考对 Custom 特判返回 `0xFF3E8FE0`；工作区删特判。
- `LiquidUITheme(...)`：参考多两个参数 `customSeed: Color = Color(0xFF3E8FE0)`、`customShade: Float = 0.5f`；工作区**已去掉**。
- `rememberAnimatedColorScheme` + `schemeFor(customSeed, customShade)` + `seedSchemeFor()` + `Color.lerp`：全部删除，工作区主题切换是**瞬间跳变**（无 320ms 交叉淡入）。
- 其余（`ThemeMode`/`VisualMode`/`AppChrome`/`LocalAppChrome`/`supportsBackdropBlur`/`scaledShapes`）一致。

**(d) `screens/AppSettingsState.kt` — 删除了 Custom 相关状态**
- 删除：`customSeed`、`customShade`、`updateCustomSeed(Color)`、`updateCustomShade(Float)`、`KEY_CUSTOM_SEED`、`KEY_CUSTOM_SHADE`（`reset()` 同时去掉对应两行）。
- 其余（themeMode/palette/visualMode/reducedMotion/motionDamping/wallpaperBackground/glassOpacity/glassRefraction/cornerScale + update* + `legacyVisualMode()` + `rememberAppSettingsState()`) 一致。

**(e) 整个缺失的参考文件（工作区 `ui/liquid` 没有）**
- `ui/navigation/AppRoot.kt` — ExpiryGuard 用自有导航替代（见下）。
- `ui/navigation/Screen.kt`。
- `ui/screens/HomeScreen.kt` / `SettingsScreen.kt` / `ComponentsScreen.kt` / `MotionScreen.kt`（模板演示壳）。

### 7.3 工作区 `ui/liquid` 中新增、参考没有的内容

`ui/liquid` 目录内**没有新增任何参考没有的文件**；18 个文件全部对应参考中的同名文件。新增均发生在工作区自己的业务层：

- **导航/入口（未用参考 AppRoot）**：`MainActivity.kt`、`ExpiryGuardApp.kt`、`navigation/AppNavigation.kt`（自有导航图）+ 每屏 ViewModel。
- **13 个业务屏幕**（目标改造对象）：
  - `ui/home/HomeScreen.kt`
  - `ui/product/ProductListScreen.kt`、`ProductDetailScreen.kt`、`AddProductScreen.kt`、`EditProductScreen.kt`
  - `ui/settings/SettingsScreen.kt`、`CategoryManagementScreen.kt`、`ShelfLifeGroupManagementScreen.kt`、`NotificationSettingsScreen.kt`、`BackupRestoreScreen.kt`、`AboutScreen.kt`
  - `ui/stats/StatsScreen.kt`
  - `ui/trash/TrashScreen.kt`
- **业务自定义组件（`ui/components/`，非 liquid 包）**：`GlassTopAppBar`（目前 12 处引用）、`GlassComponents`（`GlassCard`/`GradientBackground`/`GlassCardSmall` 等，11 处引用）、`ProductCard`、`StatCard`、`StatusBadge`、`DateInputField`、`DatePickerDialog`、`EmptyState`。
- **独立主题包**：`ui/theme/{Color,Theme,Type}.kt`（与 `ui/liquid/theme/` 并存；业务若保留旧主题树会与 LiquidUI 主题冲突，接入时应收敛到 `LiquidUITheme`）。
- 这些业务代码已通过以下 liquid 符号接入设计系统（来自 import 统计）：`LiquidUITheme`、`AppChrome`、`LocalAppChrome`、`PaletteId`、`ThemeMode`、`VisualMode`、`palettePrimaryLight`、`liquidGlass`、`SegmentedTabs`、`LiquidBottomTabs`、`LiquidBottomTab`、`AuroraBackground`、`GlassDialogHost`、`LocalGlassDialogHost`、`LocalGlassBackdrop`、`LocalGlassContentBackdrop`、`AppSettingsState`、`rememberAppSettingsState`、`Motion`。
- 依赖差异：参考刻意不用 `material-icons-extended`；工作区 `app/build.gradle.kts` 已加 `androidx.compose.material:material-icons-extended` + `io.github.kyant0:backdrop-android:2.0.0-alpha03` + `io.github.kyant0:shapes-android:1.2.0`（玻璃基础设施已就绪）。

---

## 8. 13 个业务屏幕套用本设计系统的接入指引

### 8.1 全局接线（一次性）
1. **主题**：入口改用 `LiquidUITheme(themeMode, visualMode, palette, chrome)`（`ui/liquid/theme/Theme.kt`），不要在子层再开 `MaterialTheme` 覆盖。`AppSettingsState` 用 `rememberAppSettingsState()` 创建并传入根。
2. **外壳 Backdrop**：参考 `AppRoot.kt` 的三步（aurora/content 双层 `rememberLayerBackdrop` + `rememberCombinedBackdrop`）。ExpiryGuard 若沿用自有 `AppNavigation`，需在 Shell 里**同样提供** `LocalGlassBackdrop`、`LocalGlassContentBackdrop`、`LocalGlassDialogHost` 三个 CompositionLocal，并在背景放 `AuroraBackground`。
3. **底栏**：用 `LiquidBottomTabs(selectedTabIndex = {...}, onTabSelected, backdrop = rememberCombinedBackdrop(aurora, content), tabsCount, modifier=align(BottomCenter).padding(bottom=14.dp))` + `LiquidBottomTab`；每个 tab slot 内 `Icon(22.dp) + Text(11.sp)`。若业务 tab 数可变（Home/统计/设置…），目标集用 `List<Screen>` + `Screen.entries` 思路管理。
4. **页面切换动画**：抄 `AnimatedContent` 的 `scaleIn(0.94, bouncy 或 tween Fast) + fadeIn(Fast)` togetherWith `scaleOut(0.97, Fast) + fadeOut(Fast)`（`reducedMotion` 时全用 `tween(Fast)`）。

### 8.2 每屏组件速查（13 屏）
- **列表页**（Home/ProductList/Trash/Stats/Category/ShelfLifeGroup）：`LazyColumn(statusBarsPadding, contentPadding=bottom 108.dp)` + `GlassCard` 承载行 / `PreferenceRow`(可点击传 onClick) / `SectionTitle` / 空态用 `EmptyState(title,message,icon,action)`。
- **详情/表单页**（ProductDetail/Add/Edit）：`GlassCard` + `GlassTextField(label, supportingText, isError, trailingIcon, keyboardOptions)`；日期/选择器用现有业务 `DatePickerDialog`；主按钮 `PrimaryButton`、次按钮 `SecondaryButton`。
- **设置页**（Settings/Notification/BackupRestore/About）：`PreferenceCard { PreferenceRow(icon, title, subtitle, trailing = { CompactSwitch / SegmentedTabs / Tag }) }`；主题相关直接读 `AppSettingsState.update*()`（themeMode/palette/visualMode/reducedMotion/motionDamping/wallpaperBackground/glassOpacity/glassRefraction/cornerScale）。
- **确认弹窗**：`GlassDialog(onDismissRequest, title, onConfirm, confirmText, dismissText, confirmEnabled) { content }`，置于屏幕可组合内即可（由 host 上抛到根）。
- **动效点缀**：首屏列表项 `StaggeredReveal(index = i, ...)`；卡片按压 `pressScale(interactionSource)`（与 `clickable` 共享 MutableInteractionSource）；完成动作 `impactShake(trigger)`；状态数字卡片用 `StatTile(label, value, accent)`。

### 8.3 规则约束（避免回归）
- **不要**在调用点写手写 `spring(...)`/`tween(...)`；统一走 `Motion.Duration.*`、`Motion.gentle/standard/snappy/bouncy/heavy/stiff(threshold)`、`Motion.Stagger.*` 与 `Motion.Press/etc`。该文件 KDoc 明确 “no ad-hoc `spring(...)` at call sites”。
- **尊重 `reducedMotion`**（`AppSettingsState.reducedMotion`）：动画降级为临界/无过冲，出场效果默认终态（`StaggeredReveal` 的 `enabled=false`、`impactShake` 的 `trigger=null` 模式即为此设计）。
- 玻璃折射透明度只读 `LocalAppChrome`（glassOpacity/glassRefraction），不要在面板里硬编码。
- `LiquidBottomTabs` 的 backdrop **必须传组合层**，否则底栏不折射。
- 12 个业务屏幕引用 `GlassTopAppBar` —— 接入液态主题后确认其内部样式与 `LiquidUITheme` 融合（当前它仍在独立 `ui/theme` 下），避免双主题冲突。

### 8.4 若需还原参考中被删的能力
- 主题跨切平滑过渡：把参考 `rememberAnimatedColorScheme` 拷回 `theme/Theme.kt`（约 50 行，依赖 `Motion.Duration.Standard`）。
- 自定义取色：把参考 `PaletteId.Custom`/`seedSchemeFor`/`customSeed`/`customShade`/`GradientBar` 一并拷回（还要把 `AppSettingsState` 的对应字段/update 函数加回）。
- 可拖拽 `CompactSwitch` / `SegmentedTabs`：可直接从参考文件整体覆盖工作区同名函数体。