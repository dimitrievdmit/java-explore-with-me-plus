package ru.practicum.explorewithme.stats.aggregator.service;

import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.stats.common.service.ActionWeightResolver;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

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
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = weightResolver.getWeight(action.getActionType());

        Map<Long, Double> eventUsers = userEventWeights.computeIfAbsent(eventId, key -> new HashMap<>());
        double oldWeight = eventUsers.getOrDefault(userId, 0.0);
        if (newWeight <= oldWeight) {
            return List.of();
        }

        List<Long> otherEvents = new ArrayList<>(eventWeightSums.keySet());
        for (Long otherEvent : otherEvents) {
            if (otherEvent == eventId) {
                continue;
            }
            Double otherWeight = userEventWeights.get(otherEvent).get(userId);
            if (otherWeight == null) {
                continue;
            }
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            if (Double.compare(oldMin, newMin) != 0) {
                addMinWeightSum(eventId, otherEvent, newMin - oldMin);
            }
        }

        eventUsers.put(userId, newWeight);
        eventWeightSums.merge(eventId, newWeight - oldWeight, Double::sum);

        java.time.Instant timestamp = action.getTimestamp();
        List<EventSimilarityAvro> result = new ArrayList<>();
        for (Long otherEvent : otherEvents) {
            if (otherEvent == eventId || !userEventWeights.containsKey(otherEvent)) {
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
