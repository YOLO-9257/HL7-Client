package com.hl7.client.infrastructure.adapter.network.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息完整性检查策略管理器
 * 负责根据设备型号选择合适的策略
 */
@Slf4j
@Component
public class MessageCompletionStrategyManager {

    private final List<MessageCompletionStrategy> strategies;
    private final DefaultMessageCompletionStrategy defaultStrategy;

    // 缓存设备型号到策略的映射，提高查找效率
    private final Map<String, MessageCompletionStrategy> strategyCache = new ConcurrentHashMap<>();

    @Autowired
    public MessageCompletionStrategyManager(
            List<MessageCompletionStrategy> strategies,
            DefaultMessageCompletionStrategy defaultStrategy) {
        this.strategies = strategies;
        this.defaultStrategy = defaultStrategy;
    }

    /**
     * 初始化策略管理器
     * 在Bean创建后自动执行
     */
    @PostConstruct
    public void initialize() {
        // 对策略列表进行排序，按优先级升序排列
        strategies.sort(Comparator.comparingInt(MessageCompletionStrategy::getPriority));

        // 日志输出所有可用的策略
        log.info("共加载 {} 个消息完整性检查策略:", strategies.size());
        for (MessageCompletionStrategy strategy : strategies) {
            log.info("- {} (优先级: {})", strategy.getDescription(), strategy.getPriority());
        }
    }

    /**
     * 根据设备型号获取适用的消息完整性检查策略
     *
     * @param deviceModel 设备型号
     * @return 适用的策略
     */
    public MessageCompletionStrategy getStrategy(String deviceModel) {
        if (deviceModel == null || deviceModel.isEmpty()) {
            log.warn("设备型号为空，使用默认消息完整性检查策略");
            return defaultStrategy;
        }

        // 首先尝试从缓存中获取
        MessageCompletionStrategy cachedStrategy = strategyCache.get(deviceModel);
        if (cachedStrategy != null) {
            return cachedStrategy;
        }

        // 查找支持指定设备型号的第一个策略（已按优先级排序）
        for (MessageCompletionStrategy strategy : strategies) {
            if (strategy.supports(deviceModel)) {
                log.info("找到设备型号 {} 的消息完整性检查策略: {} (优先级: {})",
                        deviceModel, strategy.getDescription(), strategy.getPriority());

                // 将结果加入缓存
                strategyCache.put(deviceModel, strategy);
                return strategy;
            }
        }

        // 如果没有找到特定的策略，使用默认策略
        log.debug("未找到设备型号 {} 的特定策略，使用默认策略", deviceModel);
        strategyCache.put(deviceModel, defaultStrategy);
        return defaultStrategy;
    }

    /**
     * 清除策略缓存
     * 当有新策略加入或策略更新时调用
     */
    public void clearCache() {
        strategyCache.clear();
        log.debug("策略缓存已清除");
    }

    /**
     * 获取所有可用的策略列表
     *
     * @return 策略列表
     */
    public List<MessageCompletionStrategy> getAllStrategies() {
        return strategies;
    }
}
