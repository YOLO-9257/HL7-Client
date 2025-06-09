package com.hl7.client.infrastructure.config;

import com.hl7.client.infrastructure.exception.ApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理配置类
 * 配置异步任务执行器和异常处理器
 */
@Slf4j
@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig implements AsyncConfigurer {

    @Value("${spring.task.execution.pool.core-size:5}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:10}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${spring.task.execution.pool.keep-alive:60s}")
    private Duration keepAliveDuration;

    @Value("${spring.task.execution.thread-name-prefix:async-executor-}")
    private String threadNamePrefix;

    @Value("${spring.task.execution.pool.allow-core-thread-timeout:true}")
    private boolean allowCoreThreadTimeout;

    /**
     * 配置异步任务执行器
     *
     * @return 线程池执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(corePoolSize);
        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        // 线程活跃时间（秒）
        executor.setKeepAliveSeconds((int)keepAliveDuration.getSeconds());
        // 线程名前缀
        executor.setThreadNamePrefix(threadNamePrefix);
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        executor.setAllowCoreThreadTimeOut(allowCoreThreadTimeout);

        // 线程池初始化
        executor.initialize();

        log.info("异步任务执行器已初始化: 核心线程数={}, 最大线程数={}, 队列容量={}, 线程存活时间={}秒, AllowCoreThreadTimeout={}",
                corePoolSize, maxPoolSize, queueCapacity, keepAliveDuration.getSeconds(), allowCoreThreadTimeout);

        return executor;
    }

    /**
     * 配置自动代理创建器，确保使用基于类的代理
     */
    @Bean
    @Primary
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator proxyCreator = new DefaultAdvisorAutoProxyCreator();
        proxyCreator.setProxyTargetClass(true);
        return proxyCreator;
    }

    /**
     * 配置异步任务异常处理器
     *
     * @return 异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * 自定义异步异常处理器
     */
    static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            // 记录异常的详细信息
            log.error("异步任务执行异常:");
            log.error("方法: {}.{}", method.getDeclaringClass().getName(), method.getName());
            log.error("参数: {}", params);

            if (ex instanceof ApplicationException) {
                ApplicationException appEx = (ApplicationException) ex;
                log.error("业务异常: [{}] {}", appEx.getErrorCode(), appEx.getMessage());
            } else {
                log.error("异常类型: {}", ex.getClass().getName());
                log.error("异常消息: {}", ex.getMessage());
            }

            // 记录堆栈跟踪，但仅限严重错误
            if (!(ex instanceof ApplicationException)) {
                log.error("异常堆栈:", ex);
            }
        }
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public Duration getKeepAliveDuration() {
        return keepAliveDuration;
    }

    public void setKeepAliveDuration(Duration keepAliveDuration) {
        this.keepAliveDuration = keepAliveDuration;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public boolean isAllowCoreThreadTimeout() {
        return allowCoreThreadTimeout;
    }

    public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }
}
