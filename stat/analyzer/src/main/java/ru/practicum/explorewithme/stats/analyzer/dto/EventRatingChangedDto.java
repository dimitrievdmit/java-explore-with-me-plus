package ru.practicum.explorewithme.stats.analyzer.dto;

import java.time.Instant;

public record EventRatingChangedDto(long eventId, double rating, Instant timestamp) {
}
