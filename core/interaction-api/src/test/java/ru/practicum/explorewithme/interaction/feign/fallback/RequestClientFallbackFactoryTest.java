package ru.practicum.explorewithme.interaction.feign.fallback;

import org.junit.jupiter.api.Test;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.RequestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class RequestClientFallbackFactoryTest {

    private final RequestClientFallbackFactory factory = new RequestClientFallbackFactory();

    @Test
    void getConfirmedRequestsCounts_ServiceDown_ShouldReturnEmptyList() {
        RequestClient client = factory.create(new IOException("connection refused"));

        List<?> result = client.getConfirmedRequestsCounts(new EventIdListDto(List.of(1L, 2L, 3L)));

        assertThat(result).isEmpty();
    }

    @Test
    void getConfirmedRequestsCounts_NotFoundCause_ShouldRethrowAsIs() {
        NotFoundException cause = new NotFoundException("Не найдено");
        RequestClient client = factory.create(cause);

        assertThatThrownBy(() -> client.getConfirmedRequestsCounts(new EventIdListDto(List.of(1L))))
                .isSameAs(cause);
    }

    @Test
    void getConfirmedRequestsCounts_EmptyEventIds_ShouldNotFail() {
        RequestClient client = factory.create(new IOException("timeout"));

        List<?> result = client.getConfirmedRequestsCounts(any(EventIdListDto.class));

        assertThat(result).isEmpty();
    }
}