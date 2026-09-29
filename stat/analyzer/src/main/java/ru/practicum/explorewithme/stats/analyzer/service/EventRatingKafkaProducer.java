package ru.practicum.explorewithme.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.practicum.ewm.stats.avro.EventRatingAvro;
import ru.practicum.explorewithme.stats.analyzer.dto.EventRatingChangedDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventRatingKafkaProducer {
    private final KafkaProducer<String, EventRatingAvro> producer;

    @Value("${kafka.topics.event-ratings}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EventRatingChangedDto event) {
        EventRatingAvro rating = new EventRatingAvro(
                event.eventId(),
                event.rating(),
                event.timestamp());
        producer.send(new ProducerRecord<>(topic, String.valueOf(event.eventId()), rating),
                (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Не удалось отправить рейтинг мероприятия в Kafka, eventId={}",
                                event.eventId(), exception);
                    }
                });
    }
}
