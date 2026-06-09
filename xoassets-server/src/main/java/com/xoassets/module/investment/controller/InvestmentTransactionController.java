package com.xoassets.module.investment.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.investment.dto.InvestmentTransactionConvertRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionRevokeRequest;
import com.xoassets.module.investment.service.InvestmentTransactionService;
import com.xoassets.module.investment.vo.FundConfirmPreviewVO;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投资交易接口。
 */
@RestController
@RequestMapping("/api/investment-transactions")
public class InvestmentTransactionController {

    /**
     * 流水服务。
     */
    private final InvestmentTransactionService transactionService;

    /**
     * 注入接口依赖。
     */
    public InvestmentTransactionController(InvestmentTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 创建买入或卖出交易。
     */
    @PostMapping
    public Result<InvestmentTransactionVO> create(@Valid @RequestBody InvestmentTransactionRequest request) {
        return Result.success(transactionService.create(request));
    }

    /**
     * 转换持仓，生成一笔卖出和一笔买入交易。
     */
    @PostMapping("/convert")
    public Result<List<InvestmentTransactionVO>> convert(@Valid @RequestBody InvestmentTransactionConvertRequest request) {
        return Result.success(transactionService.convert(request));
    }

    /**
     * 查询投资交易，可按持仓过滤。
     */
    @GetMapping
    public Result<List<InvestmentTransactionVO>> list(@RequestParam(required = false) Long holdingId) {
        return Result.success(transactionService.list(holdingId));
    }

    /**
     * 预估基金金额买入的申请日和确认日。
     */
    @GetMapping("/fund-confirm-preview")
    public Result<FundConfirmPreviewVO> fundConfirmPreview(
            @RequestParam Long assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime transactionTime) {
        return Result.success(transactionService.previewFundConfirm(assetId, transactionTime));
    }

    /**
     * 撤销投资交易，并反向恢复资金账户和持仓。
     */
    @PutMapping("/{id}/revoke")
    public Result<InvestmentTransactionVO> revoke(@PathVariable Long id, @Valid @RequestBody InvestmentTransactionRevokeRequest request) {
        return Result.success(transactionService.revoke(id, request));
    }
}
