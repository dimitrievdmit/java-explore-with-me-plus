package ru.practicum.explorewithme.stats.aggregator.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.serialization.AvroSerializer;
import ru.practicum.ewm.stats.avro.serialization.UserActionDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConfig {
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.producer.acks: all}")
    private String producerAcks;

    @Value("${kafka.producer.enable-idempotence: true}")
    private String producerEnableIdempotence;

    @Bean(destroyMethod = "close")
    public KafkaConsumer<String, UserActionAvro> userActionConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    @Bean(destroyMethod = "close")
    public KafkaProducer<String, EventSimilarityAvro> similarityProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, producerAcks);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producerEnableIdempotence);
        return new KafkaProducer<>(properties);
    }
}
