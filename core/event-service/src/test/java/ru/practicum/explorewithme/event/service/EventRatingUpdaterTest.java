package ru.practicum.explorewithme.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.avro.EventRatingAvro;
import ru.practicum.explorewithme.event.dal.EventRepository;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRatingUpdaterTest {
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventRatingUpdater updater;

    @Test
    void update_ShouldPersistGreaterRating() {
        when(eventRepository.updateRatingIfGreater(10L, 4.5)).thenReturn(1);

        updater.update(new EventRatingAvro(10L, 4.5, Instant.now()));

        verify(eventRepository).updateRatingIfGreater(10L, 4.5);
    }

    @Test
    void update_ShouldSkipWhenRatingIsNotGreater() {
        when(eventRepository.updateRatingIfGreater(10L, 4.5)).thenReturn(0);

        updater.update(new EventRatingAvro(10L, 4.5, Instant.now()));

        verify(eventRepository).updateRatingIfGreater(10L, 4.5);
    }
}
