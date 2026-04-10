package com.eatzy.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.context.annotation.ComponentScan;
import com.eatzy.common.config.FeignConfig;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
@ComponentScan(basePackages = {"com.eatzy.communication", "com.eatzy.common"})
public class EatzyCommunicationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EatzyCommunicationServiceApplication.class, args);
    }
}
