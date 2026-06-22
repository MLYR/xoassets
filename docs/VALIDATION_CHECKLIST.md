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

## 3. React Native 验证

```bash
cd xoassets-app
npm install
npm run typecheck
npm run start
```

Android：

```bash
npm run android:emulator
npm run android
```

iOS：

```bash
npm run ios
```

如果当前环境不可运行 Android / iOS，必须说明原因。

## 3.1 记账页面交互验证

- 首页「最近记录」的「查看全部」可跳转到记账页日历，并默认定位当天。
- 记账页日历支持左右滑动切换月份。
- 记账页统计模块的趋势图支持手势查看日期与金额。
- 统计页「分类排行」与「明细排行」点击可查看对应明细。
- 记账明细详情页按两列展示字段。

## 4. MVP 验收清单

- 登录 `demo / xoassets123` 后能进入首页。
- 首页总资产和净资产优先读取后端数据。
- 现金资产只统计正余额账户，负余额账户计入负债。
- 记账新增收入后账户余额增加。
- 记账新增支出后账户余额减少。
- 转账只改变账户分布，不计入收支。
- 预算统计只计算支出和退款，转账不计入预算。
- 投资持仓市值使用后端返回字段。
- 投资买入扣减资金账户余额。
- 投资卖出增加资金账户余额。
- 投资收益口径分基金、股票、虚拟货币展示。
- 用户 A 不能查看或修改用户 B 的账户、分类、流水、持仓、预算、目标。
- App 不直连数据库。
- App 不保存生产密钥。
- App 不在日志打印 Token、密码、金额明细。
