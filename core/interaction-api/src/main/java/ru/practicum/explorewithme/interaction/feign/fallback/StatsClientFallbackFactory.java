package ru.practicum.explorewithme.interaction.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.interaction.dto.EndpointHitDto;
import ru.practicum.explorewithme.interaction.dto.ViewStatsDto;
import ru.practicum.explorewithme.interaction.feign.StatsClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatsClientFallbackFactory implements FallbackFactory<StatsClient> {

    @Override
    public StatsClient create(Throwable cause) {
        return new StatsClient() {

            @Override
            public void hit(EndpointHitDto endpointHitDTO) {
                // потеря одного хита не должна ронять основной запрос пользователя
                log.warn("stats-service недоступен, хит по uri={} не сохранён",
                        endpointHitDTO.getUri(), cause);
            }

            @Override
            public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                               List<String> uris, Boolean unique) {
                log.warn("stats-service недоступен, views по умолчанию = 0", cause);
                return Collections.emptyList();
            }
        };
    }
}