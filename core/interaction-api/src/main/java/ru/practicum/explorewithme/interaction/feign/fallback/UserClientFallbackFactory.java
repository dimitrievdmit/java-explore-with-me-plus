package ru.practicum.explorewithme.interaction.feign.fallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.exception.ServiceUnavailableException;
import ru.practicum.explorewithme.interaction.feign.UserClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public UserShortDto getUserShort(Long userId) {
                if (cause instanceof NotFoundException) {
                    throw (NotFoundException) cause; // честный 404 — прокидываем как есть
                }
                log.warn("user-service недоступен при проверке userId={}", userId, cause);
                throw new ServiceUnavailableException("user-service", cause);
            }

            @Override
            public List<UserShortDto> getUsersShort(List<Long> ids) {
                if (cause instanceof NotFoundException) {
                    throw (NotFoundException) cause;
                }
                log.warn("user-service недоступен, батч-обогащение инициаторов пропущено", cause);
                return List.of();
            }
        };
    }
}