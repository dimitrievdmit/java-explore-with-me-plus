package ru.practicum.explorewithme.stats.analyzer.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import ru.practicum.ewm.stats.proto.dashboard.*;
import ru.practicum.explorewithme.stats.analyzer.dto.RecommendationDto;
import ru.practicum.explorewithme.stats.analyzer.service.AnalyzerService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RecommendationsGrpcController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final AnalyzerService analyzerService;

    @Value("${recommendations.neighbor-count:5}")
    private int neighborCount;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendationDto> recommendations = analyzerService.getRecommendationsForUser(
                    request.getUserId(),
                    request.getMaxResults(),
                    request.getMaxResults(),
                    neighborCount
            );
            send(recommendations, responseObserver);
        } catch (Exception e) {
            onError(responseObserver, e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            send(analyzerService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults()),
                    responseObserver);
        } catch (Exception e) {
            onError(responseObserver, e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            analyzerService.getInteractionsCount(request.getEventIdList())
                    .forEach((eventId, score) -> responseObserver.onNext(RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(score)
                            .build()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            onError(responseObserver, e);
        }
    }

    private void send(List<RecommendationDto> recommendations,
                      StreamObserver<RecommendedEventProto> responseObserver) {
        recommendations.forEach(recommendation -> responseObserver.onNext(RecommendedEventProto.newBuilder()
                .setEventId(recommendation.eventId())
                .setScore(recommendation.score())
                .build()));
        responseObserver.onCompleted();
    }

    private void onError(StreamObserver<RecommendedEventProto> responseObserver, Exception e) {
        log.error("Ошибка gRPC запроса рекомендаций", e);
        responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
}
