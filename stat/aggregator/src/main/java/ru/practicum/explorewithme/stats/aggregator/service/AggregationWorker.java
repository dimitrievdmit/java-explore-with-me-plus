package ru.practicum.explorewithme.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationWorker implements SmartLifecycle {
    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final KafkaProducer<String, EventSimilarityAvro> producer;
    private final SimilarityCalculator calculator;

    @Value("${kafka.topics.user-actions}")
    private String inputTopic;

    @Value("${kafka.topics.events-similarity}")
    private String outputTopic;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r ->
            new Thread(r, "aggregator-kafka-consumer"));
    private volatile boolean running;

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::consume);
    }

    private void consume() {
        consumer.subscribe(List.of(inputTopic));
        try {
            while (running) {
                try {
                    processBatch();
                } catch (WakeupException e) {
                    if (running) {
                        throw e;
                    }
                } catch (Exception e) {
                    log.error("Ошибка обработки очередной порции Kafka в Aggregator, продолжение работы", e);
                }
            }
        } catch (WakeupException e) {
            if (running) {
                throw e;
            }
        } catch (Exception e) {
            log.error("Ошибка обработки Kafka в Aggregator", e);
        } finally {
            try {
                consumer.unsubscribe();
            } catch (Exception ignored) {
                // игнорировать ошибки при выключении
            }
        }
    }

    private void processBatch() {
        ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofSeconds(1));
        for (var record : records) {
            UserActionAvro action = record.value();
            if (action == null) {
                continue;
            }
            try {
                for (EventSimilarityAvro similarity : calculator.process(action)) {
                    producer.send(new ProducerRecord<>(outputTopic,
                            similarity.getEventA() + ":" + similarity.getEventB(), similarity));
                }
            } catch (Exception e) {
                log.error("Ошибка обработки Kafka-сообщения с key={}", record.key(), e);
                throw e;
            }
        }
        producer.flush();
        if (!records.isEmpty()) {
            consumer.commitSync();
        }
    }

    @Override
    public void stop() {
        running = false;
        consumer.wakeup();
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
