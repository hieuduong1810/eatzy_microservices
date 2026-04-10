package com.eatzy.interaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.context.annotation.ComponentScan;
import com.eatzy.common.config.FeignConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
@ComponentScan(basePackages = {"com.eatzy.interaction", "com.eatzy.common"})
public class EatzyInteractionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EatzyInteractionServiceApplication.class, args);
    }

}
