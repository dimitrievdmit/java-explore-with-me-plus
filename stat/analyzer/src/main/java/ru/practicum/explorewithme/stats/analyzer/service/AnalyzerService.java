package ru.practicum.explorewithme.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyzerService {
    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;
    private final ActionWeightResolver weightResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processUserAction(UserActionAvro action) {
        UserEventInteraction interaction = getOrCreateInteraction(action);
        double weight = weightResolver.getWeight(action.getActionType());
        if (weight <= interaction.getWeight()) {
            return;
        }

        interaction.setWeight(weight);
        interaction.setTimestamp(action.getTimestamp());
        interactionRepository.save(interaction);

        Double rating = interactionRepository.sumWeightsByEventId(action.getEventId());
        eventPublisher.publishEvent(new EventRatingChanged(
                action.getEventId(),
                rating == null ? 0.0 : rating,
                action.getTimestamp()));
    }

    private UserEventInteraction getOrCreateInteraction(UserActionAvro action) {
        UserEventInteractionId id = new UserEventInteractionId(action.getUserId(), action.getEventId());
        return interactionRepository.findById(id)
                .orElseGet(() -> new UserEventInteraction(action.getUserId(), action.getEventId(), 0.0,
                        action.getTimestamp()));
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

    @Transactional(readOnly = true)
    public List<Recommendation> getSimilarEvents(long eventId, long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }
        Set<Long> interacted = getInteractedEventIds(userId);
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

    private Set<Long> getInteractedEventIds(long userId) {
        return interactionRepository.findAllByUserId(userId).stream()
                .map(UserEventInteraction::getEventId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsForUser(long userId, int maxResults,
                                                          int recentLimit, int neighborCount) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserEventInteraction> allInteractions = interactionRepository.findAllByUserId(userId);
        if (allInteractions.isEmpty()) {
            return List.of();
        }

        List<UserEventInteraction> recentInteractions = findRecentInteractions(allInteractions, recentLimit);
        Set<Long> interactedIds = collectInteractedIds(allInteractions);
        Set<Long> seedIds = collectEventIds(recentInteractions);
        Map<Long, Double> candidateSimilarity = findCandidateSimilarity(seedIds, interactedIds);
        if (candidateSimilarity.isEmpty()) {
            return List.of();
        }

        Map<Long, Map<Long, Double>> similaritiesByCandidate = findSimilaritiesByCandidate(candidateSimilarity.keySet());
        Map<Long, Double> userWeights = buildUserWeights(allInteractions);
        List<Recommendation> recommendations = buildRecommendations(
                candidateSimilarity.keySet(), similaritiesByCandidate, userWeights, neighborCount);

        return recommendations.stream()
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed()
                        .thenComparingLong(Recommendation::eventId))
                .limit(maxResults)
                .toList();
    }

    private List<UserEventInteraction> findRecentInteractions(List<UserEventInteraction> interactions,
                                                              int recentLimit) {
        return interactions.stream()
                .sorted(Comparator.comparing(UserEventInteraction::getTimestamp).reversed())
                .limit(Math.max(1, recentLimit))
                .toList();
    }

    private Set<Long> collectInteractedIds(List<UserEventInteraction> interactions) {
        return interactions.stream()
                .map(UserEventInteraction::getEventId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<Long> collectEventIds(List<UserEventInteraction> interactions) {
        return interactions.stream()
                .map(UserEventInteraction::getEventId)
                .collect(Collectors.toSet());
    }

    private Map<Long, Double> findCandidateSimilarity(Set<Long> seedIds, Set<Long> interactedIds) {
        Map<Long, Double> candidateSimilarity = new HashMap<>();
        if (seedIds.isEmpty()) {
            return candidateSimilarity;
        }

        for (EventSimilarity similarity : similarityRepository.findAllForEvents(seedIds)) {
            Long other = otherEvent(similarity, seedIds);
            if (other != null && !interactedIds.contains(other)) {
                candidateSimilarity.merge(other, similarity.getScore(), Math::max);
            }
        }
        return candidateSimilarity;
    }

    private Map<Long, Map<Long, Double>> findSimilaritiesByCandidate(Set<Long> candidateIds) {
        Map<Long, Map<Long, Double>> result = new HashMap<>();
        for (EventSimilarity similarity : similarityRepository.findAllForEvents(candidateIds)) {
            addSimilarity(result, similarity.getEventA(), similarity.getEventB(), similarity.getScore(), candidateIds);
            addSimilarity(result, similarity.getEventB(), similarity.getEventA(), similarity.getScore(), candidateIds);
        }
        return result;
    }

    private void addSimilarity(Map<Long, Map<Long, Double>> similarities,
                               Long candidateId, Long neighborId, double score,
                               Set<Long> candidateIds) {
        if (candidateIds.contains(candidateId)) {
            similarities.computeIfAbsent(candidateId, key -> new HashMap<>())
                    .put(neighborId, score);
        }
    }

    private Map<Long, Double> buildUserWeights(List<UserEventInteraction> interactions) {
        return interactions.stream()
                .collect(Collectors.toMap(
                        UserEventInteraction::getEventId,
                        UserEventInteraction::getWeight,
                        Math::max,
                        LinkedHashMap::new));
    }

    private List<Recommendation> buildRecommendations(Set<Long> candidateIds,
                                                      Map<Long, Map<Long, Double>> similaritiesByCandidate,
                                                      Map<Long, Double> userWeights,
                                                      int neighborCount) {
        List<Recommendation> recommendations = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            List<Map.Entry<Long, Double>> topNeighbors = getTopNeighbors(
                    similaritiesByCandidate.getOrDefault(candidateId, Map.of()), userWeights, neighborCount);
            Recommendation recommendation = calculateRecommendation(candidateId, topNeighbors, userWeights);
            if (recommendation != null) {
                recommendations.add(recommendation);
            }
        }
        return recommendations;
    }

    private List<Map.Entry<Long, Double>> getTopNeighbors(Map<Long, Double> neighbors,
                                                          Map<Long, Double> userWeights,
                                                          int neighborCount) {
        return neighbors.entrySet().stream()
                .filter(entry -> userWeights.containsKey(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()
                        .thenComparingLong(Map.Entry::getKey))
                .limit(Math.max(1, neighborCount))
                .toList();
    }

    private Recommendation calculateRecommendation(long candidateId,
                                                   List<Map.Entry<Long, Double>> neighbors,
                                                   Map<Long, Double> userWeights) {
        double weightedSum = 0.0;
        double similaritySum = 0.0;
        for (Map.Entry<Long, Double> neighbor : neighbors) {
            weightedSum += userWeights.get(neighbor.getKey()) * neighbor.getValue();
            similaritySum += neighbor.getValue();
        }
        if (similaritySum <= 0.0) {
            return null;
        }
        return new Recommendation(candidateId, weightedSum / similaritySum);
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

    public record EventRatingChanged(long eventId, double rating, java.time.Instant timestamp) {
    }
}
