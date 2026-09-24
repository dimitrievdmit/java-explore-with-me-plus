package ru.practicum.explorewithme.stats.collector.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.serialization.AvroSerializer;

import java.util.Properties;

@Configuration
public class KafkaProducerConfig {
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${kafka.producer.acks}")
    private String producerAcks;
    @Value("${kafka.producer.enable-idempotence}")
    private String producerEnableIdempotence;

    @Bean(destroyMethod = "close")
    public KafkaProducer<String, org.apache.avro.specific.SpecificRecordBase> kafkaProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, producerAcks);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producerEnableIdempotence);
        return new KafkaProducer<>(properties);
    }
}
