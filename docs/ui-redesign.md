# 全局设计系统（第二阶段）

> 目标：把“每个页面各自长一套”的现状收口成一套 token + 少量基础组件。
> 边界：只动 UI 层；业务（Room / DAO / Repository / ViewModel / domain / 导航 / 提醒 / CSV）不改。

## 1. 已确认的产品决策

| 决策 | 结论 |
| --- | --- |
| 动态取色 | 关闭。不跟随壁纸，避免同一页面在不同机器上变灰变紫 |
| 主色 | 低饱和自然绿，配暖白纸感底 |
| 深色模式 | 保留，使用品牌深色板，不是简单反色 |
| 字体 | 系统字体，只统一字阶，不引入字体包 |
| 业务功能 | 本阶段不新增趋势图、搜索等任何功能 |

## 2. Token

### 2.1 颜色

| 角色 | 浅色 | 深色 | 用途 |
| --- | --- | --- | --- |
| background | `#F7F6F1` | `#12140F` | 页面底 |
| surface | `#FFFFFF` | `#1B1E17` | 卡片、面板 |
| surfaceVariant | `#EFEDE5` | `#23271F` | 次级填充、未记录格 |
| outlineVariant | `#E5E2D9` | `#2E332A` | 1dp 分隔线 |
| onSurface | `#1E241E` | `#E9EDE4` | 主文字 |
| onSurfaceVariant | `#5E645A` | `#A8AFA3` | 次要文字 |
| primary | `#4E7A58` | `#8FB79A` | 主操作、今天描边 |
| primaryContainer | `#DDEBE0` | `#243026` | 选中底、记满格 |
| tertiary | `#B08748` | `#D6B47C` | 部分完成、提醒 |
| tertiaryContainer | `#F1E6D0` | `#3A3324` | 部分完成格 |
| error | `#B25A45` | `#E0927F` | 危险操作 |

补充：M3 的 `surfaceContainer*` 系列一并显式给了暖色值，否则导航栏、Sheet 会回落到默认紫灰。`secondaryContainer` 取浅绿，因为它承担的是 M3 的“选中态”语义（导航指示器、分段按钮、进度条轨道）。

### 2.2 字阶（系统字体）

| 样式 | 字号/行高 | 用途 |
| --- | --- | --- |
| displaySmall | 40/44 Bold | 热量大数字 |
| headlineMedium | 28/34 Bold | 页面主标题 |
| titleLarge | 20/26 SemiBold | 页面标题 |
| titleMedium | 17/24 SemiBold | 卡片标题 |
| bodyLarge | 16/24 | 正文 |
| bodyMedium | 15/22 | 列表正文 |
| bodySmall | 13/20 | 辅助说明 |
| labelLarge | 14/20 Medium | 按钮、chip |

### 2.3 间距与形状

- 间距：4 / 8 / 12 / 16 / 20 / 24 / 32；页面左右 20，卡片内 16，按钮高 48，列表行 56。
- 圆角：8 / 12 / 16 / 20 / 28；卡片 16，按钮与输入 12。
- 层级：不用阴影，靠 `surface` 白底 + 1dp `outlineVariant` 分隔。

## 3. 基础组件（`ui/design/`）

| 组件 | 签名 | 说明 |
| --- | --- | --- |
| `Spacing` | 常量对象 | 唯一间距来源 |
| `SurfaceCard` | `(modifier, padding = 16, onClick = null, content)` | 白底 + 1dp 描边；带 onClick 时整卡可点 |
| `SectionHeader` | `(title, modifier, subtitle = null, trailing = null)` | 区块标题 + 说明 + 右侧操作 |
| `AppButton` | `(text, onClick, modifier, style = Primary, enabled)` | Primary / Secondary / Text 三态 |
| `AppChip` | `(text, selected, onClick, modifier, enabled)` | 选中用浅绿底深绿字，不用实心主色 |
| `ListRow` | `(title, modifier, subtitle = null, onClick = null, leading = null, trailing = null)` | 最小 56dp，触控友好 |

`ui/Cards.kt` 里的旧 `AppCard` 保留原签名，内部改为 `SurfaceCard(padding = 0.dp)`，三个旧页面零改动即可吃到新卡片外观。

## 4. 兼容处理（本阶段唯一的页面改动）

`ui/CalendarScreen.kt` 三行颜色：`PARTIAL` 从 `secondaryContainer` 换成 `tertiaryContainer`（文字同步换 `onTertiaryContainer`，图例同步）。

原因：`secondaryContainer` 被 M3 的选中态占用（导航指示器、分段按钮、进度条轨道），拿它当“部分完成”状态色会和选中态撞色；`tertiary` 本来就是今日页「部分」圆点的颜色，改完两处状态色统一。只换 token，不改布局与行为。

另：`res/values/themes.xml`、`res/values-night/themes.xml` 的 `windowBackground` 同步为 `#F7F6F1` / `#12140F`，消除首帧闪白/闪灰。

## 5. 验证记录（2026-10-22，模拟器 Pixel 6 / API 35）

| 项 | 结果 |
| --- | --- |
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | 8 suite / 30 用例 / 0 失败 |
| 浅色 | 暖白底 + 白卡 + 绿环 + 琥珀状态点；导航栏不再发紫 |
| 深色 | 近黑底 + 深色卡 + 浅绿强调；统计进度轨道不再发棕 |
| 日历三态 | 记满（浅绿）/ 部分（暖沙）/ 没记录（暖灰）三色可区分 |
| 1.3× 字体 | 设置页换行正常、按钮不裁切；今日页「连续 0 天」折成两行（见 §6） |

截图：`docs/screenshots/p2-light.png`、`p2-dark.png`、`p2-light-calendar.png`、`p2-dark-calendar.png`、`p2-font-130.png`。

## 6. 未解决 / 已知问题

1. 今日页状态行（`今日打卡：部分 / 连续 0 天`）在 1.3× 字号下换行，挤成两行；需要改成 `FlowRow` 或缩短文案。
2. 设置页档位仍是实心主色药丸，未换成 `AppChip`（属页面重构，留给第三阶段）。
3. 添加面板/编辑弹窗仍用默认 `SegmentedButton`、`TextField` 样式，且面板信息层级偏多（标题/餐次/搜索/分类/最近/列表/输入）。
4. `NavigationBar` 选中指示器仍是 M3 默认形状（浅绿药丸），Phase 3 统一成 chip 风格。
5. 缺少 `EmptyState`、`ErrorBanner` 等组件：现有空态是纯文本，需要新建组件再落页面。
6. 深色下 FAB 仍会遮住最后一张卡片，页面需要 `contentPadding` 兜底。

## 7. 第三阶段建议（逐页重构）

| 页面 | 动作 |
| --- | --- |
| 今日 | 标题区压缩；状态行改 FlowRow；卡片密度重排；FAB 让位 |
| 添加面板 | 重构为「搜索 + 分类 + 结果」三层，最近记录上移；统一 chip |
| 日历 | 格子改用新的三态 token；图例与今日统一 |
| 统计 | 指标卡改 `MetricTile`；占比条统一轨道色 |
| 食物库 | 列表改 `ListRow`；补 `EmptyState` 与删除确认 |
| 设置 | 分组 + `AppChip` 档位 + 统一按钮 |

每页改完都要过一遍浅色 / 深色 / 1.3× 字号三张截图。

## 8. 第三阶段 P1：今日页重构（2026-10-22）

范围：只改 `ui/TodayScreen.kt` 的呈现层；ViewModel / Repository / DAO / Room / domain / 导航 / 提醒 / CSV 未动。第二阶段成果先落为本地提交 `380ff1b feat(ui): establish global design system`（未推送）。

### 8.1 结构

| 区块 | 之前 | 现在 |
| --- | --- | --- |
| 顶栏 | 「今天」+ 副标题 + 左右箭头 + 「今天」文字按钮 | `DateHeader`：`今天`/`10/21` + 副标题 + 左右箭头；非今日才出现 `回到今天`（`AppButton` Text 风格） |
| 热量概览 | 圆环 + 三行文字 | `SummaryCard`：左侧大数字 `72 kcal`、右侧「还剩 1728 kcal」、细进度条、「部分打卡」胶囊 + 连续天数 |
| 四餐 | 四张独立卡片，空餐占一整行提示 | 单张 `SurfaceCard` 内四段分组，`HorizontalDivider` 分隔；空餐把提示收进头行副标题「未记录 · 点这里记一笔」 |
| 条目行 | `Row` + 自绘文字 + 铅笔图标 | `ListRow`（标题/副标题/右trailing 热量+编辑图标），点击进编辑弹窗 |
| 主操作 | `ExtendedFloatingActionButton`「记一笔」 | 同一按钮，改 `primaryContainer` 底色、`labelLarge` 文案；滚动内容底部留 120dp |
| 面板 | 默认 `FilterChip`/`Button`/白底 chip | 餐次与分类改 `AppChip`，「添加」改 `AppButton`，最近记录改白底描边 chip |

### 8.2 行为保持

日期前后切换与回到今天、热量目标/剩余/打卡状态、四餐分组与条目顺序、添加/编辑/删除/撤销、份量克数换算与热量预览、导航与 ViewModel 调用——全部沿用原实现，仅重排呈现。

### 8.3 验证

| 项 | 结果 |
| --- | --- |
| `:app:assembleDebug` | 通过 |
| `:app:testDebugUnitTest` | 8 suite / 30 用例 / 0 失败 |
| 浅色首屏 | `docs/screenshots/p3-today-light.png` |
| 深色 | `docs/screenshots/p3-today-dark.png` |
| 1.3× 字号 | `docs/screenshots/p3-today-font130.png` |
| 行为走查 | 日期切到 10/21（739 kcal、出现「回到今天」）、回今日、面板「记到 午餐」、最近记录一步添加（144 kcal + 撤销 Snackbar）、编辑弹窗、删除撤销均正常 |
| FAB | 空餐收进头行后四餐在首屏完整可见；条目多时 FAB 覆盖底部滚动内容，滚到底时最后一行完整露出（内容底部留白 120dp） |

### 8.4 遗留

- 条目多时首屏 FAB 仍会压住最后一行右侧，靠滚动让位；若要彻底不遮挡，需要把主操作改成常驻底部栏（跨页面决策）。
- 1.3× 下四餐单卡变长，需滚动才能看全；状态行已用 `FlowRow`，不再折行。
- 面板仍是「标题/餐次/搜索/分类/最近/列表/输入」七层，留给下一轮。

## 9. 第三阶段 P2：四页 + 添加面板收尾（2026-10-24）

用户拍板：范围全做（四页 + 面板/弹窗一次收尾）；「我的食物」删除用**直接删 + 4 秒撤销 Snackbar**（与 R4 轮全 App 既定模式一致），本文件 §7 里「删除确认」的说法作废。

### 9.1 新增设计组件（`ui/design/`）

| 组件 | 说明 |
| --- | --- |
| `MetricTile(label, value)` | 统计指标瓦片：SurfaceCard + labelMedium 标签 + titleLarge Bold 数值 |
| `EmptyState(title, hint)` | 居中弱化文字空态，不引图标 |
| `AppTextField(...)` | OutlinedTextField 薄封装：12dp 圆角、静止描边 outlineVariant（与 AppChip 一致）、聚焦主色 |
| `StatusToken`（`DayStatus.statusStyle()`） | 三态统一配色与文案：COMPLETE=primaryContainer「记满」、PARTIAL=tertiaryContainer「部分」、MISSING=surfaceVariant「未记录」；今日页胶囊、日历格子、图例共用 |
| `AppButton(destructive = true)` | Text 风格可用 error 色，用于「删除」类操作 |

另从面板与编辑弹窗抽出共用的 `PortionEditor`（克/份切换 + AmountInput + 热量预览，trailing 插槽挂「添加」按钮），消除两处 ~60 行重复。

### 9.2 添加面板与弹窗

- `AddEntrySheet` 七层压三层：①标题 +「记到 X」副标题（或 FAB 入口的餐次 chips）+ 自定义；②搜索（AppTextField + 搜索图标）+ 分类；③结果区（最近记录 + ListRow 食物列表）+ 选中后的份量录入浮出。`height(300.dp)` 硬编码移除，列表按剩余空间填充。
- 食物行从手写 Row 改 `ListRow`（min 56dp 触控）；搜索无结果时显示 `EmptyState`。
- 「最近记录」每次打开面板刷新（`LaunchedEffect` 调 `picker.refreshRecent()`）——旧行为只在 ViewModel 创建时加载一次，刚记过的食物要重开 App 才出现，顺手修掉。
- **实现坑**：把「最近记录」作为条件 `item(key=...)` 放进 LazyColumn 时，状态到达后该 item 不渲染（条件为真也不出现）；移回 LazyColumn 外做兄弟节点即正常。原因未深究，记录在此避免再踩。
- `CustomFoodDialog` / `EditEntryDialog`：输入框换 `AppTextField`、按钮换 `AppButton`，编辑弹窗删除按钮 destructive 红字，保存按钮在克数非法时禁用。

### 9.3 四个页面

- **日历**：页面边距 16→`Spacing.page`；月份标题升 headlineSmall；格子与图例改用 `StatusToken`，图例文案与今日页胶囊统一为「记满/部分/未记录」；删死代码 `daysBetween()`。格子圆角维持 12dp（16dp 在该尺寸过圆，有意决策）。
- **统计**：`StatCard` → `MetricTile`×4；占比条显式 `trackColor=outlineVariant` + `color=primary`，与今日页进度条轨道统一；卡内标题用 `SectionHeader`；间距全走 `Spacing`。
- **我的食物**：去掉嵌套 Scaffold，改今日页同款 `Box` + 列表 + FAB（primaryContainer）+ SnackbarHost；列表改单张 `SurfaceCard(padding=0)` 内 `ListRow` + 分隔线（同四餐卡）；空态换 `EmptyState`；删除改「直接删 + 4 秒撤销」，`FoodsViewModel.undoDelete()` + `repo.restoreFood(food)` **按原 id 插回**（无外键，entries 的 foodId 关联随之恢复）；列表底部 120dp 让位 FAB。
- **设置**：档位实心药丸 → `AppChip`；「保存」「导出 CSV」→ `AppButton`；自定义输入 → `AppTextField`，常显「自定义范围 600–6000 kcal」提示、超范围禁用保存（原来是静默不生效）；提醒卡改两行 `ListRow`：「开启提醒」+Switch（整行可点）、「提醒时间」+右侧主色时间（点开平台 TimePickerDialog，48dp 触控高）；卡内标题用 `SectionHeader`。
- **NavigationBar 维持默认**：指示器经主题映射已是 primaryContainer 药丸，与 AppChip 选中态语义一致，视为已统一，不自绘导航栏。
- `ui/Cards.kt` 的 `AppCard` 兼容层在四页迁移完成后零引用，已删除。

### 9.4 验证（2026-10-24，模拟器 Pixel 6 / API 35，清数据后实测）

| 项 | 结果 |
| --- | --- |
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | 8 suite / 30 用例 / 0 失败 |
| 浅色 | `p3-today-light.png`、`p3-calendar-light.png`、`p3-stats-light.png`、`p3-foods-light.png`、`p3-settings-light.png`、`p3-panel-light.png` |
| 深色 | `p3-calendar-dark.png`、`p3-stats-dark.png`、`p3-foods-dark.png`、`p3-settings-dark.png`、`p3-panel-dark.png` |
| 1.3× 字号 | `p3-today-font130.png`、`p3-settings-font130.png`、`p3-panel-font130.png` |
| 行为走查 | 点早餐卡开面板（「记到 早餐」）；最近记录 chip 一步直记（417 kcal、Snackbar 已记/撤销）；选三文鱼 → 克/份切换与「约 139 kcal · 100 g」预览；状态胶囊新文案「部分」；新建自定义食物 TestNoodle → 列表 ListRow；删除 → 撤销（DB 验证按原 id 恢复）；空态组件渲染；日历横滑 10 月→11 月 |

### 9.5 遗留

1. 面板在无键盘时列表占满剩余高度，整体偏高；可考虑限高或半展开态。
2. 设置导出结果仍是内联文字，未换 Snackbar。
3. 「提醒时间」行在开关关闭时仍可点击，且 `setReminderMinute` 总是 `apply(true)`（沿袭旧逻辑），是否要联动开关待业务层确认。
4. 今日页 FAB 压最后一行（P1 遗留，见 §8.4）。
5. 「最近记录」条件 item 在 LazyColumn 内不渲染的原因未查明（见 §9.2），当前用兄弟节点规避。

## 10. Phase 4：薄荷风改版 + 三大营养素（2026-10-24）

用户拍板：补营养素数据（做薄荷同款三色条）、全部页面改薄荷风、保留深色模式。业务决策（Q15–Q19）与数据模型见 `docs/design.md`。

### 10.1 Token 变更（`ui/theme/Color.kt` 全套替换）

| 角色 | 浅色 | 深色 | 说明 |
| --- | --- | --- | --- |
| primary | `#31B672` | `#6FD598` | 清新薄荷绿，替换低饱和自然绿 |
| primaryContainer | `#DDF5E7` | `#1D4433` | 选中态/记满格 |
| background | `#F5F7F6` | `#0E1512` | 冷白底替换暖纸感；windowBackground 同步 |
| surface / outlineVariant | `#FFFFFF` / `#E3E9E5` | `#18201B` / `#2A362E` | 卡片与描边改冷灰调 |
| tertiary | `#E8A23D` | `#EDB566` | 部分完成/提醒改清爽橙 |
| NutrientColors | 碳水 `#4D8DE8` / 蛋白 `#9B7BD8` / 脂肪 `#F2A04E` | 亮化版 | 三大营养素固定三色，独立于 ColorScheme（M3 无对应槽位） |

字阶、圆角、间距、`ui/design/` 组件全部沿用。

### 10.2 结构变更

- **今日页**：`SummaryCard` 重构为左侧 `CalorieRing`（新组件 `ui/design/CalorieRing.kt`：轨道 outlineVariant、进度弧 primary、超目标整环 error；环心「还可摄入 N / 已摄入 X/Y」）+ 右侧三行 `NutrientBar`（名称 + `X/Y g` + 三色圆头条）；状态胶囊与连续徽章保留。四餐从单卡分组改四张独立 `SurfaceCard`（薄荷式）。
- **统计页**：新增「营养素构成」卡，三行三色条显示近 30 天供能占比。
- **自定义食物弹窗**：新增可选填「蛋白质/碳水/脂肪 g/100g」三个字段，留空 = 未知。
- **数据**：数据库 2→3 + 营养快照口径（详见 `docs/design.md`）；今日/统计状态新增 `todayMacro / macroTargets / macroShare`。

### 10.3 验证（2026-10-24，模拟器 Pixel 6 / API 35）

| 项 | 结果 |
| --- | --- |
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | 9 suite / 37 用例 / 0 失败（NutritionTest 7 个新用例） |
| **v2→v3 升级迁移** | 旧版（`eed7c7c`）记「三文鱼 100g」→ 装新版：条目原样保留，营养回填 蛋白 17.2 / 碳水 0 / 脂肪 7.8；159 条内置食物营养 0 缺失；schema 9+3 列齐全 |
| 清数据走查 | 河粉 1 碗（碳水 59/225 g）+ 鸡蛋 1 个（蛋白 9/90、脂肪 5/60）三色条与热量环联动正确 |
| 浅色 | `p4-today-light.png`、`p4-calendar-light.png`、`p4-stats-light.png`、`p4-foods-light.png`、`p4-settings-light.png`、`p4-panel-light.png` |
| 深色 | `p4-today-dark.png`、`p4-stats-dark.png`、`p4-panel-dark.png` |
| 1.3× 字号 | `p4-today-font130.png`、`p4-settings-font130.png` |

### 10.4 遗留

1. 酒精类（啤酒/白酒等）热量含酒精供能但不计入三大营养素，条目热量与营养供能合计可能对不上（种子注释已说明）。
2. 营养目标是固定供能比推导，不是个性化值；后续可考虑按体重/目标做可选输入。
3. Phase 3 的遗留（§9.5）在本轮未处理：面板偏高、导出结果内联文字、提醒时间与开关联动、FAB 压行。

## 11. Phase 4 后续：冗余修正（2026-10-24）

设计走查（对照 HTML 原型）后用户拍板「冗余修正包」：

- **日历页**：落地当日详情卡（状态胶囊 + 热量进度条 + 四餐分布 + 「去这天」跳今日页）与月概览四个 `MetricTile`（记录天数/记满三餐/达标天数/日均热量）；格子点击从直接跳转改为选中速览，消除与今日页的双入口歧义；切月清空选中。
- **统计页**：瓦片改为 当前连续/完整打卡/达标天数/日均热量，删除底部单薄的「当前连续」卡（说明文字并为瓦片下一行）；「最长连续」暂不展示，数据保留给后续成就系统。
- **今日页**：streak=0 时「连续 0 天」改为「记满三餐开启连续打卡」（负反馈修正）；餐次卡头行去掉冗余「+」号（整卡已可点）。
- 原型（`three-meals-boohee-prototype.html`）与 App 同步修改；模拟器实测选中切换、「去这天」跳转、统计页结构、新文案均正常，截图 `p4-calendar-light.png`、`p4-stats-light.png`、`p4-today-light.png` 已更新。
- 情绪价值方向（成就徽章、记满庆祝、本周小结、打卡海报）已评审并记录，用户选择暂缓。

