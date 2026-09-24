package ru.practicum.ewm.stats.avro.serialization;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.IOException;

public class UserActionDeserializer implements Deserializer<UserActionAvro> {
    private final DecoderFactory decoderFactory = DecoderFactory.get();

    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
            SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.class);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Не удалось десериализовать UserActionAvro", e);
        }
    }
}
