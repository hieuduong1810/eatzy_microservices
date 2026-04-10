package com.eatzy.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration that registers the custom RestResponseDecoder.
 * 
 * This is NOT a @Configuration class — it must NOT be component-scanned.
 * Instead, it is provided as defaultConfiguration to @EnableFeignClients
 * so that it applies to all Feign client child contexts.
 * 
 * Usage in each service's main class:
 *   @EnableFeignClients(defaultConfiguration = FeignConfig.class)
 */
public class FeignConfig {

    @Bean
    public Decoder feignDecoder(ObjectMapper objectMapper) {
        return new RestResponseDecoder(objectMapper);
    }
}
