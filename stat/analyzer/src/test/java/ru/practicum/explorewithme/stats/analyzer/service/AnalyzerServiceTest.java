package ru.practicum.explorewithme.stats.analyzer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.explorewithme.stats.analyzer.dto.EventRatingChangedDto;
import ru.practicum.explorewithme.stats.analyzer.dto.RecommendationDto;
import ru.practicum.explorewithme.stats.analyzer.model.EventSimilarity;
import ru.practicum.explorewithme.stats.analyzer.model.UserEventInteraction;
import ru.practicum.explorewithme.stats.analyzer.model.UserEventInteractionId;
import ru.practicum.explorewithme.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.explorewithme.stats.analyzer.repository.EventWeightProjection;
import ru.practicum.explorewithme.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.explorewithme.stats.common.service.ActionWeightResolver;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzerServiceTest {
    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserEventInteractionRepository interactionRepository;
    @Mock
    private EventSimilarityRepository similarityRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ActionWeightResolver weightResolver = new ActionWeightResolver(0.4, 0.8, 1.0);

    @Test
    void processUserAction_ShouldKeepMaximumWeightAndPublishRating() {
        AnalyzerService service = new AnalyzerService(interactionRepository, similarityRepository,
                weightResolver, eventPublisher);
        UserActionAvro action = new UserActionAvro(10L, 20L, ActionTypeAvro.LIKE, TIME);
        when(interactionRepository.findById(new UserEventInteractionId(10L, 20L))).thenReturn(Optional.empty());
        when(interactionRepository.sumWeightsByEventId(20L)).thenReturn(1.8);

        service.processUserAction(action);

        ArgumentCaptor<UserEventInteraction> interactionCaptor = ArgumentCaptor.forClass(UserEventInteraction.class);
        verify(interactionRepository).save(interactionCaptor.capture());
        assertThat(interactionCaptor.getValue().getWeight()).isEqualTo(1.0);
        verify(eventPublisher).publishEvent(new EventRatingChangedDto(20L, 1.8, TIME));
    }

    @Test
    void processUserAction_ShouldIgnoreActionWithoutWeightIncrease() {
        AnalyzerService service = new AnalyzerService(interactionRepository, similarityRepository,
                weightResolver, eventPublisher);
        UserEventInteraction interaction = new UserEventInteraction(10L, 20L, 1.0, TIME);
        when(interactionRepository.findById(new UserEventInteractionId(10L, 20L)))
                .thenReturn(Optional.of(interaction));

        service.processUserAction(new UserActionAvro(10L, 20L, ActionTypeAvro.LIKE, TIME));

        verify(interactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void processSimilarity_ShouldNormalizePairOrder() {
        AnalyzerService service = new AnalyzerService(interactionRepository, similarityRepository,
                weightResolver, eventPublisher);
        EventSimilarityAvro similarity = new EventSimilarityAvro(20L, 10L, 0.75, TIME);
        when(similarityRepository.findById(any())).thenReturn(Optional.empty());

        service.processSimilarity(similarity);

        ArgumentCaptor<EventSimilarity> captor = ArgumentCaptor.forClass(EventSimilarity.class);
        verify(similarityRepository).save(captor.capture());
        assertThat(captor.getValue().getEventA()).isEqualTo(10L);
        assertThat(captor.getValue().getEventB()).isEqualTo(20L);
        assertThat(captor.getValue().getScore()).isEqualTo(0.75);
    }

    @Test
    void getSimilarEvents_ShouldExcludeInteractedAndSortByScore() {
        AnalyzerService service = new AnalyzerService(interactionRepository, similarityRepository,
                weightResolver, eventPublisher);
        when(interactionRepository.findAllByUserId(10L)).thenReturn(List.of(
                new UserEventInteraction(10L, 20L, 1.0, TIME)));
        when(similarityRepository.findAllByEventAOrEventB(10L, 10L)).thenReturn(List.of(
                new EventSimilarity(10L, 20L, 0.99, TIME),
                new EventSimilarity(10L, 30L, 0.80, TIME),
                new EventSimilarity(40L, 10L, 0.90, TIME)));

        List<RecommendationDto> result = service.getSimilarEvents(10L, 10L, 2);

        assertThat(result).extracting(RecommendationDto::eventId).containsExactly(40L, 30L);
        assertThat(result).extracting(RecommendationDto::score).containsExactly(0.90, 0.80);
    }

    @Test
    void getRecommendationsForUser_ShouldCalculateWeightedScore() {
        AnalyzerService service = new AnalyzerService(interactionRepository, similarityRepository,
                weightResolver, eventPublisher);
        when(interactionRepository.findAllByUserId(10L)).thenReturn(List.of(
                new UserEventInteraction(10L, 1L, 1.0, TIME),
                new UserEventInteraction(10L, 2L, 0.8, TIME.minusSeconds(1))));
        when(similarityRepository.findAllForEvents(Set.of(1L, 2L))).thenReturn(List.of(
                new EventSimilarity(1L, 3L, 0.5, TIME),
                new EventSimilarity(2L, 3L, 0.9, TIME),
                new EventSimilarity(1L, 4L, 0.7, TIME)));
        when(similarityRepository.findAllForEvents(Set.of(3L, 4L))).thenReturn(List.of(
                new EventSimilarity(1L, 3L, 0.5, TIME),
                new EventSimilarity(2L, 3L, 0.9, TIME),
                new EventSimilarity(1L, 4L, 0.7, TIME)));

        List<RecommendationDto> result = service.getRecommendationsForUser(10L, 2, 2, 2);

        assertThat(result).extracting(RecommendationDto::eventId).containsExactly(4L, 3L);
        assertThat(result.get(0).score()).isEqualTo(1.0);
        assertThat(result.get(1).score()).isEqualTo((0.5 + 0.8 * 0.9) / 1.4);
    }

    @Test
    void getInteractionsCount_ShouldReturnRequestedIdsIncludingZeros() {
        AnalyzerService service = new AnalyzerService(interactionRepository, similarityRepository,
                weightResolver, eventPublisher);
        EventWeightProjection projection = mock(EventWeightProjection.class);
        when(projection.getEventId()).thenReturn(10L);
        when(projection.getScore()).thenReturn(1.8);
        when(interactionRepository.sumWeightsByEventIds(List.of(10L, 20L))).thenReturn(List.of(projection));

        Map<Long, Double> result = service.getInteractionsCount(List.of(10L, 20L));

        assertThat(result).containsEntry(10L, 1.8).containsEntry(20L, 0.0);
    }
}
