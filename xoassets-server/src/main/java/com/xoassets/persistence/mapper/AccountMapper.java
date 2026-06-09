package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.Account;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 账户 Mapper。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 加锁读取用户账户，供余额修正这类需要记录修正前余额的事务使用。
     */
    @Select("""
            SELECT *
            FROM xo_account
            WHERE id = #{accountId} AND user_id = #{userId} AND deleted = 0
            FOR UPDATE
            """)
    /**
     * 锁定当前用户账户。
     */
    Account selectOwnedForUpdate(@Param("userId") Long userId, @Param("accountId") Long accountId);

    /**
     * 原子调整账户余额，避免并发读改写覆盖。
     */
    @Update("""
            UPDATE xo_account
            SET balance = balance + #{delta}, version = version + 1
            WHERE id = #{accountId} AND user_id = #{userId} AND deleted = 0
            """)
    /**
     * 按增量更新账户余额。
     */
    int incrementBalance(@Param("userId") Long userId, @Param("accountId") Long accountId, @Param("delta") BigDecimal delta);
}
