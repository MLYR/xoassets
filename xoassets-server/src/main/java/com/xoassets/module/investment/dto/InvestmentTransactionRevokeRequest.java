package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 投资交易撤销请求，原因可选但长度受控。
 */
@Data
public class InvestmentTransactionRevokeRequest {

    /**
     * 原因。
     */
    @Size(max = 255, message = "撤销原因不能超过255个字符")
    private String reason;
}
