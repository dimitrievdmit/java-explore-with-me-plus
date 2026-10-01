package ru.practicum.explorewithme.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventRatingAvro;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(prefix = "kafka.consumer", name = "enabled", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EventRatingKafkaConsumer implements SmartLifecycle {
    private final KafkaConsumer<String, EventRatingAvro> consumer;
    private final EventRatingUpdater updater;

    @Value("${kafka.topics.event-ratings}")
    private String topic;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r ->
            new Thread(r, "event-service-rating-consumer"));
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
        consumer.subscribe(List.of(topic));
        try {
            while (running) {
                try {
                    processBatch();
                } catch (WakeupException e) {
                    if (running) {
                        throw e;
                    }
                } catch (Exception e) {
                    log.error("Ошибка обработки порции Kafka рейтингов в event-service, продолжение работы", e);
                }
            }
        } catch (WakeupException e) {
            if (running) {
                throw e;
            }
        } catch (Exception e) {
            log.error("Ошибка обработки Kafka рейтингов в event-service", e);
        } finally {
            try {
                consumer.unsubscribe();
            } catch (Exception ignored) {
                // игнорировать ошибки при выключении
            }
        }
    }

    private void processBatch() {
        ConsumerRecords<String, EventRatingAvro> records = consumer.poll(Duration.ofSeconds(1));
        for (var record : records) {
            if (record.value() != null) {
                updater.update(record.value());
            }
        }
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
