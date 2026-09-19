package ru.practicum.explorewithme.interaction.feign.fallback;

import org.junit.jupiter.api.Test;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.exception.ServiceUnavailableException;
import ru.practicum.explorewithme.interaction.feign.EventClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventClientFallbackFactoryTest {

    private final EventClientFallbackFactory factory = new EventClientFallbackFactory();

    @Test
    void getEventInternal_NotFoundCause_ShouldRethrowAsIs() {
        NotFoundException cause = new NotFoundException("Событие с id=1 не найдено");
        EventClient client = factory.create(cause);

        assertThatThrownBy(() -> client.getEventInternal(1L)).isSameAs(cause);
    }

    @Test
    void getEventInternal_ServiceDown_ShouldThrowServiceUnavailable() {
        EventClient client = factory.create(new IOException("connection refused"));

        assertThatThrownBy(() -> client.getEventInternal(1L))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void searchByRadius_ServiceDown_ShouldReturnEmptyList() {
        EventClient client = factory.create(new IOException("connection refused"));

        var result = client.searchByRadius(55.75f, 37.62f, 50f, 0, 10);

        assertThat(result).isEmpty();
    }
}