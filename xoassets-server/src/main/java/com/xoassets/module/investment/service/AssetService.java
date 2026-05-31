package com.xoassets.module.investment.service;

import com.xoassets.module.investment.dto.AssetRequest;
import com.xoassets.module.investment.vo.AssetVO;
import com.xoassets.persistence.entity.Asset;
import java.util.List;

/**
 * 公共资产服务。
 */
public interface AssetService {

    /**
     * 按关键词和类型搜索公共资产。
     */
    List<AssetVO> search(String keyword, String type);

    /**
     * 创建公共资产。
     */
    AssetVO create(AssetRequest request);

    /**
     * 查询公共资产，不存在时抛出业务异常。
     */
    Asset findAsset(Long id);
}
