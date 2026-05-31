package com.xoassets.module.investment.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.investment.dto.AssetRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.vo.AssetVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共资产接口。
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /**
     * 搜索公共资产。
     */
    @GetMapping("/search")
    public Result<List<AssetVO>> search(@RequestParam(required = false) String keyword, @RequestParam(required = false) String type) {
        return Result.success(assetService.search(keyword, type));
    }

    /**
     * 新增公共资产。
     */
    @PostMapping
    public Result<AssetVO> create(@Valid @RequestBody AssetRequest request) {
        return Result.success(assetService.create(request));
    }
}
