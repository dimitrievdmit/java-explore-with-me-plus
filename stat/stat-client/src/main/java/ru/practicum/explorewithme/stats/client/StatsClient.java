package ru.practicum.explorewithme.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.explorewithme.stats.dto.EndpointHitDTO;
import ru.practicum.explorewithme.stats.dto.ViewStatsDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class StatsClient extends BaseClient {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(@Value("${stats-service.url:http://stat-server}") String serverUrl,
                       WebClient.Builder loadBalancedWebClientBuilder) {
        super(loadBalancedWebClientBuilder.baseUrl(serverUrl).build());
    }

    public ResponseEntity<Object> saveHit(EndpointHitDTO hitDto) {
        try {
            log.info("Отправка статистики на сервер: {}", hitDto);
            return post("/hit", hitDto);
        } catch (Exception e) {
            log.warn("Не удалось сохранить хит в статистику. Ошибка: {}. Тело: {}", e.getMessage(), hitDto);
            log.error("DEBUG: Поймали исключение: ", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    public ResponseEntity<List<ViewStatsDTO>> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/stats")
                            .queryParam("start", start.format(FORMATTER))
                            .queryParam("end", end.format(FORMATTER));
                    if (uris != null && !uris.isEmpty()) {
                        uriBuilder.queryParam("uris", uris);
                    }
                    if (unique != null) {
                        uriBuilder.queryParam("unique", unique);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .toEntityList(ViewStatsDTO.class)
                .block();
    }
}
