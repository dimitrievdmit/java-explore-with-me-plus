package ru.practicum.explorewithme.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.explorewithme.interaction.api.StatsApi;
import ru.practicum.explorewithme.interaction.feign.fallback.StatsClientFallbackFactory;

@FeignClient(name = "stat-server", configuration = FeignConfig.class,
        fallbackFactory = StatsClientFallbackFactory.class)
public interface StatsClient extends StatsApi {
}