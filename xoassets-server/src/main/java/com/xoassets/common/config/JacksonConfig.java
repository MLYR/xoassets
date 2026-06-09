package com.xoassets.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * JSON 序列化配置：统一处理 Long ID 和前端日期时间格式。
 */
@Configuration
public class JacksonConfig {

    /**
     * 时间输出格式化器。
     */
    private static final DateTimeFormatter LOCAL_DATE_TIME_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 时间输入格式列表。
     */
    private static final List<DateTimeFormatter> LOCAL_DATE_TIME_INPUTS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    /**
     * MyBatis-Plus 雪花 ID 超过 JS 安全整数范围，LocalDateTime 兼容移动端分钟级时间输入。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longIdToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance)
                .serializerByType(LocalDateTime.class, new LocalDateTimeJsonSerializer())
                .deserializerByType(LocalDateTime.class, new LocalDateTimeJsonDeserializer());
    }

    /**
     * LocalDateTime 输出保持后端接口现有空格分隔格式，减少前端展示端兼容成本。
     */
    private static class LocalDateTimeJsonSerializer extends JsonSerializer<LocalDateTime> {
        /**
         * 序列化本地日期时间。
         */
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.format(LOCAL_DATE_TIME_OUTPUT));
        }
    }

    /**
     * 兼容 uni-app time picker 只返回 HH:mm 导致的分钟级时间，同时保留秒级和 ISO 输入。
     */
    private static class LocalDateTimeJsonDeserializer extends JsonDeserializer<LocalDateTime> {
        /**
         * 反序列化本地日期时间。
         */
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText();
            if (!StringUtils.hasText(value)) {
                return null;
            }
            String normalized = value.trim();
            for (DateTimeFormatter formatter : LOCAL_DATE_TIME_INPUTS) {
                try {
                    return LocalDateTime.parse(normalized, formatter);
                } catch (RuntimeException ignored) {
                    // 继续尝试下一个格式，统一在最后交给 Jackson 报出原始字段错误。
                }
            }
            return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, value, "Unsupported LocalDateTime format");
        }
    }
}
