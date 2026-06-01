package com.xoassets.module.export.service;

import com.xoassets.module.export.dto.AccountLedgerExportQuery;
import com.xoassets.module.export.dto.InvestmentTransactionExportQuery;
import com.xoassets.module.transaction.dto.TransactionQuery;

/**
 * CSV 数据导出服务。
 */
public interface ExportService {

    /**
     * 导出账户资金明细 CSV。
     */
    ExportFile accountLedger(AccountLedgerExportQuery query);

    /**
     * 导出普通流水 CSV。
     */
    ExportFile transactions(TransactionQuery query);

    /**
     * 导出投资交易 CSV。
     */
    ExportFile investmentTransactions(InvestmentTransactionExportQuery query);

    /**
     * 导出文件内容和文件名。
     */
    record ExportFile(String filename, byte[] content) {
    }
}
