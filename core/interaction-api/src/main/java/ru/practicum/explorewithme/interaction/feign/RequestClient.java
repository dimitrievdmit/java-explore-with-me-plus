package ru.practicum.explorewithme.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.explorewithme.interaction.api.RequestInternalApi;
import ru.practicum.explorewithme.interaction.feign.fallback.RequestClientFallbackFactory;

@FeignClient(name = "request-service", configuration = FeignConfig.class,
        fallbackFactory = RequestClientFallbackFactory.class)
public interface RequestClient extends RequestInternalApi {
}