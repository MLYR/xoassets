package com.xoassets.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 配置：提供第一期后端接口文档入口。
 */
@Configuration
public class Knife4jConfig {

    /**
     * 注册 OpenAPI 基础信息，供 Knife4j 页面展示。
     */
    @Bean
    public OpenAPI xoAssetsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("小〇财迹 API")
                        .description("第一期 MVP 后端接口")
                        .version("0.1.0"));
    }
}
