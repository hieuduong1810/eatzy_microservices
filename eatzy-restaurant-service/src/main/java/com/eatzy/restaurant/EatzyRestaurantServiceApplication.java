package com.eatzy.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EatzyRestaurantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EatzyRestaurantServiceApplication.class, args);
    }
}
