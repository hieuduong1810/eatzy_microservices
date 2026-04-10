package com.eatzy.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.context.annotation.ComponentScan;
import com.eatzy.common.config.FeignConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
@ComponentScan(basePackages = {"com.eatzy.payment", "com.eatzy.common"})
public class EatzyPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EatzyPaymentServiceApplication.class, args);
    }
}
