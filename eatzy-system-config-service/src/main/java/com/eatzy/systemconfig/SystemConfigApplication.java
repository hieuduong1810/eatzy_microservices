package com.eatzy.systemconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SystemConfigApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemConfigApplication.class, args);
    }
}
