package ru.practicum.explorewithme.interaction.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.dashboard.*;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
@ConditionalOnProperty(prefix = "grpc.client.analyzer", name = "address")
public class AnalyzerGrpcClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return asStream(client.getRecommendationsForUser(request));
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return asStream(client.getSimilarEvents(request));
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        return asStream(client.getInteractionsCount(request))
                .collect(java.util.stream.Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore,
                        Math::max));
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
