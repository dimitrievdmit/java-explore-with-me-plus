package ru.practicum.explorewithme.interaction.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.exception.ServiceUnavailableException;
import ru.practicum.explorewithme.interaction.feign.EventClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EventClientFallbackFactory implements FallbackFactory<EventClient> {

    private static final String SERVICE_NAME = "event-service";

    @Override
    public EventClient create(Throwable cause) {
        return new EventClient() {

            @Override
            public EventInternalDto getEventInternal(Long eventId) {
                if (cause instanceof NotFoundException) {
                    // событие правда не существует - это не деградация, а бизнес-факт
                    throw (NotFoundException) cause;
                }
                log.error("event-service недоступен при проверке события id={}, " +
                        "заявку создать нельзя без данных о событии", eventId, cause);
                throw new ServiceUnavailableException(SERVICE_NAME, cause);
            }

            @Override
            public List<EventFullDto> searchByRadius(Float lat, Float lon, Float radius,
                                                     Integer from, Integer size) {
                if (cause instanceof NotFoundException) {
                    throw (NotFoundException) cause;
                }
                log.warn("event-service недоступен, поиск событий по радиусу вернёт пустой список", cause);
                return Collections.emptyList();
            }
        };
    }
}