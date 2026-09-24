package ru.practicum.explorewithme.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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
public class AnalyzerKafkaWorkers implements SmartLifecycle {
    private final KafkaConsumer<String, UserActionAvro> actionConsumer;
    private final KafkaConsumer<String, EventSimilarityAvro> similarityConsumer;
    private final AnalyzerService analyzerService;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    @Value("${kafka.topics.events-similarity}")
    private String eventsSimilarityTopic;

    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "analyzer-kafka-worker");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean running;

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::consumeActions);
        executor.submit(this::consumeSimilarities);
    }

    private void consumeActions() {
        consume(actionConsumer, userActionsTopic, analyzerService::processUserAction);
    }

    private void consumeSimilarities() {
        consume(similarityConsumer, eventsSimilarityTopic, analyzerService::processSimilarity);
    }

    private <T> void consume(KafkaConsumer<String, T> consumer, String topic,
                             java.util.function.Consumer<T> handler) {
        consumer.subscribe(List.of(topic));
        try {
            while (running) {
                ConsumerRecords<String, T> records = consumer.poll(Duration.ofSeconds(1));
                for (var record : records) {
                    if (record.value() != null) {
                        handler.accept(record.value());
                    }
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            if (running) {
                throw e;
            }
        } catch (Exception e) {
            log.error("Ошибка обработки Kafka в Analyzer", e);
        } finally {
            try {
                consumer.unsubscribe();
            } catch (Exception ignored) {
                // игнорировать ошибки при выключении
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        actionConsumer.wakeup();
        similarityConsumer.wakeup();
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
