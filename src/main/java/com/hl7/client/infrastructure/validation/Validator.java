package com.hl7.client.infrastructure.validation;

import com.hl7.client.infrastructure.exception.ConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用验证器
 * 用于验证Bean的属性是否符合要求
 */
@Slf4j
@Component
public class Validator {

    private final javax.validation.Validator validator;

    /**
     * 构造函数
     * 初始化验证器
     */
    public Validator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    /**
     * 验证对象
     *
     * @param <T> 对象类型
     * @param object 要验证的对象
     * @throws ConfigurationException 验证失败时抛出异常
     */
    public <T> void validate(T object) {
        validate(object, "对象验证失败");
    }

    /**
     * 验证对象
     *
     * @param <T> 对象类型
     * @param object 要验证的对象
     * @param errorMessage 错误消息前缀
     * @throws ConfigurationException 验证失败时抛出异常
     */
    public <T> void validate(T object, String errorMessage) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if (!violations.isEmpty()) {
            String violationMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.error("验证失败: {}, 错误: {}", errorMessage, violationMessage);
            throw new ConfigurationException("001", errorMessage + ": " + violationMessage);
        }
    }
}
