package com.logistics.fleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PickupFleetServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PickupFleetServiceApplication.class, args);
    }
}
