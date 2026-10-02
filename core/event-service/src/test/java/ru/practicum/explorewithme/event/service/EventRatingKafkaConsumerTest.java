package ru.practicum.explorewithme.event.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.ewm.stats.avro.EventRatingAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventRatingKafkaConsumerTest {
    private static final String TOPIC = "stats.event-ratings.v1";

    @Mock
    private KafkaConsumer<String, EventRatingAvro> consumer;
    @Mock
    private EventRatingUpdater updater;

    private EventRatingKafkaConsumer ratingConsumer;

    @BeforeEach
    void setUp() {
        ratingConsumer = new EventRatingKafkaConsumer(consumer, updater);
        ReflectionTestUtils.setField(ratingConsumer, "topic", TOPIC);
    }

    @Test
    void start_ShouldProcessRecordsAndStopAfterPoll() {
        EventRatingAvro rating = new EventRatingAvro(10L, 4.5, Instant.now());
        ConsumerRecord<String, EventRatingAvro> record = new ConsumerRecord<>(TOPIC, 0, 0L, "10", rating);
        ConsumerRecords<String, EventRatingAvro> records = new ConsumerRecords<>(
                Map.of(new TopicPartition(TOPIC, 0), List.of(record)));

        doAnswer(invocation -> {
            ReflectionTestUtils.setField(ratingConsumer, "running", false);
            return records;
        }).when(consumer).poll(any(Duration.class));

        try {
            ratingConsumer.start();

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                verify(consumer).subscribe(List.of(TOPIC));
                verify(updater).update(rating);
                verify(consumer).commitSync();
                verify(consumer).unsubscribe();
            });
        } finally {
            ratingConsumer.stop();
        }
    }
}
