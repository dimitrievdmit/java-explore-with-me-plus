package ru.practicum.explorewithme.event.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventRatingAvro;
import ru.practicum.explorewithme.event.dal.EventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRatingUpdater {
    private final EventRepository eventRepository;

    @Transactional
    public void update(EventRatingAvro rating) {
        int updatedRows = eventRepository.updateRatingIfGreater(rating.getEventId(), rating.getRating());
        if (updatedRows > 0) {
            log.debug("Рейтинг события обновлен: eventId={}, rating={}",
                    rating.getEventId(), rating.getRating());
        }
    }
}
