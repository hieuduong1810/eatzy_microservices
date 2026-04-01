package com.eatzy.eatzyconfigserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;


@EnableConfigServer
@SpringBootApplication
public class EatzyConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EatzyConfigServerApplication.class, args);
    }

}
