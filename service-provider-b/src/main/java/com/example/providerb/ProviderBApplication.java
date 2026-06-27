package com.example.providerb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProviderBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderBApplication.class, args);
    }
}
