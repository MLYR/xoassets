package com.xoassets.module.export.controller;

import com.xoassets.module.export.dto.AccountLedgerExportQuery;
import com.xoassets.module.export.dto.InvestmentTransactionExportQuery;
import com.xoassets.module.export.service.ExportService;
import com.xoassets.module.transaction.dto.TransactionQuery;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户数据 CSV 导出接口。
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * 导出账户资金明细。
     */
    @GetMapping("/account-ledger")
    public ResponseEntity<byte[]> accountLedger(@ModelAttribute AccountLedgerExportQuery query) {
        return csv(exportService.accountLedger(query));
    }

    /**
     * 导出普通流水。
     */
    @GetMapping("/transactions")
    public ResponseEntity<byte[]> transactions(@ModelAttribute TransactionQuery query) {
        return csv(exportService.transactions(query));
    }

    /**
     * 导出投资交易。
     */
    @GetMapping("/investment-transactions")
    public ResponseEntity<byte[]> investmentTransactions(@ModelAttribute InvestmentTransactionExportQuery query) {
        return csv(exportService.investmentTransactions(query));
    }

    /**
     * CSV 使用 UTF-8 BOM，兼容 Excel 中文打开。
     */
    private ResponseEntity<byte[]> csv(ExportService.ExportFile file) {
        String filename = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}
