package com.eatzy.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.eatzy.common.config.FeignConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
public class EatzyCartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EatzyCartServiceApplication.class, args);
    }
}
