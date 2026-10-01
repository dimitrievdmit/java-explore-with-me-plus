package ru.practicum.explorewithme.interaction.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.dashboard.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "grpc.client.analyzer", name = "address")
public class AnalyzerGrpcClient {

    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${grpc.client.analyzer.deadline-ms:2000}")
    private long deadlineMs;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        List<RecommendedEventProto> events = circuitBreakerFactory.create("analyzer").run(
                () -> readAll(client.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getRecommendationsForUser(request)),
                cause -> {
                    log.warn(
                            "analyzer недоступен, рекомендации для userId={} " +
                                    "временно недоступны",
                            userId, cause);
                    return List.of();
                }
        );
        return events.stream();
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        List<RecommendedEventProto> events = circuitBreakerFactory.create("analyzer").run(
                () -> readAll(client.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getSimilarEvents(request)),
                cause -> {
                    log.warn(
                            "analyzer недоступен, похожие события для eventId={} " +
                                    "временно недоступны",
                            eventId, cause);
                    return List.of();
                }
        );
        return events.stream();
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        List<RecommendedEventProto> events = circuitBreakerFactory.create("analyzer").run(
                () -> readAll(client.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).getInteractionsCount(request)),
                cause -> {
                    log.warn(
                            "analyzer недоступен, статистика по {} событиям " +
                                    "временно недоступна",
                            eventIds.size(), cause);
                    return List.of();
                }
        );

        return events.stream().collect(Collectors.toMap(
                RecommendedEventProto::getEventId,
                RecommendedEventProto::getScore,
                Math::max));
    }

    private List<RecommendedEventProto> readAll(Iterator<RecommendedEventProto> iterator) {
        List<RecommendedEventProto> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }
}
