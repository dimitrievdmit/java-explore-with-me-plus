package ru.practicum.explorewithme.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.explorewithme.stats.analyzer.model.EventSimilarity;
import ru.practicum.explorewithme.stats.analyzer.model.EventSimilarityId;
import ru.practicum.explorewithme.stats.analyzer.model.UserEventInteraction;
import ru.practicum.explorewithme.stats.analyzer.model.UserEventInteractionId;
import ru.practicum.explorewithme.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.explorewithme.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.explorewithme.stats.common.service.ActionWeightResolver;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyzerService {
    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;
    private final ActionWeightResolver weightResolver;

    @Transactional
    public void processUserAction(UserActionAvro action) {
        UserEventInteractionId id = new UserEventInteractionId(action.getUserId(), action.getEventId());
        UserEventInteraction interaction = interactionRepository.findById(id)
                .orElseGet(() -> new UserEventInteraction(action.getUserId(), action.getEventId(), 0.0, action.getTimestamp()));

        double weight = weightResolver.getWeight(action.getActionType());
        interaction.setWeight(Math.max(interaction.getWeight(), weight));
        interaction.setTimestamp(action.getTimestamp());
        interactionRepository.save(interaction);
    }

    @Transactional
    public void processSimilarity(EventSimilarityAvro similarity) {
        long eventA = Math.min(similarity.getEventA(), similarity.getEventB());
        long eventB = Math.max(similarity.getEventA(), similarity.getEventB());
        EventSimilarityId id = new EventSimilarityId(eventA, eventB);
        EventSimilarity entity = similarityRepository.findById(id)
                .orElseGet(() -> new EventSimilarity(eventA, eventB, similarity.getScore(), similarity.getTimestamp()));
        entity.setScore(similarity.getScore());
        entity.setTimestamp(similarity.getTimestamp());
        similarityRepository.save(entity);
    }

    @Transactional
    public List<Recommendation> getSimilarEvents(long eventId, long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }
        Set<Long> interacted = interactionRepository.findAllByUserId(userId).stream()
                .map(UserEventInteraction::getEventId)
                .collect(java.util.stream.Collectors.toSet());
        return similarityRepository.findAllByEventAOrEventB(eventId, eventId).stream()
                .map(similarity -> new Recommendation(
                        similarity.getEventA().equals(eventId) ? similarity.getEventB() : similarity.getEventA(),
                        similarity.getScore()))
                .filter(item -> !interacted.contains(item.eventId()))
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed()
                        .thenComparingLong(Recommendation::eventId))
                .limit(maxResults)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsForUser(long userId, int maxResults, int recentLimit, int neighborCount) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserEventInteraction> allInteractions = interactionRepository.findAllByUserId(userId);
        if (allInteractions.isEmpty()) {
            return List.of();
        }

        List<UserEventInteraction> recentInteractions = allInteractions.stream()
                .sorted(Comparator.comparing(UserEventInteraction::getTimestamp).reversed())
                .limit(Math.max(1, recentLimit))
                .toList();

        Set<Long> interactedIds = new HashSet<>();
        allInteractions.forEach(item -> interactedIds.add(item.getEventId()));

        Set<Long> seedIds = recentInteractions.stream()
                .map(UserEventInteraction::getEventId)
                .collect(java.util.stream.Collectors.toSet());
        List<EventSimilarity> seedSimilarities = seedIds.isEmpty()
                ? List.of()
                : similarityRepository.findAllForEvents(seedIds);

        Map<Long, Double> candidateSimilarity = new HashMap<>();
        for (EventSimilarity similarity : seedSimilarities) {
            Long other = otherEvent(similarity, seedIds);
            if (other != null && !interactedIds.contains(other)) {
                candidateSimilarity.merge(other, similarity.getScore(), Math::max);
            }
        }

        if (candidateSimilarity.isEmpty()) {
            return List.of();
        }

        Set<Long> candidateIds = candidateSimilarity.keySet();
        List<EventSimilarity> candidateSimilarities = similarityRepository.findAllForEvents(candidateIds);
        Map<Long, Map<Long, Double>> similaritiesByCandidate = new HashMap<>();
        for (EventSimilarity similarity : candidateSimilarities) {
            if (candidateIds.contains(similarity.getEventA())) {
                similaritiesByCandidate
                        .computeIfAbsent(similarity.getEventA(), key -> new HashMap<>())
                        .put(similarity.getEventB(), similarity.getScore());
            }
            if (candidateIds.contains(similarity.getEventB())) {
                similaritiesByCandidate
                        .computeIfAbsent(similarity.getEventB(), key -> new HashMap<>())
                        .put(similarity.getEventA(), similarity.getScore());
            }
        }

        Map<Long, Double> userWeights = allInteractions.stream()
                .collect(java.util.stream.Collectors.toMap(UserEventInteraction::getEventId,
                        UserEventInteraction::getWeight,
                        Math::max,
                        LinkedHashMap::new));

        List<Recommendation> recommendations = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            Map<Long, Double> neighbors = similaritiesByCandidate.getOrDefault(candidateId, Map.of());
            List<Map.Entry<Long, Double>> topNeighbors = neighbors.entrySet().stream()
                    .filter(entry -> userWeights.containsKey(entry.getKey()))
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                            .thenComparingLong(Map.Entry::getKey))
                    .limit(Math.max(1, neighborCount))
                    .toList();

            double weightedSum = 0.0;
            double similaritySum = 0.0;
            for (Map.Entry<Long, Double> neighbor : topNeighbors) {
                weightedSum += userWeights.get(neighbor.getKey()) * neighbor.getValue();
                similaritySum += neighbor.getValue();
            }
            if (similaritySum > 0.0) {
                recommendations.add(new Recommendation(candidateId, weightedSum / similaritySum));
            }
        }

        return recommendations.stream()
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed()
                        .thenComparingLong(Recommendation::eventId))
                .limit(maxResults)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> result = new LinkedHashMap<>();
        eventIds.forEach(id -> result.put(id, 0.0));
        for (UserEventInteractionRepository.EventWeightProjection projection
                : interactionRepository.sumWeightsByEventIds(eventIds)) {
            result.put(projection.getEventId(), projection.getScore());
        }
        return result;
    }

    private Long otherEvent(EventSimilarity similarity, Set<Long> eventIds) {
        if (eventIds.contains(similarity.getEventA()) && !eventIds.contains(similarity.getEventB())) {
            return similarity.getEventB();
        }
        if (eventIds.contains(similarity.getEventB()) && !eventIds.contains(similarity.getEventA())) {
            return similarity.getEventA();
        }
        return null;
    }

    public record Recommendation(long eventId, double score) {
    }
}
