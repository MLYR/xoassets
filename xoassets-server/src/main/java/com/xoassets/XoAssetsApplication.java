package com.xoassets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 小〇财迹后端启动类。
 */
@EnableScheduling
@SpringBootApplication
public class XoAssetsApplication {

    /**
     * 启动 Spring Boot 应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(XoAssetsApplication.class, args);
    }
}
