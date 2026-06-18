# 原文档拆分映射

| 原文件 | 主要内容 | 拆分到 |
|---|---|---|
| `AGENTS.md` | 项目定位、技术栈、业务口径、接口、安全、Codex 规则 | `AGENTS.md`、`docs/BUSINESS_RULES.md`、`docs/API_CONTRACTS.md`、`docs/AI_AGENT_WORKFLOW.md`、`.cursor/rules/*.mdc` |
| `README.md` | 项目结构、当前进度、联调状态、本地开发、部署、MVP 验收 | `README.md`、`docs/PROJECT_OVERVIEW.md`、`docs/LOCAL_DEVELOPMENT.md`、`docs/VALIDATION_CHECKLIST.md` |
| `xoassets-mobile/AGENTS.md` | Flutter 技术栈、目录、UI、网络、页面、阶段规则 | `xoassets-mobile/AGENTS.md`、`xoassets-mobile/docs/MOBILE_PRODUCT_SPEC.md`、`xoassets-mobile/docs/MOBILE_API_INTEGRATION.md` |
| `xoassets-mobile/MOBILE_APP_PHASES.md` | 移动端阶段计划、UI 规范、图标规范、Codex 工作流 | `xoassets-mobile/MOBILE_APP_PHASES.md`、`xoassets-mobile/docs/MOBILE_UI_DESIGN_SYSTEM.md`、`xoassets-mobile/docs/MOBILE_ICON_ASSETS_GUIDE.md` |
| `xoassets-mobile/README.md` | 移动端运行方式、接口状态、已完成阶段 | `xoassets-mobile/README.md`、`xoassets-mobile/docs/MOBILE_API_INTEGRATION.md` |
| `xoassets-mobile/design-qa.md` | 登录/注册页 Design QA | `xoassets-mobile/docs/MOBILE_QA.md` |

## 处理策略

1. `AGENTS.md` 保持短而强，只放 AI 必须遵守的规则。
2. 大量业务口径从 `AGENTS.md` 拆到 `docs/BUSINESS_RULES.md`。
3. 大量接口清单从 `README.md` 拆到 `docs/API_CONTRACTS.md`。
4. 移动端 UI 和图标规范从阶段文档中独立出来，避免 `MOBILE_APP_PHASES.md` 过长。
5. 阶段文档只保留阶段目标、任务、禁止项、验收标准。
6. Cursor、Copilot 规则只做引用和摘要，不重复塞入完整业务文档。
