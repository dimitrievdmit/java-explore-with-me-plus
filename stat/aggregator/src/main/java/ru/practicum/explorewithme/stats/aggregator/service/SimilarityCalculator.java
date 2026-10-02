package ru.practicum.explorewithme.stats.aggregator.service;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.explorewithme.stats.aggregator.dto.WeightChangeDto;
import ru.practicum.explorewithme.stats.common.service.ActionWeightResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimilarityCalculator {
    private final ActionWeightResolver weightResolver;
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public SimilarityCalculator(ActionWeightResolver weightResolver) {
        this.weightResolver = weightResolver;
    }

    public synchronized List<EventSimilarityAvro> process(UserActionAvro action) {
        WeightChangeDto change = resolveWeightChange(action);
        if (!change.increased()) {
            return List.of();
        }

        List<Long> otherEvents = findOtherEvents(change.eventId());
        updateMinWeightSums(change, otherEvents);
        applyWeightChange(change);
        return calculateSimilarities(change.eventId(), otherEvents, change.timestamp());
    }

    private WeightChangeDto resolveWeightChange(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = weightResolver.getWeight(action.getActionType());
        Map<Long, Double> eventUsers = userEventWeights.getOrDefault(eventId, Map.of());
        double oldWeight = eventUsers.getOrDefault(userId, 0.0);
        return new WeightChangeDto(eventId, userId, oldWeight, newWeight, action.getTimestamp());
    }

    private List<Long> findOtherEvents(long eventId) {
        return eventWeightSums.keySet().stream()
                .filter(otherEvent -> otherEvent != eventId)
                .toList();
    }

    private void updateMinWeightSums(WeightChangeDto change, List<Long> otherEvents) {
        for (Long otherEvent : otherEvents) {
            Double otherWeight = userEventWeights.getOrDefault(otherEvent, Map.of()).get(change.userId());
            if (otherWeight == null) {
                continue;
            }
            double oldMin = Math.min(change.oldWeight(), otherWeight);
            double newMin = Math.min(change.newWeight(), otherWeight);
            if (Double.compare(oldMin, newMin) != 0) {
                addMinWeightSum(change.eventId(), otherEvent, newMin - oldMin);
            }
        }
    }

    private void applyWeightChange(WeightChangeDto change) {
        userEventWeights.computeIfAbsent(change.eventId(), key -> new HashMap<>())
                .put(change.userId(), change.newWeight());
        eventWeightSums.merge(change.eventId(), change.newWeight() - change.oldWeight(), Double::sum);
    }

    private List<EventSimilarityAvro> calculateSimilarities(long eventId,
                                                            List<Long> otherEvents,
                                                            Instant timestamp) {
        List<EventSimilarityAvro> result = new ArrayList<>();
        for (Long otherEvent : otherEvents) {
            if (!userEventWeights.containsKey(otherEvent)) {
                continue;
            }
            double score = calculateScore(eventId, otherEvent);
            if (score > 0.0) {
                long first = Math.min(eventId, otherEvent);
                long second = Math.max(eventId, otherEvent);
                result.add(new EventSimilarityAvro(first, second, score, timestamp));
            }
        }
        return result;
    }

    private double calculateScore(long eventA, long eventB) {
        double sumA = eventWeightSums.getOrDefault(eventA, 0.0);
        double sumB = eventWeightSums.getOrDefault(eventB, 0.0);
        if (sumA == 0.0 || sumB == 0.0) {
            return 0.0;
        }
        double minSum = getMinWeightSum(eventA, eventB);
        return minSum / (Math.sqrt(sumA) * Math.sqrt(sumB));
    }

    private void addMinWeightSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums.computeIfAbsent(first, key -> new HashMap<>())
                .merge(second, delta, Double::sum);
    }

    private double getMinWeightSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums.getOrDefault(first, Map.of())
                .getOrDefault(second, 0.0);
    }
}
