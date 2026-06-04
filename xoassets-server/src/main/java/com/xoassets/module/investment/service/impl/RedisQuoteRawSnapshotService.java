package com.xoassets.module.investment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xoassets.module.investment.service.QuoteRawSnapshot;
import com.xoassets.module.investment.service.QuoteRawSnapshotService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis ZSET 实现，key 形如 price:snapshot:{assetId}:{yyyyMM}，score 为报价时间毫秒。
 */
@Slf4j
@Service
public class RedisQuoteRawSnapshotService implements QuoteRawSnapshotService {

    private static final Duration SNAPSHOT_TTL = Duration.ofDays(35);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisQuoteRawSnapshotService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Redis 是短期原始快照缓存，写入失败只记录日志，不能阻断 MySQL 当前价更新。
     */
    @Override
    public void append(QuoteRawSnapshot snapshot) {
        if (snapshot == null || snapshot.assetId() == null || snapshot.quoteTime() == null) {
            return;
        }
        String key = key(snapshot.assetId(), YearMonth.from(snapshot.quoteTime()));
        try {
            String value = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForZSet().add(key, value, score(snapshot.quoteTime()));
            redisTemplate.expire(key, SNAPSHOT_TTL);
        } catch (Exception exception) {
            log.warn("Redis 行情原始快照写入失败 assetId={}", snapshot.assetId(), exception);
        }
    }

    /**
     * 按自然日读取原始快照；日级收益最终落 MySQL，Redis 缺失时汇总任务会用 current 兜底。
     */
    @Override
    public List<QuoteRawSnapshot> listByDate(Long assetId, LocalDate date) {
        if (assetId == null || date == null) {
            return Collections.emptyList();
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        Set<String> values = redisTemplate.opsForZSet().rangeByScore(key(assetId, YearMonth.from(date)), score(start), score(end));
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::readSnapshot)
                .filter(item -> item != null && assetId.equals(item.assetId()))
                .sorted((left, right) -> left.quoteTime().compareTo(right.quoteTime()))
                .toList();
    }

    private QuoteRawSnapshot readSnapshot(String value) {
        try {
            return objectMapper.readValue(value, QuoteRawSnapshot.class);
        } catch (JsonProcessingException exception) {
            log.warn("Redis 行情原始快照解析失败", exception);
            return null;
        }
    }

    private String key(Long assetId, YearMonth month) {
        return "price:snapshot:" + assetId + ":" + month.format(MONTH_FORMATTER);
    }

    private double score(LocalDateTime quoteTime) {
        return quoteTime.atZone(ZONE_ID).toInstant().toEpochMilli();
    }
}
