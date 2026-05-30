package com.xoassets.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 序列化配置：Long ID 以字符串返回，避免前端 JavaScript number 精度丢失。
 */
@Configuration
public class JacksonConfig {

    /**
     * MyBatis-Plus 雪花 ID 超过 JS 安全整数范围，统一序列化为字符串。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longIdToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
