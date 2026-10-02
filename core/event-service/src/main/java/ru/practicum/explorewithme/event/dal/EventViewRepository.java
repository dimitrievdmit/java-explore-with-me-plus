package ru.practicum.explorewithme.event.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.event.model.EventView;

public interface EventViewRepository extends JpaRepository<EventView, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}
