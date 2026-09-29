package ru.practicum.explorewithme.stats.aggregator.dto;

import java.time.Instant;

public record WeightChangeDto(long eventId, long userId, double oldWeight, double newWeight, Instant timestamp) {
    public boolean increased() {
        return newWeight > oldWeight;
    }
}
