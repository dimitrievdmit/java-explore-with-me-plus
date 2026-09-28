package ru.practicum.explorewithme.stats.aggregator.service;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.explorewithme.stats.common.service.ActionWeightResolver;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SimilarityCalculatorTest {
    private final SimilarityCalculator calculator =
            new SimilarityCalculator(new ActionWeightResolver(0.4, 0.8, 1.0));

    @Test
    void shouldCalculateSimilarityWithoutSelfPair() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");

        assertTrue(calculator.process(action(1, 100, ActionTypeAvro.VIEW, timestamp)).isEmpty());

        var similarities = calculator.process(action(2, 100, ActionTypeAvro.VIEW, timestamp));

        assertEquals(1, similarities.size());
        assertEquals(1L, similarities.getFirst().getEventA());
        assertEquals(2L, similarities.getFirst().getEventB());
        assertEquals(1.0, similarities.getFirst().getScore(), 1e-9);
        assertFalse(similarities.stream()
                .anyMatch(item -> item.getEventA() == item.getEventB()));
    }

    @Test
    void shouldIgnoreActionWithWeightNotGreaterThanCurrent() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        calculator.process(action(1, 100, ActionTypeAvro.VIEW, timestamp));
        calculator.process(action(2, 100, ActionTypeAvro.VIEW, timestamp));

        var similarities = calculator.process(action(2, 100, ActionTypeAvro.VIEW, timestamp.plusSeconds(1)));

        assertTrue(similarities.isEmpty());
    }

    @Test
    void shouldUpdateSimilarityWhenMaximumWeightIncreases() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        calculator.process(action(1, 100, ActionTypeAvro.VIEW, timestamp));
        calculator.process(action(2, 100, ActionTypeAvro.VIEW, timestamp));

        var similarities = calculator.process(action(2, 100, ActionTypeAvro.LIKE, timestamp.plusSeconds(1)));

        assertEquals(1, similarities.size());
        assertEquals(0.6324555320336759, similarities.getFirst().getScore(), 1e-9);
    }

    private UserActionAvro action(long eventId, long userId, ActionTypeAvro actionType, Instant timestamp) {
        return new UserActionAvro(userId, eventId, actionType, timestamp);
    }
}
