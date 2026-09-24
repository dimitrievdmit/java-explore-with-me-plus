package ru.practicum.explorewithme.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.stats.analyzer.model.EventSimilarity;
import ru.practicum.explorewithme.stats.analyzer.model.EventSimilarityId;

import java.util.List;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {
    List<EventSimilarity> findAllByEventAOrEventB(Long eventA, Long eventB);

    @Query("SELECT s FROM EventSimilarity s " +
            "WHERE s.eventA IN :eventIds OR s.eventB IN :eventIds")
    List<EventSimilarity> findAllForEvents(@Param("eventIds") Set<Long> eventIds);
}
