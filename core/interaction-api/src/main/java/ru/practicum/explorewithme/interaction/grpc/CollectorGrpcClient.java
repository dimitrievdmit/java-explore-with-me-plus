package ru.practicum.explorewithme.interaction.grpc;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "grpc.client.collector", name = "address")
public class CollectorGrpcClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserAction(long userId, long eventId, ActionTypeProto actionType, Instant timestamp) {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(toTimestamp(timestamp))
                .build();
        client.collectUserAction(request);
    }

    private Timestamp toTimestamp(Instant instant) {
        Instant value = instant == null ? Instant.now() : instant;
        return Timestamp.newBuilder()
                .setSeconds(value.getEpochSecond())
                .setNanos(value.getNano())
                .build();
    }
}
