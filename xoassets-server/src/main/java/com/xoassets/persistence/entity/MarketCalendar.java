package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 市场交易日历，作为基金确认日和后续行情交易日判断的数据库权威来源。
 */
@Data
@TableName("xo_market_calendar")
public class MarketCalendar {

    /**
     * 主键ID。
     */
    @TableId
    private Long id;
    /**
     * 交易市场。
     */
    private String market;
    /**
     * 交易日期。
     */
    private LocalDate tradeDate;
    /**
     * 是否交易日。
     */
    private Boolean tradingDay;
    /**
     * 来源。
     */
    private String source;
    /**
     * 备注。
     */
    private String remark;
    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;
}
