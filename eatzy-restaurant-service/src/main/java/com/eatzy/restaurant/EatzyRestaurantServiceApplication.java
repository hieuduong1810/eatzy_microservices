package com.eatzy.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.springframework.cloud.openfeign.EnableFeignClients;
import com.eatzy.common.config.FeignConfig;

@SpringBootApplication(scanBasePackages = {"com.eatzy.restaurant", "com.eatzy.common"})
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
public class EatzyRestaurantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EatzyRestaurantServiceApplication.class, args);
    }
}
