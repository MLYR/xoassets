# 验证清单

## 1. 后端验证

```bash
cd xoassets-server
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
```

或：

```bash
./mvnw test
```

如果环境不适合运行测试，必须说明原因和风险。

## 2. Web 验证

```bash
cd xoassets-web
npm install
npm run build
```

如果存在 lint / typecheck 脚本，优先运行：

```bash
npm run lint
npm run typecheck
```

## 3. Flutter 验证

```bash
cd xoassets-mobile
flutter pub get
flutter analyze
flutter test
flutter run
```

后续上架阶段验证：

```bash
flutter build apk
flutter build appbundle
flutter build ios
```

如果 iOS 环境不可用，需要说明原因。

## 4. MVP 验收清单

- 登录 `demo / xoassets123` 后能进入首页。
- 首页总资产和净资产优先读取最新资产快照。
- 现金资产只统计正余额账户，负余额账户计入负债。
- 手动调用 `POST /api/snapshots/generate-today` 可生成 / 更新今天快照。
- 数据分析页展示净资产、总资产、现金 / 投资资产趋势。
- 新增收入后账户余额增加。
- 新增支出后账户余额减少。
- 转账只改变账户分布，不计入收支。
- 账户详情能展示普通收支、转账、退款、投资买入、投资卖出和余额修正资金明细。
- 删除流水后账户余额按原流水影响反向恢复。
- 预算统计只计算支出和退款，转账不计入预算。
- 投资持仓市值使用后端返回的 `latestPrice` 计算。
- DOGE 当前价至少显示 6 位小数。
- 投资买入扣减资金账户余额。
- 投资卖出增加资金账户余额。
- 投资交易撤销后账户余额和持仓数量 / 成本反向恢复。
- 已撤销交易不参与账户资金明细汇总。
- 账户详情和普通流水可导出 CSV，Excel 打开中文不乱码。
- 数据分析页收支趋势排除转账。
- 投资盈亏使用最新价格快照。
- 用户 A 不能查看或修改用户 B 的账户、分类、流水、持仓、预算、目标。

## 5. GitHub Actions 预期

- 后端执行 `mvn test`。
- Web 执行 `npm run build`。
- App 执行 `npm run type-check`，旧 uni-app 项目如继续维护才执行。
