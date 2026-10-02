package ru.practicum.explorewithme.interaction.grpc;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "grpc.client.collector", name = "address")
public class CollectorGrpcClient {

    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${grpc.client.collector.deadline-ms:1000}")
    private long deadlineMs;

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserAction(long userId, long eventId, ActionTypeProto actionType, Instant timestamp) {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(toTimestamp(timestamp))
                .build();

        circuitBreakerFactory.create("collector").run(
                () -> {
                    //noinspection ResultOfMethodCallIgnored
                    client.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).collectUserAction(request);
                    return Boolean.TRUE;
                },
                cause -> {
                    log.warn(
                            "collector недоступен, действие пользователя userId={}, " +
                                    "eventId={}, actionType={} не будет отправлено",
                            userId, eventId, actionType, cause);
                    return Boolean.FALSE;
                }
        );
    }

    private Timestamp toTimestamp(Instant instant) {
        Instant value = instant == null ? Instant.now() : instant;
        return Timestamp.newBuilder()
                .setSeconds(value.getEpochSecond())
                .setNanos(value.getNano())
                .build();
    }
}
