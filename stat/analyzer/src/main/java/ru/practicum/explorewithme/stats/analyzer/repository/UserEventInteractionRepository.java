package ru.practicum.explorewithme.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.stats.analyzer.model.UserEventInteraction;
import ru.practicum.explorewithme.stats.analyzer.model.UserEventInteractionId;

import java.util.List;
import java.util.Set;

public interface UserEventInteractionRepository extends JpaRepository<UserEventInteraction, UserEventInteractionId> {
    List<UserEventInteraction> findAllByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<UserEventInteraction> findAllByUserId(Long userId);

    List<UserEventInteraction> findAllByUserIdAndEventIdIn(Long userId, Set<Long> eventIds);

    @Query("SELECT i.eventId AS eventId, SUM(i.weight) AS score " +
            "FROM UserEventInteraction i WHERE i.eventId IN :eventIds GROUP BY i.eventId")
    List<EventWeightProjection> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventWeightProjection {
        Long getEventId();

        Double getScore();
    }
}
