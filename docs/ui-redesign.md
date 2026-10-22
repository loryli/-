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
