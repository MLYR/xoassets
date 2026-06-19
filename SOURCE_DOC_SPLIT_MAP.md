# 原文档拆分映射

| 原文件 | 主要内容 | 当前拆分到 |
|---|---|---|
| `AGENTS.md` | 项目定位、技术栈、业务口径、接口、安全、Codex 规则 | `AGENTS.md`、`docs/BUSINESS_RULES.md`、`docs/API_CONTRACTS.md`、`docs/CODEX_WORKFLOW.md` |
| `README.md` | 项目结构、当前进度、联调状态、本地开发、部署、MVP 验收 | `README.md`、`docs/PROJECT_OVERVIEW.md`、`docs/LOCAL_DEVELOPMENT.md`、`docs/VALIDATION_CHECKLIST.md` |
| 旧移动端约束 | Flutter / 旧 mobile 目录 / 旧 UI 风格 | 已废弃，不再作为 `xoassets-app` 标准 |
| `xoassets-app/AGENTS.md` | React Native 技术栈、移动端目录、UI、网络、阶段规则 | `xoassets-app/AGENTS.md`、`xoassets-app/docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md`、`xoassets-app/docs/MOBILE_API_INTEGRATION.md` |
| `xoassets-app/MOBILE_APP_PHASES.md` | 移动端阶段计划、阶段任务、禁止项、验收标准 | `xoassets-app/MOBILE_APP_PHASES.md`、`xoassets-app/docs/MOBILE_PRODUCT_SPEC.md`、`xoassets-app/docs/MOBILE_ICON_ASSETS_GUIDE.md` |
| `xoassets-app/README.md` | 移动端运行方式、接口地址、文档入口 | `xoassets-app/README.md`、`xoassets-app/docs/MOBILE_API_INTEGRATION.md` |

## 处理策略

1. `AGENTS.md` 保持短而强，只放 AI 必须遵守的根规则。
2. 大量业务口径从 `AGENTS.md` 拆到 `docs/BUSINESS_RULES.md`。
3. 大量接口清单从 `README.md` 拆到 `docs/API_CONTRACTS.md`。
4. 移动端 UI 以 React Native Reusables 为准，统一沉淀到 `xoassets-app/docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md`。
5. 移动端产品范围、接口接入、图标资产分别独立成文档，避免阶段文档过长。
6. `MOBILE_APP_PHASES.md` 只保留阶段目标、任务、禁止项、验收标准。
7. Cursor、Copilot、Codex 等 AI 规则只做引用和摘要，不重复塞入完整业务文档。
