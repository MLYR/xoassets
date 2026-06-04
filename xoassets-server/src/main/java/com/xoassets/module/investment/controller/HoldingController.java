package com.xoassets.module.investment.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.investment.dto.HoldingRequest;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.vo.HoldingDetailVO;
import com.xoassets.module.investment.vo.HoldingSummaryVO;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentTrendPointVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投资持仓接口。
 */
@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    /**
     * 查询当前用户持仓列表。
     */
    @GetMapping
    public Result<List<HoldingVO>> list() {
        return Result.success(holdingService.list());
    }

    /**
     * 查询当前用户持仓汇总。
     */
    @GetMapping("/summary")
    public Result<HoldingSummaryVO> summary() {
        return Result.success(holdingService.summary());
    }

    /**
     * 查询当前用户投资资产趋势，供移动端统计图拖拽查看历史点位。
     */
    @GetMapping("/trend")
    public Result<List<InvestmentTrendPointVO>> trend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(holdingService.trend(startDate, endDate));
    }

    /**
     * 查询当前用户单个持仓详情。
     */
    @GetMapping("/{id}/detail")
    public Result<HoldingDetailVO> detail(@PathVariable Long id) {
        return Result.success(holdingService.detail(id));
    }

    /**
     * 新增持仓。
     */
    @PostMapping
    public Result<HoldingVO> create(@Valid @RequestBody HoldingRequest request) {
        return Result.success(holdingService.create(request));
    }

    /**
     * 修改持仓。
     */
    @PutMapping("/{id}")
    public Result<HoldingVO> update(@PathVariable Long id, @Valid @RequestBody HoldingRequest request) {
        return Result.success(holdingService.update(id, request));
    }

    /**
     * 删除持仓。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        holdingService.delete(id);
        return Result.success(null);
    }
}
