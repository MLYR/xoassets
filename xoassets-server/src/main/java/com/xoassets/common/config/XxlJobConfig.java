package com.xoassets.common.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 执行器配置；业务任务只注册 handler，触发时间统一在调度中心可视化维护。
 */
@Configuration
public class XxlJobConfig {

    /**
     * 构建 XXL-JOB Spring 执行器，默认关闭，避免未部署调度中心时影响本地启动。
     */
    @Bean
    @ConditionalOnProperty(prefix = "xxl.job.executor", name = "enabled", havingValue = "true")
    public XxlJobSpringExecutor xxlJobExecutor(
            @Value("${xxl.job.admin.addresses}") String adminAddresses,
            @Value("${xxl.job.accessToken:}") String accessToken,
            @Value("${xxl.job.executor.appname}") String appName,
            @Value("${xxl.job.executor.address:}") String address,
            @Value("${xxl.job.executor.ip:}") String ip,
            @Value("${xxl.job.executor.port:9999}") int port,
            @Value("${xxl.job.executor.logpath:logs/xxl-job}") String logPath,
            @Value("${xxl.job.executor.logretentiondays:30}") int logRetentionDays,
            @Value("${xxl.job.executor.timeout:3}") int timeout) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setTimeout(timeout);
        executor.setAppname(appName);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
