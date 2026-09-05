package ru.practicum.explorewithme.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class StatServerApp {
    public static void main(String[] args) {
        SpringApplication.run(StatServerApp.class, args);
    }
}
