package ru.practicum.explorewithme.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.explorewithme.interaction.api.EventInternalApi;
import ru.practicum.explorewithme.interaction.feign.fallback.EventClientFallbackFactory;

@FeignClient(name = "event-service", configuration = FeignConfig.class,
        fallbackFactory = EventClientFallbackFactory.class)
public interface EventClient extends EventInternalApi {
}