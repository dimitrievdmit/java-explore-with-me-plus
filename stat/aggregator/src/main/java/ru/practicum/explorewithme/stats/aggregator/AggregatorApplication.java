package ru.practicum.explorewithme.stats.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import ru.practicum.explorewithme.stats.common.service.ActionWeightResolver;

@SpringBootApplication
@Import(ActionWeightResolver.class)
@EnableDiscoveryClient
public class AggregatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AggregatorApplication.class, args);
    }
}
