package ru.practicum.explorewithme.stats.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionKafkaProducer {
    private final KafkaProducer<String, SpecificRecordBase> producer;

    @Value("${kafka.topics.user-actions}")
    private String topic;

    public void send(UserActionProto action) {
        UserActionAvro avro = new UserActionAvro(
                action.getUserId(),
                action.getEventId(),
                toAvroActionType(action.getActionType()),
                toInstant(action)
        );

        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(topic, String.valueOf(action.getUserId()), avro);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Не удалось отправить действие пользователя в Kafka", exception);
            }
        });
    }

    private java.time.Instant toInstant(UserActionProto action) {
        com.google.protobuf.Timestamp timestamp = action.getTimestamp();
        return java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private ActionTypeAvro toAvroActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case ACTION_VIEW, UNRECOGNIZED -> ActionTypeAvro.VIEW;
        };
    }
}
