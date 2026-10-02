package ru.practicum.explorewithme.stats.analyzer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.serialization.EventSimilarityDeserializer;
import ru.practicum.ewm.stats.avro.serialization.UserActionDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConfig {
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.actions-group-id}")
    private String actionsGroupId;

    @Value("${kafka.consumer.similarity-group-id}")
    private String similarityGroupId;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Bean(destroyMethod = "close")
    public KafkaConsumer<String, UserActionAvro> actionConsumer() {
        return createConsumer(actionsGroupId, UserActionDeserializer.class);
    }

    @Bean(destroyMethod = "close")
    public KafkaConsumer<String, EventSimilarityAvro> similarityConsumer() {
        return createConsumer(similarityGroupId, EventSimilarityDeserializer.class);
    }

    private <T> KafkaConsumer<String, T> createConsumer(String groupId, Class<?> valueDeserializer) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        return new KafkaConsumer<>(properties);
    }
}
