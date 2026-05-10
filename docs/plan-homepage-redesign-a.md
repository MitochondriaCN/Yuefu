# 主页改造方案 A：古典乐章

## 背景

当前主页是一个功能性的列表布局：`Blue800`(#1565C0) 纯色扫描按钮 + 2×2 的 `ElevatedCard` 文件网格。虽然可用，但没有传达出乐府 "AI 驱动的音乐识别" 的产品气质。

本方案将主页改造为 **古典乐谱纸** 风格：暖色纸纹底纹、衬线体标题、赭金铜印扫描按钮、带有五线谱装饰的文件卡片。

## 设计效果预览

HTML Demo 文件: `demo/design-a-classical.html`（已存在，可浏览器打开预览）

## 改动文件清单

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `ui/theme/Color.kt` | **修改** | 新增暖色调色板 |
| `pages/home/HomePage.kt` | **重写** | 主页布局与视觉全面改造 |
| `ui/theme/Type.kt` | **小改** | 为 NotoSerifSc 添加 Regular 字重 |
| `res/values/strings.xml` | **小改** | 新增主页问候语字符串 |
| `res/font/` | **新增** | 添加 Noto Serif SC Regular 字体文件 |

## 详细实现计划

### Step 1: 扩展色板 (`Color.kt`)

在现有色彩定义之后新增：

```kotlin
// ── 主页古典方案 ──
val Ivory       = Color(0xFFFFFBF0)  // 页面底色
val WarmBg      = Color(0xFFF6F2E9)  // 扫描按钮渐变起点
val PaleOchre   = Color(0xFFE8D9BE)  // 扫描按钮渐变终点
val OchreEdge   = Color(0xFFB68A4C)  // 与现有 Ochre 同色，用于描边
val InkBrown    = Color(0xFF3F2E1F)  // 主文字色
val InkSoft     = Color(0xFF8D7A6A)  // 次要文字色
```

> 注意：`Ochre` (#B68A4C) 已存在，无需重复定义。

### Step 2: 字体扩展 (`Type.kt`)

当前 `NotoSerifSc` 只有 Bold 字重。主页问候语需要 Regular 字重。

1. 将 `noto_serif_sc_regular.ttf`（或 .otf）放入 `res/font/`
2. 在 `Type.kt` 中扩展：

```kotlin
val NotoSerifScRegular = FontFamily(
    Font(R.font.noto_serif_sc_regular, FontWeight.Normal)
)
```

> **备选方案**：如果不方便添加新字体文件，问候语可使用 `NotoSerifSc`（Bold）以较小字号，或将问候语改用 sans-serif。铜印和标题仍使用现有 Bold。

### Step 3: 新增字符串 (`strings.xml`)

```xml
<string name="home_greeting">今日想听哪一份谱？</string>
<string name="scan_sheet_subtitle">拍照或导入乐谱，AI 即刻识别</string>
```

### Step 4: 重写主页 (`HomePage.kt`)

这是核心改动。以下按组件逐个说明。

#### 4.1 页面容器

当前：
```kotlin
Column(modifier = modifier.fillMaxWidth()) {
    Spacer(Modifier.height(20.dp))
    ...
}
```

改为：
```kotlin
Box(modifier = modifier.fillMaxSize()) {
    // 五线谱纸纹背景（复用现有组件）
    // 将 scorePaperTexture 应用于 Box 层级
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scorePaperTexture(color = InkBrown, alpha = 0.055f)
    ) {
        // ... 内容
    }
}
```

关键点：
- 水平 padding 从 `16.dp` 改为 `20.dp`（与 demo 一致）
- 背景色通过 Theme 的 `surface` 或直接设 `Ivory` 实现
- `scorePaperTexture` 已有组件，直接复用

#### 4.2 Hero 区域（新增）

在页面顶部增加品牌标识区：

```kotlin
// "乐府" 大标题
Text(
    text = stringResource(R.string.app_name),
    style = TextStyle(
        fontFamily = NotoSerifSc,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = 0.15.em,
        color = InkBrown
    )
)

// 问候语副标题
Text(
    text = stringResource(R.string.home_greeting),
    style = TextStyle(
        fontFamily = NotoSerifScRegular, // 或 NotoSerifSc
        fontSize = 14.sp,
        color = InkSoft,
        letterSpacing = 0.05.em
    )
)
```

上方加一个 `Spacer(Modifier.height(28.dp))`，下方 `Spacer(Modifier.height(12.dp))`。

可选：在 Hero 区域增加一个微弱的暖色径向渐变光晕效果，使用 `drawBehind` 或 `background(brush = Brush.radialGradient(...))` 模拟 demo 中的 `radial-gradient`。

#### 4.3 扫描按钮（核心改造）

当前是一个 `ElevatedCard`，背景 `Blue800`，白色文字。

改为自定义卡片组件 `ScanCard`：

```kotlin
@Composable
private fun ScanCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(WarmBg, PaleOchre))
            )
            .border(
                width = 1.dp,
                color = OchreEdge.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        // 内部上边缘高光（模拟 inset shadow 顶部反光）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 相机图标区域
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        OchreEdge.copy(alpha = 0.12f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    modifier = Modifier.size(28.dp),
                    tint = OchreEdge,
                    contentDescription = null
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.shot_sheet),
                    style = TextStyle(
                        fontFamily = NotoSerifSc,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 0.04.em,
                        color = InkBrown
                    )
                )
                Text(
                    text = stringResource(R.string.scan_sheet_subtitle),
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = InkSoft
                    )
                )
            }
        }

        // 铜印徽章 "乐"
        BronzeSeal(modifier = Modifier.align(Alignment.BottomEnd))
    }
}
```

**铜印徽章组件**（新增私有 Composable）：

```kotlin
@Composable
private fun BronzeSeal(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .rotate(-12f)
            .border(1.5.dp, OchreEdge, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "乐",
            style = TextStyle(
                fontFamily = NotoSerifSc,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OchreEdge.copy(alpha = 0.55f)
            )
        )
    }
}
```

#### 4.4 段落标题（改造）

当前是 `MaterialTheme.typography.titleSmall` + `primary` 色。

改为衬线体 + 赭金色 + 字间距：

```kotlin
@Composable
private fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        // 金色竖线装饰
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .background(Ochre, RoundedCornerShape(1.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = TextStyle(
                fontFamily = NotoSerifSc,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Ochre,
                letterSpacing = 0.3.em
            )
        )
    }
}
```

#### 4.5 文件卡片（改造）

当前 `FileCard` 使用 `ElevatedCard` + `Grey800` 图标 + 默认排版。

改为自定义样式：

```kotlin
@Composable
fun FileCard(
    modifier: Modifier = Modifier,
    label: String,
    lastOpenTime: String,
    onClick: () -> Unit = {}
) {
    // 使用 staggered animation（通过 AnimatedVisibility 或自定义 alpha/offset）
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ivory)
            .border(1.dp, OchreEdge.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 图标区域 — 赭金底色方块
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(OchreEdge.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    modifier = Modifier.size(22.dp),
                    tint = OchreEdge,
                    contentDescription = null
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkBrown,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = lastOpenTime,
                    fontSize = 13.sp,
                    color = InkSoft,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}
```

> **变化点**：`ElevatedCard` → `Box` + `background` + `border`，图标色 `Grey800` → `OchreEdge`，图标背景新增 `OchreEdge.copy(alpha = 0.10f)` 方块。

#### 4.6 卡片入场动画（新增）

使用 `AnimatedVisibility` 或 `LaunchedEffect` + `Animatable` 实现交错淡入上移：

```kotlin
// 在文件卡片列表中：
var visible by remember { mutableStateOf(false) }
LaunchedEffect(Unit) { visible = true }

AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(500, delayMillis = 100 * index)) +
            slideInVertically(tween(500, delayMillis = 100 * index)) { it / 4 }
) {
    FileCard(...)
}
```

> 注意：`recent4` 最多 4 项，动画延迟 `100ms × index`（0/100/200/300ms）。

#### 4.7 空状态（保持逻辑，更新样式）

空态文案 `"无最近使用的乐谱，请扫描乐谱"` 保持不变，但文字色改为 `InkSoft`，字体改为 serif（可选）。

#### 4.8 加载状态（保持，微调）

`CircularProgressIndicator` + loading message 保持。可将 `CircularProgressIndicator` 的 `color` 改为 `Ochre` 以匹配暖色调。

### Step 5: 底部导航栏样式（如果可自定义）

当前底部导航在 `MainActivity.kt` 的 Scaffold 中。如果可以自定义：
- 背景色 → `Ivory`
- 活动项颜色 → `Ochre`
- 非活动项颜色 → `InkSoft`
- 顶部 1dp 分割线 → `OchreEdge.copy(alpha = 0.18f)`

> 注意：底部导航在 `MainActivity` 中，不在 `HomePage.kt`。修改需评估影响范围。

## 不变的部分

| 组件 | 说明 |
|------|------|
| `HomePageViewModel` | 零改动。UI State 结构不变。 |
| `ScanningTutorialBottomSheet` | 零改动。教程逻辑与 UI 不受影响。 |
| `ScorePaperBackground.kt` | 零改动。直接复用 `.scorePaperTexture()`。 |
| `Sheet` entity | 零改动。数据模型不变。 |
| 图片选择逻辑 (`takePictureLauncher` / `pickImageLauncher`) | 零改动。相机/相册逻辑不变。 |

## 关键复用

| 已有组件 | 复用方式 |
|----------|----------|
| `ScorePaperBackground.scorePaperTexture()` | 作为页面底纹，`alpha = 0.055f`，`color = InkBrown` |
| `NotoSerifSc` 字体 | 标题 "乐府"、扫描按钮文字、段落标题、铜印文字 |
| `Ochre` (#B68A4C) 色彩 | 段落标题、图标色调、描边、铜印 |

## 新增依赖

| 依赖 | 说明 |
|------|------|
| `res/font/noto_serif_sc_regular.ttf` | 问候语使用 Regular 字重（可选，见 Step 2 备选方案） |

无新的第三方库依赖。

## 实现顺序

```
1. Color.kt    — 新增暖色变量          → verify: 编译通过
2. Type.kt     — 添加 NotoSerifScRegular → verify: 编译通过
3. strings.xml — 新增问候语字符串        → verify: 编译通过
4. HomePage.kt — 重写页面布局           → verify: Preview 显示正确
5. 底部导航    — 评估是否需要调整        → verify: 预览效果
```

## 验证方式

1. 在 Android Studio 中使用 `@Preview` 查看 `HomePageContent` 的 Compose Preview
2. 检查亮色/暗色模式下的表现（暖色调主要面向亮色模式，暗色模式可暂保持默认）
3. 检查空状态（无最近乐谱）和有数据状态两种场景
4. 确认教程底部弹窗、图片选择弹窗的流程不受影响
5. 确认点击最近乐谱能正常跳转到 `SheetActivity`
