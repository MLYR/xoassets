package com.xoassets.module.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.dto.AccountBalanceAdjustmentRequest;
import com.xoassets.module.account.service.AccountBalanceService;
import com.xoassets.module.account.vo.AccountBalanceAdjustmentVO;
import com.xoassets.module.account.vo.AccountBalanceTrendPointVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.AccountBalanceAdjustment;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountBalanceAdjustmentMapper;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户余额修正和日终余额曲线实现。
 */
@Service
public class AccountBalanceServiceImpl implements AccountBalanceService {

    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final AccountMapper accountMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final AccountBalanceAdjustmentMapper adjustmentMapper;

    public AccountBalanceServiceImpl(
            AccountMapper accountMapper,
            TransactionRecordMapper transactionRecordMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            AccountBalanceAdjustmentMapper adjustmentMapper) {
        this.accountMapper = accountMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.adjustmentMapper = adjustmentMapper;
    }

    /**
     * 余额修正在同一事务内锁定账户、更新余额并写入不可变更修正事件。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AccountBalanceAdjustmentVO adjustBalance(Long accountId, AccountBalanceAdjustmentRequest request) {
        Long userId = LoginUserContext.getUserId();
        Account account = accountMapper.selectOwnedForUpdate(userId, accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账户不存在");
        }
        BigDecimal before = scale4(account.getBalance());
        BigDecimal after = scale4(request.getAfterBalance());
        BigDecimal delta = after.subtract(before).setScale(4, RoundingMode.HALF_UP);
        AccountBalanceAdjustment adjustment = new AccountBalanceAdjustment();
        adjustment.setUserId(userId);
        adjustment.setAccountId(accountId);
        adjustment.setBeforeBalance(before);
        adjustment.setAfterBalance(after);
        adjustment.setDeltaAmount(delta);
        adjustment.setReason(request.getReason());
        adjustment.setOperatorType("USER");
        LocalDateTime bizTime = resolveBizTime(request);
        adjustment.setBizTime(bizTime);
        adjustment.setBizDate(bizTime.toLocalDate());
        adjustment.setDeleted(0);
        adjustmentMapper.insert(adjustment);
        if (delta.compareTo(BigDecimal.ZERO) != 0 && accountMapper.incrementBalance(userId, accountId, delta) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户余额更新冲突，请重试");
        }
        return toVO(adjustment);
    }

    /**
     * 余额曲线按初始余额和历史资金事件重建，缺少预生成快照时也能返回正确日终余额。
     */
    @Override
    public List<AccountBalanceTrendPointVO> balanceTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long userId = LoginUserContext.getUserId();
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId)
                .eq(Account::getId, accountId));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账户不存在");
        }
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(29) : startDate;
        return rebuildTrend(userId, account, start, end);
    }

    private List<AccountBalanceTrendPointVO> rebuildTrend(Long userId, Account account, LocalDate start, LocalDate end) {
        LocalDate firstDate = account.getCreatedAt() == null ? start : account.getCreatedAt().toLocalDate();
        LocalDate cursor = firstDate.isBefore(start) ? firstDate : start;
        Map<LocalDate, DailyFlow> grouped = groupedFlows(userId, account.getId(), cursor, end);
        BigDecimal balance = scale4(account.getInitialBalance());
        List<AccountBalanceTrendPointVO> points = new ArrayList<>();
        while (!cursor.isAfter(end)) {
            DailyFlow flow = grouped.getOrDefault(cursor, new DailyFlow());
            balance = balance.add(flow.net()).setScale(4, RoundingMode.HALF_UP);
            if (!cursor.isBefore(start)) {
                points.add(AccountBalanceTrendPointVO.builder()
                        .date(cursor.toString())
                        .endBalance(balance)
                        .inflow(flow.inflow)
                        .outflow(flow.outflow)
                        .adjustmentAmount(flow.adjustment)
                        .build());
            }
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    private Map<LocalDate, DailyFlow> groupedFlows(Long userId, Long accountId, LocalDate start, LocalDate end) {
        Map<LocalDate, DailyFlow> result = new LinkedHashMap<>();
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);
        transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, userId)
                        .and(item -> item.eq(TransactionRecord::getAccountId, accountId).or().eq(TransactionRecord::getTargetAccountId, accountId))
                        .ge(TransactionRecord::getTransactionTime, startTime)
                        .le(TransactionRecord::getTransactionTime, endTime))
                .forEach(record -> addFlow(result, record.getTransactionTime().toLocalDate(), signedTransactionAmount(accountId, record), BigDecimal.ZERO));
        investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .eq(InvestmentTransaction::getAccountId, accountId)
                        .ge(InvestmentTransaction::getTransactionTime, startTime)
                        .le(InvestmentTransaction::getTransactionTime, endTime))
                .stream()
                .filter(record -> !STATUS_REVOKED.equals(record.getStatus()) && !STATUS_CANCELLED.equals(record.getStatus()))
                .forEach(record -> addFlow(result, record.getTransactionTime().toLocalDate(), signedInvestmentAmount(record), BigDecimal.ZERO));
        adjustmentMapper.selectList(new LambdaQueryWrapper<AccountBalanceAdjustment>()
                        .eq(AccountBalanceAdjustment::getUserId, userId)
                        .eq(AccountBalanceAdjustment::getAccountId, accountId)
                        .between(AccountBalanceAdjustment::getBizDate, start, end))
                .forEach(record -> addFlow(result, record.getBizDate(), record.getDeltaAmount(), record.getDeltaAmount()));
        return result;
    }

    private void addFlow(Map<LocalDate, DailyFlow> result, LocalDate date, BigDecimal amount, BigDecimal adjustment) {
        DailyFlow flow = result.computeIfAbsent(date, key -> new DailyFlow());
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            flow.inflow = flow.inflow.add(amount).setScale(4, RoundingMode.HALF_UP);
        } else {
            flow.outflow = flow.outflow.add(amount.abs()).setScale(4, RoundingMode.HALF_UP);
        }
        flow.adjustment = flow.adjustment.add(adjustment).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal signedTransactionAmount(Long accountId, TransactionRecord record) {
        if ("TRANSFER".equals(record.getType())) {
            return Objects.equals(accountId, record.getTargetAccountId()) ? record.getAmount() : record.getAmount().negate();
        }
        if ("EXPENSE".equals(record.getType())) {
            return record.getAmount().negate();
        }
        return record.getAmount();
    }

    private BigDecimal signedInvestmentAmount(InvestmentTransaction record) {
        if ("BUY".equals(record.getType())) {
            BigDecimal amount = record.getTradeAmount() == null ? record.getAmount().add(record.getFee()) : record.getTradeAmount();
            return amount.negate();
        }
        return record.getAmount().subtract(record.getFee());
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    private AccountBalanceAdjustmentVO toVO(AccountBalanceAdjustment adjustment) {
        return AccountBalanceAdjustmentVO.builder()
                .id(adjustment.getId())
                .accountId(adjustment.getAccountId())
                .beforeBalance(adjustment.getBeforeBalance())
                .afterBalance(adjustment.getAfterBalance())
                .deltaAmount(adjustment.getDeltaAmount())
                .reason(adjustment.getReason())
                .bizDate(adjustment.getBizDate())
                .bizTime(adjustment.getBizTime())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }

    private LocalDateTime resolveBizTime(AccountBalanceAdjustmentRequest request) {
        if (request.getBizTime() != null) {
            return request.getBizTime();
        }
        if (request.getBizDate() != null) {
            return LocalDateTime.of(request.getBizDate(), LocalTime.MIN);
        }
        return LocalDateTime.now();
    }

    private static class DailyFlow {
        private BigDecimal inflow = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal outflow = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        private BigDecimal adjustment = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        private BigDecimal net() {
            return inflow.subtract(outflow).setScale(4, RoundingMode.HALF_UP);
        }
    }
}
