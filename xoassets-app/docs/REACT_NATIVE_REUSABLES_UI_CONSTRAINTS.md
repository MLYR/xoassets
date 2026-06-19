# React Native Reusables UI 组件库 AI 约束

## 1. 适用范围

本文件约束 `xoassets-app` 移动端 React Native 项目的 UI 组件库、视觉风格、组件使用方式和页面实现方式。

从本文件生效后，移动端 UI 以 **React Native Reusables** 为准。

旧约束中关于以下内容的 UI 风格描述不再作为主标准：

- Web 版 shadcn/ui 安装方式
- Uiverse 风格
- 自研但未落地的 XO Design System 命名
- 纯 Web 组件库约束
- 与 React Native Reusables 冲突的视觉描述

## 2. 总结论

移动端 UI 统一采用：

```text
React Native Reusables + NativeWind + 本项目 src/components/ui 二次封装
```

核心原则：

- 组件风格以 React Native Reusables 为准。
- 页面不能直接堆原生控件，必须优先使用 `src/components/ui` 中的组件。
- `src/components/ui` 是项目内部 UI 组件唯一出口。
- React Native Reusables 组件可以按需引入、复制、改造，但必须收口到 `src/components/ui`。
- 页面层只能消费项目封装后的 UI 组件，不允许每个页面各写一套 Button、Card、Input。

## 3. 技术栈约束

必须使用：

| 类型 | 约束 |
|---|---|
| App 框架 | React Native |
| 开发框架 | Expo |
| 路由 | Expo Router |
| 语言 | TypeScript |
| 样式 | NativeWind |
| UI 组件基准 | React Native Reusables |
| 组件出口 | `src/components/ui` |
| 表单 | React Hook Form |
| 校验 | Zod |
| 状态 | TanStack Query + Zustand |

禁止使用：

| 禁止项 | 原因 |
|---|---|
| Web 版 shadcn/ui 组件 | 不能在 React Native 中直接使用 |
| Radix Web 组件 | 依赖 DOM，不适合 App |
| DOM API / CSS Modules | React Native 不支持 |
| React Native Paper | Material 风格，与本项目 UI 基准不一致 |
| NativeBase | 风格和组件体系不作为本项目标准 |
| UI Kitten | 风格不作为本项目标准 |
| Ant Design Mobile RN | 风格不作为本项目标准 |
| 页面内散写复杂 StyleSheet | 会导致风格失控 |
| 页面内写死颜色、圆角、字号 | 必须走 token / theme / UI 组件 |

## 4. 组件使用规则

### 4.1 组件来源优先级

实现 UI 时按以下优先级选择：

1. 优先使用 `src/components/ui` 已有组件。
2. 如果缺少组件，参考 React Native Reusables 的同类组件实现。
3. 将新增组件放入 `src/components/ui`。
4. 从 `src/components/ui/index.ts` 统一导出。
5. 页面和业务组件只能从 `@/components/ui` 引入。

正确示例：

```tsx
import { Button, Card, CardContent, Input, Text } from '@/components/ui';
```

错误示例：

```tsx
import { Button } from 'react-native-paper';
import { View, TextInput, Pressable } from 'react-native'; // 页面中自行拼复杂业务 UI
```

说明：React Native 原生组件可以用于布局，但不能在页面层重复制造通用 UI 组件。

### 4.2 当前项目基础组件

当前项目应持续维护以下基础组件：

```text
Badge
Button
Card
Input
Label
Separator
Text
```

后续优先补齐：

```text
Avatar
Checkbox
Dialog / AlertDialog
Sheet / BottomSheet
Tabs
Switch
Select
Textarea
Toast
Skeleton
Progress
```

## 5. 视觉风格约束

整体风格以 React Native Reusables 为准，落地到本项目时保持：

| 维度 | 约束 |
|---|---|
| 整体气质 | 极简、现代、克制、组件化 |
| 页面结构 | 卡片化、清晰分区、信息优先 |
| 色彩 | 低饱和，中性色为主 |
| 金融状态色 | 仅在收益、支出、错误、成功、警告场景使用 |
| 边框 | 弱边框、细边框 |
| 圆角 | 中等圆角，不能过度圆润 |
| 阴影 | 轻阴影，不能拟物化 |
| 字体 | 层级清晰，不使用花哨字体 |
| 动效 | 轻量、短时、辅助反馈 |
| 暗黑模式 | 必须同步适配 |

禁止：

- 大面积渐变背景。
- 大量霓虹色。
- 拟物化按钮。
- 玻璃拟态堆叠。
- 页面过度装饰。
- 为了炫酷牺牲可读性。
- Web 端布局直接缩小搬到 App。

## 6. 主题与 token 规则

所有颜色、圆角、间距、字号、阴影必须集中管理。

允许：

```tsx
const theme = useTheme();

<View style={{ backgroundColor: theme.background }} />
```

更推荐：

```tsx
<Card>
  <CardContent>
    <Text variant="subtitle">总资产</Text>
  </CardContent>
</Card>
```

禁止：

```tsx
<Text style={{ color: '#333', fontSize: 17 }} />
<View style={{ borderRadius: 13 }} />
```

例外：

- 临时尺寸微调可以在局部 StyleSheet 中写，但不能定义新的视觉体系。
- 状态色必须来自 theme，例如 `success`、`warning`、`destructive`。

## 7. 页面实现规则

页面应遵循以下结构：

```text
Screen
├── 页面容器
├── Header / Title 区域
├── 核心 Summary Card
├── 分组 Card
├── List / Form / Action
└── 状态反馈
```

页面实现要求：

- 页面内不要重复定义通用组件。
- 复杂 UI 抽到 feature 内部组件或 `src/components/ui`。
- 表单控件优先使用 `Input`、`Button`、`Text`。
- 信息块优先使用 `Card`。
- 标签、状态、分类优先使用 `Badge`。
- 分割线优先使用 `Separator`。
- 文案层级优先使用 `Text` 的 `variant`。

## 8. 移动端交互规则

必须考虑：

- 安全区适配。
- 键盘遮挡。
- 小屏幕滚动。
- iOS / Android 点击反馈差异。
- loading 状态。
- disabled 状态。
- 表单错误提示。
- 空状态。
- 接口失败状态。
- 暗黑模式可读性。

按钮规则：

- 主操作使用 `Button variant="default"`。
- 次操作使用 `secondary` 或 `outline`。
- 弱操作使用 `ghost`。
- 删除、退出、危险操作使用 `destructive`。
- 文本跳转使用 `link`。

## 9. 业务场景 UI 规则

### 9.1 登录 / 注册

- 使用居中或近居中的卡片布局。
- 表单字段使用 `Input`。
- 主按钮使用 `Button size="lg"`。
- 错误提示使用 `Text variant="error"`。
- 页面不能出现复杂装饰背景。

### 9.2 首页资产驾驶舱

- 顶部展示用户和主要操作。
- 第一张卡片展示总资产 / 净资产等核心指标。
- 指标使用卡片网格。
- 最近记录、资产分类使用分组 Card。
- 金额必须格式化，不能直接展示原始 number。

### 9.3 列表页

- 列表项使用卡片或弱分割线。
- 金额、状态、分类必须有清晰层级。
- 空列表必须有空状态。
- 分页 / 下拉刷新后续统一封装。

### 9.4 表单页

- 表单字段纵向排列。
- 错误贴近字段显示。
- 提交按钮固定在底部或表单末尾。
- 保存中必须显示 loading。
- 禁止无反馈重复提交。

## 10. 文件组织规则

UI 组件目录：

```text
src/components/ui
├── Badge.tsx
├── Button.tsx
├── Card.tsx
├── Input.tsx
├── Label.tsx
├── Separator.tsx
├── Text.tsx
└── index.ts
```

新增组件必须遵循：

```text
src/components/ui/ComponentName.tsx
```

并在：

```text
src/components/ui/index.ts
```

统一导出。

业务组件放在：

```text
src/features/<feature>/components
```

页面放在：

```text
src/features/<feature>/screens
```

路由文件只做页面挂载，不写复杂 UI。

## 11. 命名规则

组件命名：

```text
Button
Card
CardHeader
CardContent
CardFooter
Input
Text
Badge
Separator
```

不要再新增未落地的 `XoButton`、`XoCard`、`XoTextField` 这类并行命名，除非全项目统一迁移。

业务组件可以带业务前缀：

```text
AssetSummaryCard
TransactionListItem
BudgetProgressCard
InvestmentPositionCard
```

## 12. AI 生成代码规则

Codex / AI 修改移动端 UI 时必须遵守：

1. 先查看 `xoassets-app/AGENTS.md`。
2. 再查看本文件。
3. 再查看当前 `src/components/ui` 实现。
4. 不允许绕过已有 UI 组件重新造一套样式。
5. 新组件必须和现有组件风格一致。
6. 新增组件必须支持 light / dark theme。
7. 新增组件必须有 TypeScript 类型。
8. 不允许引入与 React Native Reusables 风格冲突的 UI 库。
9. 不允许为了实现页面一次性引入大型依赖。
10. 修改 UI 后必须运行类型检查。

## 13. 验证要求

每次涉及 UI 组件变更后，至少执行：

```bash
npm run typecheck
```

如果涉及 NativeWind、Metro、Babel、主题、路由变更，还需要执行：

```bash
npm run start
npm run android
# 或
npm run ios
```

无法验证时必须在回复中说明：

- 没有验证的命令。
- 没有验证的原因。
- 可能存在的风险。

## 14. 最终约束

本项目移动端 UI 的最终原则：

```text
React Native Reusables 是组件和视觉基准。
NativeWind 是样式基础。
src/components/ui 是项目唯一 UI 组件出口。
页面只组合组件，不重复制造组件体系。
```
