# Codex 工作流

## 1. 执行前

Codex 每次执行任务前必须先输出：

```text
当前目录结构分析
已读取的约束文档
准备新增的文件
准备修改的文件
本次执行计划
风险点
需要用户确认的问题
```

规则：

- 高风险变更必须先确认，例如删除文件、大规模重构、替换技术栈、引入重型依赖。
- 用户已经明确要求修改文档或代码时，允许直接执行，但必须保持最小影响范围。
- 不确定接口、字段、业务口径时，优先查文档和代码，不要凭空编。

## 2. 执行中

必须遵守：

```text
只做当前任务
只做当前阶段
不越阶段实现
不引入未批准依赖
不删除无关文件
不修改无关模块
不做顺手重构
不批量格式化无关文件
不修改后端以外的无关项目
不修改 Web 以外的无关项目
不修改移动端以外的无关项目
```

## 3. 执行后

Codex 必须输出：

```text
完成内容
新增文件
修改文件
删除文件，若有
新增依赖，若有
运行命令
验证结果
遗留问题
风险点
下一步建议
```

## 4. 依赖管理

新增依赖必须说明：

- 依赖名称
- 用途
- 为什么需要
- 是否有替代方案
- 是否影响包体积
- 是否影响构建
- 是否为长期维护依赖

未经用户确认，不要引入重型依赖。

移动端 UI 依赖规则：

- UI 组件和视觉基准以 React Native Reusables 为准。
- 样式基础为 NativeWind。
- 项目 UI 组件统一从 `src/components/ui` 导出。
- 不引入 React Native Paper / NativeBase / UI Kitten 等与当前风格冲突的大型主题型 UI 库。

## 5. React Native 阶段任务模板

```md
请根据当前目录下的约束文档执行任务。

必须读取：

1. 根目录 `AGENTS.md`
2. `xoassets-app/AGENTS.md`
3. `xoassets-app/docs/REACT_NATIVE_REUSABLES_UI_CONSTRAINTS.md`
4. `xoassets-app/docs/MOBILE_API_INTEGRATION.md`
5. `xoassets-app/MOBILE_APP_PHASES.md`
6. 当前代码实现

当前阶段：

阶段 X：{阶段名称}

要求：

1. 严格遵守 `AGENTS.md`。
2. 严格遵守 `MOBILE_APP_PHASES.md`。
3. UI 以 React Native Reusables + NativeWind + `src/components/ui` 为准。
4. 只做当前阶段内容，不要越阶段实现。
5. 不要引入 SQLite / WatermelonDB / Realm / TypeORM / Prisma Client。
6. 不要直接连接 MySQL。
7. 不要修改后端代码，除非本阶段明确要求。
8. 不要修改 Web 端代码，除非本阶段明确要求。
9. 不要删除无关文件。
10. 修改前先扫描项目结构。

请先输出：

- 当前项目结构分析
- 准备新增的文件
- 准备修改的文件
- 本阶段实现计划
- 风险点
```

## 6. 验证要求

常用验证：

```bash
cd xoassets-server && ./mvnw test
cd xoassets-web && npm run build
cd xoassets-app && npm run typecheck
```

移动端涉及运行环境时：

```bash
cd xoassets-app
npm run start
npm run android
npm run ios
```

无法验证时必须说明：

- 未运行的命令。
- 未运行的原因。
- 可能存在的风险。
