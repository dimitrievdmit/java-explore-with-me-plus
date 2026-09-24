package ru.practicum.ewm.stats.avro.serialization;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.IOException;

public class EventSimilarityDeserializer implements Deserializer<EventSimilarityAvro> {
    private final DecoderFactory decoderFactory = DecoderFactory.get();

    @Override
    public EventSimilarityAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
            SpecificDatumReader<EventSimilarityAvro> reader =
                    new SpecificDatumReader<>(EventSimilarityAvro.class);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Не удалось десериализовать EventSimilarityAvro", e);
        }
    }
}
