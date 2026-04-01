package com.eatzy.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@ComponentScan(basePackages = {"com.eatzy.order", "com.eatzy.common"})
public class EatzyOrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EatzyOrderServiceApplication.class, args);
    }
}
