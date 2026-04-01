package com.eatzy.order.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Adapter Pattern: Feign Client to interact with System Config Service
 */
@FeignClient(name = "eatzy-system-config-service", path = "/api/v1/system-configurations")
public interface SystemConfigServiceClient {

    @GetMapping("/key/{configKey}")
    Map<String, Object> getSystemConfigurationByKey(@PathVariable("configKey") String configKey);
}
