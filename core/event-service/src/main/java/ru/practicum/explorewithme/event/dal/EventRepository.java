package ru.practicum.explorewithme.event.dal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.event.model.Event;

public interface EventRepository extends JpaRepository<Event, Long>, ListQuerydslPredicateExecutor<Event> {
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    java.util.Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    int countByCategoryId(Long catId);

    @Query(value = "SELECT e.* FROM events e " +
            "WHERE distance(e.lat, e.lon, :lat, :lon) <= :radius " +
            "AND e.state = 'PUBLISHED'",
            countQuery = "SELECT count(*) FROM events e " +
                    "WHERE distance(e.lat, e.lon, :lat, :lon) <= :radius " +
                    "AND e.state = 'PUBLISHED'",
            nativeQuery = true)
    Page<Event> findEventsByRadius(@Param("lat") Float lat, @Param("lon") Float lon,
                                   @Param("radius") Float radius, Pageable pageable);
}