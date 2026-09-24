package ru.practicum.explorewithme.stats.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;
import ru.practicum.explorewithme.stats.collector.service.UserActionKafkaProducer;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionKafkaProducer producer;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            if (request.getUserId() <= 0 || request.getEventId() <= 0 || !request.hasTimestamp()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("user_id, event_id и timestamp обязательны")
                        .asRuntimeException());
                return;
            }
            producer.send(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя", e);
            responseObserver.onError(Status.INTERNAL.withCause(e).asRuntimeException());
        }
    }
}
