package ru.practicum.explorewithme.stats.analyzer.repository;

public interface EventWeightProjection {
    Long getEventId();

    Double getScore();
}
