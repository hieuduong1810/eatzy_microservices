package com.eatzy.eatzydiscoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EatzyDiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EatzyDiscoveryServerApplication.class, args);
    }

}
