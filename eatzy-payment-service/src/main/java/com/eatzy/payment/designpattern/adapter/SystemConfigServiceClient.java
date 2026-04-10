package com.eatzy.payment.designpattern.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "eatzy-system-config-service")
public interface SystemConfigServiceClient {

    @GetMapping("/api/v1/system-configurations/key/{configKey}")
    Map<String, Object> getSystemConfigurationByKey(@PathVariable("configKey") String configKey);
}
