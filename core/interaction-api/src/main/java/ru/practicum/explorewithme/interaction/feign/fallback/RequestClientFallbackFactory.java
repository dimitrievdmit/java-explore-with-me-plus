package ru.practicum.explorewithme.interaction.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.RequestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {
        return new RequestClient() {

            @Override
            public List<ConfirmedRequestsCountDto> getConfirmedRequestsCounts(EventIdListDto eventIdListDto) {
                if (cause instanceof NotFoundException) {
                    throw (NotFoundException) cause;
                }
                List<Long> eventIds;
                if (eventIdListDto == null || eventIdListDto.getEventIds() == null
                        || eventIdListDto.getEventIds().isEmpty()) {
                    eventIds = List.of();
                } else {
                    eventIds = eventIdListDto.getEventIds();
                }
                log.warn("request-service недоступен, confirmedRequests по {} событиям = 0",
                        eventIds.size(), cause);
                return Collections.emptyList(); // getOrDefault(id, 0L) на стороне event-service превратит в нули
            }
        };
    }
}