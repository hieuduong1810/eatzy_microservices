package com.eatzy.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignAutoConfiguration {

    @Bean
    public feign.RequestInterceptor feignClientInterceptor() {
        return new FeignClientInterceptor();
    }
}
