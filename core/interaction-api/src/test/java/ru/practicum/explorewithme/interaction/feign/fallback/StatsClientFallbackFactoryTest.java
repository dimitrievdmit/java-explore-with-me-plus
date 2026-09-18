package ru.practicum.explorewithme.interaction.feign.fallback;

import org.junit.jupiter.api.Test;
import ru.practicum.explorewithme.interaction.dto.EndpointHitDto;
import ru.practicum.explorewithme.interaction.feign.StatsClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class StatsClientFallbackFactoryTest {

    private final StatsClientFallbackFactory factory = new StatsClientFallbackFactory();

    @Test
    void hit_ServiceDown_ShouldNotThrow() {
        StatsClient client = factory.create(new IOException("connection refused"));

        EndpointHitDto hit = EndpointHitDto.builder()
                .app("event-service")
                .uri("/events/1")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        assertThatCode(() -> client.hit(hit)).doesNotThrowAnyException();
    }

    @Test
    void getStats_ServiceDown_ShouldReturnEmptyList() {
        StatsClient client = factory.create(new IOException("connection refused"));

        List<?> result = client.getStats(LocalDateTime.now().minusDays(1), LocalDateTime.now(),
                List.of("/events/1"), true);

        assertThat(result).isEmpty();
    }

    @Test
    void getStats_NullUris_ShouldStillReturnEmptyListOnFailure() {
        StatsClient client = factory.create(new RuntimeException("timeout"));

        List<?> result = client.getStats(LocalDateTime.now().minusDays(1), LocalDateTime.now(), null, null);

        assertThat(result).isEmpty();
    }
}