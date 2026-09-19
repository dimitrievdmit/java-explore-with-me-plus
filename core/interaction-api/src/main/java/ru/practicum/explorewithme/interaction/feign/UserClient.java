package ru.practicum.explorewithme.interaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.explorewithme.interaction.api.UserInternalApi;
import ru.practicum.explorewithme.interaction.feign.fallback.UserClientFallbackFactory;

@FeignClient(name = "user-service", configuration = FeignConfig.class,
        fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient extends UserInternalApi {
}