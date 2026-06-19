# 移动端图标与资产 AI 约束

## 1. 适用范围

本文件约束 `xoassets-app` 中图标、Logo、启动图、空状态图和图片资产的使用方式。

UI 视觉基准以 `REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md` 为准。

## 2. 总原则

- 图标风格必须服务于 React Native Reusables 的简洁组件风格。
- 优先使用线性图标。
- 颜色优先使用主题色，不要写死颜色。
- 图标尺寸必须统一。
- 图标资产必须集中管理。
- 不允许页面中散落多个来源、多个风格的图标。

## 3. 推荐图标规则

| 场景 | 规则 |
|---|---|
| Tab 图标 | 线性图标，激活态使用主题主色 |
| 操作按钮 | 图标 + 文案，图标尺寸 16-20 |
| 空状态 | 简洁线性插画或图标，不能过度装饰 |
| 状态提示 | 成功、警告、错误使用主题状态色 |
| 财务分类 | 优先用统一线性图标，不混用彩色图标包 |

## 4. 资产目录建议

```text
assets
├── icons
├── images
├── logo
└── splash
```

如果后续需要业务分类图标，建议：

```text
assets/icons/categories
assets/icons/accounts
assets/icons/investments
```

## 5. 禁止事项

- 不要直接使用 Web SVG 组件代码。
- 不要混用多套风格差异很大的图标库。
- 不要在页面内写死图标颜色。
- 不要使用复杂渐变图标作为基础 UI 图标。
- 不要使用版权不明确的第三方图片。
- 不要把大图直接塞进代码仓库而不压缩。
- 不要使用过大的 PNG 作为普通图标。

## 6. SVG 使用规则

如使用 SVG：

- 必须适配 React Native。
- 使用 `react-native-svg`。
- SVG 组件必须集中放置。
- 颜色应支持通过 props 或 theme 注入。
- 不允许直接复制带 DOM 属性的 Web SVG 而不清理。

## 7. App 图标与启动图

App 图标：

- 保持简洁、可识别。
- 适配浅色和深色背景。
- 避免复杂细节。
- iOS / Android 尺寸按 Expo 配置要求维护。

启动图：

- 简洁展示 Logo 或品牌名。
- 背景色跟随项目主题。
- 不做复杂动效。
- 不放业务数据。

## 8. AI 执行规则

Codex / AI 处理图标资产时必须：

1. 先读 `xoassets-app/AGENTS.md`。
2. 再读 `xoassets-app/docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md`。
3. 再读本文件。
4. 新增图标前检查是否已有可复用资产。
5. 不新增版权不明图片。
6. 不把图标颜色写死在页面里。
7. 不引入大型图标依赖，除非用户明确确认。
8. 生成或新增资产后说明文件路径、用途和风险。
